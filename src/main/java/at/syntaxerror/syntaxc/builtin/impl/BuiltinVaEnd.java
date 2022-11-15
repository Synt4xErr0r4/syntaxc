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
package at.syntaxerror.syntaxc.builtin.impl;

import at.syntaxerror.syntaxc.builtin.BuiltinContext;
import at.syntaxerror.syntaxc.builtin.BuiltinFunction;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.type.FunctionType;

/**
 * @author Thomas Kasper
 * 
 */
public class BuiltinVaEnd extends BuiltinFunction {

	public BuiltinVaEnd() {
		super("va_end");
	}
	
	@Override
	public void populate(BuiltinContext context) {
		if(!context.isInsideFunction())
			context.error(Warning.SEM_NONE, "Cannot call »__builtin_va_end« outside of a function");
		
		ExpressionArgument expr = context.nextExpression();
		
		context.ensureClosed();
		
		VariadicUtils.ensureVaListType(expr, expr.getType());
		ExpressionNode.markUsed(expr.getExpression());
		
		FunctionType type = context.getEnclosingFunction();
		
		VariadicUtils.ensureVariadic(context, type, "__builtin_va_end");

		args.add(expr);
	}
	
}
