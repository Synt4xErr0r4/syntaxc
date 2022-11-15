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
package at.syntaxerror.syntaxc.parser;

import static at.syntaxerror.syntaxc.parser.ExpressionParser.isNullPointer;
import static at.syntaxerror.syntaxc.parser.ExpressionParser.newArithmetic;
import static at.syntaxerror.syntaxc.parser.ExpressionParser.newBinary;
import static at.syntaxerror.syntaxc.parser.ExpressionParser.newNumber;
import static at.syntaxerror.syntaxc.parser.ExpressionParser.newPromote;

import java.math.BigInteger;

import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.parser.node.expression.BinaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;
import at.syntaxerror.syntaxc.type.TypeUtils;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class ExpressionChecker implements Logable {

	@Override
	public Position getPosition() {
		return null;
	}
	
	@Override
	public Warning getDefaultWarning() {
		return Warning.SEM_NONE;
	}
	
	public BinaryExpressionNode checkBitwise(Token op, ExpressionNode exprLeft, ExpressionNode exprRight, String name) {
		Type left = exprLeft.getType();
		Type right = exprRight.getType();
		
		if(!(left.isInteger() && right.isInteger()))
			error(
				op,
				"Expected integer operands for %s (got »%s« and »%s«)",
				name,
				left,
				right
			);
		
		return newBinary(
			op,
			newPromote(exprLeft),
			newPromote(exprRight),
			op.getPunctuator()
		);
	}
	
	public BinaryExpressionNode checkLogical(Token op, ExpressionNode exprLeft, ExpressionNode exprRight, String name) {
		Type left = exprLeft.getType();
		Type right = exprRight.getType();
		
		if(!(left.isScalar() && right.isScalar()))
			error(
				op,
				"Expected number or pointer operands for %s (got »%s« and »%s«)",
				name,
				left,
				right
			);
		
		return newBinary(
			op,
			exprLeft,
			exprRight,
			op.getPunctuator(),
			Type.INT
		);
	}
	
	public BinaryExpressionNode checkArithmetic(Token op, ExpressionNode exprLeft, ExpressionNode exprRight, String name) {
		Type left = exprLeft.getType();
		Type right = exprRight.getType();
		
		if(!(left.isArithmetic() && right.isArithmetic()))
			error(
				op,
				"Expected number operands for %s (got »%s« and »%s«)",
				name,
				left,
				right
			);
		
		Punctuator punct = op.getPunctuator();
		
		if(punct == Punctuator.POINTER || punct == Punctuator.INDIRECTION)
			punct = Punctuator.MULTIPLY;
		
		return newArithmetic(
			op,
			exprLeft,
			exprRight,
			punct
		);
	}
	
	public BinaryExpressionNode checkModulo(Token op, ExpressionNode exprLeft, ExpressionNode exprRight) {
		Type left = exprLeft.getType();
		Type right = exprRight.getType();
		
		if(!(left.isInteger() && right.isInteger()))
			error(
				op,
				"Expected integer operands for modulo (got »%s« and »%s«)",
				left,
				right
			);

		return newArithmetic(
			op,
			exprLeft,
			exprRight,
			op.getPunctuator()
		);
	}
	
	public BinaryExpressionNode checkAddition(Token op, ExpressionNode exprLeft, ExpressionNode exprRight) {
		Type left = exprLeft.getType();
		Type right = exprRight.getType();
		
		if(left.isPointerLike() || right.isPointerLike()) { // pointer + offset
			
			if(!left.isInteger() && !right.isInteger())
				error(
					op,
					"Expected pointer and integer operands for addition (got »%s« and »%s«)",
					left,
					right
				);

		}
		
		else if(!left.isArithmetic() || !right.isArithmetic())
			error(
				op,
				"Expected number or pointer operands for addition (got »%s« and »%s«)",
				left,
				right
			);
		
		return newArithmetic( // x + y
			op,
			exprLeft,
			exprRight,
			Punctuator.ADD
		);
	}
	
	public BinaryExpressionNode checkSubtraction(Token op, ExpressionNode exprLeft, ExpressionNode exprRight) {
		Type left = exprLeft.getType();
		Type right = exprRight.getType();
		
		if(left.isPointerLike()) {
		
			if(right.isPointerLike()) { // pointer - pointer
				if(!TypeUtils.isEqual(left, right))
					error(
						op,
						"Expected compatible pointer operands for subtraction (got »%s« and »%s«)",
						left,
						right
					);

				Type type = NumericValueType.PTRDIFF.asType();
				
				return newBinary( // ((char *) x - (char *) y) / sizeof *x
					op,
					newBinary( // (char *) x - (char *) y
						op,
						exprLeft,
						exprRight,
						Punctuator.SUBTRACT,
						type
					),
					newNumber( // sizeof *x
						op.getPosition(),
						BigInteger.valueOf(left.dereference().sizeof()),
						type
					),
					Punctuator.DIVIDE
				);
			}
			
			else if(!right.isInteger())
				error(
					op,
					"Expected pointer and integer operands for subtraction (got »%s« and »%s«)",
					left,
					right
				);

		}
		
		else if(!left.isArithmetic() || !right.isArithmetic())
			error(
				op,
				"Expected number or pointer operands for addition (got »%s« and »%s«)",
				left,
				right
			);

		return newArithmetic( // x - y
			op,
			exprLeft,
			exprRight,
			Punctuator.SUBTRACT
		);
	}
	
	public BinaryExpressionNode checkComparison(Token op, ExpressionNode exprLeft, ExpressionNode exprRight) {
		Type left = exprLeft.getType();
		Type right = exprRight.getType();
		
		if(left.isPointerLike() || right.isPointerLike()) {
			
			if(!(left.isPointerLike() && right.isPointerLike()))
				error(
					op,
					"Expected pointer operands for comparison (got »%s« and »%s«)",
					left,
					right
				);
			
			if(!TypeUtils.isVoidPointer(left)
				&& !TypeUtils.isVoidPointer(right)
				&& !TypeUtils.isEqual(left, right))
				warn(Warning.INCOMPATIBLE_POINTERS, "Comparison of different pointer types");
			
			return newBinary(
				op,
				exprLeft,
				exprRight,
				op.getPunctuator(),
				Type.INT
			);
		}
		
		else if(!(left.isArithmetic() && right.isArithmetic()))
			error(
				op,
				"Expected number operands for relational operator (got »%s« and »%s«)",
				left,
				right
			);
		
		return newArithmetic( // perform usual arithmetic conversion
			op,
			exprLeft,
			exprRight,
			op.getPunctuator(),
			Type.INT
		);
	}
	
	public BinaryExpressionNode checkAssignment(Token op, ExpressionNode exprLeft, ExpressionNode exprRight, boolean isReturn) {
		Type left = exprLeft.getType();
		Type right = exprRight.getType();
		
		do {
			if(left.isArray())
				error(op, "Cannot assign to array type");
			
			if(left.isArithmetic() && right.isArithmetic())
				break;
			
			if(left.isPointer() && right.isPointer()) {
	
				Type lbase = left.dereference();
				Type rbase = right.dereference();
				
				if(!lbase.isConst() && rbase.isConst())
					error(
						op,
						"%s discards »const« qualifier from target type",
						isReturn ? "»return«" : "Assignment"
					);
				
				if(!lbase.isVolatile() && rbase.isVolatile())
					error(
						op,
						"%s discards »volatile« qualifier from target type",
						isReturn ? "»return«" : "Assignment"
					);
				
				if(TypeUtils.isVoidPointer(left) || TypeUtils.isVoidPointer(right) || isNullPointer(exprRight))
					break;
				
			}
			
			if(TypeUtils.isEqual(left, right))
				break;
			
			error(
				op,
				"Incompatible types for %s (got »%s« and »%s«)",
				isReturn ? "»return«" : "assignment",
				left,
				right
			);
		} while(false);
		
		return newBinary(
			op,
			exprLeft,
			exprRight,
			Punctuator.ASSIGN,
			left.unqualified()
		);
	}
}
