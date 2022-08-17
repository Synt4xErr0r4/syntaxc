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
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.parser.node.expression.BinaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CastExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ConditionalExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.MemberAccessExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.NumberLiteralExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.UnaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.global.AddressInitializer;
import at.syntaxerror.syntaxc.symtab.global.GlobalVariableInitializer;
import at.syntaxerror.syntaxc.symtab.global.IntegerInitializer;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.NumberType;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class ConstantExpressionEvaluator {

	private static final Map<Punctuator, Pair<MathFunction<BigInteger>, MathFunction<BigDecimal>>> ARITHMETIC_OPERATIONS = Map.of(
		Punctuator.ADD,				Pair.of(BigInteger::add,		BigDecimal::add),
		Punctuator.SUBTRACT,		Pair.of(BigInteger::subtract,	BigDecimal::subtract),
		Punctuator.MULTIPLY,		Pair.of(BigInteger::multiply,	BigDecimal::multiply),
		Punctuator.EQUAL,			comparison(i -> i == 0),
		Punctuator.NOT_EQUAL,		comparison(i -> i != 0),
		Punctuator.LESS,			comparison(i -> i < 0),
		Punctuator.LESS_EQUAL,		comparison(i -> i <= 0),
		Punctuator.GREATER,			comparison(i -> i > 0),
		Punctuator.GREATER_EQUAL,	comparison(i -> i >= 0),
		Punctuator.COMMA,			Pair.of((l, r) -> r, (l, r) -> r)
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
	
	private static BigInteger performOperation(Positioned pos, Punctuator punct, BigInteger left, BigInteger right) {
		if(BITWISE_OPERATIONS.containsKey(punct)) {
			if((punct == Punctuator.LSHIFT || punct == Punctuator.RSHIFT) && right.compareTo(BigInteger.ZERO) < 0) {
				Logger.warn(pos, Warning.NEGATIVE_SHIFT, "Shift count is negative");
				return BigInteger.ZERO;
			}
			
			return BITWISE_OPERATIONS.get(punct).calculate(left, right);
		}
		
		if(DIVISION_OPERATIONS.containsKey(punct)) {
			if(isZero(right))
				error(pos, "Division by zero");
			
			return DIVISION_OPERATIONS.get(punct).getLeft().calculate(left, right);
		}
		
		if(ARITHMETIC_OPERATIONS.containsKey(punct))
			return ARITHMETIC_OPERATIONS.get(punct).getLeft().calculate(left, right);
		
		return null;
	}

	private static Number performOperation(Positioned pos, Punctuator punct, Number left, Number right) {
		if(left instanceof BigInteger l && right instanceof BigInteger r)
			return performOperation(pos, punct, l, r);

		BigDecimal l = toBigDecimal(left);
		BigDecimal r = toBigDecimal(right);
		
		if(BITWISE_OPERATIONS.containsKey(punct))
			error(pos, "Illegal operands for »%s«", punct);
		
		if(DIVISION_OPERATIONS.containsKey(punct)) {
			if(isZero(r))
				error(pos, "Division by zero");
			
			return DIVISION_OPERATIONS.get(punct).getRight().calculate(l, r);
		}
		
		if(ARITHMETIC_OPERATIONS.containsKey(punct))
			return ARITHMETIC_OPERATIONS.get(punct).getRight().calculate(l, r);
		
		return null;
	}

	private static Number performLogical(Positioned pos, Punctuator punct, Number left, Supplier<Number> right, Type rightType) {
		LogicalFunction fun = LOGICAL_OPERATIONS.get(punct);
		
		if(left instanceof BigInteger l && rightType.isInteger())
			return fun.calculate(l, () -> (BigInteger) right.get());
		
		return fun.calculate(toBigDecimal(left), () -> toBigDecimal(right.get()));
	}
	
	private static boolean isZero(BigInteger value) {
		return value.compareTo(BigInteger.ZERO) == 0;
	}
	
	private static boolean isZero(Number value) {
		return value instanceof BigDecimal dec
			? dec.compareTo(BigDecimal.ZERO) == 0
			: isZero((BigInteger) value);
	}
	
	private static Number checkBounds(Number value, Type type) {
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
	
	public static BigInteger evalInteger(ExpressionNode expr) {
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
	
	private static BigDecimal evalBigArithmetic(ExpressionNode expr) {
		return toBigDecimal(evalArithmetic(expr));
	}

	public static Number evalArithmetic(ExpressionNode expr) {
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
		
		if(expr instanceof ConditionalExpressionNode cond)
			return !isZero(evalArithmetic(cond.getCondition()))
				? evalArithmetic(cond.getWhenTrue())
				: evalArithmetic(cond.getWhenFalse());
		
		if(expr instanceof UnaryExpressionNode unary) {
			Number target = evalArithmetic(unary.getTarget());
			
			Number val = null;
			
			if(unary.getOperation() == Punctuator.BITWISE_NOT) {
				if(!(target instanceof BigInteger bigint))
					error(expr, "Illegal operand for »~«");
					
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
	
	private static final BigInteger TRUE = BigInteger.ONE;
	
	public static GlobalVariableInitializer evalAddress(ExpressionNode expr) {
		AddressState state = evalAddressExpr(expr);
		
		if(state.symbol() == null)
			return new IntegerInitializer(state.offset(), expr.getType().sizeof());
		
		if(!state.isAddress())
			error(expr, "Expression is not constant");
		
		return new AddressInitializer(state.symbol(), state.offset());
	}
	
	private static AddressState evalAddressExpr(ExpressionNode expr) {
		if(expr instanceof VariableExpressionNode var)
			return AddressState.ofAddress(var.getVariable(), BigInteger.ZERO);
		
		if(expr instanceof NumberLiteralExpressionNode)
			return AddressState.ofValue(evalInteger(expr));
		
		if(expr instanceof BinaryExpressionNode binary) {
			if(isConstant(binary.getLeft()) && isConstant(binary.getRight()))
				return AddressState.ofValue(evalInteger(binary));
			
			var left = evalAddressExpr(binary.getLeft());
			
			Punctuator op = binary.getOperation();
			
			if(op == Punctuator.LOGICAL_OR || op == Punctuator.LOGICAL_AND)
				return AddressState.ofValue(
					(BigInteger) performLogical(
						binary,
						op,
						left.booleanValue(),
						() -> evalAddressExpr(binary.getRight()).booleanValue(),
						binary.getRight().getType()
					)
				);

			var right = evalAddressExpr(binary.getRight());
			
			if(!left.isAddress() && !right.isAddress()) {

				/* occurs as part of pointer subtraction (&x - &x)
				 * returns zero since left should always be zero at this point
				 */
				if(op == Punctuator.DIVIDE)
					return AddressState.ofValue(BigInteger.ZERO);

				error(expr, "Expression is not constant");
			}
			
			if(left.isAddress() && right.isAddress() && op == Punctuator.SUBTRACT) {
				if(left.symbol() == right.symbol())
					return AddressState.ofValue(BigInteger.ZERO); // (&x - &x) always returns 0

				error(expr, "Expression is not constant");
			}
			
			if(op == Punctuator.SUBTRACT) {
				
				/* possible combinations:
				 * 
				 * ptr - off
				 * (ptr + off) - off
				 * (ptr - off) - off
				 */
				
				if(right.isAddress()) // right side must not be a pointer
					error(expr, "Expression is not constant");
				
				return AddressState.ofAddress(
					left,
					right.offset()
						.negate()
				);
			}
			
			if(op == Punctuator.ADD) {
				
				/* possible combinations:
				 * ptr + off
				 * off + ptr
				 * (ptr + off) + off
				 * off + (ptr + off)
				 */

				if(left.isAddress() && right.isAddress()) // only one side must be a pointer
					error(expr, "Expression is not constant");

				// convert into canonical form 'pointer + offset'
				if(right.isAddress()) {
					AddressState temp = left;
					left = right;
					right = temp;
				}
				
				return AddressState.ofAddress(
					left,
					right.offset()
				);
			}
		}
		
		if(expr instanceof CastExpressionNode cast) {
			if(!cast.getType().isInteger() && !cast.getType().isPointer())
				error(cast, "Cannot cast to non-integer and non-pointer type in constant expression");
			
			ExpressionNode target = cast.getTarget();
			
			Type type = target.getType();
			
			if(!type.isScalar())
				error(target, "Expected number or pointer for cast in constant expression");

			if(cast.getType().isPointer())
				return evalAddressExpr(target);
			
			NumberType num = type.toNumber();
			
			BigInteger bigint = null;
			
			if(num.getNumericType().isFloating()) {
				if(target instanceof NumberLiteralExpressionNode lit)
					bigint = ((BigDecimal) lit.getLiteral()).toBigInteger();
				
				else error(target, "Floating point number must be immediate operand of cast in constant expression");
			}
			else bigint = evalInteger(target);
			
			return AddressState.ofValue(bigint);
		}
		
		if(expr instanceof ConditionalExpressionNode cond)
			return evalAddressExpr(cond.getCondition())
					.booleanValue()
					.compareTo(BigInteger.ZERO) != 0
				? evalAddressExpr(cond.getWhenTrue())
				: evalAddressExpr(cond.getWhenFalse());
		
		if(expr instanceof UnaryExpressionNode unary) {
			if(isConstant(unary.getTarget()))
				return AddressState.ofValue(evalInteger(unary.getTarget()));

			AddressState target = evalAddressExpr(unary.getTarget());
			
			if(unary.getOperation() == Punctuator.ADDRESS_OF)
				return AddressState.ofAddress(target.symbol(), target.offset());

			if(unary.getOperation() == Punctuator.INDIRECTION)
				return AddressState.ofAccess(target.symbol(), target.offset());
		}
		
		if(expr instanceof MemberAccessExpressionNode access) {
			AddressState target = evalAddressExpr(access.getTarget());
			
			return AddressState.ofAccess(
				target,
				BigInteger.valueOf(
					access.getTarget()
						.getType()
						.toStructLike()
						.getMember(access.getMember())
						.getOffset()
				)
			);
		}
		
		error(expr, "Expression is not constant");
		return null;
	}
	
	public static boolean isConstant(ExpressionNode expr) {
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
	
	private static void error(Positioned pos, String message, Object...args) {new Throwable().printStackTrace();
		Logger.error(pos, Warning.SEM_NONE, message, args);
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

	public static record AddressState(SymbolObject symbol, BigInteger offset, boolean isAddress) {
		
		public static AddressState ofValue(BigInteger value) {
			return new AddressState(null, value, false);
		}

		public static AddressState ofAddress(SymbolObject symbol, BigInteger offset) {
			return new AddressState(symbol, offset, true);
		}
		
		public static AddressState ofAddress(AddressState state, BigInteger offset) {
			return ofAddress(state.symbol(), state.offset().add(offset));
		}
		
		public static AddressState ofAccess(SymbolObject symbol, BigInteger offset) {
			return new AddressState(symbol, offset, false);
		}
		
		public static AddressState ofAccess(AddressState state, BigInteger offset) {
			return ofAccess(state.symbol(), state.offset().add(offset));
		}
		
		public BigInteger booleanValue() {
			return symbol != null ? TRUE : offset;
		}
		
	}
	
}
