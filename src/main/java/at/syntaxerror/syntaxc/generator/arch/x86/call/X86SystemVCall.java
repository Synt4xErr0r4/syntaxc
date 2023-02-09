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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaArg;
import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaEnd;
import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaStart;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86AssemblyGenerator;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.insn.StoreRegistersInstruction;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.intermediate.operand.Operand;
import at.syntaxerror.syntaxc.type.FunctionType;
import at.syntaxerror.syntaxc.type.FunctionType.Parameter;
import at.syntaxerror.syntaxc.type.StructType.Member;
import at.syntaxerror.syntaxc.type.Type;

/**
 * The default calling convention for 64-bit Linux
 * 
 *  - integers are returned in rax
 *  - floats and doubles are returned in xmm0
 *  - long doubles are returned in st(0)
 *  - returning structs/unions
 *  - parameter passing:
 *    - the first n call arguments are passed in the following registers:
 *      1. 
 *      2. 
 *      3. 
 *      4.
 *      - rdi, rsi, rdx, rcx, r8, r9: (signed and unsigned) char, short, int, long, type_t*
 *      - xmm0-7: float and double
 *      - st(i): long double
 *    - structs/unions:
 *      - if the size exceeds 4 qwords, they are passed via the stack
 *      - if the size exceeds 1 qword, each qword is classified separately:
 *        - TODO
 *        - if there is no register left for at least one qword, the whole argument is passed on the stack
 *    - all other arguments are pushed to the stack
 *      - the rightmost argument is pushed first (right-to-left)
 *      - structs/unions are padded to be a multiple of 4 bytes
 *      - the stack is callee-cleaned
 *    - for variadic function, al specifies the number of vector registers used
 *  - TODO
 * 
 * @author Thomas Kasper
 * 
 */
public class X86SystemVCall extends X86CallingConvention {

	private boolean hasRegisterSaveArea;

	private StoreRegistersInstruction registerStore, callStore;
	
	public X86SystemVCall(FunctionType function, Instructions asm, X86AssemblyGenerator generator) {
		super(function, asm, generator);
	}
	
	private void createRegisterSaveArea() {
		if(hasRegisterSaveArea)
			return;
		
		hasRegisterSaveArea = true;
		
		/*
		 * https://www.uclibc.org/docs/psABI-x86_64.pdf
		 * Figure 3.33
		 * 
		 * save registers to the register save area:
		 * 
		 * 	register	| offset
		 * -------------+----------
		 * 	rdi			| 0
		 * 	rsi			| 8
		 * 	rdx			| 16
		 * 	rcx			| 24
		 * 	r8			| 32
		 * 	r9			| 40
		 * 	xmm0		| 48
		 * 	xmm1		| 64
		 * 	...			| ...
		 * 	xmm15		| 288
		 * 
		 * 	xmm0-15 are only saved if rax != 0
		 * 
		 */
	}

	@Override
	public AssemblyTarget getReturnValue() {
		return X86Register.RAX;
	}
	
	@Override
	public AssemblyTarget getParameter(String name) {
		return X86Register.RAX;
	}

	@Override
	public void onEntry() {

		// r12, r13, r14, and r15 belong to the caller
		asm.add(registerStore = new StoreRegistersInstruction(
			asm,
			X86Register.R12,
			X86Register.R13,
			X86Register.R14,
			X86Register.R15
		));
	}

	@Override
	public void onLeave() {
		asm.add(registerStore.restore());
	}

	@Override
	public void call(AssemblyTarget functionTarget, FunctionType callee, Iterator<AssemblyTarget> args, AssemblyTarget destination) {
		
		classifyParameters(callee);
		
	}
	
	@Override
	public void vaStart(BuiltinVaStart vaStart) {
		
	}
	
	@Override
	public void vaArg(Operand result, BuiltinVaArg vaArg) {
		
	}
	
	@Override
	public void vaEnd(BuiltinVaEnd vaEnd) {
		
	}
	
	private void classifyParameters(FunctionType function) {
		classifyParameters(
			function.getParameters()
				.stream()
				.map(Parameter::type)
				.toList()
		);
	}

	private void classifyParameters(List<Type> types) {
		
		types.stream()
			.map(this::classifyParameter)
			.toList();
		
	}

	private Classification classifyParameter(Type type) {
		if(type.isInteger() || type.isPointerLike())
			return Classification.INTEGER;
		
		if(type.isFloating())
			return type == Type.LDOUBLE
				? Classification.X87
				: Classification.SSE;
		
		int size = Math.ceilDiv(type.sizeof(), 8);
		
		if(size > 4)
			return Classification.MEMORY;
		
		List<Member> members = type.toStructLike().getMembers();
		
		Map<Integer, Set<Classification>> eightbytes = new HashMap<>();
		
		for(Member member : members) {
			int from = member.getOffset();
			int to = from + member.sizeof() - 1;
			
			from >>= 3;
			to >>= 3;
			
			Classification classification = classifyParameter(member.getType());
			
			if(classification == Classification.MEMORY || classification == Classification.X87)
				return Classification.MEMORY;
			
			for(int i = from; i <= to; ++i)
				eightbytes.computeIfAbsent(i, j -> new HashSet<>())
					.add(classification);
		}
		
		List<Classification> classifications = new ArrayList<>();
		
		for(int i = 0; i < size; ++i) {
			
			if(!eightbytes.containsKey(i)) {
				classifications.add(Classification.NO_CLASS);
				continue;
			}
			
			Set<Classification> eightbyte = eightbytes.get(i);
			
			if(eightbyte.contains(Classification.INTEGER))
				classifications.add(Classification.INTEGER);
			
			else {
				eightbyte.remove(Classification.NO_CLASS);
				
				if(eightbyte.isEmpty())
					classifications.add(Classification.SSE);
				
				else classifications.add(eightbyte.iterator().next());
			}
		}
		
		if(classifications.isEmpty())
			return Classification.MEMORY;
		
		if(size > 2 && classifications.get(0) != Classification.SSE)
			return Classification.MEMORY;
		
		System.out.println(classifications);
		
		return Classification.NO_CLASS;
	}
	
	private static enum Classification {
		
		INTEGER,
		SSE,
		X87,
		NO_CLASS,
		MEMORY
		
	}
	
}
