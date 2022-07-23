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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.parser.node.expression.BinaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CallExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CastExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CommaExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ConditionalExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.MemberAccessExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.NumberLiteralExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.UnaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.SymbolTable;
import at.syntaxerror.syntaxc.symtab.global.AddressInitializer;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.FunctionType;
import at.syntaxerror.syntaxc.type.FunctionType.Parameter;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.StructType;
import at.syntaxerror.syntaxc.type.StructType.Member;
import at.syntaxerror.syntaxc.type.Type;
import at.syntaxerror.syntaxc.type.TypeUtils;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class ExpressionParser extends AbstractParser {
	
	private final AbstractParser parser;
	
	private final ConstantExpressionEvaluator evaluator = new ConstantExpressionEvaluator();
	
	@Override
	public SymbolTable getSymbolTable() {
		return parser.getSymbolTable();
	}
	
	@Override
	public void markTokenState() {
		parser.markTokenState();
	}
	
	@Override
	public void resetTokenState() {
		parser.resetTokenState();
	}
	
	@Override
	public void unmarkTokenState() {
		parser.unmarkTokenState();
	}
	
	@Override
	public Token readNextToken() {
		return parser.readNextToken();
	}
	
	/* 
	 * § 6.3.1 Primary expression
	 * 
	 * primary-expression = identifier | constant | string-literal | '(' expression ')'
	 */
	public ExpressionNode nextPrimary() {
		
		// variable, function, enum constant
		if(equal(TokenType.IDENTIFIER)) {
			Token ident = current;
			
			SymbolObject sym = getSymbolTable().findObject(ident.getString());
			
			if(sym != null) {
				if(sym.isEnumerator()) // enum constant
					return newNumber(
						getPosition(),
						sym.getEnumeratorData()
							.value(),
						sym.getType()
							.toEnum()
							.asNumberType()
					);

				if(sym.isVariable()) // variable or function
					return new VariableExpressionNode(getPosition(), sym);
				
				// typedef name is an error here
			}
			
			if(optional("(")) // function call, but function is not declared
				error(ident, "Implicit declaration of function »%s«", ident.getString());
			
			// variable is not declared
			error(ident, "Undefined variable »%s«", ident.getString());
		}

		// string literal
		if(equal(TokenType.STRING)) {
			String value = current.getString();
			
			SymbolObject var = SymbolObject.string(
				current.getPosition(),
				getSymbolTable().getStringTable().add(value)
			);
			
			getSymbolTable().addObject(var);
			
			return new VariableExpressionNode(
				current.getPosition(),
				var
			);
		}
		
		// number literal
		if(equal(TokenType.CONSTANT))
			return newNumber(
				getPosition(),
				current.getNumericType().isFloating()
					? current.getDecimal()
					: current.getInteger(),
				current.getNumericType().asType()
			);
		
		// parenthesized expression
		if(skip("(")) {
			ExpressionNode expr = nextExpression();
			
			require(")");
			
			return expr;
		}
		
		error("Expected expression");
		return null;
	}
	
	/*
	 * § 6.3.2 Postfix operators
	 * 
	 * postfix-expression = primary-expression ( '[' expression ']'
	 * 					| '(' ( assignment-expression ( ',' assignment-expression )* )? ')'
	 * 					| '.' identifier | '->' identifier | '++' | '--' )*
	 */
	public ExpressionNode nextPostfix() {
		ExpressionNode expr = null;
		
		if(equal(TokenType.IDENTIFIER) && peek("(")) {
			// implicitly declare function as 'extern int NAME_HERE();' if it does not exist

			Token fnName = current; // current = '('
			
			String name = fnName.getString();
			Position pos = fnName.getPosition();
			
			SymbolObject sym = getSymbolTable().findObject(name);
			
			if(sym == null) { // function does not exist
				sym = SymbolObject.implicit(pos, name);
				
				warn(pos, Warning.IMPLICIT_FUNCTION, "Implicit declaration of function »%s«", name);
			}
			
			expr = new VariableExpressionNode(pos, sym);
		}
		
		if(expr == null)
			expr = nextPrimary();
		
		while(optional("[", "(", ".", "->", "++", "--")) {
			
			Token op = current;
			Type exprType = expr.getType();
			
			/* § 6.3.2.1 Array subscripting
			 * 
			 * '[' expression ']'
			 */
			if(skip("[")) {
				ExpressionNode index = nextExpression();
				
				Position pos = require("]").getPosition();
				
				Type indexType = index.getType();
				
				// must be either 'pointer[index]' or 'index[pointer]'
				
				if(!exprType.isPointerLike() && !indexType.isPointerLike())
					error(op, "Indexed type is not a pointer");
				
				if(!exprType.isInteger() && !indexType.isInteger())
					error(op, "Index is not an integer");
				
				exprType = exprType.dereference();
				
				if(exprType.isIncomplete())
					error(op, "Cannot get element of pointer to incomplete type »%s«", exprType);
				
				// ptr[idx] is equivalent to (*(ptr+idx)) ; yields an lvalue
				expr = newUnary(
					pos,
					addPointer(
						pos,
						expr,
						index
					),
					Punctuator.INDIRECTION,
					exprType
				);
				continue;
			}
			
			/* § 6.3.2.2 Function calls
			 * 
			 * '(' ( assignment-expression ( ',' assignment-expression )* )? ')'
			 */
			if(equal("(")) {
				// must be either function or pointer to function
				
				if(!exprType.isFunction() && !(exprType.isPointer() && exprType.dereference().isFunction()))
					error(expr, "Invoked object is not a function or function pointer");
				
				FunctionType fnType;
				
				if(exprType.isPointer())
					fnType = exprType.dereference().toFunction();
				else fnType = exprType.toFunction();
				
				List<Parameter> parameters = fnType.getParameters();
				List<ExpressionNode> arguments = new ArrayList<>();

				boolean prototype = fnType.isPrototype();
				boolean variadic = fnType.isVariadic();
				boolean kAndR = fnType.isKAndR();
				
				final int n = parameters.size();
				
				while(!optional(")")) {
					
					if(!arguments.isEmpty())
						require(",");
					
					next();
					
					ExpressionNode arg = nextAssignment();
					
					if(prototype && !variadic && arguments.size() >= n)
						error(arg, "Too many arguments for function call");
					
					Type argType = arg.getType();
					
					if(!kAndR) {
						if(!prototype || (variadic && arguments.size() >= n)) { // default argument promotion
							
							if(argType.isArithmetic()) {
								NumericValueType num = argType.toNumber().getNumericType();
	
								// promote float to double
								if(num == NumericValueType.FLOAT)
									arg = newCast(
										arg.getPosition(),
										arg,
										Type.DOUBLE
									);
	
								// promote (un)signed char/short to (un)signed int
								else switch(num) {
								case SIGNED_CHAR:
								case SIGNED_SHORT:
									arg = newCast(
										arg.getPosition(),
										arg,
										Type.UINT
									);
									break;
										
								case UNSIGNED_CHAR:
								case UNSIGNED_SHORT:
									arg = newCast(
										arg.getPosition(),
										arg,
										Type.INT
									);
									break;
									
								default:
									break;
								}
							}
							
						}
						
						else {
							int idx = arguments.size();
							
							if(!argType.isStructLike() && idx < n)
								// implicitly cast arguments to match parameter type (unless struct or union)
								// actual type compatability check is performed later on
								arg = newCast(
									arg.getPosition(),
									arg,
									parameters.get(idx).type()
								);
						}
					}
					
					arguments.add(arg);
				}
				
				if(prototype && arguments.size() < n)
					error(expr, "Too few arguments for function call");
				
				/* dereference function pointer:
				 * 
				 * void (*func)();
				 * ...
				 * func();
				 * ...
				 * 
				 * is equivalent to:
				 * 
				 * void (*func)();
				 * ...
				 * (*func)();
				 * ...
				 */
				if(exprType.isPointer())
					expr = newUnary(expr, expr, Punctuator.INDIRECTION, fnType);
				
				expr = new CallExpressionNode(expr.getPosition(), expr, arguments, fnType);
				continue;
			}
			
			/* § 6.3.2.3 Structure and union members
			 * 
			 * ( '.' | '->' ) identifier
			 */
			if(equal(".", "->")) {
				
				// a->b is equivalent to (*a).b
				if(equal("->")) {
					if(!exprType.isPointer() || !exprType.dereference().isStructLike())
						error(op, "Target for member access is not pointer to struct or union");
					
					expr = newUnary(
						op,
						expr,
						Punctuator.INDIRECTION,
						exprType = exprType.dereference()
					);
				}
				else if(!exprType.isStructLike())
					error(op, "Target for member access is not struct or union");
				
				StructType struct = exprType.toStructLike();

				if(struct.isIncomplete())
					error(op, "Cannot access member of incomplete type »%s«", struct);
				
				Token ident = require(TokenType.IDENTIFIER);
				
				Member member = struct.getMember(ident.getString());
				
				if(member == null)
					error(op, "Type »%s« does not have a member named »%s«", struct, ident.getString());
				
				expr = new MemberAccessExpressionNode(
					op.getPosition(),
					expr,
					ident.getString(),
					member.getType()
				);
				continue;
			}
			
			if(equal("++", "--")) {
				
				Punctuator assignOp;
				Punctuator binaryOp;
				
				if(equal("++")) {
					assignOp = Punctuator.ADD;
					binaryOp = Punctuator.SUBTRACT;
				}
				else {
					assignOp = Punctuator.SUBTRACT;
					binaryOp = Punctuator.ADD;
				}
				
				Position pos = op.getPosition();
				
				// x++ is equivalent to ((x += 1) - 1)
				// x-- is equivalent to ((x -= 1) + 1)
				
				NumberLiteralExpressionNode one = Constants.one(pos);
				
				expr = newCast( // cast to typeof x
					pos,
					newBinary( // (x += 1) - 1 or (x -= 1) + 1
						pos,
						newAssignment( // x += 1 or x -= 1
							pos,
							expr,
							one,
							assignOp
						),
						one,
						binaryOp,
						exprType
					),
					exprType
				);
				continue;
			}
		}
		
		return expr;
	}

	/*
	 * § 6.3.3 Unary operators
	 * 
	 * unary-expression = postfix-expression
	 * 					| ( 'sizeof' | '++' | '--' ) unary-expression
	 * 					| ('&' | '*' | '+' | '-' | '~' | '!') cast-expression
	 * 					| 'sizeof' '(' type-name ')'
	 */
	public ExpressionNode nextUnary() {
		if(equal("&", "*", "+", "-", "~", "!")) {
			Token op = current;

			next();
			ExpressionNode expr = nextCast();
			
			Type type = expr.getType();

			// § 6.3.3.2 Address and indirection operators
			
			if(op.is("&")) {
				if(type.isBitfield())
					error(op, "Cannot take address of bit-field");
				
				else if(!((expr instanceof VariableExpressionNode var) && var.getVariable().isFunction())
					&& !expr.isLvalue())
					error(op, "Cannot take address of rvalue");
				
				return newUnary(
					op,
					expr,
					Punctuator.ADDRESS_OF,
					expr.getType().addressOf()
				);
			}
			
			if(op.is("*")) {
				if(type.isFunction()) // dereferencing a function doesn't do anything
					return expr;
				
				if(!type.isPointerLike())
					error(op, "Expected pointer for indirection");
				
				type = type.dereference();
				
				if(type.isVoid())
					warn(op, Warning.DEREF_VOID, "Dereferencing of a pointer to »void«");
				
				if(type.isIncomplete())
					error(op, "Cannot dereference pointer to incomplete type");
				
				return newUnary(
					op,
					expr,
					Punctuator.INDIRECTION,
					type
				);
			}

			// § 6.3.3.3 Unary arithmetic operators
			
			if(op.is("+")) {
				if(!type.isArithmetic())
					error(op, "Expected number for unary plus", op.getPunctuator());
				
				return newPromote(expr); // '+x' only performs integral promotion on x
			}

			if(op.is("-")) {
				if(!type.isArithmetic())
					error(op, "Expected number for unary minus", op.getPunctuator());
				
				return newUnary(
					op,
					newPromote(expr),
					Punctuator.MINUS
				);
			}
			
			if(op.is("~")) {
				if(!type.isInteger())
					error(op, "Expected integer for bitwise complement");

				return newUnary(
					op,
					newPromote(expr),
					Punctuator.BITWISE_NOT
				);
			}
			
			if(op.is("!")) {
				if(!type.isScalar())
					error(op, "Expected number or pointer for bitwise complement");

				// '!x' is equivalent to 'x == 0'
				return newBinary(
					op,
					expr,
					Constants.zero(op),
					Punctuator.EQUAL,
					Type.INT
				);
			}
			
			// unreachable
			throw new RuntimeException();
		}

		// § 6.3.3.1 Prefix increment and decrement operators
		if(equal("++", "--")) {
			Token op = current;

			next();
			ExpressionNode expr = nextUnary();
			
			String nameOp;
			Punctuator assignOp;
			
			if(op.is("++")) {
				nameOp = "increment";
				assignOp = Punctuator.ADD;
			}
			else {
				nameOp = "decrement";
				assignOp = Punctuator.SUBTRACT;
			}
			
			if(!expr.isLvalue())
				error(op, "Expected lvalue for %s", nameOp);
			
			if(!expr.getType().isScalar())
				error(op, "Expected number or pointer for %s", nameOp);
			
			// '++x' is equivalent to 'x += 1'
			// '--x' is equivalent to 'x -= 1'
			
			return newAssignment(
				op.getPosition(),
				expr,
				Constants.one(op),
				assignOp
			);
		}
		
		if(equal("sizeof")) {
			Token sizeof = current;
			
			Type type = null;
			
			mark();
			
			if(optional("(")) {
				next();
				
				if(isTypeName()) {
					unmark();
					
					type = nextTypeName();
					
					require(")");
				}

				else reset();
			}
			
			else reset();

			if(type == null) {
				next();
				type = nextUnary().getType();
			}
			
			if(type.isFunction())
				error(sizeof, "Cannot get size of a function");
			
			if(type.isIncomplete())
				error(sizeof, "Cannot get size of an incomplete type");
			
			if(type.isBitfield())
				error(sizeof, "Cannot get size of a bit-field");
			
			return newNumber(
				sizeof.getPosition(),
				BigInteger.valueOf(type.sizeof()),
				NumericValueType.SIZE.asType()
			);
		}
		
		return nextPostfix();
	}

	/*
	 * § 6.3.4 Cast operators
	 * 
	 * cast-expression = ( '(' type-name ')' )* unary-expression
	 */
	public ExpressionNode nextCast() {
		mark();
		
		if(equal("(")) {
			next();
			
			if(isTypeName()) {
				unmark();
				
				Type type = nextTypeName();
				
				Position pos = require(")").getPosition();

				next();
				ExpressionNode target = nextCast();
				
				if(!type.isScalar() && !type.isVoid())
					error(pos, "Cannot cast to non-scalar and non-void type");
				
				if(!target.getType().isScalar())
					error(pos, "Expected scalar type for cast");
				
				return newCast(
					pos,
					nextCast(),
					type
				);
			}
		}
		
		reset();
		
		return nextUnary();
	}
	
	private Type nextTypeName() {
		return null; // TODO provided by declaration parser
	}

	private boolean isTypeName() {
		return false; // TODO provided by declaration parser
	}

	/*
	 * § 6.3.5 Multiplicative operators
	 * 
	 * multiplicative-expression = cast-expression ( ( '*' | '/' | '%' ) cast-expression )*
	 */
	public ExpressionNode nextMultiplicative() {
		ExpressionNode exprLeft = nextCast();
		
		while(optional("*", "/", "%")) {
			Token op = current;

			next();
			ExpressionNode exprRight = nextCast();
			
			if(op.is("%"))
				exprLeft = checkModulo(op, exprLeft, exprRight);
			
			else exprLeft = checkArithmetic(
				op,
				exprLeft,
				exprRight,
				op.is("/")
					? "division"
					: "multiplication"
			);
		}
		
		return exprLeft;
	}
	
	/*
	 * § 6.3.6 Additive operators
	 * 
	 * additive-expression = multiplicative-expression ( ( '+' | '-' ) multiplicative-expression )*
	 */
	public ExpressionNode nextAdditive() {
		ExpressionNode exprLeft = nextMultiplicative();
		
		while(optional("+", "-")) {
			Token op = current;

			next();
			ExpressionNode exprRight = nextMultiplicative();
			
			if(op.is("-"))
				exprLeft = checkSubtraction(op, exprLeft, exprRight);
			
			else exprLeft = checkAddition(op, exprLeft, exprRight);
		}
		
		return exprLeft;
	}
	
	/*
	 * § 6.3.7 Bitwise shift operators
	 * 
	 * shift-expression = additive-expression ( ( '<<' | '>>' ) additive-expression )*
	 */
	public ExpressionNode nextShift() {
		return nextBitwise(this::nextAdditive, "bitwise shift", "<<", ">>");
	}
	
	/*
	 * § 6.3.8 Relational operators
	 * 
	 * relational-expression = shift-expression ( ( '<' | '>' | '<=' | '>=' ) shift-expression )*
	 */
	public ExpressionNode nextRelational() {
		ExpressionNode exprLeft = nextShift();
		
		while(optional("<", ">", "<=", ">=")) {
			Token op = current;

			next();
			ExpressionNode exprRight = nextShift();
			
			exprLeft = checkComparison(op, exprLeft, exprRight);
		}
		
		return exprLeft;
	}
	
	/*
	 * § 6.3.9 Equality operators
	 * 
	 * equality-expression = relational-expression ( ( '==' | '!=' ) relational-expression )*
	 */
	public ExpressionNode nextEquality() {
		ExpressionNode exprLeft = nextRelational();
		
		while(optional("==", "!=")) {
			Token op = current;

			next();
			ExpressionNode exprRight = nextRelational();
			
			exprLeft = checkComparison(op, exprLeft, exprRight);
		}
		
		return exprLeft;
	}
	
	/*
	 * § 6.3.10 Bitwise AND operator
	 * 
	 * AND-expression = equality-expression ( '&' equality-expression )*
	 */
	public ExpressionNode nextBitwiseAND() {
		return nextBitwise(this::nextEquality, "bitwise AND", "&");
	}
	
	/*
	 * § 6.3.11 Bitwise exclusive OR operator
	 * 
	 * exclusive-OR-expression = AND-expression ( '^' AND-expression )*
	 */
	public ExpressionNode nextBitwiseXOR() {
		return nextBitwise(this::nextBitwiseAND, "bitwise XOR", "^");
	}
	
	/*
	 * § 6.3.12 Logical inclusive OR operator
	 * 
	 * inclusive-OR-expression = exclusive-OR-expression ( '|' exclusive-OR-expression )*
	 */
	public ExpressionNode nextBitwiseOR() {
		return nextBitwise(this::nextBitwiseXOR, "bitwise OR", "|");
	}
	
	/*
	 * § 6.3.13 Logical AND operator
	 * 
	 * logical-AND-expression = inclusive-OR-expression ( '&&' inclusive-OR-expression )*
	 */
	public ExpressionNode nextLogicalAND() {
		return nextLogical(this::nextBitwiseOR, "logical AND", "&&");
	}
	
	/*
	 * § 6.3.14 Logical OR operator
	 * 
	 * logical-OR-expression = logical-AND-expression ( '||' logical-AND-expression )*
	 */
	public ExpressionNode nextLogicalOR() {
		return nextLogical(this::nextLogicalAND, "logical OR", "||");
	}
	
	/*
	 * § 6.3.15 Conditional operator
	 * 
	 * conditional-expression = logical-OR-expression ( '?' expression ':' conditional-expression)?
	 */
	public ExpressionNode nextConditional() {
		ExpressionNode exprCondition = nextLogicalOR();
		
		if(optional("?")) {
			next();
			ExpressionNode exprWhenTrue = nextExpression();
			
			Position pos = require(":").getPosition();

			next();
			ExpressionNode exprWhenFalse = nextConditional();
			
			Type condition = exprCondition.getType();
			Type whenTrue = exprWhenTrue.getType();
			Type whenFalse = exprWhenFalse.getType();
			
			if(!condition.isScalar())
				error(
					pos,
					"Expected number or pointer for ternary condition (got »%s«)",
					condition
				);


			Type result = null;
			
			do {
				if(whenTrue.isArithmetic() && whenFalse.isArithmetic()) {
					result = TypeUtils.convertUsualArithmetic(whenTrue, whenFalse);
					break;
				}
				
				if(TypeUtils.isEqual(whenTrue, whenFalse)) {
					if(whenTrue.isPointerLike())
						result = TypeUtils.inheritPointerQualifiers(whenTrue, whenFalse);
					
					else result = whenTrue;
					
					break;
				}
				
				if((whenTrue.isPointerLike() && TypeUtils.isVoidPointer(whenFalse))
					|| (whenFalse.isPointerLike() && TypeUtils.isVoidPointer(whenTrue))) {

					Type voidPointer;
					Type typePointer;
					ExpressionNode voidExpression;
					
					if(TypeUtils.isVoidPointer(whenTrue)) {
						voidPointer = whenTrue;
						typePointer = whenFalse;
						
						voidExpression = exprWhenTrue;
					}
					else {
						voidPointer = whenFalse;
						typePointer = whenTrue;

						voidExpression = exprWhenFalse;
					}
					
					if(isNullPointer(voidExpression))
						result = typePointer;
					
					else result = TypeUtils.inheritPointerQualifiers(voidPointer, typePointer);
					
					break;
				}
				
				error(
					pos,
					"Incompatible types for ternary conditional results (got »%s« and »%s«)",
					whenTrue,
					whenFalse
				);
			} while(false);
			
			return new ConditionalExpressionNode(
				pos,
				exprCondition,
				newLazyCast(exprWhenTrue, exprWhenTrue, result),
				newLazyCast(exprWhenFalse, exprWhenFalse, result),
				result
			);
		}
		
		return exprCondition;
	}
	
	private static final Map<Punctuator, Punctuator> ASSIGN_TO_BINARY = Map.of(
		Punctuator.ASSIGN_MULTIPLY,	Punctuator.MULTIPLY,
		Punctuator.ASSIGN_DIVIDE,	Punctuator.DIVIDE,
		Punctuator.ASSIGN_MODULO,	Punctuator.MODULO,
		Punctuator.ASSIGN_ADD,		Punctuator.ADD,
		Punctuator.ASSIGN_SUBTRACT,	Punctuator.SUBTRACT,
		Punctuator.ASSIGN_LSHIFT,	Punctuator.LSHIFT,
		Punctuator.ASSIGN_RSHIFT,	Punctuator.RSHIFT,
		Punctuator.ASSIGN_AND,		Punctuator.BITWISE_AND,
		Punctuator.ASSIGN_XOR,		Punctuator.BITWISE_XOR,
		Punctuator.ASSIGN_OR,		Punctuator.BITWISE_OR
	);
	
	/*
	 * § 6.3.16 Assignment operators
	 * 
	 * assignment-expression = conditional-expression
	 * 						| unary-expression ( '=' | '*=' | '/=' | '%=' | '+=' | '-='
	 * 						| '<<=' | '>>=' | '&=' | '^=' | '|=' ) assignment-expression
	 */
	public ExpressionNode nextAssignment() {
		ExpressionNode exprLeft = nextConditional();
		
		if(optional("=", "*=", "/=", "%=", "+=", "-=", "<<=", ">>=", "&=", "^=", "|=")) {
			Token op = current;
			
			next();
			ExpressionNode exprRight = nextAssignment();
			
			if(!exprLeft.isLvalue())
				error(op, "Expected lvalue for left-hand side of assignment");
			
			if(exprLeft.hasConstQualifier())
				error(op, "Cannot assign value to storage qualified as »const«");
			
			if(op.is("="))
				return checkAssignment(op, exprLeft, exprRight);
			
			op = Token.ofPunctuator(op.getPosition(), ASSIGN_TO_BINARY.get(op.getPunctuator()));
			
			BinaryExpressionNode assign;
			
			if(op.is("%"))
				assign = checkModulo(op, exprLeft, exprRight);
			
			else if(op.is("/"))
				assign = checkArithmetic(op, exprLeft, exprRight, "division");
			
			else if(op.is("*"))
				assign = checkArithmetic(op, exprLeft, exprRight, "multiplication");
			
			else if(op.is("<<", ">>"))
				assign = checkBitwise(op, exprLeft, exprRight, "bitwise shift");
			
			else if(op.is("&"))
				assign = checkBitwise(op, exprLeft, exprRight, "bitwise AND");

			else if(op.is("^"))
				assign = checkBitwise(op, exprLeft, exprRight, "bitwise XOR");
			
			else if(op.is("|"))
				assign = checkBitwise(op, exprLeft, exprRight, "bitwise OR");

			else if(op.is("+")) {
				assign = checkAddition(op, exprLeft, exprRight);
				
				if(exprRight.getType().isPointerLike())
					error(op, "Unexpected pointer on right-hand side of compound assignment");
			}

			else if(op.is("-")) {
				assign = checkSubtraction(op, exprLeft, exprRight);

				if(exprRight.getType().isPointerLike())
					error(op, "Unexpected pointer on right-hand side of compound assignment");
			}
			
			// unreachable
			else throw new RuntimeException();
			
			return newAssignment(
				op,
				assign.getLeft(),
				assign.getRight(),
				op.getPunctuator()
			);
		}
		
		return exprLeft;
	}
	
	/*
	 * § 6.3.17 Comma operator
	 * 
	 * expression = assignment-expression ( ',' assignment-expression )*
	 */
	public ExpressionNode nextExpression() {
		ExpressionNode left = nextAssignment();
		
		while(optional(",")) {
			Position pos = current.getPosition();
			
			next();
			ExpressionNode right = nextAssignment();
			
			left = new CommaExpressionNode(pos, left, right);
		}
		
		return left;
	}
	
	/*
	 * § 6.4 Constant expressions
	 * 
	 * constant-expression = conditional-expression
	 */
	private ExpressionNode nextConstantExpression() {
		return nextConditional();
	}
	
	public BigInteger nextIntegerConstantExpression() {
		return evaluator.evalInteger(nextConstantExpression());
	}

	public Number nextArithmeticConstantExpression() {
		return evaluator.evalArithmetic(nextConstantExpression());
	}

	public AddressInitializer nextAddressConstantExpression() {
		return evaluator.evalAddress(nextConstantExpression());
	}
	
	public boolean isNullPointer(ExpressionNode expr) {
		return evaluator.isConstant(expr)
			&& evaluator.evalInteger(expr).compareTo(BigInteger.ZERO) == 0;
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

			// pointer + offset
			return addPointer(
				op,
				exprLeft,
				exprRight
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

			// pointer - offset
			return subtractPointer(op, exprLeft, exprRight);
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
	
	public BinaryExpressionNode checkAssignment(Token op, ExpressionNode exprLeft, ExpressionNode exprRight) {
		Type left = exprLeft.getType();
		Type right = exprRight.getType();
		
		do {
			if(left.isArithmetic() && right.isArithmetic())
				break;
			
			if(left.isPointer() && right.isPointer()) {
	
				Type lbase = left.dereference();
				Type rbase = right.dereference();
				
				if(!lbase.isConst() && rbase.isConst())
					error(op, "Assignment removes »const« qualifier from target type");
				
				if(!lbase.isVolatile() && rbase.isVolatile())
					error(op, "Assignment removes »volatile« qualifier from target type");
				
				if(TypeUtils.isVoidPointer(left) || TypeUtils.isVoidPointer(right) || isNullPointer(exprRight))
					break;
				
			}
			
			if(TypeUtils.isEqual(left, right))
				break;
			
			error(
				op,
				"Incompatible types for assignment (got »%s« and »%s«)",
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
	
	// helper function for bitwise operations (<<, >>, &, ^, |)
	private ExpressionNode nextBitwise(Supplier<ExpressionNode> next, String name, Object...ops) {
		ExpressionNode exprLeft = next.get();
		
		while(optional(ops)) {
			Token op = current;

			next();
			ExpressionNode exprRight = next.get();
			
			exprLeft = checkBitwise(op, exprLeft, exprRight, name);
		}
		
		return exprLeft;
	}
	
	// helper function for logical operations (&&, ||)
	private ExpressionNode nextLogical(Supplier<ExpressionNode> next, String name, Object...ops) {
		ExpressionNode exprLeft = next.get();
		
		while(optional(ops)) {
			Token op = current;

			next();
			ExpressionNode exprRight = next.get();
			
			exprLeft = checkLogical(op, exprLeft, exprRight, name);
		}
		
		return exprLeft;
	}
	
	private NumberLiteralExpressionNode newNumber(Positioned pos, Number value, Type type) {
		return new NumberLiteralExpressionNode(pos.getPosition(), value, type);
	}
	
	private ExpressionNode newPromote(ExpressionNode expr) {
		Type type = expr.getType();
		Type promoted = TypeUtils.promoteInteger(type);
		
		if(TypeUtils.isEqual(type, promoted))
			return expr;
		
		return newCast(expr, expr, promoted);
	}
	
	private ExpressionNode newLazyCast(Positioned pos, ExpressionNode expr, Type type) {
		Type current = expr.getType();
		
		if(TypeUtils.isEqual(current, type))
			return expr;
		
		return newCast(pos, expr, type);
	}
	
	private CastExpressionNode newCast(Positioned pos, ExpressionNode expr, Type type) {
		return new CastExpressionNode(pos.getPosition(), expr, type);
	}
	
	private UnaryExpressionNode newUnary(Positioned pos, ExpressionNode expr, Punctuator op) {
		return newUnary(pos, expr, op, expr.getType());
	}

	private UnaryExpressionNode newUnary(Positioned pos, ExpressionNode expr, Punctuator op, Type type) {
		return new UnaryExpressionNode(pos.getPosition(), expr, op, type);
	}

	private BinaryExpressionNode newArithmetic(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator op) {
		return newArithmetic(pos, left, right, op, null);
	}

	private BinaryExpressionNode newArithmetic(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator op, Type type) {
		Type lType = left.getType();
		Type rType = right.getType();
		
		Type usual = TypeUtils.convertUsualArithmetic(lType, rType);
		
		if(!TypeUtils.isEqual(lType, usual)) left = newCast(pos, left, usual);
		if(!TypeUtils.isEqual(rType, usual)) right = newCast(pos, right, usual);
		
		return newBinary(pos, left, right, op, Objects.requireNonNullElse(type, usual));
	}

	private BinaryExpressionNode newBinary(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator op) {
		return newBinary(pos, left, right, op, left.getType());
	}

	private BinaryExpressionNode newBinary(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator op, Type type) {
		return new BinaryExpressionNode(pos.getPosition(), left, right, op, type);
	}

	private CommaExpressionNode newComma(Positioned pos, ExpressionNode left, ExpressionNode right) {
		return new CommaExpressionNode(pos.getPosition(), left, right);
	}
	
	// An assignment 'x op= y' is equivalent to 'z = &x, *z = *z op y'
	private ExpressionNode newAssignment(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator operation) {
		Type leftType = left.getType();
		Type leftAddr = leftType.addressOf();
		
		SymbolObject sym = SymbolObject.temporary(
			pos.getPosition(),
			leftAddr
		);
		
		getSymbolTable().addObject(sym);
		
		// z (temporary variable)
		VariableExpressionNode var = new VariableExpressionNode(
			pos.getPosition(),
			sym
		);
		
		UnaryExpressionNode deref = newUnary( // *z
			pos,
			var,
			Punctuator.INDIRECTION,
			leftType
		);
		
		return newComma( // z = &x, *z = *z op y
			pos,
			newBinary( // z = &x
				pos,
				var,
				newUnary( // &x
					pos,
					left,
					Punctuator.ADDRESS_OF,
					leftAddr
				),
				Punctuator.ASSIGN,
				leftAddr.unqualified()
			),
			newBinary( // *z = *z op y
				pos,
				deref,
				newBinary( // *z op y
					pos,
					deref,
					right,
					operation
				),
				Punctuator.ASSIGN,
				leftType.unqualified()
			)
		);
	}
	
	private BinaryExpressionNode subtractPointer(Positioned pos, ExpressionNode pointer, ExpressionNode offset) {
		return addOrSubtractPointer(pos, pointer, offset, Punctuator.SUBTRACT, false);
	}
	
	private BinaryExpressionNode addPointer(Positioned pos, ExpressionNode pointer, ExpressionNode offset) {
		boolean swap = false;

		// convert into canonical form 'pointer + offset'
		if(offset.getType().isPointerLike()) {
			swap = true;
			ExpressionNode temporary = pointer;
			pointer = offset;
			offset = temporary;
		}
		
		return addOrSubtractPointer(pos, pointer, offset, Punctuator.ADD, swap);
	}

	/* For an expression 'ptr+off' (where 'ptr' is a pointer and 'off' is an integer),
	 * 'off' specifies the offset in number of elements (not bytes) and must
	 * therefore be scaled accordingly ('off * sizeof(ptr)')
	 * 
	 * when 'swap' is true, ptr and off are swapped (changes order of evaluation)
	 */
	private BinaryExpressionNode addOrSubtractPointer(Positioned pos, ExpressionNode pointer, ExpressionNode offset, Punctuator op, boolean swap) {
		Type offsetType = NumericValueType.SIZE.asType();
		
		// offset * sizeof(pointer)
		ExpressionNode scaledOffset = newBinary(
			pos,
			offset,
			newNumber(
				pos,
				BigInteger.valueOf(pointer.getType().sizeof()),
				offsetType
			),
			Punctuator.MULTIPLY,
			offsetType
		);
		
		ExpressionNode a, b;
		
		if(swap) { // offset + pointer
			a = scaledOffset;
			b = pointer;
		}
		else { // pointer + offset
			a = pointer;
			b = scaledOffset;
		}
		
		return newBinary(pos, a, b, op, pointer.getType());
	}
	
}
