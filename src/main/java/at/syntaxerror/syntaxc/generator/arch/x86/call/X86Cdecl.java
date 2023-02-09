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
package at.syntaxerror.syntaxc.generator.arch.x86.call;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaArg;
import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaEnd;
import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaStart;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86AssemblyGenerator;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86OperandHelper;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86MemoryTarget;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.insn.StoreRegistersInstruction;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualStackTarget;
import at.syntaxerror.syntaxc.intermediate.operand.Operand;
import at.syntaxerror.syntaxc.type.FunctionType;
import at.syntaxerror.syntaxc.type.FunctionType.Parameter;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.Getter;

/**
 * The default calling convention for 32-bit Linux
 * 
 *  - integers are returned in eax
 *  - floating point numbers are returned in st(0)
 *  - returning structs/unions:
 *    - the required space is allocated by the callee
 *    - the first (hidden) call argument is the addres to the allocated space
 *    - the address is also returned in the eax register
 *  - all call arguments are pushed to the stack
 *    - the rightmost argument is pushed first (right-to-left)
 *    - structs/unions are padded to be a multiple of 4 bytes
 *    - the stack is callee-cleaned
 *  - eax, ecx and edx are callee-saved
 * 
 * @author Thomas Kasper
 * 
 */
public class X86Cdecl extends X86CallingConvention {
	
	@Getter
	private AssemblyTarget returnValue;

	private Map<String, X86MemoryTarget> parameters;
	
	private StoreRegistersInstruction registerStore, callStore;
	private List<VirtualStackTarget> fpuStore;
	
	public X86Cdecl(FunctionType function, Instructions asm, X86AssemblyGenerator generator) {
		super(function, asm, generator);
		
		returnValue = getReturnValue(function.getReturnType());
	}
	
	private AssemblyTarget getReturnValue(Type returnType) {
		if(returnType.isVoid() || returnType.isInteger() || returnType.isPointerLike())
			return X86Register.EAX;
		
		if(returnType.isFloating())
			return X86Register.ST0;
		
		if(returnValue == null)
			returnValue = X86MemoryTarget.ofDisplaced(
				returnType.addressOf(),
				X86OperandHelper.constant(8),
				X86Register.EBP
			);
		
		return returnValue;
	}
	
	@Override
	public AssemblyTarget getParameter(String name) {
		return parameters.get(name);
	}

	@Override
	public void onEntry() {
		parameters = new HashMap<>();
		
		int stackOffset = 0;
		
		Type returnType = function.getReturnType();
		
		if(returnType.isStructLike())
			stackOffset += NumericValueType.POINTER.getSize();
		
		for(Parameter param : function.getParameters()) {
			Type type = param.type();
			
			parameters.put(
				param.name(),
				X86MemoryTarget.ofDisplaced(
					type,
					X86OperandHelper.constant(8 + stackOffset),
					X86Register.EBP
				)
			);

			stackOffset += type.sizeof();
		}
		
		// ebx, edi, and esi belong to the caller
		asm.add(registerStore = new StoreRegistersInstruction(
			asm,
			X86Register.EDI, X86Register.ESI, X86Register.EBX
		));
	}

	@Override
	public void onLeave() {
		asm.add(registerStore.restore());
	}

	@Override
	public void beforeCall() {
		// FPU stack must be empty
		fpuStore = new ArrayList<>();
		
		while(!generator.isFPUStackEmpty()) {
			VirtualStackTarget tmp = new VirtualStackTarget(Type.LDOUBLE);
			
			generator.fstp(tmp);
			
			fpuStore.add(0, tmp);
		}
		
		// eax, ecx, edx belong to the callee
		X86Register[] scratchRegisters;
		
		if(returnValue == X86Register.EAX)
			scratchRegisters = new X86Register[] {
				X86Register.ECX,
				X86Register.EDX,
			};
		
		else scratchRegisters = new X86Register[] {
			X86Register.EAX,
			X86Register.ECX,
			X86Register.EDX,
		};
		
		callStore = new StoreRegistersInstruction(asm, scratchRegisters);
		asm.add(callStore);
	}
	
