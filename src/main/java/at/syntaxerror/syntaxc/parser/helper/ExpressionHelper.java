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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.parser.ConstantExpressionEvaluator;
import at.syntaxerror.syntaxc.parser.node.expression.BinaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CastExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.NumberLiteralExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.UnaryExpressionNode;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.Type;
import at.syntaxerror.syntaxc.type.TypeUtils;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class ExpressionHelper {
	
	public static NumberLiteralExpressionNode newNumber(Positioned pos, Number value, Type type) {
		return new NumberLiteralExpressionNode(pos.getPosition(), value, type);
	}
	
	public static ExpressionNode newPromote(ExpressionNode expr) {
		Type promoted = TypeUtils.promoteInteger(expr.getType());
		
		return newCast(expr, expr, promoted);
	}
	
	public static ExpressionNode newCast(Positioned pos, ExpressionNode expr, Type type) {
		Type current = expr.getType();
		
		if(TypeUtils.isCompatible(current, type))
			return expr;
		
		return new CastExpressionNode(pos.getPosition(), expr, type);
	}
	
	public static UnaryExpressionNode newUnary(Positioned pos, ExpressionNode expr, Punctuator op) {
		return newUnary(pos, expr, op, expr.getType());
	}

	public static UnaryExpressionNode newUnary(Positioned pos, ExpressionNode expr, Punctuator op, Type type) {
		return new UnaryExpressionNode(pos.getPosition(), expr, op, type);
	}

	public static BinaryExpressionNode newArithmetic(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator op) {
		return newArithmetic(pos, left, right, op, null);
	}

	public static BinaryExpressionNode newArithmetic(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator op, Type type) {
		Type lType = left.getType();
		Type rType = right.getType();
		
		Type usual = TypeUtils.convertUsualArithmetic(lType, rType);
		
		if(left instanceof NumberLiteralExpressionNode lit)
			left = adjustLiteral(usual, lit);

		if(right instanceof NumberLiteralExpressionNode lit)
			right = adjustLiteral(usual, lit);

		left = newCast(pos, left, usual);
		right = newCast(pos, right, usual);

		return newBinary(pos, left, right, op, Objects.requireNonNullElse(type, usual));
	}

	private static NumberLiteralExpressionNode adjustLiteral(Type newType, NumberLiteralExpressionNode literal) {

		Type oldType = literal.getType();

		boolean oldFloat = oldType.isFloating();
		boolean newFloat = newType.isFloating();
		
		if(oldFloat && !newFloat)
			return literal;
		
		if(!newType.toNumber().getNumericType().inRange(literal.getLiteral()))
			return literal;
		
		Number value = literal.getLiteral();
		
		if(!oldFloat && newFloat)
			value = new BigDecimal((BigInteger) value);

		return new NumberLiteralExpressionNode(
			literal.getPosition(),
			value,
			newType
		);
	}
	
	public static BinaryExpressionNode newBinary(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator op) {
		return newBinary(pos, left, right, op, left.getType());
	}

	public static BinaryExpressionNode newBinary(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator op, Type type) {
		return new BinaryExpressionNode(pos.getPosition(), left, right, op, type);
	}

	public static BinaryExpressionNode newComma(Positioned pos, ExpressionNode left, ExpressionNode right) {
		return newBinary(pos, left, right, Punctuator.COMMA, right.getType());
	}
	
	public static boolean isNullPointer(ExpressionNode expr) {
		return ConstantExpressionEvaluator.isConstant(expr)
			&& ConstantExpressionEvaluator.evalInteger(expr).compareTo(BigInteger.ZERO) == 0;
	}
	
}
