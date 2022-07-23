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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.parser.node.expression.BinaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CastExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CommaExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ConditionalExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.NumberLiteralExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.UnaryExpressionNode;
import at.syntaxerror.syntaxc.symtab.global.AddressInitializer;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.NumberType;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public class ConstantExpressionEvaluator implements Logable {

	private static final Map<Punctuator, Pair<MathFunction<BigInteger>, MathFunction<BigDecimal>>> ARITHMETIC_OPERATIONS = Map.of(
		Punctuator.ADD,				Pair.of(BigInteger::add,		BigDecimal::add),
		Punctuator.SUBTRACT,		Pair.of(BigInteger::subtract,	BigDecimal::subtract),
		Punctuator.MULTIPLY,		Pair.of(BigInteger::multiply,	BigDecimal::multiply),
		Punctuator.EQUAL,			comparison(i -> i == 0),
		Punctuator.NOT_EQUAL,		comparison(i -> i != 0),
		Punctuator.LESS,			comparison(i -> i < 0),
		Punctuator.LESS_EQUAL,		comparison(i -> i <= 0),
		Punctuator.GREATER,			comparison(i -> i > 0),
		Punctuator.GREATER_EQUAL,	comparison(i -> i >= 0)
	);
	
	private static final Map<Punctuator, MathFunction<BigInteger>> BITWISE_OPERATIONS = Map.of(
		Punctuator.BITWISE_AND,		BigInteger::and,
		Punctuator.BITWISE_XOR,		BigInteger::xor,
		Punctuator.BITWISE_OR,		BigInteger::or,
		Punctuator.LSHIFT,			(a, b) -> a.shiftLeft(b.intValue()),
		Punctuator.RSHIFT,			(a, b) -> a.shiftRight(b.intValue())
	);
	
	private static final Map<Punctuator, Pair<MathFunction<BigInteger>, MathFunction<BigDecimal>>> DIVISION_OPERATIONS = Map.of(
		Punctuator.DIVIDE,			Pair.of(BigInteger::divide,	(a, b) -> a.divide(b, RoundingMode.HALF_EVEN)),
		Punctuator.MODULO,			Pair.of(BigInteger::mod,	(a, b) -> { throw new IllegalArgumentException(); })
	);
	
	private static final Map<Punctuator, LogicalFunction> LOGICAL_OPERATIONS = Map.of(
		Punctuator.LOGICAL_AND,	(a, b) -> a && b.get(),
		Punctuator.LOGICAL_OR,	(a, b) -> a || b.get()
	);
	
	private static Pair<MathFunction<BigInteger>, MathFunction<BigDecimal>> comparison(Predicate<Integer> predicate) {
		return Pair.of(
			(a, b) -> intBool(predicate.test(a.compareTo(b))),
			(a, b) -> decBool(predicate.test(a.compareTo(b)))
		);
	}
	
	private static BigInteger intBool(boolean value) {
		return value ? BigInteger.ONE : BigInteger.ZERO;
	}

	private static BigDecimal decBool(boolean value) {
		return value ? BigDecimal.ONE : BigDecimal.ZERO;
	}
	
	private static BigDecimal toBigDecimal(Number value) {
		return value instanceof BigInteger bigint
			? new BigDecimal(bigint)
			: (BigDecimal) value;
	}
	
	private BigInteger performOperation(Positioned pos, Punctuator punct, BigInteger left, BigInteger right) {
		if(BITWISE_OPERATIONS.containsKey(punct)) {
			if((punct == Punctuator.LSHIFT || punct == Punctuator.RSHIFT) && right.compareTo(BigInteger.ZERO) < 0) {
				warn(pos, Warning.NEGATIVE_SHIFT, "Shift count is negative");
				return BigInteger.ZERO;
			}
			
			return BITWISE_OPERATIONS.get(punct).calculate(left, right);
		}
		
		if(DIVISION_OPERATIONS.containsKey(punct)) {
			if(isZero(right))
				error(pos, "Division by zero");
			
			return DIVISION_OPERATIONS.get(punct).getFirst().calculate(left, right);
		}
		
		if(ARITHMETIC_OPERATIONS.containsKey(punct))
			return ARITHMETIC_OPERATIONS.get(punct).getFirst().calculate(left, right);
		
		return null;
	}

	private Number performOperation(Positioned pos, Punctuator punct, Number left, Number right) {
		if(left instanceof BigInteger l && right instanceof BigInteger r)
			return performOperation(pos, punct, l, r);

		BigDecimal l = toBigDecimal(left);
		BigDecimal r = toBigDecimal(right);
		
		if(BITWISE_OPERATIONS.containsKey(punct))
			error(pos, "Illegal operands for »%s«", punct);
		
		if(DIVISION_OPERATIONS.containsKey(punct)) {
			if(isZero(r))
				error(pos, "Division by zero");
			
			return DIVISION_OPERATIONS.get(punct).getSecond().calculate(l, r);
		}
		
		if(ARITHMETIC_OPERATIONS.containsKey(punct))
			return ARITHMETIC_OPERATIONS.get(punct).getSecond().calculate(l, r);
		
		return null;
	}

	private Number performLogical(Positioned pos, Punctuator punct, Number left, Supplier<Number> right, Type rightType) {
		LogicalFunction fun = LOGICAL_OPERATIONS.get(punct);
		
		if(left instanceof BigInteger l && rightType.isInteger())
			return fun.calculate(l, () -> (BigInteger) right.get());
		
		return fun.calculate(toBigDecimal(left), () -> toBigDecimal(right.get()));
	}
	
	@Override
	public Position getPosition() {
		return null;
	}
	
	@Override
	public Warning getDefaultWarning() {
		return Warning.SEM_NONE;
	}
	
	private static boolean isZero(BigInteger value) {
		return value.compareTo(BigInteger.ZERO) == 0;
	}
	
	private static boolean isZero(Number value) {
		return value instanceof BigDecimal dec
			? dec.compareTo(BigDecimal.ZERO) == 0
			: isZero((BigInteger) value);
	}
	
	private Number checkBounds(Number value, Type type) {
		NumericValueType numType = type.toNumber().getNumericType();
		
		if(numType.isFloating()) {
			BigDecimal bigdec = toBigDecimal(value);
			
			if(numType.inRange(bigdec))
				return bigdec;
			
			if(bigdec.compareTo((BigDecimal) numType.getMax()) > 0)
				return numType.getMax();
			
			return numType.getMin();
		}
		
		return numType.mask((BigInteger) value);
	}
	
	public BigInteger evalInteger(ExpressionNode expr) {
		if(expr instanceof NumberLiteralExpressionNode num) {
			if(num.getType().isFloating())
				error(num, "Illegal floating point number in constant expression");
			
			return (BigInteger) num.getLiteral();
		}
		
		if(expr instanceof BinaryExpressionNode binary) {
			BigInteger left = evalInteger(binary.getLeft());
			
			Punctuator op = binary.getOperation();
			
			if(op == Punctuator.LOGICAL_OR || op == Punctuator.LOGICAL_AND)
				return (BigInteger) performLogical(
					binary,
					op,
					left,
					() -> evalInteger(binary.getRight()),
					binary.getRight().getType()
				);
			
			Number val = performOperation(
				binary,
				op,
				left,
				evalInteger(binary.getRight())
			);
			
			if(val != null)
				return (BigInteger) checkBounds(val, binary.getType());
		}
		
		if(expr instanceof CastExpressionNode cast) {
			if(!cast.getType().isInteger())
				error(cast, "Cannot cast to non-integer type in constant expression");
			
			ExpressionNode target = cast.getTarget();
			
			Type type = target.getType();
			
			if(!type.isArithmetic())
				error(target, "Expected number for cast in constant expression");
			
			NumberType num = type.toNumber();
			
			BigInteger bigint = null;
			
			if(num.getNumericType().isFloating()) {
				if(target instanceof NumberLiteralExpressionNode lit)
					bigint = ((BigDecimal) lit.getLiteral()).toBigInteger();
				
				else error(target, "Floating point number must be immediate operand of cast in constant expression");
			}
			else bigint = evalInteger(target);
			
			return (BigInteger) checkBounds(bigint, cast.getType());
		}
		
		if(expr instanceof CommaExpressionNode cast)
			return evalInteger(cast.getRight());
		
		if(expr instanceof ConditionalExpressionNode cond)
			return evalInteger(cond.getCondition()).compareTo(BigInteger.ZERO) != 0
				? evalInteger(cond.getWhenTrue())
				: evalInteger(cond.getWhenFalse());
		
		if(expr instanceof UnaryExpressionNode unary) {
			BigInteger target = evalInteger(unary.getTarget());
			
			BigInteger val = switch(unary.getOperation()) {
			case MINUS:			yield target.negate();
			case BITWISE_NOT:	yield target.not();
			default: yield null;
			};
			
			if(val != null)
				return (BigInteger) checkBounds(val, unary.getType());
		}
		
		error(expr, "Expression is not constant");
		return null;
	}
	
	private BigDecimal evalBigArithmetic(ExpressionNode expr) {
		return toBigDecimal(evalArithmetic(expr));
	}

	public Number evalArithmetic(ExpressionNode expr) {
		if(expr instanceof NumberLiteralExpressionNode num) {
			if(num.getType().isFloating())
				return num.getLiteral();
			
			return num.getLiteral();
		}
		
		if(expr instanceof BinaryExpressionNode binary) {
			Number left = evalArithmetic(binary.getLeft());

			Punctuator op = binary.getOperation();
			
			if(op == Punctuator.LOGICAL_OR || op == Punctuator.LOGICAL_AND)
				return (BigInteger) performLogical(
					binary,
					op,
					left,
					() -> evalArithmetic(binary.getRight()),
					binary.getRight().getType()
				);
			
			Number val = performOperation(
				binary,
				op,
				left,
				evalArithmetic(binary.getRight())
			);
			
			if(val != null)
				return checkBounds(val, binary.getType());
		}
		
		if(expr instanceof CastExpressionNode cast) {
			if(!cast.getType().isArithmetic())
				error(cast, "Cannot cast to non-numeric type in constant expression");
			
			ExpressionNode target = cast.getTarget();
			
			Type type = target.getType();
			
			if(!type.isArithmetic())
				error(target, "Expected number for cast in constant expression");
			
			Number value = evalBigArithmetic(target);
			
			if(cast.getType().isInteger())
				value = ((BigDecimal) value).toBigInteger();
			
			return checkBounds(value, cast.getType());
		}
		
		if(expr instanceof CommaExpressionNode cast)
			return evalArithmetic(cast.getRight());
		
		if(expr instanceof ConditionalExpressionNode cond)
			return !isZero(evalArithmetic(cond.getCondition()))
				? evalArithmetic(cond.getWhenTrue())
				: evalArithmetic(cond.getWhenFalse());
		
		if(expr instanceof UnaryExpressionNode unary) {
			Number target = evalArithmetic(unary.getTarget());
			
			Number val = null;
			
			if(unary.getOperation() == Punctuator.BITWISE_NOT) {
				if(!(target instanceof BigInteger bigint))
					error("Illegal operand for »~«");
					
				else val = bigint.not();
			}
			else if(unary.getOperation() == Punctuator.MINUS) {
				if(target instanceof BigInteger bigint)
					val = bigint.negate();
				
				else val = ((BigDecimal) target).negate();
			}
			
			if(val != null)
				return checkBounds(val, unary.getType());
		}
		
		error(expr, "Expression is not constant");
		return null;
	}
	
	public AddressInitializer evalAddress(ExpressionNode expr) {
		return new AddressInitializer(null, BigInteger.ZERO); // TODO
	}
	
	public boolean isConstant(ExpressionNode expr) {
		if(expr instanceof NumberLiteralExpressionNode)
			return true;
		
		if(expr instanceof BinaryExpressionNode binary)
			return binary.getOperation() != Punctuator.ASSIGN
				&& isConstant(binary.getLeft())
				&& isConstant(binary.getRight());
		
		if(expr instanceof UnaryExpressionNode unary)
			return unary.getOperation() != Punctuator.INDIRECTION
				&& unary.getOperation() != Punctuator.ADDRESS_OF
				&& isConstant(unary.getTarget());
		
		if(expr instanceof CommaExpressionNode comma)
			return isConstant(comma.getRight());
		
		if(expr instanceof CastExpressionNode cast)
			return isConstant(cast.getTarget());
		
		if(expr instanceof ConditionalExpressionNode cond) {
			if(!isConstant(cond.getCondition()))
				return false;
			
			return isZero(evalInteger(cond.getCondition()))
				? isConstant(cond.getWhenFalse())
				: isConstant(cond.getWhenTrue());
		}
		
		return false;
	}
	
	@FunctionalInterface
	private static interface MathFunction<N> {
		
		N calculate(N a, N b);
		
	}
	
	@FunctionalInterface
	private static interface LogicalFunction {
		
		boolean compare(boolean a, Supplier<Boolean> b);
		
		default BigInteger calculate(BigInteger a, Supplier<BigInteger> b) {
			return intBool(compare(isZero(a), () -> isZero(b.get())));
		}

		default BigDecimal calculate(BigDecimal a, Supplier<BigDecimal> b) {
			return decBool(compare(isZero(a), () -> isZero(b.get())));
		}
		
	}

}
