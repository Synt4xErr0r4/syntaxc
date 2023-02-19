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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaArg;
import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaEnd;
import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaStart;
import at.syntaxerror.syntaxc.generator.alloc.RegisterAllocator;
import at.syntaxerror.syntaxc.generator.alloc.impl.GraphColoringRegisterAllocator;
import at.syntaxerror.syntaxc.generator.alloc.impl.RegisterSupplier;
import at.syntaxerror.syntaxc.generator.arch.x86.X86Architecture;
import at.syntaxerror.syntaxc.generator.arch.x86.X86FloatTable;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86BitfieldHelper.BitfieldSegment;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86OperandHelper.RegisterFlags;
import at.syntaxerror.syntaxc.generator.arch.x86.call.X86CallingConvention;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.ConditionFlags;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Instruction;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionSelector;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Size;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86IntegerTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86LabelTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86MemoryTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86OffsetTarget;
import at.syntaxerror.syntaxc.generator.asm.AssemblyGenerator;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.PrologueEpilogueInserter;
import at.syntaxerror.syntaxc.generator.asm.insn.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.insn.AssemblyInstructionKind;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.generator.asm.target.RegisterTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualRegisterTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualStackTarget;
import at.syntaxerror.syntaxc.intermediate.operand.ConditionOperand;
import at.syntaxerror.syntaxc.intermediate.operand.ConditionOperand.Condition;
import at.syntaxerror.syntaxc.intermediate.operand.MemberOperand;
import at.syntaxerror.syntaxc.intermediate.operand.Operand;
import at.syntaxerror.syntaxc.intermediate.operand.TemporaryOperand;
import at.syntaxerror.syntaxc.intermediate.representation.AssignIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BinaryIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BuiltinIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.CallIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.CallIntermediate.CallParameterIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.CallIntermediate.CallStartIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.CastIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate;
import at.syntaxerror.syntaxc.intermediate.representation.JumpIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.LabelIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.MemcpyIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.MemsetIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.UnaryIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.UnaryIntermediate.UnaryOperation;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.misc.config.Flags;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.type.FunctionType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class X86AssemblyGenerator extends AssemblyGenerator {

	private static X86PrologueEpilogueInserter prologueEpilogueInserter;
	
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
	
	@Delegate
	private X86FPUHelper fpu;
	
	private List<ConditionFlags> conditionFlags;
	private Condition condition;
	
	private X86CallingConvention<? extends Object> callingConvention;
	
	@Override
	public RegisterAllocator getRegisterAllocator(Instructions asm) {
		return new GraphColoringRegisterAllocator(asm, architecture.getAlignment()) {
			
			@Override
			public List<RegisterSupplier> getRegisterSuppliers() {
				return x86.getRegisterProvider().getSuppliers();
			}
			
			@Override
			public AssemblyTarget resolveVirtualMemory(long address, Type type) {
				return X86MemoryTarget.ofDisplaced(
					type,
					constant(-address - type.sizeof()),
					x86.RBP
				);
			}
			
			@Override
			public boolean isBlockEnd(AssemblyInstruction insn) {
				AssemblyInstructionKind kind = insn.getKind();
				
				return kind instanceof X86InstructionKinds x86
					&& (x86.isJump()
					|| x86 == X86InstructionKinds.LABEL);
			}
			
			@Override
			public boolean isCopy(AssemblyInstruction insn) {
				return insn.getKind() instanceof X86InstructionKinds x86
					&& x86.isCopy();
			}
			
			@Override
			public AssemblyInstruction store(Instructions asm, VirtualStackTarget target, RegisterTarget register) {
				return new X86Instruction(
					asm,
					X86InstructionSelector.select(X86InstructionKinds.MOV, target.getType()),
					target,
					register
				);
			}
			
			@Override
			public AssemblyInstruction restore(Instructions asm, RegisterTarget register, VirtualStackTarget target) {
				return new X86Instruction(
					asm,
					X86InstructionSelector.select(X86InstructionKinds.MOV, register.getType()),
					register,
					target
				);
			}
			
		};
	}
	
	@Override
	public PrologueEpilogueInserter getPrologueEpilogueInserter() {
		return Objects.requireNonNullElseGet(
			prologueEpilogueInserter,
			() -> prologueEpilogueInserter = new X86PrologueEpilogueInserter(x86)
		);
	}
	
	@Override
	public void onEntry(Instructions asm, FunctionType type, List<SymbolObject> parameters) {
		this.asm = asm;
		
		asm.setConstructor(X86Instruction::new);
		asmHead = asm.getHead();
		
		callingConvention = x86.getCallingConvention(type, asm, this);
		callingConvention.onEntry();

		fpu = new X86FPUHelper(asm, this);
		operandHelper = new X86OperandHelper(asm, x86, floatTable, fpu, callingConvention);
		
		isFPUControlWordStored = false;
	}
	
	@Override
	public void onLeave(FunctionType type) {
		callingConvention.onLeave();
	}
	
	@SuppressWarnings("preview")
	@Override
	public void generate(Intermediate intermediate) {
		switch(intermediate) {
		case AssignIntermediate assign:			generateAssign			(assign);	break;
		case BinaryIntermediate binary:			generateBinary			(binary);	break;
		case BuiltinIntermediate builtin:		generateBuiltin			(builtin);	break;
		case CallStartIntermediate call:		generateCallStart		(call);		break;
		case CallParameterIntermediate call:	generateCallParameter	(call);		break;
		case CallIntermediate call:				generateCall			(call);		break;
		case CastIntermediate cast:				generateCast			(cast);		break;
		case JumpIntermediate jump:				generateJump			(jump);		break;
		case LabelIntermediate label:			generateLabel			(label);	break;
		case UnaryIntermediate unary:			generateUnary			(unary);	break;
		case MemcpyIntermediate memcpy:			generateMemcpy			(memcpy);	break;
		case MemsetIntermediate memset:			generateMemset			(memset);	break;
		default:
			Logger.error("Illegal intermediate: %s", intermediate.getClass());
			break;
		}
	}
	
	@Override
	public void generateNop() {
		asm.add(X86InstructionKinds.NOP);
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
				constant(segment.offset() - segment.memOffset())
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
		
		if(X86FPUHelper.useFPU(typeDst)) {
			/*
			 * long double:
			 * float/double (32-bit):
			 * 
			 * fld <src>
			 * fstp <dst>
			 */
			
			fld(src);
			fstp(dst);
			
			return;
		}

		X86InstructionKinds mov = X86InstructionSelector.select(X86InstructionKinds.MOV, typeDst);
		
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
			 */
			
			VirtualRegisterTarget tmp = new VirtualRegisterTarget(typeDst);
			
			if(dst.getType().isPointer() && src.getType().isArray())
				asm.add(X86InstructionKinds.LEA, tmp, src);
			else asm.add(mov, tmp, src);
			
			asm.add(mov, dst, tmp);
			
			return;
		}
		
		asm.add( // mov <dst>, <src>
			mov,
			dst,
			src.resized(dst.getType())
		);
	}
	
	private void generateAssign(AssignIntermediate assign) {
		Operand dst = assign.getTarget();
		
		if(dst == null) {
			// expression is useless, but still free FPU stack
			freeFPUOperand(assign.getValue());
			return;
		}
		
		if(dst instanceof TemporaryOperand temp
			&& temp.getId() == TemporaryOperand.RETURN_VALUE_ID
			&& callingConvention.getFunction().getReturnType().isStructLike()) {
			
			Type type = callingConvention.getReturnValue().getType();
			
			if(type.isPointer() && type.dereference().isStructLike()) {
				generateMemcpy(
					generateOperand(dst),
					generateOperand(assign.getValue()),
					callingConvention.getFunction().getReturnType().sizeof(),
					true
				);
				return;
			}
		}
		
		assign(
			dst,
			generateOperand(assign.getValue())
		);
	}
	
	public void assign(AssemblyTarget target, AssemblyTarget value) {
		Type type = target.getType();
		
		if(type.isStructLike() || (!x86.bit32 && type.isVaList()))
			generateMemcpy(
				target,
				value,
				type.sizeof()
			);
		
		else generateAssignBody(
			target,
			value
		);
	}
	
	public void assign(Operand target, AssemblyTarget value) {
		Type type = target.getType();
		
		if(type.isBitfield()) {
			generateAssignBitfield(
				(MemberOperand) target,
				value
			);
			return;
		}
		
		assign(generateOperand(target), value);
	}
	
	public void assignDynamicMemory(Operand target, Consumer<AssemblyTarget> assignment) {
		Type type = target.getType();
		
		if(target.isMemory() && !type.isBitfield())
			assignment.accept(generateOperand(target));
		
		else {
			VirtualStackTarget memory = new VirtualStackTarget(type);
			
			assignment.accept(memory);
			
			assign(target, memory);
		}
	}
	
	public void assignDynamicRegister(Operand target, Consumer<AssemblyTarget> assignment) {
		if(target == null)
			return;
		
		if(!target.isMemory())
			assignment.accept(generateOperand(target));
		
		else {
			VirtualRegisterTarget register = new VirtualRegisterTarget(target.getType());
			
			assignment.accept(register);
			
			assign(target, register);
		}
	}
	
	public void assignDynamic(Operand target, Consumer<AssemblyTarget> assignment) {
		if(target == null)
			return;
		
		if(target.isMemory())
			assignDynamicMemory(target, assignment);
		else assignDynamicRegister(target, assignment);
	}
	
	private void generateBinaryArithmetic(Operand dst, AssemblyTarget left, AssemblyTarget right, X86InstructionKinds insnBase) {
		Type type = left.getType();
		
		if(X86FPUHelper.useFPU(type)) {
			
			/*
			 * long double:
			 * float/double (32-bit):
			 * 
			 * fld <A>
			 * fld <B>
			 * faddp st(1), st(0)
			 * fstp <R>
			 */
			
			fld(left);
			fld(right);
			
			asm.add(
				X86InstructionSelector.select(insnBase, Type.LDOUBLE),
				X86Register.ST1,
				X86Register.ST0
			);

			fpuStackPop();
			assignDynamic(dst, this::fstp);
			
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
			AssemblyTarget leftTarget = toRegister(left, RegisterFlags.NO_LITERAL);
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
			
			if(!leftTarget.isRegister()) {
				
				if(!compact)
					leftTarget = toRegister(leftTarget, RegisterFlags.NO_LITERAL);
				
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
			right.isMemory()
				? right
				: toRegister(right, RegisterFlags.NO_LITERAL)
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
			
			if(!left.isRegister()) {
				
				if(!compact)
					left = toRegister(left, RegisterFlags.NO_LITERAL);
				
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
			
			if(!left.isRegister() && !compact)
				left = toRegister(left, RegisterFlags.NO_LITERAL);
			
			generateAssignBody(X86Register.ECX.resized(right.getType()), right);
			
			X86InstructionKinds insn = unsigned
				? insnUint
				: insnSint;
			
			asm.add(insn, left, X86Register.CL);
	
			if(!compact)
				generateAssignBody(result, left);
		});
	}
	
	private void generateBinaryFloatingComparison(Type type, AssemblyTarget first, AssemblyTarget second, boolean ordered) {
		
		if(X86FPUHelper.useFPU(type)) {
			
			/* 
			 * long double comparison:
			 * float/double comparison (32-bit):
			 * 
			 * fld <a>
			 * fld <b>
			 * fcomip/fucomip st, st(1)
			 * fstp st(1)
			 */
			
			fld(first);
			fld(second);
			
			asm.add(
				ordered
					? X86InstructionKinds.FCOMIP
					: X86InstructionKinds.FUCOMIP,
				X86Register.ST0,
				X86Register.ST1
			);
			
			fpuStackPop();
			fpop();
			
		}
		else {
			
			/*
			 * float/double comparison (64-bit):
			 * 
			 * movss/movsd xmm0, <a>
			 * comiss/comisd/ucomiss/ucomisd xmm0, <b>
			 */
			
			asm.add(
				X86InstructionSelector.select(
					ordered
						? X86InstructionKinds.COMISS
						: X86InstructionKinds.UCOMISS,
					type
				),
				toRegister(first, RegisterFlags.NO_LITERAL),
				second
			);
			
		}
		
	}

	private ConditionFlags generateBinaryComparisonBody(Operand dst, AssemblyTarget left, AssemblyTarget right,
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
			
			generateBinaryFloatingComparison(type, first, second, true);
			
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
			
			if(!first.isRegister() && second.isMemory())
				first = toRegister(first, RegisterFlags.NO_LITERAL);
			
			if(first instanceof X86IntegerTarget) {
				AssemblyTarget tmp = first;
				first = second;
				second = tmp;
			}
			
			asm.add(X86InstructionKinds.CMP, null, first, second);
			
		}
		
		return flag;
	}
	
	private void generateBinaryComparison(Operand dst, AssemblyTarget left, AssemblyTarget right,
			ConditionFlags flagSint, ConditionFlags flagUint, ConditionFlags flagFloat,
			boolean swapUint, boolean swapFloat) {
		
		if(dst instanceof ConditionOperand cond) { // flag is used by jump instruction
			ConditionFlags flag = generateBinaryComparisonBody(
				dst, left, right,
				flagSint, flagUint, flagFloat,
				swapUint, swapFloat
			);
			
			condition = cond.getCondition();
			conditionFlags = List.of(flag);
		}
		
		/*
		 * xor eax, eax
		 * ...
		 * set<cc>, al
		 */
		else assignDynamicRegister(
			dst,
			reg -> {
				AssemblyTarget reg32 = reg.resized(Type.INT);
				
				asm.add(X86InstructionKinds.XOR, reg32, reg32);
				
				ConditionFlags flag = generateBinaryComparisonBody(
					dst, left, right,
					flagSint, flagUint, flagFloat,
					swapUint, swapFloat
				);
				
				asm.add(flag.getSetInstruction(), reg.resized(Type.CHAR));
			}
		);
		
	}
	
	private void generateBinaryEquality(Operand dst, AssemblyTarget left, AssemblyTarget right, boolean inverted) {
		
		Type type = left.getType();
		
		if(type.isFloating()) {
			
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
			 * 
			 * float/double (64-bit):
			 * 
			 * 	xor eax, eax
			 * 	mov edx, 0/1
			 * 	movss/movsd xmm0, <A>
			 * 	ucomiss xmm0, <B>
			 */
			
			if(dst instanceof ConditionOperand cond) { // flag is used by jump instruction
				generateBinaryFloatingComparison(type, left, right, true);
				
				condition = cond.getCondition();
				conditionFlags = inverted
					? List.of(
						ConditionFlags.PARITY,
						ConditionFlags.EQUAL
					)
					: List.of(
						ConditionFlags.NOT_PARITY,
						ConditionFlags.NOT_EQUAL
					);
			}
			
			else assignDynamicRegister(
				dst,
				reg -> {
					/*
					 * 	setnp/setp al
					 * 	cmovne eax, edx
					 * 	mov <R>, eax
					 */
					
					AssemblyTarget value = new VirtualRegisterTarget(Type.INT);
					AssemblyTarget reg32 = reg.resized(Type.INT);
					
					asm.add(X86InstructionKinds.XOR, reg32, reg32);
					
					asm.add(
						X86InstructionKinds.MOV,
						value,
						inverted
							? constant(1)
							: constant(0)
					);
					
					generateBinaryFloatingComparison(type, left, right, true);
					
					asm.add(
						(inverted
							? ConditionFlags.PARITY
							: ConditionFlags.NOT_PARITY)
							.getSetInstruction(),
						reg.resized(Type.CHAR)
					);
					
					asm.add(X86InstructionKinds.CMOVNE, reg32, value);
				}
			);
			
			return;
		}
		
		/*
		 * int:
		 * 
		 * 	mov eax, <A>
		 * 	cmp eax, <B>
		 * 	sete/setne al
		 * 	movzx eax, al
		 * 	mov <R>, eax
		 */
		
		ConditionFlags flag = inverted
			? ConditionFlags.NOT_EQUAL
			: ConditionFlags.EQUAL;
		
		generateBinaryComparison(
			dst, left, right,
			flag, flag, null,
			false, false
		);
	}
	
	private void generateBinary(BinaryIntermediate binary) {
		Operand dst = binary.getResult();
		
		if(dst == null) {
			// expression is useless, but still free FPU stack
			freeFPUOperand(binary.getLeft());
			freeFPUOperand(binary.getRight());
			return;
		}
		
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
		}
		
	}

	@SuppressWarnings("preview")
	private void generateBuiltin(BuiltinIntermediate builtin) {
		switch(builtin.getFunction()) {
		case BuiltinVaStart vaStart:	callingConvention.vaStart(vaStart); break;
		case BuiltinVaArg vaArg:		callingConvention.vaArg(builtin.getResult(), vaArg); break;
		case BuiltinVaEnd vaEnd:		callingConvention.vaEnd(vaEnd); break;
		default:
			Logger.warn("Unknown builtin function »%s«", builtin.getFunction().getName());
			break;
		}
	}
	
	private Map<CallIntermediate, Object> callingContexts = new HashMap<>();

	private void generateCallStart(CallStartIntermediate call) {
		callingContexts.put(call.getCall(), callingConvention.createCallingContext(call));
	}
	
	private void generateCallParameter(CallParameterIntermediate call) {
		callingConvention.passParameterUnchecked(callingContexts.get(call.getCall()), call);
	}
	
	private void generateCall(CallIntermediate call) {
		Type type = call.getFunction().getType();
		
		if(type.isPointer())
			type = type.dereference();
		
		AssemblyTarget function = generateOperand(call.getFunction());
		
		if(function instanceof X86IntegerTarget)
			function = toRegister(function, RegisterFlags.NO_LITERAL);
		
		if(function instanceof X86MemoryTarget mem
			&& mem.getType().isFunction()
			&& mem.hasDisplacement()
			&& mem.getDisplacement() instanceof X86LabelTarget label
			&& mem.getBase() instanceof X86Register base
			&& X86Register.RIP.intersects(base)
			&& !mem.hasIndex())
			function = X86MemoryTarget.ofDisplaced(
				Type.VOID,
				mem.getDisplacement(),
				x86.RIP
			);
		
		callingConvention.callUnchecked(
			callingContexts.remove(call),
			function,
			call.getTarget()
		);
	}

	private void generateCast(CastIntermediate cast) {
		Operand dst = cast.getResult();

		if(dst == null) {
			// expression is useless, but still free FPU stack
			freeFPUOperand(cast.getTarget());
			return;
		}
		
		AssemblyTarget src = generateOperand(cast.getTarget());

		Type typeDst = dst.getType();
		Type typeSrc = src.getType();
		
		X86Size sizeDst = X86Size.of(typeDst);
		X86Size sizeSrc = X86Size.of(typeSrc);
		
		boolean ldoubleSrc =	X86FPUHelper.useFPU(typeSrc);
		boolean doubleSrc =		X86FPUHelper.isDouble(typeSrc);
		boolean floatSrc =		X86FPUHelper.isFloat(typeSrc);
		boolean intSrc =		!typeSrc.isFloating();

		boolean ldoubleDst =	X86FPUHelper.useFPU(typeDst);
		boolean doubleDst =		X86FPUHelper.isDouble(typeDst);
		boolean floatDst =		X86FPUHelper.isFloat(typeDst);
		boolean intDst =		!typeDst.isFloating();
		
		if(!Flags.LONG_DOUBLE.isEnabled()) {
			doubleDst |= ldoubleDst;
			doubleSrc |= ldoubleSrc;
			
			ldoubleDst = ldoubleSrc = false;
		}

		if(X86FPUHelper.useFPU(typeSrc)) {
			doubleSrc = floatSrc = false;
			ldoubleSrc = true;
		}

		if(X86FPUHelper.useFPU(typeDst)) {
			doubleDst = floatDst = false;
			ldoubleDst = true;
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
			
			fld(src);
			
			assignDynamicMemory(dst, this::fstp);
			
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
			
			fld(src);
			
			if(intDst)
				assignDynamicMemory(
					dst,
					mem -> {
						overrideFPUState();
						fstp(mem);
						restoreFPUState();
					}
				);
			else assignDynamicMemory(dst, this::fstp);
			
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
		
		X86LabelTarget label = new X86LabelTarget(Type.VOID, jump.getLabel());
		
		if(!jump.isConditional()) {
			asm.add(X86InstructionKinds.JMP, label);
			return;
		}
		
		Operand operand = jump.getCondition().get();
		
		if(!(operand instanceof ConditionOperand)) {
			
			ConditionOperand condition = new ConditionOperand(
				Condition.NOT_EQUAL,
				operand.getType()
			);
			
			generateBinaryEquality(
				condition,
				generateOperand(operand),
				constant(0),
				true
			);
			
			operand = condition;
		}

		boolean negated = ((ConditionOperand) operand).getCondition() != condition;
		
		for(ConditionFlags flag : conditionFlags) {
			if(negated)
				flag = flag.negate();
			
			asm.add(flag.getJumpInstruction(), label);
		}
		
		condition = null;
		conditionFlags = List.of();
	}

	private void generateLabel(LabelIntermediate label) {
		asm.add(
			X86InstructionKinds.LABEL,
			new X86LabelTarget(Type.VOID, label.getLabel())
		);
	}
	
	private void generateUnaryNegation(AssemblyTarget dst, AssemblyTarget src, boolean equalOperands, X86InstructionKinds insn) {

		if(equalOperands)
			asm.add(insn, src);
		
		else {
			VirtualRegisterTarget reg = new VirtualRegisterTarget(src.getType());
			
			assign(reg, src);
			
			asm.add(insn, reg);
			
			assign(dst, reg);
		}
		
	}

	private void generateUnary(UnaryIntermediate unary) {
		Operand dst = unary.getResult();
		
		if(dst == null) {
			// expression is useless, but still free FPU stack
			freeFPUOperand(unary.getTarget());
			return;
		}
		
		UnaryOperation op = unary.getOp();
		
		AssemblyTarget target = generateOperand(unary.getTarget());
		
		/*
		 * get address of symbol:
		 * 
		 * 	lea <dst>, <src>
		 * 
		 * 
		 * if the destination is not a register:
		 * 
		 * 	lea rax, <src>
		 * 	mov <dst>, rax
		 */
		if(op == UnaryOperation.ADDRESS_OF)
			assignDynamicRegister(
				dst,
				reg -> asm.add(
					target instanceof X86OffsetTarget
						? X86InstructionKinds.MOV
						: X86InstructionKinds.LEA,
					reg,
					target
				)
			);

		/*
		 * logical NOT:
		 * 
		 * 	xor eax, eax
		 * 	cmp <src>, 0
		 * 	sete al
		 * 	mov <dst>, eax	
		 * 
		 * (effectively equal to '<src> != 0')
		 */
		else if(op == UnaryOperation.LOGICAL_NOT)
			generateBinaryEquality(
				dst,
				target,
				constant(0),
				false
			);
		
		else assignDynamic(
			dst,
			dyn -> {
				
				boolean equalOperands = target.equals(dyn);
				
				switch(op) {
				case INDIRECTION:
					
					/*
					 * dereference/indirection:
					 * 
					 * 	mov eax, [<src>]
					 * 	mov <dst>, eax
					 */
					
					assign(
						dyn,
						X86MemoryTarget.of(
							dyn.getType(),
							requireDword(toRegister(target))
						)
					);
					
					break;
					
				case BITWISE_NOT:
					
					/*
					 * one's complement:
					 * 	
					 * 	mov eax, <src>
					 * 	not eax
					 * 	mov <dst>, eax
					 * 
					 * if <src> and <dst> denote the same memory address/register:
					 * 
					 * 	not <src/dst>
					 */
					
					generateUnaryNegation(dyn, target, equalOperands, X86InstructionKinds.NOT);
					
					break;
					
				case MINUS:
					
					/*
					 * negation:
					 * 	
					 * 	mov eax, <src>
					 * 	neg eax
					 * 	mov <dst>, eax
					 * 
					 * if <src> and <dst> denote the same memory address/register:
					 * 
					 * 	neg <src/dst>
					 */
					
					generateUnaryNegation(dyn, target, equalOperands, X86InstructionKinds.NEG);
					
					break;
					
				default:
					break;
				}
			}
		);
	}
	
	private void generateMemcpy(AssemblyTarget dst, AssemblyTarget src, int size) {
		generateMemcpy(dst, src, size, false);
	}
	
	private void generateMemcpy(AssemblyTarget dst, AssemblyTarget src, int size, boolean derefDst) {
		if(size < 1)
			return;
		
		// use 'rep movs' only if copying a large number of bytes
		if(size > x86.threshold) {
			
			var opSize = calculateMemoryOperationSize(size);
			
			Type type = opSize.getRight().getType();
			
			asm.add(
				derefDst
					? X86InstructionKinds.MOV
					: X86InstructionKinds.LEA,
				x86.RDI,
				dst
			);
			
			asm.add(X86InstructionKinds.LEA, x86.RSI, src);
			
			asm.add( // mov rcx, n
				X86InstructionKinds.MOV,
				x86.RCX,
				constant(opSize.getLeft())
			);
			
			// 'rep movsq' copies rcx bytes from rsi to rdi
			
			asm.add( // rep movsq QWORD PTR es:[rdi], QWORD PTR ds:[rsi]
				X86InstructionKinds.REP_MOVS,
				X86MemoryTarget.ofSegmented(
					type,
					X86Register.ES,
					x86.RDI
				),
				X86MemoryTarget.ofSegmented(
					type,
					X86Register.DS,
					x86.RSI
				)
			);
		
			return;
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
		
		int offset = 0;
		
		for(int block : calculateMemoryBlocks(size)) {
			Type blockType = X86Size.ofPrimitive(block).getType(); 
			
			tmp = tmp.resized(blockType);
			
			asm.add(
				X86InstructionKinds.MOV,
				tmp,
				addOffset(blockType, src, offset)
			);
			asm.add(
				X86InstructionKinds.MOV,
				addOffset(blockType, dst, offset),
				tmp
			);
			
			offset += block;
		}
	}
	
	private void generateMemset(AssemblyTarget dst, int size, int value) {
		if(size < 1)
			return;

		// use 'rep stos' only if setting a large number of bytes
		if(size > x86.threshold) {
			var opSize = calculateMemoryOperationSize(size);
			
			X86Register valueRegister = X86Register.RAX.resized(opSize.getRight());

			asm.add(X86InstructionKinds.LEA, x86.RDI, dst);
			
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
			
			// 'rep stosq' sets rcx bytes from rdi to rax
			
			asm.add( // rep stosq QWORD PTR es:[rdi], rax
				X86InstructionKinds.REP_STOS,
				X86MemoryTarget.ofSegmented(
					opSize.getRight().getType(),
					X86Register.ES,
					x86.RDI
				),
				valueRegister
			);
			
			return;
		}
		
		/*
		 * mov rbx, <dst>
		 * 
		 * mov QWORD PTR [rbx], <val>
		 * mov WORD PTR 8[rbx], <val>
		 * 
		 * etc.
		 */
		
		int offset = 0;
		
		Map<Integer, BigInteger> blockValues = new HashMap<>();
		BigInteger rawValue = BigInteger.valueOf(value & 0xFF);
		
		for(int block : calculateMemoryBlocks(size)) {
			asm.add(
				X86InstructionKinds.MOV,
				addOffset(
					X86Size.ofPrimitive(block).getType(),
					dst,
					offset
				),
				constant(
					blockValues.computeIfAbsent(
						block,
						blk -> {
							BigInteger val = BigInteger.ZERO;
							
							while(blk-- > 0)
								val = val.shiftLeft(8)
									.or(rawValue);
							
							return val;
						}
					)
				)
			);
			
			offset += block;
		}
	}

	private void generateMemcpy(MemcpyIntermediate memcpy) {
		int size = memcpy.getLength();
		
		if(size < 1)
			return;
		
		AssemblyTarget dst = generateOperand(memcpy.getDestination());
		AssemblyTarget src = generateOperand(memcpy.getSource());
		
		dst = addOffset(dst, memcpy.getDestinationOffset());
		src = addOffset(src, memcpy.getSourceOffset());
		
		generateMemcpy(dst, src, size);
	}

	private void generateMemset(MemsetIntermediate memset) {
		int size = memset.getLength();
		int value = memset.getValue();
		
		if(size < 1)
			return;
		
		AssemblyTarget dst = generateOperand(memset.getTarget());
		
		dst = addOffset(dst, memset.getOffset());

		generateMemset(dst, size, value);
	}
	
	private List<Integer> calculateMemoryBlocks(int size) {
		/*
		 * split into possibly multiple assignments,
		 * assuming dword/qword alignment
		 */
		
		final int blockSizes[] = x86.bit32
			? new int[] { 4, 2, 1 }
			: new int[] { 8, 4, 2, 1 };
		
		List<Integer> blocks = new ArrayList<>();
		
		while(size > 0)
			for(int block : blockSizes)
				if(size >= block) {
					size -= block;
					blocks.add(block);
					break;
				}
		
		return blocks;
	}
	
	private Pair<Integer, X86Size> calculateMemoryOperationSize(int size) {
		/* default state: process 1 byte at once */
		
		int byteCount = size;
		X86Size wordSize = X86Size.BYTE;
		
		if((byteCount & 7) == 0 && !x86.bit32) {
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
	
}
