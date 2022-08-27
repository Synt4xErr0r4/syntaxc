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

import static at.syntaxerror.syntaxc.generator.arch.x86.X86CodeGenerator.asm;

import java.util.Arrays;
import java.util.Map;

import at.syntaxerror.syntaxc.SyntaxCException;
import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.generator.asm.AssemblyInstruction;

/**
 * @author Thomas Kasper
 * 
 */
public class X86Assembly {

	private static final AssemblyInstruction RET = asm("ret");
	
	private static final Map<Integer, Character> SUFFIXES = Map.of(
		1, 'b',
		2, 'w',
		4, 'l',
		8, 'q'
	);
	
	public final X86Register RBP;
	public final X86Register RSP;

	public final X86Register RAX;
	public final X86Register RBX;
	public final X86Register RCX;
	public final X86Register RDX;
	
	public final X86Register RIP;
	
	public X86Assembly(BitSize bits) {
		
		if(bits == BitSize.B64) {
			
			RBP = X86Register.RBP;
			RSP = X86Register.RSP;
			
			RAX = X86Register.RAX;
			RBX = X86Register.RBX;
			RCX = X86Register.RCX;
			RDX = X86Register.RDX;

			RIP = X86Register.RIP;
			
		}
		else {

			RBP = X86Register.EBP;
			RSP = X86Register.ESP;
			
			RAX = X86Register.EAX;
			RBX = X86Register.EBX;
			RCX = X86Register.ECX;
			RDX = X86Register.EDX;
			
			RIP = X86Register.EIP;
			
		}
		
	}
	
	private char getSuffix(X86Register...regs) {
		int size = regs[0].getSize();
		
		for(X86Register reg : regs)
			if(reg.getSize() != size)
				throw new SyntaxCException("Register size mismatch: " + Arrays.toString(regs));
		
		return SUFFIXES.get(regs[0].getSize());
	}
	
	public AssemblyInstruction push(X86Register register) {
		return asm("push%c %s", getSuffix(register), register);
	}
	
	public AssemblyInstruction pop(X86Register register) {
		return asm("pop%c %s", getSuffix(register), register);
	}
	
	public AssemblyInstruction mov(X86Register a, X86Register b) {
		return asm("mov%c %s, %s", getSuffix(a, b), a, b);
	}
	
	public AssemblyInstruction ret() {
		return RET;
	}
	
}
