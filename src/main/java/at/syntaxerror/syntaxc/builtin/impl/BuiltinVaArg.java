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
import at.syntaxerror.syntaxc.misc.config.Warnings;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
public class BuiltinVaArg extends BuiltinFunction {

	@Getter
	private ExpressionArgument vaList;
	
	public BuiltinVaArg() {
		super("va_arg");
	}
	
	@Override
	public void populate(BuiltinContext context) {
		if(!context.isInsideFunction())
			context.error(Warnings.SEM_NONE, "Cannot call »__builtin_va_arg« outside of a function");
		
		ExpressionArgument expr = context.nextExpression();
		TypeArgument type = context.nextType();
		
		context.ensureClosed();
		
		VariadicUtils.ensureVaListType(expr, expr.getType());
		ExpressionNode.markUsed(expr.getExpression());
		
		returnType = type.type();
		
		args.add(vaList = expr);
		args.add(type);
	}
	
}
