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
package at.syntaxerror.syntaxc.generator.arch.x86;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.generator.asm.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyInteger;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyLabel;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.misc.Flag;

/**
 * @author Thomas Kasper
 * 
 */
public class X86Assembly {

	private static final AssemblyInstruction RET = () -> "\tret";
	
	private static final Map<Integer, Character> SUFFIXES = Map.of(
		1, 'b',
		2, 'w',
		4, 'l',
		8, 'q',
		10, '?',
		16, '?'
	);
	
	public final List<X86Register> registers;
	
	public final X86Register RBP;
	public final X86Register RSP;
	public final X86Register RIP;

	public final X86Register RAX;
	public final X86Register RBX;
	public final X86Register RCX;
	public final X86Register RDX;

	public final X86Register RSI;
	public final X86Register RDI;
	
	public final boolean intelSyntax;
	public final boolean bit32;
	
	@SuppressWarnings("unchecked")
	public X86Assembly(boolean intelSyntax, BitSize bits) {
		this.intelSyntax = intelSyntax;
		
		if(!Flag.LONG_DOUBLE.isEnabled())
			X86Register.disable(X86Register.GROUP_ST);
		
		if(bit32 = bits != BitSize.B64) {
			
			X86Register.disable(
				X86Register.RAX,
				X86Register.RBX,
				X86Register.RCX,
				X86Register.RDX,
				X86Register.RSI,
				X86Register.RDI,
				X86Register.RBP,
				X86Register.RSP,
				X86Register.RIP
			);
			
			X86Register.disable(
				X86Register.GROUP_R8,
				X86Register.GROUP_R9,
				X86Register.GROUP_R10,
				X86Register.GROUP_R11,
				X86Register.GROUP_R12,
				X86Register.GROUP_R13,
				X86Register.GROUP_R14,
				X86Register.GROUP_R15
			);

			RBP = X86Register.EBP;
			RSP = X86Register.ESP;
			RIP = X86Register.EIP;

			RAX = X86Register.EAX;
			RBX = X86Register.EBX;
			RCX = X86Register.ECX;
			RDX = X86Register.EDX;

			RSI = X86Register.ESI;
			RDI = X86Register.EDI;
			
		}
		else {

			RBP = X86Register.RBP;
			RSP = X86Register.RSP;
			RIP = X86Register.RIP;

			RAX = X86Register.RAX;
			RBX = X86Register.RBX;
			RCX = X86Register.RCX;
			RDX = X86Register.RDX;

			RSI = X86Register.RSI;
			RDI = X86Register.RDI;
			
		}
		
		registers = Stream.of(
			X86Register.GROUP_R15,
			X86Register.GROUP_R14,
			X86Register.GROUP_R13,
			X86Register.GROUP_R12,
			X86Register.GROUP_R11,
			X86Register.GROUP_R10,
			X86Register.GROUP_B,
			X86Register.GROUP_R9,
			X86Register.GROUP_R8,
			X86Register.GROUP_C,
			X86Register.GROUP_D,
			X86Register.GROUP_SI,
			X86Register.GROUP_DI,
			X86Register.GROUP_A,
			X86Register.GROUP_XMM,
			X86Register.GROUP_ST
		).collect(
			ArrayList::new,
			ArrayList::addAll,
			ArrayList::addAll
		);
	}

	private char getSuffix(int size) {
		return SUFFIXES.get(size);
	}
	
	private char getSuffix(X86Register reg) {
		return getSuffix(reg.getSize());
	}
	
	private String toString(AssemblyTarget target) {
		if(target instanceof AssemblyLabel label)
			return label.getName();
		
		if(target instanceof AssemblyInteger integer)
			return integer.getValue().toString();
		
		return target.toString();
	}
	
	private AssemblyInstruction asm(String base, Object...args) {
		String instruction = "\t" + base;
		
		if(!intelSyntax)
			for(Object arg : args)
				if(arg instanceof X86Register reg)
					instruction += getSuffix(reg);
		
		List<String> strArgs = Stream.of(args)
			.map(obj -> {
				
				if(!intelSyntax) {
					
					if(obj instanceof X86Register)
						return "%" + obj;
					
					if(obj instanceof Integer)
						return "$" + obj;
					
					if(obj instanceof AssemblyInteger asm)
						return "$" + toString(asm);
					
				}
				
				if(obj instanceof AssemblyTarget target)
					return toString(target);
				
				if(obj == null)
					return "<null>";
				
				return obj.toString();
			})
			.collect(
				ArrayList::new,
				ArrayList::add,
				ArrayList::addAll
			);
		
		if(!intelSyntax) // AT&T syntax reverses instruction operands
			Collections.reverse(strArgs);
		
		instruction += " " + String.join(", ", strArgs);
		
		final String asm = instruction;
		return () -> asm;
	}
	
	public AssemblyInstruction push(AssemblyTarget register) {
		return asm("push", register);
	}
	
	public AssemblyInstruction pop(AssemblyTarget register) {
		return asm("pop", register);
	}
	
	public AssemblyInstruction mov(AssemblyTarget dst, AssemblyTarget src) {
		if(dst == src)
			return null;
		
		return asm("mov", dst, src);
	}
	
	public AssemblyInstruction mov(AssemblyTarget dst, int src) {
		return asm("mov", dst, src);
	}

	public AssemblyInstruction test(AssemblyTarget register) {
		return asm("test", register, register);
	}

	public AssemblyInstruction je(String label) {
		return () -> "\tje " + label;
	}

	public AssemblyInstruction jmp(String label) {
		return () -> "\tjmp " + label;
	}

	public AssemblyInstruction call(AssemblyTarget target) {
		return asm("call", target);
	}
	
	public AssemblyInstruction ret() {
		return RET;
	}

	public AssemblyInstruction add(AssemblyTarget dst, AssemblyTarget src) {
		return asm("add", dst, src);
	}

	public AssemblyInstruction add(AssemblyTarget dst, long value) {
		return asm("add", dst, value);
	}

	public AssemblyInstruction addss(AssemblyTarget dst, AssemblyTarget src) {
		return asm("addss", dst, src);
	}
	
	public AssemblyInstruction addsd(AssemblyTarget dst, AssemblyTarget src) {
		return asm("addsd", dst, src);
	}
	
}
