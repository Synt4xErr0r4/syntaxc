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
import at.syntaxerror.syntaxc.intermediate.representation.CallIntermediate.CallParameterIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.CallIntermediate.CallStartIntermediate;
import at.syntaxerror.syntaxc.type.FunctionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public abstract class X86CallingConvention<CONTEXT> {
	
	@Getter
	protected final FunctionType function;
	protected final Instructions asm;
	protected final X86AssemblyGenerator generator;
	
	public abstract AssemblyTarget getParameter(String name);
	
	public void onEntry() { }
	public void onLeave() { }
	
	public CONTEXT createCallingContext(CallStartIntermediate call) { return null; }
	public void passParameter(CONTEXT ctx, CallParameterIntermediate call) { }
	public void call(CONTEXT ctx, AssemblyTarget function, Operand destination) { }

	@SuppressWarnings("unchecked")
	public void passParameterUnchecked(Object ctx, CallParameterIntermediate call) {
		passParameter((CONTEXT) ctx, call);
	}

	@SuppressWarnings("unchecked")
	public void callUnchecked(Object ctx, AssemblyTarget function, Operand destination) {
		call((CONTEXT) ctx, function, destination);
	}
	
	public abstract AssemblyTarget getReturnValue();
	
	public abstract void vaStart(BuiltinVaStart vaStart);
	public abstract void vaArg(Operand result, BuiltinVaArg vaArg);
	public abstract void vaEnd(BuiltinVaEnd vaEnd);
	
}
