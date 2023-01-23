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
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.type.FunctionType;
import at.syntaxerror.syntaxc.type.FunctionType.Parameter;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public class BuiltinVaStart extends BuiltinFunction {
	
	private ExpressionArgument vaList;
	private Parameter parameter;
	private FunctionType type;
	
	public BuiltinVaStart() {
		super("va_start");
	}
	
	@Override
	public void populate(BuiltinContext context) {
		if(!context.isInsideFunction())
			context.error(Warnings.SEM_NONE, "Cannot call »__builtin_va_start« outside of a function");
		
		ExpressionArgument expr = context.nextExpression();
		IdentifierArgument param = context.nextIdentifier();
		
		context.ensureClosed();
		
		VariadicUtils.ensureVaListType(expr, expr.getType());
		ExpressionNode.markUsed(expr.getExpression());
		
		String paramName = param.identifier().getString();
		
		type = context.getEnclosingFunction();
		parameter = null;
		
		VariadicUtils.ensureVariadic(context, type, "__builtin_va_start");
		
		SymbolObject object = context.getParser().getSymbolTable().findObject(paramName);
		
		if(object == null)
			context.error(param, "Parameter name does not exist within this function");
		
		if(!object.isParameter())
			context.error(param, "Identifier does not denote a function parameter");
		
		var params = type.getParameters();
		
		parameter = params.get(params.size() - 1);
		
		if(!parameter.name().equals(paramName)) {
			parameter = params
				.stream()
				.filter(p -> p.name().equals(paramName))
				.findFirst()
				.orElse(null);
			
			if(parameter != null)
				context.warn(param, Warnings.VARARGS, "Parameter supplied to __builtin_va_start is not the last function parameter");
		}
		
		args.add(vaList = expr);
		args.add(param);
	}
	
}
