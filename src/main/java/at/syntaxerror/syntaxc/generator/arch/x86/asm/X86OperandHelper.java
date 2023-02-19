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
import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxc.generator.arch.x86.X86FloatTable;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86BitfieldHelper.BitfieldSegment;
import at.syntaxerror.syntaxc.generator.arch.x86.call.X86CallingConvention;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionSelector;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Size;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86DeferredMemoryTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86IntegerTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86LabelTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86MemoryTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86MemoryTarget.X86Displacement;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86OffsetTarget;
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
import at.syntaxerror.syntaxc.intermediate.operand.OffsetOperand;
import at.syntaxerror.syntaxc.intermediate.operand.Operand;
import at.syntaxerror.syntaxc.intermediate.operand.TemporaryOperand;
import at.syntaxerror.syntaxc.logger.Logger;
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
	
	private final Instructions asm;
	private final X86Assembly x86;
	private final X86FloatTable floatTable;
	private final X86FPUHelper fpu;
	private final X86CallingConvention<?> callingConvention;
	
	private final Map<SymbolObject, AssemblyTarget> localVariables = new HashMap<>();
	
	public void setLocalVariable(SymbolObject object, AssemblyTarget target) {
		localVariables.put(object, target);
	}
	
	public boolean isConstant(AssemblyTarget target) {
		return target != null
			&& ((target instanceof X86IntegerTarget)
			|| (target instanceof X86LabelTarget)
			|| (target instanceof X86OffsetTarget));
	}
	
	public AssemblyTarget requireDword(AssemblyTarget target) {
		if(target == null || !(target instanceof VirtualRegisterTarget || target instanceof X86Register))
			return target;
		
		Type type = target.getType();
		
		if(type.sizeof() < X86Size.DWORD.getType().sizeof())
			target = toRegister(
				target,
				RegisterFlags.REASSIGN,
				type.isSigned()
					? RegisterFlags.SIGN_EXTEND
					: RegisterFlags.ZERO_EXTEND
			);
		
		return target;
	}
	
	public AssemblyTarget scale(int size, AssemblyTarget target) {
		if(size == 1)
			return target;
		
		Type type = target.getType();
		
		if(target instanceof X86IntegerTarget integer)
			return new X86IntegerTarget(
				type,
				integer.getValue()
					.multiply(BigInteger.valueOf(size))
			);
		
		if(isConstant(target) && type.sizeof() < X86Size.DWORD.getType().sizeof())
			type = X86Size.DWORD.getType();
		
		AssemblyTarget scaled = new VirtualRegisterTarget(type)
			.minimum(X86Size.WORD.getType());
		
		asm.add(
			X86InstructionKinds.IMUL,
			scaled,
			toRegister(target, RegisterFlags.NO_LITERAL)
				.minimum(X86Size.WORD.getType()),
			constant(size)
		);
		
		return scaled;
	}
	
	public AssemblyTarget mergeMemory(Type type, AssemblyTarget base, AssemblyTarget offset) {
		AssemblyTarget displacement = null;
		AssemblyTarget index = null;
		
		boolean isDisplacement = isConstant(offset);
		
		boolean alreadyProcessed = false;
		
		if(base instanceof X86MemoryTarget mem) {
			alreadyProcessed = true;
			
			base = mem.getBase();
			displacement = mem.getDisplacement();
			index = mem.getIndex();
			
			if(isDisplacement) {
				
				if(mem.hasDisplacement()) {
					
					if(offset instanceof X86IntegerTarget && displacement instanceof X86IntegerTarget) {
						X86IntegerTarget idxOff = (X86IntegerTarget) offset;
						X86IntegerTarget memOff = (X86IntegerTarget) displacement;
						
						displacement = new X86IntegerTarget(
							type,
							idxOff.getValue().add(memOff.getValue())
						);
					}
					else displacement = new X86Displacement(displacement, offset);
					
				}
				else displacement = offset;
				
			}
			else {
				
				if(mem.hasIndex()) {
					
					AssemblyTarget temporary = toRegister(index, RegisterFlags.REASSIGN);
					
					asm.add(X86InstructionKinds.ADD, temporary, offset);
					
					index = temporary;
					
				}
				else index = toRegister(offset);
				
			}
		
		}
		
		else if(base.isMemory() || base instanceof X86OffsetTarget) {
			
			if(base instanceof X86DeferredMemoryTarget deferred) {
				if(deferred.accepts(offset))
					return deferred.with(type, requireDword(offset));
			}
			
			else {
				var parts = List.of(requireDword(base), requireDword(offset));
				
				if(X86DeferredMemoryTarget.isValid(parts))
					return new X86DeferredMemoryTarget(type, parts);
			}
		}
		
		if(!alreadyProcessed) {
			if(isDisplacement)
				displacement = offset;
			
			else index = toRegister(offset);
		}
		
		var r = X86MemoryTarget.ofDisplaced(
			type,
			displacement,
			requireDword(toRegister(base, RegisterFlags.NO_LITERAL, RegisterFlags.ARRAY_ADDRESS)),
			requireDword(index)
		);
		
		return r;
	}
	
	public void freeFPUOperand(Operand operand) {
		if(isFPUStackTop(operand))
			fpu.fpop();
	}
	
	public boolean isFPUStackTop(Operand operand) {
		return operand instanceof TemporaryOperand temp
			&& X86FPUHelper.useFPU(temp.getType());
	}
	
	public AssemblyTarget generateOperand(Operand operand) {
		return generateOperand(operand, false);
	}
	
	@SuppressWarnings("preview")
	private AssemblyTarget generateOperand(Operand operand, boolean mergable) {
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
			if(global.getObject().getType().isArray()) // OFFSET FLAT:name
				return new X86OffsetTarget(
					global.getType(),
					global.getObject().getFullName()
				);
			
			// name[rip]
			return X86MemoryTarget.ofDisplaced(
				global.getType(),
				label(global.getObject().getFullName()),
				x86.RIP
			);
			
		case IndexOperand index: { // [target+offset]
			AssemblyTarget base = generateOperand(index.getTarget(), true);
			AssemblyTarget offset = generateOperand(index.getOffset(), true);

			if(base.getType().isPointer())
				base = toRegister(base);

			if(base.getType().isPointerLike())
				offset = scale(
					base.getType()
						.toPointerLike()
						.getBase()
						.sizeof(),
					offset
				);
			
			return mergeMemory(
				index.getType(),
				base,
				toRegister(offset)
			);
		}
		
		case OffsetOperand off: { // target+offset
			AssemblyTarget base = generateOperand(off.getTarget(), true);
			AssemblyTarget offset = generateOperand(off.getOffset(), true);
			
			if(base.getType().isPointer())
				base = toRegister(base);

			if(base.getType().isPointerLike())
				offset = scale(
					base.getType()
						.toPointerLike()
						.getBase()
						.sizeof(),
					offset
				);
			
			AssemblyTarget result = mergeMemory(
				off.getType(),
				base,
				toRegister(offset)
			);
			
			return mergable
				? result
				: toRegister(
					result,
					RegisterFlags.FORCE_ADDRESS
				);
		}
		
		case LocalOperand local: // -offset[rbp]
			return generateLocalOperand(local.getObject());
			
		case MemberOperand member: {
			AssemblyTarget target = generateOperand(member.getTarget(), true);
			
			Type structType = member.getTarget().getType();
			
			if(structType.isPointer()) {
				VirtualRegisterTarget temporary = new VirtualRegisterTarget(structType);
				
				asm.add(
					X86InstructionKinds.MOV,
					temporary,
					mergeMemory(structType, target, constant(0))
				);
				
				target = temporary;
			}
			
			Member mem = member.getMember();
			Type type = member.getType();

			if(mem.isBitfield())
				return generateBitfieldOperand(type, target, mem);
			
			return mergeMemory(type, target, constant(mem.getOffset()));
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
					constant(segment.offset() - memOffset)
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
		boolean arrayAddress = false;
		boolean forceAddress = false;
		
		for(RegisterFlags flag : flags)
			if(flag != null)
				switch(flag) {
				case NO_LITERAL:	noLiteral = true; break;
				case REASSIGN:		reassign = true; break;
				case ZERO_EXTEND:	zeroExtend = true; break;
				case SIGN_EXTEND:	signExtend = true; break;
				case ARRAY_ADDRESS:	arrayAddress = true; break;
				case FORCE_ADDRESS:	forceAddress = true; break;
				}

		Type type = target.getType();
		X86Size size = X86Size.of(type);
		
		boolean needExtension = size != X86Size.DWORD && size != X86Size.QWORD && !isConstant(target);
		
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
		
		X86InstructionKinds insn;
		
		if(zeroExtend && needExtension) {
			insn = X86InstructionKinds.MOVZX;
			virt = virt.resized(X86Size.DWORD.getType());
		}
		
		else if(signExtend && needExtension) {
			insn = X86InstructionKinds.MOVSX;
			virt = virt.resized(X86Size.DWORD.getType());
		}
		
		else insn = target.isMemory() && (forceAddress || (arrayAddress && type.isArray())) && !(target instanceof X86OffsetTarget)
			? X86InstructionKinds.LEA
			: X86InstructionSelector.select(
				X86InstructionKinds.MOV,
				type
			);
		
		asm.add(insn, virt, target);
		
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
		
		BigInteger disp;
		
		if(mem.hasDisplacement()) {
			AssemblyTarget idx = mem.getDisplacement();
			
			if(idx instanceof X86IntegerTarget integer)
				disp = integer.getValue();
			
			else disp = null;
		}
		else disp = BigInteger.ZERO;
		
		if(disp != null) {
			/*
			 * modify existing target by adding the offset to
			 * the index and adjusting the scale, if necessary
			 */
			
			disp = disp.add(BigInteger.valueOf(offset));
			
			return X86MemoryTarget.ofSegmentedDisplaced(
				type,
				mem.getSegment(),
				constant(disp),
				requireDword(mem.getBase()),
				requireDword(mem.getIndex())
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
		NO_LITERAL,
		ARRAY_ADDRESS,
		FORCE_ADDRESS
	}
	
}
