/* MIT License
 * 
 * Copyright (c) 2022 Thomas Kasper
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package at.syntaxerror.syntaxc.generator.arch.x86.asm;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.generator.arch.x86.X86FloatTable;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Instruction;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionSelector;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Size;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86IntegerTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86LabelTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86MemoryTarget;
import at.syntaxerror.syntaxc.generator.asm.AssemblyGenerator;
import at.syntaxerror.syntaxc.generator.asm.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.generator.asm.target.RegisterTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualRegisterTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualStackTarget;
import at.syntaxerror.syntaxc.intermediate.operand.ConditionOperand;
import at.syntaxerror.syntaxc.intermediate.operand.ConstantOperand;
import at.syntaxerror.syntaxc.intermediate.operand.DiscardOperand;
import at.syntaxerror.syntaxc.intermediate.operand.GlobalOperand;
import at.syntaxerror.syntaxc.intermediate.operand.IndexOperand;
import at.syntaxerror.syntaxc.intermediate.operand.LocalOperand;
import at.syntaxerror.syntaxc.intermediate.operand.MemberOperand;
import at.syntaxerror.syntaxc.intermediate.operand.Operand;
import at.syntaxerror.syntaxc.intermediate.operand.TemporaryOperand;
import at.syntaxerror.syntaxc.intermediate.representation.AssignIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BinaryIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BuiltinIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.CallIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.CastIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate;
import at.syntaxerror.syntaxc.intermediate.representation.JumpIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.LabelIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.MemcpyIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.MemsetIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.UnaryIntermediate;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.StructType;
import at.syntaxerror.syntaxc.type.StructType.Member;
import at.syntaxerror.syntaxc.type.Type;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class X86AssemblyGenerator extends AssemblyGenerator {

	private final X86FloatTable floatTable;
	private final X86Assembly x86;
	
	private Instructions asm;
	
	private boolean isFPUControlWordStored;
	
	private VirtualStackTarget fpuCWOld;
	private VirtualStackTarget fpuCWNew;
	
	@SuppressWarnings("preview")
	@Override
	public List<AssemblyInstruction> generate(Intermediate intermediate) {
		System.out.println(intermediate.getClass().getSimpleName());
		
		asm = new Instructions(new ArrayList<>());
		isFPUControlWordStored = false;
		
		switch(intermediate) {
		case AssignIntermediate assign:		generateAssign	(assign);	break;
		case BinaryIntermediate binary:		generateBinary	(binary);	break;
		case BuiltinIntermediate builtin:	generateBuiltin	(builtin);	break;
		case CallIntermediate call:			generateCall	(call);		break;
		case CastIntermediate cast:			generateCast	(cast);		break;
		case JumpIntermediate jump:			generateJump	(jump);		break;
		case LabelIntermediate label:		generateLabel	(label);	break;
		case UnaryIntermediate unary:		generateUnary	(unary);	break;
		case MemcpyIntermediate memcpy:		generateMemcpy	(memcpy);	break;
		case MemsetIntermediate memset:		generateMemset	(memset);	break;
		default:
			Logger.error("Illegal intermediate: %s", intermediate.getClass());
			break;
		}
		
		return asm.assembly();
	}
	
	private void storeFPUControlWord() {
		if(isFPUControlWordStored)
			return;
		
		isFPUControlWordStored = true;
		
		fpuCWOld = new VirtualStackTarget(Type.SHORT);
		fpuCWNew = new VirtualStackTarget(Type.SHORT);
		
		var asm = this.asm.assembly();
		
		/*
		 * Store the current FPU control word and set bits 10 and 11
		 * (rounding control: round toward zero)
		 */
		
		asm.addAll(0, List.of(
			new X86Instruction(X86InstructionKinds.FNSTCW, fpuCWOld),
			new X86Instruction(X86InstructionKinds.MOVZX, X86Register.EAX, fpuCWOld),
			new X86Instruction(X86InstructionKinds.OR, X86Register.AH, constant(12)),
			new X86Instruction(X86InstructionKinds.MOV, fpuCWNew, X86Register.AX)
		));
	}

	private void overrideFPUState() {
		storeFPUControlWord();
		asm.add(X86InstructionKinds.FLDCW, fpuCWNew);
	}

	private void restoreFPUState() {
		asm.add(X86InstructionKinds.FLDCW, fpuCWOld);
	}
	
	private AssemblyTarget constant(Type type, Number value) {
		return new X86IntegerTarget(
			type,
			value instanceof BigInteger bi
				? bi
				: BigInteger.valueOf(value.longValue())
		);
	}
	
	private AssemblyTarget constant(Number value) {
		return constant(Type.LONG, value);
	}
	
	private AssemblyTarget label(String name) {
		return new X86LabelTarget(Type.VOID, name);
	}
	
	private Pair<AssemblyTarget, Long> mergeOffsets(BigInteger a, long scaleA, BigInteger b, long scaleB) {
		if(scaleA == 0)
			return Pair.of(constant(b), scaleB);
		
		if(scaleB == 0)
			return Pair.of(constant(a), scaleA);
		
		long newScale = Math.min(scaleA, scaleB);
		
		return Pair.of(
			constant(
				a.multiply(BigInteger.valueOf(scaleA / newScale))
					.add(b.multiply(BigInteger.valueOf(scaleB / newScale)))
			),
			newScale
		);
	}
	
	@SuppressWarnings("preview")
	private AssemblyTarget generateOperand(Operand operand) {
		switch(operand) {
		case TemporaryOperand temp:
			return new VirtualRegisterTarget(temp.getType(), temp.getId());
			
		case ConditionOperand cond:
			return null; // TODO
			
		case ConstantOperand cnst:
			if(cnst.isFloating())
				return X86MemoryTarget.ofDisplaced(
					cnst.getType(),
					floatTable.get(
						cnst.getType(),
						(BigDecimal) cnst.getValue()
					),
					x86.RIP
				);
			
			return new X86IntegerTarget(
				cnst.getType(),
				(BigInteger) cnst.getValue()
			);
			
		case DiscardOperand discard:
			return null;
			
		case GlobalOperand global: // name[rip]
			return X86MemoryTarget.ofDisplaced(
				global.getType(),
				label(global.getName()),
				x86.RIP
			);
			
		case IndexOperand index: { // [target+index*scale]
			AssemblyTarget displacement = null;
			AssemblyTarget base = generateOperand(index.getTarget());
			AssemblyTarget offset = generateOperand(index.getOffset());
			long idxScale = index.getScale();
			
			System.out.println(base + " + " + offset + " * " + idxScale);
			
			if(idxScale != 0 && idxScale != 1 && idxScale != 2 && idxScale != 4 && idxScale != 8) {
				
				if(offset instanceof X86IntegerTarget idxOff) {
					offset = constant(
						idxOff.getValue()
							.multiply(BigInteger.valueOf(idxScale))
					);
					idxScale = 1;
				}
				else {
					offset = toRegister(offset);
					
					asm.add(
						X86InstructionSelector.select(X86InstructionKinds.IMUL, offset.getType()),
						offset,
						constant(idxScale)
					);
				}
				
			}
			
			boolean toRegister = true;
			
			if(base instanceof X86MemoryTarget mem) {

				displacement = mem.getDisplacement();
				
				if(mem.hasIndex()) {
					
					AssemblyTarget memIdx = mem.getIndex();
					
					if(offset instanceof X86IntegerTarget idxOff &&
						memIdx instanceof X86IntegerTarget memOff) {
						
						var merged = mergeOffsets(
							idxOff.getValue(),
							idxScale,
							memOff.getValue(),
							mem.getScale()
						);
						
						offset = merged.getLeft();
						idxScale = merged.getRight();

						base = mem.getBase();
						toRegister = false;
					}
					
				}
				else {
					base = mem.getBase();
					toRegister = false;
				}
				
			}
			
			if(toRegister) {
				offset = toRegister(offset);
				base = toRegister(base);
				
				displacement = null;
			}
			
			return X86MemoryTarget.ofDisplaced(
				index.getType(),
				displacement,
				base,
				offset,
				idxScale
			);
		}
			
		case LocalOperand local:
			return X86MemoryTarget.ofDisplaced( // -offset[rbp]
				local.getType(),
				new X86IntegerTarget(
					NumericValueType.PTRDIFF.asType(),
					BigInteger.valueOf(-local.getOffset())
				),
				x86.RBP
			);
			
		case MemberOperand member:
			return null; // TODO
			
		default:
			Logger.error("Unknown operand type: %s", operand.getClass());
			return null;
		}
	}
	
	private AssemblyTarget toRegister(AssemblyTarget target) {
		if(target == null || !target.isMemory())
			return target;
		
		Type type = target.getType();
		
		VirtualRegisterTarget virt = new VirtualRegisterTarget(type);
		
		asm.add(
			X86InstructionSelector.select(X86InstructionKinds.MOV, type),
			virt,
			target
		);
		
		return virt;
	}
	
	private AssemblyTarget toMemory(AssemblyTarget target) {
		if(target == null || target.isMemory())
			return target;
		
		Type type = target.getType();
		
		VirtualStackTarget virt = new VirtualStackTarget(type);
		
		asm.add(
			X86InstructionSelector.select(X86InstructionKinds.MOV, type),
			virt,
			target
		);
		
		return virt;
	}
	
	private void generateAssignStruct(AssignIntermediate assign, StructType type) {
		int size = type.sizeof();
		
		if(size < 1)
			return;
		
		AssemblyTarget dst = generateOperand(assign.getTarget());
		AssemblyTarget src = generateOperand(assign.getValue());
		
		if(!dst.isMemory() || !src.isMemory())
			Logger.error(assign, "Expected structure to be stored in memory");
		
		/* use memcpy for structs larger than 64 bytes */
		if(size > 64) {
			
			/*
			 * copy n bytes/words/dwords/qwords from d into s
			 * 
			 * mov rdi, d
			 * mov rsi, s
			 * mov rcx, n
			 * rep movsb/movsw/movsd/movsq BYTE/WORD/DWORD/QWORD PTR es:[rdi], BYTE/WORD/DWORD/QWORD PTR ds:[rdi]
			 */
			
			asm.add(X86InstructionKinds.LEA, x86.RDI, dst);
			asm.add(X86InstructionKinds.LEA, x86.RSI, src);
			
			generateMemcpyTail(size);
			
			return;
		}
		
		/*
		 * split into possibly multiple assignments
		 */
		
		final int blockSizes[] = { 8, 4, 2, 1 };
		
		List<Integer> blocks = new ArrayList<>();
		
		while(size > 0)
			for(int block : blockSizes)
				if(size >= block) {
					size -= block;
					blocks.add(block);
					break;
				}
		
		/*
		 * mov rax, <src>
		 * mov rbx, <dst>
		 * 
		 * mov rcx, QWORD PTR [rax]
		 * mov QWORD PTR [rbx], rcx
		 * 
		 * mov cx, WORD PTR 8[rax]
		 * mov WORD PTR 8[rbx], cx
		 * 
		 * etc.
		 */
		
		VirtualRegisterTarget tmp = new VirtualRegisterTarget(Type.LONG);
		
		AssemblyTarget source = new VirtualRegisterTarget(NumericValueType.POINTER.asType());
		AssemblyTarget destination = new VirtualRegisterTarget(NumericValueType.POINTER.asType());
		
		asm.add(X86InstructionKinds.LEA, source, src);
		asm.add(X86InstructionKinds.LEA, destination, dst);
		
		int offset = 0;
		
		for(int block : blocks) {
			Type blockType = X86Size.of(block).getType();
			
			asm.add(
				X86InstructionKinds.MOV,
				tmp,
				X86MemoryTarget.ofDisplaced(
					blockType,
					constant(offset),
					source
				)
			);
			asm.add(
				X86InstructionKinds.MOV,
				X86MemoryTarget.ofDisplaced(
					blockType,
					constant(offset),
					destination
				),
				tmp
			);
			
			offset += block;
		}
		
		
	}
	
	private void generateAssignBitfield(MemberOperand target, AssemblyTarget value) {
		/*
		 * assigning a value to a bitfield member:
		 * 
		 * Since bitfields can only be (unsigned) ints, a bitfield
		 * can at most span across 5 bytes.
		 * 
		 * BYTE_OFFSET is the number of bytes the bitfield
		 * member is offset from the beginning of the struct.
		 * BIT_OFFSET is the number of bits the bitfield
		 * member is offset from its BYTE_OFFSET. BIT_OFFSET
		 * is always 0 unless -fpacked is set.
		 * BIT_WIDTH is the actual size of the bitfield member,
		 * specified in its declaration after the colon ':'
		 * 
		 * e.g.:
		 * 
		 *  struct s {
		 *  	char c[1234];
		 *  	int:3;
		 *  	int bitfield:15
		 *  };
		 *  
		 *  BYTE_OFFSET = 1234, BIT_OFFSET = 3, BIT_WIDTH = 15
		 * 
		 * 
		 * assigning to a signed bitfield:
		 * 
		 * 	
		 * 
		 * assigning to an unsigned bitfield (up to 4 bytes):
		 * 
		 * 	mov/movzx eax, <src>
		 * 	mov/movzx ebx, <dst>
		 * 	and eax, <(1 << BIT_WIDTH) - 1>
		 * 	and edx, <((1 << BIT_WIDTH) - 1) << BIT_OFFSET>
		 * 	sal eax, <BIT_OFFSET>
		 * 	or edx, edx
		 * 	mov <dst>, eax/ax/al
		 * 
		 * assigning to an unsigned bitfield (5 bytes)
		 * 
		 * 	
		 * 
		 */
		
		Member member = target.getMember();
		
		int offset = member.getOffset();
		int bitOffset = member.getBitOffset();
		int bitWidth = member.getBitWidth();
		
		// TODO
	}
	
	private void generateAssignBody(AssemblyTarget dst, AssemblyTarget src) {
		if(dst.isMemory() && src.isMemory()) {
			/*
			 * When moving values across memory, there is no need to
			 * use any type-specific instructions.
			 * 
			 * Also x86 supports at most one memory operand per instruction,
			 * therefore we need to split the assignment into two parts:
			 * 
			 * 	mov rax, <src>
			 * 	mov <dst>, rax
			 * 
			 * For long doubles, the XMM registers are used since they are
			 * the only registers capable of holding 80-bit values
			 * (alongside the FPU stack)
			 * 
			 * 	mov xmm0, <src>
			 * 	mov <dst>, xmm0
			 */
			
			List<? extends RegisterTarget> hints;
			X86InstructionKinds insn;
			
			if(dst.getType() == Type.LDOUBLE) {
				hints = X86Register.GROUP_XMM;
				insn = X86InstructionKinds.MOVSS;
			}
			else {
				hints = List.of();
				insn = X86InstructionKinds.MOV;
			}
			
			VirtualRegisterTarget tmp = new VirtualRegisterTarget(dst.getType(), hints);
			
			asm.add(insn, tmp, src);
			asm.add(insn, dst, tmp);
		}
		
		X86InstructionKinds insn = X86InstructionSelector.select(X86InstructionKinds.MOV, dst.getType());
		
		if(insn == X86InstructionKinds.FSTP) {
			/*
			 * long double:
			 * 
			 * fld <src>
			 * fstp <dst>
			 * 
			 * src/dst: either ST(i) or TBYTE memory operand
			 */
			
			asm.add(X86InstructionKinds.FLD, src);
			asm.add(X86InstructionKinds.FSTP, dst);
			
			return;
		}
		
		else asm.add( // mov <dst>, <src>
			insn,
			dst,
			src
		);
	}
	
	private void generateAssign(AssignIntermediate assign) {
		Type type = assign.getTarget().getType();
		
		if(type.isStructLike()) {
			generateAssignStruct(assign, type.toStructLike());
			return;
		}
		
		AssemblyTarget dst = generateOperand(assign.getTarget());
		AssemblyTarget src = generateOperand(assign.getValue());

		generateAssignBody(dst, src);
	}

	private void generateBinary(BinaryIntermediate binary) {
		
		
	}

	private void generateBuiltin(BuiltinIntermediate builtin) {
		
		
	}

	private void generateCall(CallIntermediate call) {
		
		
		
	}

	private void generateCast(CastIntermediate cast) {
		AssemblyTarget dst = generateOperand(cast.getResult());
		AssemblyTarget src = generateOperand(cast.getTarget());

		Type typeDst = dst.getType();
		Type typeSrc = src.getType();
		
		X86Size sizeDst = X86Size.of(typeDst);
		X86Size sizeSrc = X86Size.of(typeSrc);
		
		boolean ldoubleSrc =	typeSrc == Type.LDOUBLE;
		boolean doubleSrc =		typeSrc == Type.DOUBLE;
		boolean floatSrc =		typeSrc == Type.FLOAT;
		boolean intSrc =		!typeSrc.isFloating();

		boolean ldoubleDst =	typeDst == Type.LDOUBLE;
		boolean doubleDst =		typeDst == Type.DOUBLE;
		boolean floatDst =		typeDst == Type.FLOAT;
		boolean intDst =		!typeDst.isFloating();
		
		if(intDst && intSrc && sizeDst != sizeSrc) {
			// cast from int to int
			
			boolean signedSrc = typeSrc.isSigned();
			
			int bytesSrc = typeSrc.sizeof();
			int bytesDst = typeDst.sizeof();
			
			if(signedSrc && bytesSrc * 2 == bytesDst) {
				/*
				 * use special sign extension instructions:
				 * 
				 *  cbw:	AL->AX		(char->short)
				 *  cwde:	AX->EAX		(short->int)
				 *  cdqe:	EAX->RAX	(int->long)
				 *  
				 *  
				 *  mov/movzx eax, <src>
				 *  cbw/cwde/cdqe
				 *  mov <dst>, ax/eax/rax
				 */
				
				X86InstructionKinds mov;
				X86InstructionKinds insn;
				X86Register reg;
				
				if(bytesSrc == 1) {
					mov = X86InstructionKinds.MOVZX;
					insn = X86InstructionKinds.CBW;
					reg = X86Register.AX;
				}
				else if(bytesSrc == 2) {
					mov = X86InstructionKinds.MOVZX;
					insn = X86InstructionKinds.CWDE;
					reg = X86Register.EAX;
				}
				else {
					mov = X86InstructionKinds.MOV;
					insn = X86InstructionKinds.CDQE;
					reg = X86Register.RAX;
				}

				asm.add(mov, X86Register.EAX, src);
				asm.add(insn);
				asm.add(X86InstructionKinds.MOV, dst, reg);
				return;
			}
			
			VirtualRegisterTarget virt = new VirtualRegisterTarget(
				bytesSrc > 4
					? Type.LONG // e.g. rax
					: Type.INT  // e.g. eax
			);
			
			asm.add(
				bytesSrc < 4
					? X86InstructionKinds.MOVZX // explicit zeroing of upper bits
					: X86InstructionKinds.MOV,  // implicit zeroing of upper bits (when writing to 32-bit registers)
				virt,
				src
			);
			
			if(bytesSrc < bytesDst) {
			
				if(signedSrc) {
					/*
					 * signed integers: sign extension
					 * 
					 * 	movzx eax, <src>
					 * 	movsx eax, ax/al
					 * 	mov <dst>, rax/eax
					 */
					
					asm.add(
						X86InstructionKinds.MOVSX,
						virt,
						virt.resized(sizeSrc)
					);
				}
				else if(!(bytesSrc == 4 && bytesDst == 8)) {
					/*
					 * unsigned integers: zero extension
					 * 
					 * 	movzx/mov rax/eax, <src>
					 * 	movzx eax, ax/al
					 * 	mov <dst>, eax/ax/al
					 * 
					 * this is not performed when casting from
					 * int to long; the zero extension implicitly
					 * occurs in this case
					 */
					
					asm.add(
						X86InstructionKinds.MOVZX,
						virt,
						virt.resized(sizeSrc)
					);
				}
			
			}

			asm.add(X86InstructionKinds.MOV, dst, virt.resized(sizeDst));
			
			return;
		}
		
		if((ldoubleDst && ldoubleSrc) || (doubleDst && doubleSrc) || (floatDst && floatSrc) || (intDst && intSrc)) {
			generateAssignBody(dst, src);
			return;
		}
		
		if(ldoubleDst) {
			/* double/float to long double:
			 * 
			 * 	fld <double/float>
			 * 	fstp <long double>
			 * 
			 * 
			 * int to long double:
			 * 
			 * 	fild <float>
			 * 	fstp <long double>
			 */
			
			X86InstructionKinds insn = intSrc
				? X86InstructionKinds.FILD
				: X86InstructionKinds.FLD;
			
			asm.add(insn, toMemory(src));
			asm.add(X86InstructionKinds.FSTP, dst);
			
			return;
		}

		if(ldoubleSrc) {
			/* long double to double/float
			 * 
			 * 	fld <long double>
			 * 	fstp <double/float>
			 * 
			 * 
			 * if the destination is an XMM register:
			 * 
			 * 	fld <long double>
			 * 	fstp offset[rbp]
			 * 	movsd <double/float>, offset[rbp]
			 * 
			 * 
			 * long double to int:
			 * 
			 * 	fld <long double>
			 * 	fldcw fpu_round_toward_zero[rbp]
			 * 	fistp <int>
			 * 	fldcw fpu_previous[rbp]
			 * 
			 * 
			 * if the destination is a register:
			 * 
			 * 	fld <long double>
			 * 	fldcw fpu_round_toward_zero[rbp]
			 * 	fistp offset[rbp]
			 * 	fldcw fpu_previous[rbp]
			 * 	mov <int>, offset[rbp]
			 */
			
			AssemblyTarget destination;
			
			boolean outsource = !dst.isMemory();
			
			if(outsource)
				destination = toMemory(dst);
			else destination = dst;
			
			asm.add(X86InstructionKinds.FLD, src);
			
			if(intDst) {
				overrideFPUState();
				asm.add(X86InstructionKinds.FISTP, destination);
				restoreFPUState();
			}
			else asm.add(X86InstructionKinds.FSTP, destination);

			if(outsource)
				asm.add(
					X86InstructionSelector.select(X86InstructionKinds.MOV, typeDst),
					dst,
					destination
				);
			
			return;
		}
		
		if(doubleDst || floatDst) {
			/* int to double:
			 * 
			 * 	cvtsi2sd <double>, <int>
			 * 
			 * 
			 * float to double:
			 * 
			 * 	cvtss2sd <double>, <float>
			 *
			 *
			 * int to float:
			 * 
			 * 	cvtsi2ss <float>, <int>
			 * 
			 * 
			 * double to float:
			 * 
			 * 	cvtsd2ss <float>, <double>
			 * 
			 * 
			 * if the destination is not an XMM register:
			 * 
			 * 	cvtsi2ss/cvtsd2ss/cvtsi2sd/cvtss2sd xmm0, <int/float/double>
			 * 	movss/movsd <float/double>, xmm0
			 */
			
			AssemblyTarget destination;
			
			boolean outsource = dst.isMemory();
			
			if(outsource)
				destination = toRegister(dst);
			else destination = dst;
			
			X86InstructionKinds insn = floatDst
				? intSrc
					? X86InstructionKinds.CVTSI2SS // (float) int
					: X86InstructionKinds.CVTSD2SS // (float) double
				: intSrc
					? X86InstructionKinds.CVTSI2SD // (double) int
					: X86InstructionKinds.CVTSS2SD;// (double) float

			asm.add(insn, destination, src);
			
			if(outsource)
				asm.add(X86InstructionKinds.MOVSD, dst, destination);
			
			return;
		}
		
		/* double to int:
		 * 
		 *  cvttsd2si <int>, <double>
		 *  
		 *  
		 * float to int:
		 * 
		 * 	cvttss2si <int>, <float>
		 *
		 * 
		 * if the destination is not a register:
		 * 
		 * 	cvttss2si/cvttsd2si rax, <float/double>
		 * 	mov <int>, rax
		 */
		
		AssemblyTarget destination;
		
		boolean outsource = dst.isMemory();
		
		if(outsource)
			destination = toRegister(dst);
		else destination = dst;
		
		asm.add(
			floatSrc
				? X86InstructionKinds.CVTTSS2SI
				: X86InstructionKinds.CVTTSD2SI,
			destination,
			src
		);

		if(outsource)
			asm.add(X86InstructionKinds.MOV, dst, destination);
	}

	private void generateJump(JumpIntermediate jump) {
		
		
	}

	private void generateLabel(LabelIntermediate label) {
		
		
	}

	private void generateUnary(UnaryIntermediate unary) {
		AssemblyTarget result = generateOperand(unary.getResult());
		AssemblyTarget target = generateOperand(unary.getTarget());
		
		switch(unary.getOp()) {
		case ADDRESS_OF: {
			
			/*
			 * get address of symbol:
			 * 
			 * 	lea <dst>, <src>
			 * 
			 * 
			 * if the destination is not a register:
			 * 
			 * 	lea rax, <src>
			 * 	lea <dst>, rax
			 */
			
			AssemblyTarget dst;

			boolean outsource = result.isMemory();
			
			if(outsource)
				dst = toRegister(result);
			else dst = result;
			
			asm.add(X86InstructionKinds.LEA, dst, target);
			
			if(outsource)
				asm.add(
					X86InstructionKinds.MOV, result, dst);
			
			break;
		}
		}
		
		// TODO
	}
	
	private void generateMemcpyTail(int size) {
		var opSize = calculateMemoryOperationSize(size);

		Type type = opSize.getRight().getType();
		
		asm.add( // mov rcx, n
			X86InstructionKinds.MOV,
			x86.RCX,
			constant(opSize.getLeft())
		);
		
		asm.add( // rep movsq QWORD PTR es:[rdi], QWORD PTR ds:[rsi]
			X86InstructionKinds.REP_MOVS,
			X86MemoryTarget.ofSegmented(
				type,
				X86Register.ES,
				X86Register.RDI
			),
			X86MemoryTarget.ofSegmented(
				type,
				X86Register.DS,
				X86Register.RSI
			)
		);
	}

	private void generateMemcpy(MemcpyIntermediate memcpy) {
		if(memcpy.getLength() < 1)
			return;
		
		// TODO don't use memcpy for small segments
		
		AssemblyTarget dst = generateOperand(memcpy.getDestination());
		AssemblyTarget src = generateOperand(memcpy.getSource());
		
		dst = addOffset(dst, memcpy.getDestinationOffset());
		src = addOffset(src, memcpy.getSourceOffset());
		
		asm.add(X86InstructionKinds.LEA, x86.RDI, dst);
		asm.add(X86InstructionKinds.LEA, x86.RSI, src);
		
		generateMemcpyTail(memcpy.getLength());
	}
	
	private void generateMemsetTail(int size, int value) {
		var opSize = calculateMemoryOperationSize(size);
		
		X86Register valueRegister = X86Register.RAX.getFor(opSize.getRight());
		
		asm.add( // mov rcx, n
			X86InstructionKinds.MOV,
			x86.RCX,
			constant(x86.RCX.getType(), opSize.getLeft())
		);
		asm.add( // mov rax, v
			X86InstructionKinds.MOV,
			valueRegister,
			constant(valueRegister.getType(), value)
		);
		asm.add( // rep stosq QWORD PTR es:[rdi], rax
			X86InstructionKinds.REP_STOS,
			X86MemoryTarget.ofSegmented(
				opSize.getRight().getType(),
				X86Register.ES,
				X86Register.RDI
			),
			valueRegister
		);
	}

	private void generateMemset(MemsetIntermediate memset) {
		if(memset.getLength() < 1)
			return;

		// TODO don't use memset for small segments
		
		AssemblyTarget dst = generateOperand(memset.getTarget());
		
		dst = addOffset(dst, memset.getOffset());
		
		asm.add(X86InstructionKinds.LEA, x86.RDI, dst);
		
		generateMemsetTail(memset.getLength(), memset.getValue());
	}
	
	private AssemblyTarget addOffset(AssemblyTarget target, int offset) {
		if(offset == 0 || !(target instanceof X86MemoryTarget mem))
			return target;
		
		BigInteger index;
		long scale = 1;
		
		if(mem.hasIndex()) {
			scale = mem.getScale();
			
			AssemblyTarget idx = mem.getIndex();
			
			if(idx instanceof X86IntegerTarget integer)
				index = integer.getValue();
			
			else index = null;
		}
		else index = BigInteger.ZERO;
		
		if(index != null) {
			/*
			 * modify existing target by adding the offset to
			 * the index and adjusting the scale, if necessary
			 */
			
			index = index.multiply(BigInteger.valueOf(scale))
				.add(BigInteger.valueOf(offset));
			
			if(isMaskZero(index, 7)) scale = 8;
			else if(isMaskZero(index, 3)) scale = 4;
			else if(isMaskZero(index, 1)) scale = 2;
			else scale = 1;
			
			return X86MemoryTarget.ofSegmentedDisplaced(
				mem.getType(),
				mem.getSegment(),
				mem.getDisplacement(),
				mem.getBase(),
				constant(index),
				scale
			);
		}
		
		/*
		 * add offset at runtime
		 * 
		 * 	lea rax, <target>
		 * 	add rax, offset
		 * 	<new_target> = [rax]
		 */
		
		VirtualRegisterTarget virt = new VirtualRegisterTarget(NumericValueType.POINTER.asType());
		
		asm.add(X86InstructionKinds.LEA, virt, target);
		asm.add(X86InstructionKinds.ADD, virt, constant(offset));
		
		return X86MemoryTarget.of(target.getType(), virt);
	}
	
	private static boolean isMaskZero(BigInteger bigint, int mask) {
		return bigint.and(BigInteger.valueOf(mask))
			.compareTo(BigInteger.ZERO) == 0;
	}
	
	private Pair<Integer, X86Size> calculateMemoryOperationSize(int size) {
		/* default state: process 1 byte at once */
		
		int byteCount = size;
		X86Size wordSize = X86Size.BYTE;
		
		if((byteCount & 7) == 0) {
			/* divisible by 8: process 8 bytes at once */

			byteCount >>= 3;
			wordSize = X86Size.QWORD;
		}
		else if((byteCount & 3) == 0) {
			/* divisible by 4: process 4 bytes at once */
			
			byteCount >>= 2;
			wordSize = X86Size.DWORD;
		}
		else if((byteCount & 1) == 0) {
			/* divisible by 2: process 2 bytes at once */
			
			byteCount >>= 1;
			wordSize = X86Size.WORD;
		}
		
		return Pair.of(byteCount, wordSize);
	}
	
	private static record Instructions(List<AssemblyInstruction> assembly) {
		
		public Instructions add(AssemblyInstruction instruction) {
			assembly.add(instruction);
			return this;
		}
		
		public Instructions add(X86InstructionKinds kind, AssemblyTarget destination, AssemblyTarget...sources) {
			return add(new X86Instruction(kind, destination, sources));
		}
		
		public Instructions add(X86InstructionKinds kind) {
			return add(new X86Instruction(kind, null));
		}
		
	}
	
}
