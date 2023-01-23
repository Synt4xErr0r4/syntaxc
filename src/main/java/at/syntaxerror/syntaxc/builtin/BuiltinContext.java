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
package at.syntaxerror.syntaxc.builtin;

import at.syntaxerror.syntaxc.builtin.BuiltinFunction.ExpressionArgument;
import at.syntaxerror.syntaxc.builtin.BuiltinFunction.IdentifierArgument;
import at.syntaxerror.syntaxc.builtin.BuiltinFunction.TypeArgument;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.optimizer.ExpressionOptimizer;
import at.syntaxerror.syntaxc.parser.ExpressionParser;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.FunctionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class BuiltinContext implements Logable {

	@Getter
	private final FunctionType enclosingFunction;
	
	@Delegate(types = Logable.class, excludes = Positioned.class)
	private final ExpressionParser expr;
	
	@Getter
	private final Position position;
	
	private int consumed = 0;
	private boolean closed = false;
	
	public ExpressionParser getParser() {
		return expr;
	}
	
	public boolean isInsideFunction() {
		return getEnclosingFunction() != null;
	}
	
	private void ensureNotExhausted() {
		if(!closed && consumed++ == 0)
			return;

		expr.consume(",");
	}
	
	/**
	 * Ensures that the function call was terminated with a closing parenthesis ')'
	 */
	public void ensureClosed() {
		if(closed)
			return;
		
		closed = true;
		
		expr.expect(")");
	}
	
	/**
	 * Treats the next function call argument as an expression and returns it
	 * 
	 * @return the next function argument as an expression
	 */
	public ExpressionArgument nextExpression() {
		ensureNotExhausted();
		
		ExpressionNode node = ExpressionOptimizer.optimize(expr.nextAssignment());
		expr.next();
		
		return new ExpressionArgument(node);
	}

	/**
	 * Treats the next function call argument as a type name and returns it
	 * 
	 * @return the next function argument as a type name
	 */
	public TypeArgument nextType() {
		ensureNotExhausted();
		return new TypeArgument(expr.getPosition(), expr.nextTypeName());
	}

	/**
	 * Treats the next function call argument as an identifier and returns it
	 * 
	 * @return the next function argument as an identifier
	 */
	public IdentifierArgument nextIdentifier() {
		ensureNotExhausted();
		
		Token tok = expr.expect(TokenType.IDENTIFIER);
		expr.next();
		
		return new IdentifierArgument(tok);
	}
	
}
