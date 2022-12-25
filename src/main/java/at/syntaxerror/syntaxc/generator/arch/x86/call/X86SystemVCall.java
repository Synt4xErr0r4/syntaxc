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
import java.util.List;

import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaArg;
import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaEnd;
import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaStart;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86AssemblyGenerator;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.type.FunctionType;

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
 *  - 
 *  
 * 
 * @author Thomas Kasper
 * 
 */
public class X86SystemVCall extends X86CallingConvention {

	public X86SystemVCall(FunctionType function, Instructions asm, X86AssemblyGenerator generator, List<SymbolObject> parameters) {
		super(function, asm, generator, parameters);
	}

	@Override
	public AssemblyTarget getReturnValue() {
		return null;
	}

	@Override
	public void onEntry() {
		
	}

	@Override
	public void onLeave() {
		
	}

	@Override
	public void call(AssemblyTarget functionTarget, FunctionType callee, Iterator<AssemblyTarget> args, AssemblyTarget destination) {
		
	}
	
	@Override
	public void vaStart(BuiltinVaStart vaStart) {
		
	}
	
	@Override
	public void vaArg(BuiltinVaArg vaArg) {
		
	}
	
	@Override
	public void vaEnd(BuiltinVaEnd vaEnd) {
		
	}

}
