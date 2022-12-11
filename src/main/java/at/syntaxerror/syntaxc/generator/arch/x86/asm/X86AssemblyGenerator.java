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

import static at.syntaxerror.syntaxc.generator.arch.x86.asm.X86OperandHelper.constant;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.generator.arch.x86.X86Architecture;
import at.syntaxerror.syntaxc.generator.arch.x86.X86FloatTable;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86BitfieldHelper.BitfieldSegment;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86OperandHelper.RegisterFlags;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Instruction;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionSelector;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Size;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86IntegerTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86MemoryTarget;
import at.syntaxerror.syntaxc.generator.asm.AssemblyGenerator;
import at.syntaxerror.syntaxc.generator.asm.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualRegisterTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualStackTarget;
import at.syntaxerror.syntaxc.intermediate.operand.ConditionOperand;
import at.syntaxerror.syntaxc.intermediate.operand.MemberOperand;
import at.syntaxerror.syntaxc.intermediate.operand.Operand;
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
import at.syntaxerror.syntaxc.misc.config.Flags;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.StructType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
@Getter
public class X86AssemblyGenerator extends AssemblyGenerator {

	private final X86FloatTable floatTable;
	private final X86Assembly x86;
	private final X86Architecture architecture;
	
	private Instructions asm;
	private AssemblyInstruction asmHead;
	
	private boolean isFPUControlWordStored;
	
	private VirtualStackTarget fpuCWOld;
	private VirtualStackTarget fpuCWNew;
	
	@Delegate
	private X86OperandHelper operandHelper;
	
	private ConditionFlags conditionFlag;
	
	@SuppressWarnings("preview")
	@Override
	public void generate(Instructions instructions, Intermediate intermediate) {
		asm = instructions;
		asm.setConstructor(X86Instruction::new);
		asmHead = asm.getHead();
		
		operandHelper = new X86OperandHelper(asm, x86, floatTable);
		
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
	}
	
	private void storeFPUControlWord() {
		if(isFPUControlWordStored)
			return;
		
		isFPUControlWordStored = true;
		
		fpuCWOld = new VirtualStackTarget(Type.SHORT);
		fpuCWNew = new VirtualStackTarget(Type.SHORT);
		
		/*
		 * Store the current FPU control word and set bits 10 and 11
		 * (rounding control: round toward zero)
		 */
		
		var insns = List.of(
			new X86Instruction(asm, X86InstructionKinds.FNSTCW, fpuCWOld),
			new X86Instruction(asm, X86InstructionKinds.MOVZX, X86Register.EAX, fpuCWOld),
			new X86Instruction(asm, X86InstructionKinds.OR, X86Register.AH, constant(12)),
			new X86Instruction(asm, X86InstructionKinds.MOV, fpuCWNew, X86Register.AX)
		);
		
		AssemblyInstruction head = asmHead;
		
		if(asmHead == null) {
			
			head = asm.getHead();
			
			if(head == null)
				insns.forEach(asm::add);
			
			else for(X86Instruction insn : insns)
				head.insertBefore(insn);
		}
		
		else for(X86Instruction insn : insns) {
			head.insertAfter(insn);
			head = insn;
		}
	}

	private void overrideFPUState() {
		storeFPUControlWord();
		asm.add(X86InstructionKinds.FLDCW, fpuCWNew);
	}

	private void restoreFPUState() {
		asm.add(X86InstructionKinds.FLDCW, fpuCWOld);
	}
	
	private boolean useFPU(Type type) {
		return (ArchitectureRegistry.getBitSize() == BitSize.B32 && type.isFloating())
			|| (Flags.LONG_DOUBLE.isEnabled() && isLongDouble(type));
	}
	
	private boolean isLongDouble(Type type) {
		return type.toNumber().getNumericType() == NumericValueType.LDOUBLE;
	}
	
	private boolean isDouble(Type type) {
		return type.toNumber().getNumericType() == NumericValueType.DOUBLE;
	}
	
	private boolean isFloat(Type type) {
		return type.toNumber().getNumericType() == NumericValueType.FLOAT;
	}
	
