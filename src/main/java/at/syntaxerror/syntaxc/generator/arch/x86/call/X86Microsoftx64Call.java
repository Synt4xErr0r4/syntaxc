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

import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaArg;
import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaEnd;
import at.syntaxerror.syntaxc.builtin.impl.BuiltinVaStart;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86AssemblyGenerator;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.intermediate.operand.Operand;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.type.FunctionType;

/**
 * The default calling convention for 64-bit Windows
 * 
 * @author Thomas Kasper
 * 
 */
public class X86Microsoftx64Call extends X86CallingConvention<Void> {

	public X86Microsoftx64Call(FunctionType function, Instructions asm, X86AssemblyGenerator generator) {
		super(function, asm, generator);
		Logger.error("Not implemented yet (Microsoft x64 calling convention)");
	}

	@Override
	public AssemblyTarget getReturnValue() {
		return null;
	}

	@Override
	public AssemblyTarget getParameter(String name) {
		return null;
	}
	
	@Override
	public void onEntry() {
		
	}

	@Override
	public void onLeave() {
		
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

}
