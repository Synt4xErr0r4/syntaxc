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
import java.util.HashMap;
import java.util.Map;

import at.syntaxerror.syntaxc.generator.arch.x86.X86FloatTable;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86BitfieldHelper.BitfieldSegment;
import at.syntaxerror.syntaxc.generator.arch.x86.call.X86CallingConvention;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionSelector;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Size;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86IntegerTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86LabelTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86MemoryTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86StringTarget;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
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
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.StructType.Member;
import at.syntaxerror.syntaxc.type.Type;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class X86OperandHelper {
	
	public static AssemblyTarget constant(Type type, Number value) {
		return new X86IntegerTarget(
			type,
			value instanceof BigInteger bi
				? bi
				: BigInteger.valueOf(value.longValue())
		);
	}
	
	public static AssemblyTarget constant(Number value) {
		return constant(Type.LONG, value);
	}
	
	public static AssemblyTarget label(String name) {
		return new X86LabelTarget(Type.VOID, name);
	}
	
	private static Pair<AssemblyTarget, Long> mergeOffsets(BigInteger a, long scaleA, BigInteger b, long scaleB) {
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

	private final Instructions asm;
	private final X86Assembly x86;
	private final X86FloatTable floatTable;
	private final X86FPUHelper fpu;
	private final X86CallingConvention callingConvention;
	
	private final Map<SymbolObject, AssemblyTarget> localVariables = new HashMap<>();
	
	public void setLocalVariable(SymbolObject object, AssemblyTarget target) {
		localVariables.put(object, target);
	}
	
	public X86MemoryTarget mergeMemory(Type type, AssemblyTarget base, AssemblyTarget index, long scale) {
		
		AssemblyTarget baseOrig = base;
		
		AssemblyTarget displacement = null;
		boolean toRegister = false;
		
		if(base instanceof X86MemoryTarget mem) {
			
			displacement = mem.getDisplacement();
			base = mem.getBase();
			
			if(mem.hasIndex()) {
				
				AssemblyTarget memIdx = mem.getIndex();
				
				boolean idxInt = index instanceof X86IntegerTarget;
				boolean memInt = memIdx instanceof X86IntegerTarget;
				
				if(idxInt && memInt) {
					X86IntegerTarget idxOff = (X86IntegerTarget) index;
					X86IntegerTarget memOff = (X86IntegerTarget) memIdx;
					
					var merged = mergeOffsets(
						idxOff.getValue(),
						scale,
						memOff.getValue(),
						mem.getScale()
					);
					
					index = merged.getLeft();
					scale = merged.getRight();
					
					toRegister = false;
				}
				
				else if(scale == mem.getScale()) {
					boolean idxLabel = index instanceof X86LabelTarget;
					boolean memLabel = memIdx instanceof X86LabelTarget;
					
					toRegister = false;
					
					if(idxLabel && (memInt || memLabel))
						index = new X86LabelTarget(
							index.getType(),
							((X86LabelTarget) index).getName(),
							memIdx
						);
					
					else if(memLabel && (idxInt || idxLabel))
						index = new X86LabelTarget(
							memIdx.getType(),
							((X86LabelTarget) memIdx).getName(),
							index
						);
					
					else toRegister = true;
				}
				
			}
			else toRegister = false;
		
		}
		
		if(toRegister) {
			displacement = null;
			base = toRegister(baseOrig);
			index = toRegister(index);
		}
		
		return X86MemoryTarget.ofDisplaced(
			type,
			displacement,
			base,
			index,
			scale
		);
	}
	
	private Pair<AssemblyTarget, Long> fixScale(AssemblyTarget offset, long scale) {
		if(scale != 0 && scale != 1 && scale != 2 && scale != 4 && scale != 8) {
			
			if(offset instanceof X86IntegerTarget idxOff) {
				offset = constant(
					idxOff.getValue()
						.multiply(BigInteger.valueOf(scale))
				);
				scale = 1;
			}
			else {
				offset = toRegister(offset);
				
				asm.add(
					X86InstructionKinds.IMUL,
					offset,
					constant(scale)
				);
			}
			
		}
		
		return Pair.of(offset, scale);
	}
	
	public void freeFPUOperand(Operand operand) {
		if(isFPUStackTop(operand))
			fpu.fpop();
	}
	
	public boolean isFPUStackTop(Operand operand) {
		return operand instanceof TemporaryOperand temp
			&& X86FPUHelper.useFPU(temp.getType());
	}
	
	@SuppressWarnings("preview")
	public AssemblyTarget generateOperand(Operand operand) {
		if(operand == null)
			return null;
		
		switch(operand) {
		case TemporaryOperand temp:
			if(temp.getId() == TemporaryOperand.RETURN_VALUE_ID)
				return callingConvention.getReturnValue();
			
			if(X86FPUHelper.useFPU(temp.getType()))
				return X86Register.ST0;
			
			return new VirtualRegisterTarget(temp.getType(), temp.getId());
			
		case ConditionOperand cond:
			return null;
			
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
			
		case GlobalOperand global:
			if(global.getObject().isString()) // OFFSET FLAT:name
				return new X86StringTarget(
					global.getType(),
					global.getObject().getFullName()
				);
			
			// name[rip]
			return X86MemoryTarget.ofDisplaced(
				global.getType(),
				label(global.getObject().getFullName()),
				x86.RIP
			);
			
		case IndexOperand index: { // [target+index*scale]
			AssemblyTarget base = generateOperand(index.getTarget());
			AssemblyTarget offset = generateOperand(index.getOffset());
			long scale = index.getScale();
			
			var fixed = fixScale(offset, scale);
			
			return mergeMemory(
				index.getType(),
				base,
				fixed.getLeft(),
				fixed.getRight()
			);
		}
			
		case LocalOperand local: // -offset[rbp]
			return generateLocalOperand(local.getObject());
			
		case MemberOperand member: {
			AssemblyTarget target = generateOperand(member.getTarget());
			
			Member mem = member.getMember();
			Type type = member.getType();
			
			if(mem.isBitfield())
				return generateBitfieldOperand(type, target, mem);
			
			return mergeMemory(type, target, constant(mem.getOffset()), 1);
		}
		
		default:
			Logger.error("Unknown operand type: %s", operand.getClass());
			return null;
		}
	}
	
	public AssemblyTarget generateLocalOperand(SymbolObject local) {
		if(local.isParameter())
			return callingConvention.getParameter(local.getName());
		
		return localVariables.computeIfAbsent(
			local,
			obj -> new VirtualStackTarget(local.getType())
		);
	}
	
	public AssemblyTarget generateBitfieldOperand(Type type, AssemblyTarget target, Member member) {
		BitfieldSegment[] segments = X86BitfieldHelper.determineBitfieldSegments(member);

		// whether multiple segments need to be combined
		boolean combine = segments.length > 1;
		
		AssemblyTarget value = combine
			? new VirtualRegisterTarget(type)
			: null;
		
		for(BitfieldSegment segment : segments) {

			/* see generateAssignBitfield
			 * 
			 * unsigned:
			 * 
			 * 	mov/movzx eax, [<src>+OFFSET]
			 * 	and eax, <(<(1 << BIT_WIDTH) - 1>) << BIT_OFFSET>
			 * 	shr/shl eax, <BIT_OFFSET - RELATIVE_OFFSET>
			 * 	or <val>, eax	(optional)
			 * 	
			 * signed: (only for the last operation)
			 * 
			 * 	mov/movzx eax, [<src>+OFFSET]
			 * 	sal eax, <8 * NUM_BYTES - BIT_OFFSET - BIT_WIDTH>
			 * 	sar eax, <8 * NUM_BYTES - BIT_WIDTH - RELATIVE_OFFSET>
			 * 	or <val>, eax	(optional)
			 * 	
			 */
			
			int memOffset = segment.memOffset();
			
			AssemblyTarget src = toRegister(
				mergeMemory(
					segment.memSize().getType(),
					target,
					constant(segment.offset() - memOffset),
					1
				),
				RegisterFlags.ZERO_EXTEND
			);
			
			if(!combine)
				value = src;
			
			if(!segment.isLast() || type.isUnsigned()) {
				
				asm.add(
					X86InstructionKinds.AND,
					src,
					constant(segment.bitMask() << segment.bitOffset())
				);
				
				int shiftCount = segment.bitOffset() - segment.relativeOffset() + 8 * memOffset;
				
				asm.add(
					shiftCount > 0
						? X86InstructionKinds.SHR
						: X86InstructionKinds.SHL,
					src,
					constant(Math.abs(shiftCount))
				);
				
			}
			else {
				
				int shiftPart = 8 * segment.numBytes() - segment.bitWidth();
				
				asm.add(
					X86InstructionKinds.SAL,
					src,
					constant(shiftPart - segment.bitOffset())
				);

				asm.add(
					X86InstructionKinds.SAR,
					src,
					constant(shiftPart - segment.relativeOffset() + 8 * memOffset)
				);
				
			}
			
			if(combine)
				asm.add(X86InstructionKinds.OR, value, src);
			
		}
		
		return value;
	}
	
	public AssemblyTarget toRegister(AssemblyTarget target, RegisterFlags...flags) {
		boolean noLiteral = false;
		boolean reassign = false;
		boolean zeroExtend = false;
		boolean signExtend = false;
		
		for(RegisterFlags flag : flags)
			if(flag != null)
				switch(flag) {
				case NO_LITERAL:	noLiteral = true; break;
				case REASSIGN:		reassign = true; break;
				case ZERO_EXTEND:	zeroExtend = true; break;
				case SIGN_EXTEND:	signExtend = true; break;
				}

		Type type = target.getType();
		X86Size size = X86Size.of(type);
		
		boolean needExtension = size != X86Size.DWORD && size != X86Size.QWORD;
		
		boolean quickExit = false;
		
		if(!reassign && !target.isMemory())
			quickExit = !noLiteral || target.isRegister();
		
		if(quickExit) {
			
			if(needExtension)
				asm.add(
					zeroExtend
						? X86InstructionKinds.MOVZX
						: X86InstructionKinds.MOVSX,
					target.resized(X86Size.DWORD.getType()),
					target
				);
			
			return target;
		}
		
		if(X86FPUHelper.useFPU(target.getType())) {
			asm.add(X86InstructionKinds.FLD, target);
			return X86Register.ST0;
		}
		
		if(type.isVaList()) {
			if(x86.bit32)
				type = NumericValueType.POINTER.asType();
			
			else Logger.warn("Moving »__builtin_va_list« to register. This is a bug.");
		}
		
		VirtualRegisterTarget virt = new VirtualRegisterTarget(type);
		VirtualRegisterTarget dest = virt;
		
		X86InstructionKinds insn;
		
		if(zeroExtend && needExtension) {
			insn = X86InstructionKinds.MOVZX;
			dest = virt.resized(X86Size.DWORD.getType());
		}
		
		else if(signExtend && needExtension) {
			insn = X86InstructionKinds.MOVSX;
			dest = virt.resized(X86Size.DWORD.getType());
		}
		
		else insn = X86InstructionSelector.select(X86InstructionKinds.MOV, type);
		
		asm.add(insn, dest, target);
		
		return virt;
	}
	
	public AssemblyTarget toMemory(AssemblyTarget target) {
		if(target == null || target.isMemory())
			return target;
		
		Type type = target.getType();
		
		VirtualStackTarget virt = new VirtualStackTarget(type);
		
		if(X86FPUHelper.useFPU(type)) {
			asm.add(X86InstructionKinds.FLD, target);
			asm.add(X86InstructionKinds.FSTP, virt);
		}
		else asm.add(
			X86InstructionSelector.select(X86InstructionKinds.MOV, type),
			virt,
			target
		);
		
		return virt;
	}

	public AssemblyTarget addOffset(AssemblyTarget target, int offset) {
		return addOffset(target.getType(), target, offset);
	}
	
	public AssemblyTarget addOffset(Type type, AssemblyTarget target, int offset) {
		if(offset == 0 || !(target instanceof X86MemoryTarget mem))
			return target.resized(type);
		
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
			
			return X86MemoryTarget.ofSegmentedDisplaced(
				type,
				mem.getSegment(),
				mem.getDisplacement(),
				mem.getBase(),
				constant(index),
				1
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
		
		return X86MemoryTarget.of(type, virt);
	}
	
	public static enum RegisterFlags {
		ZERO_EXTEND,
		SIGN_EXTEND,
		REASSIGN,
		NO_LITERAL
	}
	
}