	private void generateAssignStruct(StructType type, AssemblyTarget dst, AssemblyTarget src) {
		int size = type.sizeof();
		
		if(size < 1)
			return;
		
		if(!dst.isMemory() || !src.isMemory())
			Logger.error("Expected structure to be stored in memory");
		
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
		 */
		
		AssemblyTarget destBase = generateOperand(target.getTarget());
		
		BitfieldSegment[] segments = X86BitfieldHelper.determineBitfieldSegments(target.getMember());
		
		for(BitfieldSegment segment : segments) {
			Type memType = segment.memSize().getType();
			
			/* mov/movzx eax, <src>
			 * mov/movzx edx, [<dst>+OFFSET]
			 * and eax, <(1 << BIT_WIDTH) - 1>
			 * and edx, <~(((1 << BIT_WIDTH) - 1) << BIT_OFFSET)>
			 * sal eax, <BIT_OFFSET>
			 * or edx, eax
			 * mov [<dst>+OFFEST], eax/ax/al
			 */
			
			AssemblyTarget dest = mergeMemory(
				memType,
				destBase,
				constant(segment.offset() - segment.memOffset()),
				1
			);
			
			AssemblyTarget valueOld = toRegister(
				dest,
				RegisterFlags.ZERO_EXTEND
			).resized(memType);
			
			AssemblyTarget valueNew = toRegister(
				value,
				RegisterFlags.ZERO_EXTEND,
				segment.isLast()
					? null
					: RegisterFlags.REASSIGN
			).resized(memType);
			
			int mask = segment.bitMask();
			int bitOffset = segment.bitOffset();
			
			asm.add(X86InstructionKinds.AND, valueNew, constant(mask));
			asm.add(X86InstructionKinds.AND, valueOld, constant(~(mask << bitOffset)));
			asm.add(X86InstructionKinds.SAL, valueNew, constant(bitOffset));
			asm.add(X86InstructionKinds.OR, valueOld, valueNew);
			asm.add(X86InstructionKinds.MOV, dest, valueOld);
		}
	}
	
	private void generateAssignBody(AssemblyTarget dst, AssemblyTarget src) {
		Type typeDst = dst.getType();
		
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
			 * For long doubles, use the FPU stack instead:
			 * 
			 * 	fld <src>
			 * 	fstp <dst>
			 */
			
			if(Flags.LONG_DOUBLE.isEnabled() && isLongDouble(typeDst)) {
				asm.add(X86InstructionKinds.FLD, src);
				asm.add(X86InstructionKinds.FSTP, dst);
			}
			else {
				VirtualRegisterTarget tmp = new VirtualRegisterTarget(typeDst);
				
				asm.add(X86InstructionKinds.MOV, tmp, src);
				asm.add(X86InstructionKinds.MOV, dst, tmp);
			}
			
			return;
		}
		
		if(useFPU(typeDst)) {
			/*
			 * long double:
			 * float/double (32-bit):
			 * 
			 * fld <src>
			 * fstp <dst>
			 */
			
			asm.add(X86InstructionKinds.FLD, toMemory(src));
			
			if(dst.isMemory())
				asm.add(X86InstructionKinds.FSTP, dst);
			
			else { // float/double residing in registers (32-bit)
				/*
				 * fstp [esp+off]
				 * mov <dst>, [esp+off]
				 */
				
				VirtualStackTarget memory = new VirtualStackTarget(typeDst);
				
				asm.add(X86InstructionKinds.FSTP, memory);
				asm.add(
					X86InstructionKinds.MOV,
					dst,
					memory
				);
			}
			
			return;
		}
		
