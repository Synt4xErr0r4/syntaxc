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
package at.syntaxerror.syntaxc.parser.helper;

import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.config.Warnings;
import at.syntaxerror.syntaxc.parser.node.expression.ArrayIndexExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.UnaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.Type;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PointerHelper implements Logable {

	private static final PointerHelper INSTANCE = new PointerHelper();
	
	public static ExpressionNode dereference(Positioned pos, ExpressionNode expr) {
		return INSTANCE.dereferenceImpl(pos, expr);
	}
	
	public static ExpressionNode addressOf(Positioned pos, ExpressionNode expr) {
		return INSTANCE.addressOfImpl(pos, expr);
	}
	
	@Override
	public Position getPosition() {
		return null;
	}
	
	@Override
	public Warnings getDefaultWarning() {
		return Warnings.SEM_NONE;
	}
	
	public ExpressionNode dereferenceImpl(Positioned pos, ExpressionNode expr) {
		Type type = expr.getType();
		
		if(type.isFunction()) // dereferencing a function doesn't do anything
			return expr;
		
		if(!type.isPointerLike())
			error(pos, "Expected pointer for indirection");
		
		type = type.dereference();
		
		if(type.isVoid())
			warn(pos, Warnings.DEREF_VOID, "Dereferencing of a pointer to »void«");
		
		if(type.isIncomplete())
			error(pos, "Cannot dereference pointer to incomplete type");
		
		return ExpressionHelper.newUnary(
			pos,
			expr,
			Punctuator.INDIRECTION,
			type
		);
	}

	public ExpressionNode addressOfImpl(Positioned pos, ExpressionNode expr) {
		Type type = expr.getType();
		
		if(type.isBitfield())
			error(pos, "Cannot take address of bit-field");
		
		else if(!((expr instanceof VariableExpressionNode var) && var.getVariable().isFunction())
			&& !expr.isLvalue())
			error(pos, "Cannot take address of rvalue");

		if(expr instanceof UnaryExpressionNode unop)
			switch(unop.getOperation()) {
			// INDIRECTION and MULTIPLY have the same symbol (*)
			case INDIRECTION:
			case MULTIPLY:
				/* convert '&*ptr' into 'ptr' */
				return unop.getTarget();
				
			default:
				break;
			}
		
		if(expr instanceof ArrayIndexExpressionNode idx)
			/* convert '&ptr[off]' into 'ptr + off' */
			return ExpressionHelper.newBinary(
				expr,
				idx.getTarget(),
				idx.getIndex(),
				Punctuator.ADD,
				(idx.isSwapped()
					? idx.getIndex()
					: idx.getTarget())
					.getType()
			);
		
		return ExpressionHelper.newUnary(
			pos,
			expr,
			Punctuator.ADDRESS_OF,
			expr.getType().addressOf()
		);
	}
	
}
