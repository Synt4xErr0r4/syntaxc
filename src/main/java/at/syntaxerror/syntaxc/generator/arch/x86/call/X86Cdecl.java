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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaArg;
import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaEnd;
import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaStart;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86AssemblyGenerator;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86OperandHelper;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86MemoryTarget;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualRegisterTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualStackTarget;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.type.FunctionType;
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

	private AssemblyTarget structPointer;
	
	public X86Cdecl(FunctionType function, Instructions asm, X86AssemblyGenerator generator, List<SymbolObject> parameters) {
		super(function, asm, generator, parameters);
		
		returnValue = getReturnValue(function.getReturnType());
	}
	
	private AssemblyTarget getReturnValue(Type returnType) {
		if(returnType.isVoid() || returnType.isInteger() || returnType.isPointerLike())
			return X86Register.EAX;
		
		if(returnType.isFloating())
			return X86Register.ST0;
		
		structPointer = new VirtualStackTarget(NumericValueType.POINTER.asType());
		
		return X86MemoryTarget.of(returnType, structPointer);
	}

	@Override
	public void onEntry() {
		function.getParameters().stream()
			.map(p -> p.name());
		
		// TODO
	}

	@Override
	public void onLeave() {
		// TODO
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
			
			AssemblyTarget address = new VirtualRegisterTarget(returnTypePointer);
			
			if(callerReturnDestination == null || !callerReturnDestination.isMemory()) {
				asm.add(
					X86InstructionKinds.SUB,
					X86Register.ESP,
					X86OperandHelper.constant(structSize)
				);
				
				asm.add(
					X86InstructionKinds.LEA,
					address,
					X86MemoryTarget.of(
						returnTypePointer,
						X86Register.ESP
					)
				);
				
				alignTo += structSize;
			}
			else asm.add(
				X86InstructionKinds.LEA,
				address,
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
				address
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
				X86MemoryTarget.of(
					arg.getType(),
					X86Register.ESP,
					X86OperandHelper.constant(argOffset + structPointerOffset)
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
		asm.add(X86InstructionKinds.ADD, X86Register.ESP, X86OperandHelper.constant(allocatedStackSpace));
		
		// structs are already written
		if(returnType.isStructLike())
			return;
		
		/*
		 * assign return value or clear FPU stack:
		 * 
		 * 	fstp st(0)
		 */
		if(callerReturnDestination != null)
			generator.assign(callerReturnDestination, returnValue);
		
		else if(returnValue == X86Register.ST0)
			asm.add(X86InstructionKinds.FSTP, X86Register.ST0);
	}
	
	@Override
	public void vaStart(BuiltinVaStart vaStart) {
		// TODO
	}
	
	@Override
	public void vaArg(BuiltinVaArg vaArg) {
		// TODO
	}
	
	@Override
	public void vaEnd(BuiltinVaEnd vaEnd) {
		// TODO
	}
	
	/* align offset to 16-byte boundary */
	private static long alignStack(long offset) {
		return (16 - offset % 16) % 16;
	}

}