		else asm.add( // mov <dst>, <src>
			X86InstructionSelector.select(X86InstructionKinds.MOV, typeDst),
			dst,
			src.resized(dst.getType())
		);
	}
	
	private void generateAssign(AssignIntermediate assign) {
		assign(
			assign.getTarget(),
			generateOperand(assign.getValue())
		);
	}
	
	private void assign(Operand target, AssemblyTarget value) {
		Type type = target.getType();
		
		if(type.isBitfield()) {
			generateAssignBitfield(
				(MemberOperand) target,
				value
			);
			return;
		}
		
		AssemblyTarget destination = generateOperand(target);
		
		if(type.isStructLike())
			generateAssignStruct(
				type.toStructLike(),
				destination,
				value
			);
		
		else generateAssignBody(
			destination,
			value
		);
	}
	
	private void assignDynamicMemory(Operand target, Consumer<AssemblyTarget> assignment) {
		Type type = target.getType();
		
		if(target.isMemory() && !type.isBitfield())
			assignment.accept(generateOperand(target));
		
		else {
			VirtualStackTarget memory = new VirtualStackTarget(type);
			
			assignment.accept(memory);
			
			assign(target, memory);
		}
	}
	
	private void assignDynamicRegister(Operand target, Consumer<AssemblyTarget> assignment) {
		if(!target.isMemory())
			assignment.accept(generateOperand(target));
		
		else {
			VirtualRegisterTarget register = new VirtualRegisterTarget(target.getType());
			
			assignment.accept(register);
			
			assign(target, register);
		}
	}
	
	private void assignDynamic(Operand target, Consumer<AssemblyTarget> assignment) {
		if(target.isMemory())
			assignDynamicMemory(target, assignment);
		else assignDynamicRegister(target, assignment);
	}
	
	private void generateBinaryArithmetic(Operand dst, AssemblyTarget left, AssemblyTarget right, X86InstructionKinds insnBase) {
		Type type = left.getType();
		
		if(useFPU(type)) {
			
			/*
			 * long double:
			 * float/double (32-bit):
			 * 
			 * fld <A>
			 * fld <B>
			 * faddp st(1), st(0)
			 * fstp <R>
			 */
			
			asm.add(X86InstructionKinds.FLD, toMemory(left));
			asm.add(X86InstructionKinds.FLD, toMemory(right));
			asm.add(
				X86InstructionSelector.select(insnBase, Type.LDOUBLE),
				X86Register.ST1,
				X86Register.ST0
			);
			
			assignDynamicMemory(dst, mem -> asm.add(X86InstructionKinds.FSTP, mem));
			
			return;
		}

		X86InstructionKinds insn = X86InstructionSelector.select(insnBase, type);

		if(type.isFloating()) {
			
			/*
			 * float/double:
			 * 
			 * movss/movsd xmm0, <A>
			 * movss/movsd xmm1, <B>
			 * addss/addsd xmm0, xmm1
			 * movss/movsd <R>, xmm0
			 */

			// create new variables to make sure left and right are effectively final (required by the callback below)
			AssemblyTarget leftTarget = toRegister(left);
			AssemblyTarget rightTarget = toRegister(right);
			
			asm.add(
				insn,
				leftTarget,
				rightTarget
			);
			
			assign(dst, leftTarget);
			
			return;
		}
		
		/*
		 * int:
		 * 
		 * mov eax, <A>
		 * add eax, <B>
		 * mov <R>, eax
		 * 
		 * 'compact' specifies the following optimization:
		 * 
		 * A = A +- B
		 * A = B +- A
		 * 
		 * 	mov eax, <B>
		 * 	add <A>, eax
		 */

		assignDynamic(dst, result -> {
			AssemblyTarget leftTarget = left;
			AssemblyTarget rightTarget = right;
			
			boolean compact = false;
			
			if(compact = rightTarget.equals(result)) {
				// swap left and right
				rightTarget = leftTarget;
				leftTarget = result;
			}
			else compact = leftTarget.equals(result);
			
			if(leftTarget.isMemory()) {
				
				if(!compact)
					leftTarget = toRegister(leftTarget);
				
				else if(rightTarget.isMemory())
					rightTarget = toRegister(rightTarget);
				
			}
			
			asm.add(
				insn,
				leftTarget,
				rightTarget
			);
			
			if(!compact)
				generateAssignBody(result, leftTarget);
		});
	}

	private void generateBinaryDivision(Operand dst, AssemblyTarget left, AssemblyTarget right, boolean modulo) {
		Type type = left.getType();
		
		if(type.isFloating()) {
			generateBinaryArithmetic(dst, left, right, X86InstructionKinds.DIV);
			return;
		}
		
		/*
		 * signed integer division:
		 * 
		 * 	mov eax, <A>
		 * 	cwd/cdq/cqo		(clobbers edx, not required for BYTE)
		 * 	idiv <B>
		 * 
		 * unsigned integer division:
		 * 
		 * 	mov eax, <A>
		 * 	xor edx, edx	(not required for BYTE)
		 * 	div <B>
		 * 
		 * division result:
		 * 	
		 * 	mov <R>, rax/eax/ax/al
		 * 
		 * modulo result:
		 * 
		 * 	mov <R>, rdx/edx/dx/ah
		 */

		X86Size size = X86Size.of(type);
		boolean unsigned = type.isUnsigned();
		
		asm.add(
			size == X86Size.BYTE
				? unsigned
					? X86InstructionKinds.MOVZX
					: X86InstructionKinds.MOVSX
				: X86InstructionKinds.MOV,
			X86Register.EAX.resized(type).minimum(Type.INT),
			left
		);
		
		if(size != X86Size.BYTE) {
			if(unsigned)
				asm.add(X86InstructionKinds.XOR, X86Register.EDX, X86Register.EDX);
			
			else {
				X86InstructionKinds signExtend = X86InstructionKinds.CWD;
				
				if(size == X86Size.DWORD)
					signExtend = X86InstructionKinds.CDQ;
				
				else if(size == X86Size.QWORD)
					signExtend = X86InstructionKinds.CQO;
				
				asm.add(signExtend);
				asm.add(X86InstructionKinds.CLOBBER, X86Register.EDX);
			}
		}
		
		asm.add(
			unsigned
				? X86InstructionKinds.DIV
				: X86InstructionKinds.IDIV,
			right
		);
		
		if(size == X86Size.BYTE)
			assign(
				dst,
				modulo
					? X86Register.AH
					: X86Register.AL
			);
		else assign(
			dst,
			(modulo
				? X86Register.EDX
				: X86Register.EAX)
				.resized(dst.getType())
		);
	}

	private void generateBinaryMultiplication(Operand dst, AssemblyTarget left, AssemblyTarget right) {
		Type type = left.getType();
		
		if(type.isFloating()) {
			generateBinaryArithmetic(dst, left, right, X86InstructionKinds.IMUL);
			return;
		}
		
		/*
		 * integer multiplication:
		 * 
		 * 	mov eax, <A>
		 * 	imul eax, <B>
		 * 	mov <R>, eax
		 * 
		 * if <B> is constant:
		 * 
		 * 	imul eax, <A>, <B>
		 * 	mov <R>, eax
		 */
		
		if(right instanceof X86IntegerTarget)
			assignDynamicRegister(dst, result -> asm.add(X86InstructionKinds.IMUL, result.minimum(Type.SHORT), left, right));
		
		else assignDynamicRegister(dst, result -> {
			asm.add(X86InstructionKinds.MOV, result, left);
			asm.add(X86InstructionKinds.IMUL, result.minimum(Type.SHORT), right);
		});
	}

	private void generateBinaryComparison(Operand dst, AssemblyTarget left, AssemblyTarget right,
			ConditionFlags flagSint, ConditionFlags flagUint, ConditionFlags flagFloat,
			boolean swapUint, boolean swapFloat) {
		
		Type type = left.getType();
		ConditionFlags flag;
		
		AssemblyTarget first = left;
		AssemblyTarget second = right;
		
		if(type.isFloating()) {
			flag = flagFloat;
			
			if(swapFloat) {
				first = right;
				second = left;
			}
			
			if(useFPU(type)) {
				
				/* 
				 * long double comparison:
				 * float/double comparison (32-bit):
				 * 
				 * fld <a>
				 * fld <b>
				 * fcomip st, st(1)
				 * fstp st(1)
				 */
				
				asm.add(X86InstructionKinds.FLD, first);
				asm.add(X86InstructionKinds.FLD, second);
				asm.add(X86InstructionKinds.FCOMIP, X86Register.ST0, X86Register.ST1);
				asm.add(X86InstructionKinds.FSTP, X86Register.ST0);
				
			}
			else {
				
				/*
				 * float/double comparison (64-bit):
				 * 
				 * movss/movsd xmm0, <a>
				 * comiss/comisd xmm0, <b>
				 */
				
				asm.add(
					X86InstructionSelector.select(
						X86InstructionKinds.COMISS,
						type
					),
					toRegister(first),
					second
				);
				
			}
			
		}
		
		else {
			/*
			 * integer comparison:
			 * 
			 * mov eax, <a>
			 * cmp eax, <b>
			 */
			
			if(type.isUnsigned()) {
				flag = flagUint;

				if(swapUint) {
					first = right;
					second = left;
				}
			}
			else flag = flagSint;
			
			if(first.isMemory() && second.isMemory())
				first = toRegister(first);
			
			asm.add(X86InstructionKinds.CMP, first, second);
			
		}
		
		if(dst instanceof ConditionOperand) // flag is used by jump instruction
			conditionFlag = flag;
		
		/*
		 * set<cc>, al
		 * movzx <dst>, al
		 */
		else assignDynamicRegister(
			dst,
			reg -> {
				AssemblyTarget reg32 = reg.resized(Type.INT);
				
				asm.add(X86InstructionKinds.XOR, reg32, reg32);
				asm.add(flag.getSetInstruction(), reg.resized(Type.CHAR));
			}
		);
		
	}
	
	private void generateBinaryBitwise(Operand dst, AssemblyTarget leftTarget, AssemblyTarget rightTarget, X86InstructionKinds insn) {
		
		/*
		 * 	mov eax, <A>
		 * 	and/or/xor eax, <B>
		 * 	mov <R>, eax
		 * 
		 * 'compact' specifies the following optimization:
		 * 
		 * A = A &|^ B
		 * A = B &|^ A
		 * 
		 * 	and/or/xor eax, <B>
		 * 	and/or/xor <A>, eax
		 */
		
		assignDynamic(dst, result -> {
			AssemblyTarget left = leftTarget;
			AssemblyTarget right = rightTarget;
			
			boolean compact;
			
			if(compact = right.equals(result)) {
				// swap left and right
				right = left;
				left = result;
			}
			else compact = left.equals(result);
			
			if(left.isMemory()) {
				
				if(!compact)
					left = toRegister(left);
				
				else if(right.isMemory())
					right = toRegister(right);
				
			}
			
			asm.add(insn, left, right);
			
			if(!compact)
				generateAssignBody(result, left);
		});
	}
	
	private void generateBinaryShift(Operand dst, AssemblyTarget leftTarget, AssemblyTarget rightTarget,
			X86InstructionKinds insnSint, X86InstructionKinds insnUint) {

		/*
		 * 	mov eax, <A>
		 * 	mov ecx, <B>
		 * 	sal/shl/sar/shr eax, cl
		 * 	mov <R>, eax
		 * 
		 * 'compact' specifies the following optimization:
		 * 
		 * A = A << B
		 * 
		 * 	mov ecx, <B>
		 * 	sal/shl/sar/shr <A>, cl
		 */
		
		assignDynamic(dst, result -> {
			AssemblyTarget left = leftTarget;
			AssemblyTarget right = rightTarget;
			
			boolean unsigned = left.getType().isUnsigned();
			boolean compact = left.equals(result);
			
			if(left.isMemory() && !compact)
				left = toRegister(left);
			
			generateAssignBody(X86Register.ECX.resized(right.getType()), right);
			
			X86InstructionKinds insn = unsigned
				? insnUint
				: insnSint;
			
			asm.add(insn, left, X86Register.CL);
	
			if(!compact)
				generateAssignBody(result, left);
		});
	}
	
	private void generateBinaryEquality(Operand dst, AssemblyTarget left, AssemblyTarget right, boolean inverted) {
		
		// TODO
		
		/*
		 * long double:
		 * float/double (32-bit):
		 * 
		 * 	xor eax, eax
		 * 	mov edx, 0/1
		 * 	fld <A>
		 * 	fld <B>
		 * 	fucomip st, st(1)
		 * 	fstp st(0)
		 * 	setnp al
		 * 	cmovne eax, edx
		 * 	mov <R>, eax
		 */
		
		/*
		 * float/double:
		 * 
		 * 	xor eax, eax
		 * 	mov edx, 0/1
		 * 	movss/movsd xmm0, <A>
		 * 	ucomiss xmm0, <B>
		 * 	setnp al
		 * 	cmovne eax, edx
		 * 	mov <R>, eax
		 */
		
		/*
		 * int:
		 * 
		 * 	mov eax, <A>
		 * 	cmp eax, <B>
		 * 	sete al
		 * 	movzx eax, al
		 * 	mov <R>, eax
		 */
		
	}
	
	private void generateBinary(BinaryIntermediate binary) {
		Operand dst = binary.getResult();
		AssemblyTarget left = generateOperand(binary.getLeft());
		AssemblyTarget right = generateOperand(binary.getRight());

		switch(binary.getOp()) {
		case ADD: generateBinaryArithmetic(dst, left, right, X86InstructionKinds.ADD); break;
		case SUBTRACT: generateBinaryArithmetic(dst, left, right, X86InstructionKinds.SUB); break;
		
		case MULTIPLY: generateBinaryMultiplication(dst, left, right); break;
		
		case DIVIDE: generateBinaryDivision(dst, left, right, false); break;
		case MODULO: generateBinaryDivision(dst, left, right, true); break;
			
		case BITWISE_AND: generateBinaryBitwise(dst, left, right, X86InstructionKinds.AND); break;
		case BITWISE_OR: generateBinaryBitwise(dst, left, right, X86InstructionKinds.OR); break;
		case BITWISE_XOR: generateBinaryBitwise(dst, left, right, X86InstructionKinds.XOR); break;
		
		case SHIFT_LEFT: generateBinaryShift(dst, left, right, X86InstructionKinds.SAL, X86InstructionKinds.SHL); break;
		case SHIFT_RIGHT: generateBinaryShift(dst, left, right, X86InstructionKinds.SAR, X86InstructionKinds.SHR); break;
		
		case EQUAL: generateBinaryEquality(dst, left, right, false); break;
		case NOT_EQUAL: generateBinaryEquality(dst, left, right, true); break;
				
		case GREATER:
			generateBinaryComparison(
				dst, left, right,
				ConditionFlags.GREATER,
				ConditionFlags.BELOW,
				ConditionFlags.ABOVE,
				true, false
			);
			break;
			
		case GREATER_EQUAL:
			generateBinaryComparison(
				dst, left, right,
				ConditionFlags.GREATER_EQUAL,
				ConditionFlags.NOT_BELOW,
				ConditionFlags.NOT_BELOW,
				false, false
			);
			break;
			
		case LESS:
			generateBinaryComparison(
				dst, left, right,
				ConditionFlags.LESS,
				ConditionFlags.BELOW,
				ConditionFlags.ABOVE,
				false, true
			);
			break;
			
		case LESS_EQUAL:
			generateBinaryComparison(
				dst, left, right,
				ConditionFlags.LESS_EQUAL,
				ConditionFlags.NOT_BELOW,
				ConditionFlags.NOT_BELOW,
				true, true
			);
			break;
			
			
		case LOGICAL_AND:
			// TODO
			break;
			
		case LOGICAL_OR:
			// TODO
			break;
		}
		
	}

	private void generateBuiltin(BuiltinIntermediate builtin) {
		
		
	}

	private void generateCall(CallIntermediate call) {
		
		
		
	}

	private void generateCast(CastIntermediate cast) {
		Operand dst = cast.getResult();
		AssemblyTarget src = generateOperand(cast.getTarget());

		Type typeDst = dst.getType();
		Type typeSrc = src.getType();
		
		X86Size sizeDst = X86Size.of(typeDst);
		X86Size sizeSrc = X86Size.of(typeSrc);
		
		boolean ldoubleSrc =	useFPU(typeSrc);
		boolean doubleSrc =		isDouble(typeSrc);
		boolean floatSrc =		isFloat(typeSrc);
		boolean intSrc =		!typeSrc.isFloating();

		boolean ldoubleDst =	useFPU(typeDst);
		boolean doubleDst =		isDouble(typeDst);
		boolean floatDst =		isFloat(typeDst);
		boolean intDst =		!typeDst.isFloating();
		
		if(!Flags.LONG_DOUBLE.isEnabled()) {
			doubleDst |= ldoubleDst;
			doubleSrc |= ldoubleSrc;
			
			ldoubleDst = ldoubleSrc = false;
		}
		
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
				
				assign(dst, reg);
				return;
			}
			
			if(bytesSrc > bytesDst) {
				assign(dst, src);
				return;
			}
			
			assignDynamicRegister(dst, result -> {
				result = result.resized(
					bytesSrc > 4
						? Type.LONG // e.g. rax
						: Type.INT  // e.g. eax
				);
				
				asm.add(
					bytesSrc < 4
						? X86InstructionKinds.MOVZX // explicit zeroing of upper bits
						: X86InstructionKinds.MOV,  // implicit zeroing of upper bits (when writing to 32-bit registers),
					result,
					src
				);
				
				if(bytesSrc < bytesDst && signedSrc) {
					/*
					 * signed integers: sign extension
					 * 
					 * 	movzx eax, <src>
					 * 	movsx eax, ax/al
					 * 	mov <dst>, rax/eax
					 */
					
					asm.add(
						X86InstructionKinds.MOVSX,
						result,
						result.resized(typeSrc)
					);
				}
			});
			
			return;
		}
		
		if((ldoubleDst && ldoubleSrc) || (doubleDst && doubleSrc) || (floatDst && floatSrc) || (intDst && intSrc)) {
			assign(dst, src);
			return;
		}
		
		if(ldoubleDst) {
			/* double/float to long double:
			 * double/float to double/float (32-bit):
			 * 
			 * 	fld <double/float>
			 * 	fstp <long double>
			 * 
			 * 
			 * int to long double:
			 * int to double/float (32-bit):
			 * 
			 * 	fild <int>
			 * 	fstp <long double>
			 */
			
			X86InstructionKinds insn = intSrc
				? X86InstructionKinds.FILD
				: X86InstructionKinds.FLD;
			
			asm.add(insn, toMemory(src));
			
			assignDynamicMemory(dst, mem -> asm.add(X86InstructionKinds.FSTP, mem));
			
			return;
		}

		if(ldoubleSrc) {
			/* long double to double/float
			 * double/float to double/float (32-bit):
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
			 * double/float to int (32-bit):
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
			
			asm.add(X86InstructionKinds.FLD, src);
			
			if(intDst)
				assignDynamicMemory(
					dst,
					mem -> {
						overrideFPUState();
						asm.add(X86InstructionKinds.FISTP, mem);
						restoreFPUState();
					}
				);
			else assignDynamicMemory(dst, mem -> asm.add(X86InstructionKinds.FSTP, mem));
			
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
			
			X86InstructionKinds insn = floatDst
				? intSrc
					? X86InstructionKinds.CVTSI2SS // (float) int
					: X86InstructionKinds.CVTSD2SS // (float) double
				: intSrc
					? X86InstructionKinds.CVTSI2SD // (double) int
					: X86InstructionKinds.CVTSS2SD;// (double) float
			
			assignDynamicMemory(dst, mem -> asm.add(insn, mem, src));
			
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
		
		X86InstructionKinds insn = floatSrc
			? X86InstructionKinds.CVTTSS2SI
			: X86InstructionKinds.CVTTSD2SI;

		assignDynamicMemory(dst, mem -> asm.add(insn, mem, src));
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
				asm.add(X86InstructionKinds.MOV, result, dst);
			
			break;
		}
		
		case BITWISE_NOT:
			break;
			
		case INDIRECTION:
			break;
			
		case LOGICAL_NOT:
			break;
			
		case MINUS:
			break;
			
		default:
			break;
		}
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
		
		X86Register valueRegister = X86Register.RAX.resized(opSize.getRight());
		
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
	
	@RequiredArgsConstructor
	@Getter
	private static enum ConditionFlags {
		
		ABOVE			("a",	X86InstructionKinds.SETA,	X86InstructionKinds.JA),
		BELOW			("b",	X86InstructionKinds.SETB,	X86InstructionKinds.JB),
		EQUAL			("e",	X86InstructionKinds.SETE,	X86InstructionKinds.JE),
		GREATER			("g",	X86InstructionKinds.SETG,	X86InstructionKinds.JG),
		GREATER_EQUAL	("ge",	X86InstructionKinds.SETGE,	X86InstructionKinds.JGE),
		LESS			("l",	X86InstructionKinds.SETL,	X86InstructionKinds.JL),
		LESS_EQUAL		("le",	X86InstructionKinds.SETLE,	X86InstructionKinds.JLE),
		PARITY			("p",	X86InstructionKinds.SETP,	X86InstructionKinds.JP),
		NOT_BELOW		("nb",	X86InstructionKinds.SETNB,	X86InstructionKinds.JNB),
		NOT_PARITY		("np",	X86InstructionKinds.SETNP,	X86InstructionKinds.JNP);
		
		private final String code;
		private final X86InstructionKinds setInstruction;
		private final X86InstructionKinds jumpInstruction;
		
	}
		
}