	@Override
	public void call(AssemblyTarget functionTarget, FunctionType callee,
			Iterator<AssemblyTarget> args, AssemblyTarget callerReturnDestination) {
		
		/* calculate stack offsets for arguments */
		
		LinkedHashMap<Long, AssemblyTarget> argOffsets = new LinkedHashMap<>();
		
		long allocatedStackSpace = 0;
		
		while(args.hasNext()) {
			AssemblyTarget arg = args.next();
			
			int size = arg.getType().sizeof();

			argOffsets.put(allocatedStackSpace, arg);
			
			allocatedStackSpace += size;
		}
		
		long structPointerOffset;
		
		Type returnType = callee.getReturnType();
		AssemblyTarget returnValue;
		
		if(returnType.isStructLike()) {
			/* 
			 * allocate space for struct and arguments, pass address as (invisible) first argument
			 * 
			 * 	sub esp, <struct size>
			 * 	lea eax, [esp]
			 * 	sub esp, <required space>
			 * 	mov [esp], eax
			 * 	...
			 * 
			 * if the destination is already a struct, use its address directly instead
			 * 	
			 * 	lea eax, [<destination>]
			 * 	sub esp, <required space>
			 * 	mov [esp], eax
			 */
			
			returnValue = null;
			
			Type returnTypePointer = returnType.addressOf();
			
			long structSize = returnType.sizeof();
			structPointerOffset = returnTypePointer.sizeof();
			
			long alignTo = allocatedStackSpace + structPointerOffset;
			
			if(callerReturnDestination == null || !callerReturnDestination.isMemory()) {
				asm.add(
					X86InstructionKinds.SUB,
					X86Register.ESP,
					X86OperandHelper.constant(structSize)
				);
				
				asm.add(
					X86InstructionKinds.LEA,
					X86Register.EAX,
					X86MemoryTarget.of(
						returnTypePointer,
						X86Register.ESP
					)
				);
				
				alignTo += structSize;
			}
			else asm.add(
				X86InstructionKinds.LEA,
				X86Register.EAX,
				callerReturnDestination
			);
			
			allocatedStackSpace += structPointerOffset + alignStack(alignTo);
			
			asm.add(
				X86InstructionKinds.SUB,
				X86Register.ESP,
				X86OperandHelper.constant(allocatedStackSpace)
			);
			
			generator.assign(
				X86MemoryTarget.of(
					returnTypePointer,
					X86Register.ESP
				),
				X86Register.EAX
			);
		}
		else {
			returnValue = getReturnValue(returnType);
			structPointerOffset = 0;
			
			/*
			 * allocate space for arguments
			 * 
			 * 	sub esp, <required space>
			 */
			asm.add(
				X86InstructionKinds.SUB,
				X86Register.ESP,
				X86OperandHelper.constant(
					allocatedStackSpace += alignStack(allocatedStackSpace)
				)
			);
		}
		
		/*
		 * Push all arguments to the stack (reverse order):
		 * 
		 * 	mov [esp], <a>
		 * 	mov [esp+<offset b>], <b>
		 * 	mov [esp+<offset c>], <c>
		 * 	...
		 * 
		 * Stack is aligned to 16 bytes
		 */
		
		argOffsets.forEach(
			(argOffset, arg) -> generator.assign(
				X86MemoryTarget.ofDisplaced(
					arg.getType(),
					X86OperandHelper.constant(argOffset + structPointerOffset),
					X86Register.ESP
				),
				arg
			)
		);
		
		/**
		 * perform function call and free stack space
		 * 
		 * 	call function_name
		 * 	add esp, <required space>
		 */
		
		asm.add(X86InstructionKinds.CALL, functionTarget);
		
		asm.add(
			X86InstructionKinds.ADD,
			X86Register.ESP,
			X86OperandHelper.constant(allocatedStackSpace)
		);
		
		// structs are already written
		if(!returnType.isStructLike()) {
			/*
			 * assign return value or clear FPU stack:
			 * 
			 * 	fstp st(0)
			 */
			if(callerReturnDestination != null) {
				
				if(returnValue == X86Register.ST0)
					generator.fpuStackPush();
				
				generator.assign(callerReturnDestination, returnValue);
			}
			
			else if(returnValue == X86Register.ST0) {
				generator.fpuStackPush();
				generator.fpop();
			}
		}
		
		// restore callee-saved register and FPU stack
		asm.add(callStore.restore());
		
		for(VirtualStackTarget tmp : fpuStore)
			generator.fld(tmp);
	}
	
	@Override
	public void vaStart(BuiltinVaStart vaStart) {
		/*
		 * va_start(vp, arg):
		 * 
		 * 	lea <vp>, [<arg>]
		 */
		
		generator.assignDynamicRegister(
			vaStart.getVaList().getOperand(),
			vp -> {
				X86MemoryTarget target = parameters.get(vaStart.getParameter().name());
				
				asm.add(
					X86InstructionKinds.LEA,
					vp,
					generator.addOffset(target, target.getType().sizeof())
				);
			}
		);
	}
	
	@Override
	public void vaArg(Operand result, BuiltinVaArg vaArg) {
		/*
		 * val = va_arg(vp, type):
		 * 
		 * 	val = *((type *) vp);
		 * 	vp += sizeof(type);
		 * 
		 * 	mov <val>, [<vp>]
		 * 	add <vp>, <sizeof(type)>
		 */
		
		Type type = vaArg.getReturnType();
		
		AssemblyTarget vp = generator.generateOperand(
			vaArg.getVaList().getOperand()
		);
		
		generator.assign(
			result,
			X86MemoryTarget.of(
				type,
				generator.toRegister(vp)
			)
		);
		
		asm.add(
			X86InstructionKinds.ADD,
			vp,
			X86OperandHelper.constant(type.sizeof())
		);
	}
	
	@Override
	public void vaEnd(BuiltinVaEnd vaEnd) {
		// do nothing
	}
	
	/* align offset to 16-byte boundary */
	private static long alignStack(long offset) {
		return (16 - offset % 16) % 16;
	}

}
