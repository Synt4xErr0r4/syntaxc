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

import at.syntaxerror.syntaxc.builtin.BuiltinContext;
import at.syntaxerror.syntaxc.builtin.BuiltinFunction;
import at.syntaxerror.syntaxc.builtin.BuiltinRegistry;
import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.misc.config.Warnings;
import at.syntaxerror.syntaxc.parser.node.expression.ArrayIndexExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.BinaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.BuiltinExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CallExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CastExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ConditionalExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.MemberAccessExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.NumberLiteralExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.UnaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.SymbolTable;
import at.syntaxerror.syntaxc.symtab.global.GlobalVariableInitializer;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.ArrayType;
import at.syntaxerror.syntaxc.type.FunctionType;
import at.syntaxerror.syntaxc.type.FunctionType.Parameter;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.StructType;
import at.syntaxerror.syntaxc.type.StructType.Member;
import at.syntaxerror.syntaxc.type.Type;
import at.syntaxerror.syntaxc.type.TypeUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class ExpressionParser extends AbstractParser {
	
	private final AbstractParser parser;
	private final DeclarationParser declarationParser;
	
	private final @Getter AssignmentHelper assignmentHelper = new AssignmentHelper();
	private final @Getter ExpressionChecker checker = new ExpressionChecker();
	
	@Override
	protected void sync() {
		parser.current = current;
		parser.previous = previous;
		parser.sync();
	}
	
	@Override
	public SymbolTable getSymbolTable() {
		return parser.getSymbolTable();
	}
	
	@Override
	public void reread() {
		parser.reread();
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
	
	public boolean isTypeName() {
		return declarationParser.isTypeName();
	}
	
	public Type nextTypeName() {
		return declarationParser.nextTypeName();
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
				error(ident, Warnings.SEM_NONE, "Implicit declaration of function »%s«", ident.getString());
			
			// variable is not declared
			error(ident, Warnings.SEM_NONE, "Undefined variable »%s«", ident.getString());
		}

		// string literal
		if(equal(TokenType.STRING)) {
			String value = current.getString();
			
			SymbolObject var = SymbolObject.string(
				current.getPosition(),
				getSymbolTable().getStringTable()
					.add(value, current.isWide())
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
	
	private ExpressionNode nextBuiltin(Token tok) {
		Position pos = tok.getPosition();
		String name = tok.getString();
		
		BuiltinFunction fun = BuiltinRegistry.newFunction(name);
		
		FunctionType enclosing = null;
		
		if(parser instanceof Parser cParser)
			enclosing = cParser.getActiveFunctionType();
		
		BuiltinContext ctx = new BuiltinContext(enclosing, this, tok.getPosition());

		next(); // skip name
		next(); // skip '('
		
		fun.populate(ctx);
		
		ctx.ensureClosed();
		
		if(fun.isInline())
			return new NumberLiteralExpressionNode(
				pos,
				fun.getReturnValue(),
				fun.getReturnType()
			);
		
		return new BuiltinExpressionNode(pos, fun);
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
				
				if(BuiltinRegistry.isBuiltin(name))
					return nextBuiltin(fnName);
				
				sym = SymbolObject.implicit(pos, name);
				
				warn(pos, Warnings.IMPLICIT_FUNCTION, "Implicit declaration of function »%s«", name);
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
				
				if((expr instanceof ArrayIndexExpressionNode idx && idx.isArray()) ||
					(index instanceof ArrayIndexExpressionNode idx && idx.isArray())) {
					
					/* nested subscript operators:
					 * 
					 * The expression
					 * 
					 * 	type_t arr[n][m][o];
					 * 	type_t val = arr[i][j][k];
					 * 
					 * returns an element of an multi-dimensional array.
					 * It is effectively equal to:
					 * 
					 * 	type_t arr[n*m*o];
					 * 	type_t val = arr[(i*m+j)*o+k];
					 * 
					 * However, the expressions
					 * 
					 * 	type_t arr[n][m][o];
					 * 	type_t *ptr1 = arr[i];
					 * 	type_t *ptr2 = arr[i][j]; // arr[i*m+j]
					 * 
					 * would both create pointers to the array.
					 */

					boolean swapped = false;
					ArrayIndexExpressionNode subscript;

					if(index instanceof ArrayIndexExpressionNode idx) {
						swapped = true;
						subscript = idx;
						index = expr;
					}
					else subscript = (ArrayIndexExpressionNode) expr;
					
					ArrayType type = subscript
						.getRawType()
						.toArray();
					
					index = checker.checkAdditive(
						pos,
						checker.checkArithmetic(
							Token.ofPunctuator(pos, Punctuator.MULTIPLY),
							subscript.getIndex(),
							newNumber(
								pos,
								BigInteger.valueOf(type.getLength()),
								subscript.getIndex()
									.getType()
							),
							"subscript"
						),
						index,
						false
					);
					
					expr = new ArrayIndexExpressionNode(
						pos,
						subscript.getTarget(),
						index,
						swapped,
						type.dereference().isArray(),
						type.dereference()
					);
					continue;
				}
				
				boolean swapped = indexType.isPointerLike();
				
				if(swapped) {
					ExpressionNode temp = index;
					index = expr;
					expr = temp;
					
					exprType = expr.getType();
				}
				
				boolean isArray = exprType.isArray();
				
				exprType = exprType.dereference();
				
				if(exprType.isIncomplete())
					error(op, "Cannot get element of pointer to incomplete type »%s«", exprType);
				
				expr = new ArrayIndexExpressionNode(
					pos,
					expr,
					index,
					swapped,
					isArray && exprType.isArray(),
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

				boolean variadic = fnType.isVariadic();
				boolean kAndR = fnType.isKAndR();
				
				final int n = parameters.size();
				
				while(!optional(")")) {
					
					if(!arguments.isEmpty())
						require(",");
					
					next();
					
					ExpressionNode arg = nextAssignment();
					
					if(!kAndR && !variadic && arguments.size() >= n)
						error(arg, "Too many arguments for function call");
					
					Type argType = arg.getType();
					
					if(!kAndR) {
						if(variadic && arguments.size() >= n) { // default argument promotion
							
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
				
				if(!kAndR && arguments.size() < n)
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
					expr = PointerHelper.dereference(expr, expr);
				
				expr = new CallExpressionNode(expr.getPosition(), expr, arguments, fnType);
				continue;
			}
			
			/* § 6.3.2.3 Structure and union members
			 * 
			 * ( '.' | '->' ) identifier
			 */
			if(equal(".", "->")) {
				
				if(!expr.isLvalue())
					error(op, "Target for member access is not an lvalue");
				
				// a.b is equivalent to (&a)->b
				if(equal(".")) {
					if(!exprType.isStructLike())
						error(op, "Target for member access is not struct or union");
					
					expr = PointerHelper.addressOf(op, expr);
				}
				else if(!exprType.isPointer() || !exprType.dereference().isStructLike())
					error(op, "Target for member access is not pointer to struct or union");
				
				else exprType = exprType.dereference();
				
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
				boolean increment = equal("++");
				
				if(!exprType.isScalar())
					error(
						op,
						"Expected scalar for %s operator",
						increment
							? "increment"
							: "decrement"
					);
				
				if(!expr.isLvalue())
					error(
						op,
						"Expected lvalue for %s operator",
						increment
							? "increment"
							: "decrement"
					);
				
				/*
				 * convert 'val = x++' into
				 * 
				 *   typeof(x) *tmp = &x;
				 *   typeof(x) val = *tmp;
				 *   *tmp = val + 1;
				 *   result = val 
				 */

				Position pos = op.getPosition();
				
				SymbolObject tmp = SymbolObject.temporary(
					pos,
					exprType.addressOf()
				);

				SymbolObject val = SymbolObject.temporary(
					pos,
					exprType
				);
				
				VariableExpressionNode exprTmp = new VariableExpressionNode(
					pos,
					tmp
				);
				
				VariableExpressionNode exprVal = new VariableExpressionNode(
					pos,
					val
				);
				
				expr = newComma(
					pos,
					newComma(
						pos,
						checker.checkAssignment( // tmp = &x;
							pos,
							exprTmp,
							PointerHelper.addressOf(pos, expr), // &x
							false
						),
						checker.checkAssignment( // val = *tmp;
							pos,
							exprVal,
							PointerHelper.dereference(pos, exprTmp), // *tmp
							false
						)
					),
					newComma(
						pos,
						checker.checkAssignment( // *tmp = val + 1; or *tmp = val - 1;
							pos,
							PointerHelper.dereference(pos, exprTmp), // *tmp
							checker.checkAdditive( // val + 1 or val - 1
								pos,
								exprVal,
								newNumber(
									pos,
									BigInteger.ONE,
									NumericValueType.POINTER.asType()
								),
								!increment
							),
							false
						),
						exprVal // result = val
					)
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
			
			if(op.is("&"))
				return PointerHelper.addressOf(op, expr);
			
			if(op.is("*"))
				return PointerHelper.dereference(op, expr);

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
					error(op, "Expected number or pointer for logical NOT");

				return newUnary(
					op,
					expr,
					Punctuator.LOGICAL_NOT,
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
			
			return assignmentHelper.newAssignment(
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
					
					expect(")");
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
			Position pos = getPosition();
			
			next();
			
			if(isTypeName()) {
				unmark();
				
				Type type = nextTypeName();
				
				consume(")");
				
				ExpressionNode target = nextCast();
				
				if(!type.isScalar() && !type.isVoid())
					error(pos, "Cannot cast to non-scalar and non-void type");
				
				if(!target.getType().isScalar())
					error(pos, "Expected scalar type for cast");
				
				return newCast(
					pos,
					target,
					type
				);
			}
		}
		
		reset();
		
		return nextUnary();
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
				exprLeft = checker.checkModulo(op, exprLeft, exprRight);
			
			else exprLeft = checker.checkArithmetic(
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
				exprLeft = checker.checkSubtraction(op, exprLeft, exprRight);
			
			else exprLeft = checker.checkAddition(op, exprLeft, exprRight);
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
			
			exprLeft = checker.checkComparison(op, exprLeft, exprRight);
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
			
			exprLeft = checker.checkComparison(op, exprLeft, exprRight);
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
				
				if(TypeUtils.isCompatible(whenTrue, whenFalse)) {
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
				newCast(exprWhenTrue, exprWhenTrue, result),
				newCast(exprWhenFalse, exprWhenFalse, result),
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
				return checker.checkAssignment(op, exprLeft, exprRight, false);
			
			op = Token.ofPunctuator(op.getPosition(), ASSIGN_TO_BINARY.get(op.getPunctuator()));
			
			BinaryExpressionNode assign;
			
			if(op.is("%"))
				assign = checker.checkModulo(op, exprLeft, exprRight);
			
			else if(op.is("/"))
				assign = checker.checkArithmetic(op, exprLeft, exprRight, "division");
			
			else if(op.is("*"))
				assign = checker.checkArithmetic(op, exprLeft, exprRight, "multiplication");
			
			else if(op.is("<<", ">>"))
				assign = checker.checkBitwise(op, exprLeft, exprRight, "bitwise shift");
			
			else if(op.is("&"))
				assign = checker.checkBitwise(op, exprLeft, exprRight, "bitwise AND");

			else if(op.is("^"))
				assign = checker.checkBitwise(op, exprLeft, exprRight, "bitwise XOR");
			
			else if(op.is("|"))
				assign = checker.checkBitwise(op, exprLeft, exprRight, "bitwise OR");

			else if(op.is("+")) {
				assign = checker.checkAddition(op, exprLeft, exprRight);
				
				if(exprRight.getType().isPointerLike())
					error(op, "Unexpected pointer on right-hand side of compound assignment");
			}

			else if(op.is("-")) {
				assign = checker.checkSubtraction(op, exprLeft, exprRight);

				if(exprRight.getType().isPointerLike())
					error(op, "Unexpected pointer on right-hand side of compound assignment");
			}
			
			// unreachable
			else throw new RuntimeException();
			
			return assignmentHelper.newAssignment(
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
			
			left = newComma(pos, left, right);
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
		return ConstantExpressionEvaluator.evalInteger(nextConstantExpression());
	}

	public Number nextArithmeticConstantExpression() {
		return ConstantExpressionEvaluator.evalArithmetic(nextConstantExpression());
	}

	public GlobalVariableInitializer nextAddressConstantExpression() {
		return ConstantExpressionEvaluator.evalAddress(nextConstantExpression());
	}
	
	// helper function for bitwise operations (<<, >>, &, ^, |)
	private ExpressionNode nextBitwise(Supplier<ExpressionNode> next, String name, Object...ops) {
		ExpressionNode exprLeft = next.get();
		
		while(optional(ops)) {
			Token op = current;

			next();
			ExpressionNode exprRight = next.get();
			
			exprLeft = checker.checkBitwise(op, exprLeft, exprRight, name);
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
			
			exprLeft = checker.checkLogical(op, exprLeft, exprRight, name);
		}
		
		return exprLeft;
	}
	
	protected static NumberLiteralExpressionNode newNumber(Positioned pos, Number value, Type type) {
		return new NumberLiteralExpressionNode(pos.getPosition(), value, type);
	}
	
	protected static ExpressionNode newPromote(ExpressionNode expr) {
		Type promoted = TypeUtils.promoteInteger(expr.getType());
		
		return newCast(expr, expr, promoted);
	}
	
	protected static ExpressionNode newCast(Positioned pos, ExpressionNode expr, Type type) {
		Type current = expr.getType();
		
		if(TypeUtils.isCompatible(current, type))
			return expr;
		
		return new CastExpressionNode(pos.getPosition(), expr, type);
	}
	
	protected static UnaryExpressionNode newUnary(Positioned pos, ExpressionNode expr, Punctuator op) {
		return newUnary(pos, expr, op, expr.getType());
	}

	protected static UnaryExpressionNode newUnary(Positioned pos, ExpressionNode expr, Punctuator op, Type type) {
		return new UnaryExpressionNode(pos.getPosition(), expr, op, type);
	}

	protected static BinaryExpressionNode newArithmetic(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator op) {
		return newArithmetic(pos, left, right, op, null);
	}

	protected static BinaryExpressionNode newArithmetic(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator op, Type type) {
		Type lType = left.getType();
		Type rType = right.getType();
		
		Type usual = TypeUtils.convertUsualArithmetic(lType, rType);
		
		left = newCast(pos, left, usual);
		right = newCast(pos, right, usual);
		
		return newBinary(pos, left, right, op, Objects.requireNonNullElse(type, usual));
	}

	protected static BinaryExpressionNode newBinary(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator op) {
		return newBinary(pos, left, right, op, left.getType());
	}

	protected static BinaryExpressionNode newBinary(Positioned pos, ExpressionNode left, ExpressionNode right, Punctuator op, Type type) {
		return new BinaryExpressionNode(pos.getPosition(), left, right, op, type);
	}

	protected static BinaryExpressionNode newComma(Positioned pos, ExpressionNode left, ExpressionNode right) {
		return newBinary(pos, left, right, Punctuator.COMMA, right.getType());
	}
	
	public static boolean isNullPointer(ExpressionNode expr) {
		return ConstantExpressionEvaluator.isConstant(expr)
			&& ConstantExpressionEvaluator.evalInteger(expr).compareTo(BigInteger.ZERO) == 0;
	}
	
}
