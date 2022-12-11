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

import static at.syntaxerror.syntaxc.parser.helper.ExpressionHelper.newBinary;
import static at.syntaxerror.syntaxc.parser.helper.ExpressionHelper.newComma;

import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public class AssignmentHelper {

	private final ExpressionChecker checker = new ExpressionChecker();
	
	public ExpressionNode newAssignment(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator operation) {
		if(left instanceof VariableExpressionNode)
			return newSimpleAssignment(pos, left, right, operation);
		
		return newComplexAssignment(pos, left, right, operation);
	}
	
	/*
	 * An assignment 'x op= y' is equivalent to
	 * 
	 *   x = x op y
	 *   
	 * if 'x' has no side effects
	 */
	private ExpressionNode newSimpleAssignment(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator operator) {
		Type leftType = left.getType();
		
		if(left instanceof VariableExpressionNode var && var.getVariable().isLocalVariable())
			var.getVariable().setInitialized(true);
		
		return newBinary( // x = x op y
			pos,
			left,
			newBinary( // x op y
				pos,
				left,
				right,
				operator
			),
			Punctuator.ASSIGN,
			leftType.unqualified()
		);
	}
	
	/*
	 * An assignment 'x op= y' is equivalent to
	 * 
	 *   z = &x;
	 *   *z = *z op y
	 *   
	 * if 'x' has side effects
	 */
	private ExpressionNode newComplexAssignment(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator operation) {
		Type leftType = left.getType();
		Type leftAddr = leftType.addressOf();
		
		SymbolObject sym = SymbolObject.temporary(
			pos.getPosition(),
			leftAddr
		);
		
		// z (temporary variable)
		VariableExpressionNode var = new VariableExpressionNode(
			pos.getPosition(),
			sym
		);
		
		ExpressionNode deref = PointerHelper.dereference(pos, var); // *z
		
		return newComma( // z = &x, *z = *z op y
			pos,
			checker.checkAssignment( // z = &x
				pos,
				var,
				PointerHelper.addressOf(pos, left), // &x
				false
			),
			checker.checkAssignment( // *z = *z op y
				pos,
				deref,
				newBinary( // *z op y
					pos,
					deref,
					right,
					operation
				),
				false
			)
		);
	}
	
}
