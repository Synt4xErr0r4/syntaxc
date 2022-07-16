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

import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.parser.node.expression.BinaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CallExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CastExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.NumberLiteralExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.StringLiteralExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.UnaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.SymbolTable;
import at.syntaxerror.syntaxc.symtab.SymbolVariable;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.EnumType;
import at.syntaxerror.syntaxc.type.FunctionType;
import at.syntaxerror.syntaxc.type.FunctionType.Parameter;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class ExpressionParser extends AbstractParser {

	private final AbstractParser parser;
	
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
			
			SymbolVariable var = getSymbolTable().findVariable(ident.getString());
			
			if(var != null) {
				if(var.isEnumerator()) { // enum constant
					EnumType type = var.getType().toEnum();
					
					return new NumberLiteralExpressionNode(
						getPosition(),
						type.getValue(ident.getString()),
						type.asNumberType()
					);
				}

				if(var.isVariable()) // variable or function
					return new VariableExpressionNode(getPosition(), var.getObject());
				
				// typedef name is an error here
			}
			
			if(optional("(")) // function call, but function is not declared
				error(ident, "Implicit declaration of function »%s«", ident.getString());
			
			// variable is not declared
			error(ident, "Undefined variable »%s«", ident.getString());
		}

		// string literal
		if(equal(TokenType.STRING))
			return new StringLiteralExpressionNode(
				getPosition(),
				current.getString(),
				Type.getStringType(current.getString())
			);
		
		// number literal
		if(equal(TokenType.CONSTANT))
			return new NumberLiteralExpressionNode(
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
			
			SymbolVariable sym = getSymbolTable().findVariable(name);
			SymbolObject obj;
			
			if(sym == null) { // function does not exist
				obj = new SymbolObject(
					getPosition(),
					name,
					FunctionType.IMPLICIT
				);
				
				warn(pos, Warning.IMPLICIT_FUNCTION, "Implicit declaration of function »%s«", name);
			}
			else obj = sym.getObject();
			
			expr = new VariableExpressionNode(pos, obj);
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
				
				Position pos = require("]").getPosition().range(op);
				
				Type indexType = index.getType();
				
				// must be either 'pointer[index]' or 'index[pointer]'
				
				if(!exprType.isPointerLike() && !indexType.isPointerLike())
					error(op, "Indexed type is not a pointer");
				
				if(!exprType.isInteger() && !indexType.isInteger())
					error(op, "Index is not an integer");
				
				// swap if format is 'index[pointer]'
				if(indexType.isPointerLike()) {
					ExpressionNode tmpExpr = index;
					index = expr;
					expr = tmpExpr;
					
					Type tmpType = indexType;
					indexType = exprType;
					exprType = tmpType;
				}

				// ptr[idx] is identical to (*(ptr+idx))
				expr = new UnaryExpressionNode(
					pos,
					addPointer(expr, index, pos),
					Punctuator.DEREFERENCE,
					exprType.dereference()
				);
				continue;
			}
			
			/* § 6.3.2.2 Function calls
			 * 
			 * '(' ( assignment-expression ( ',' assignment-expression )* )? ')'
			 */
			if(equal("(")) {
				// must be either function or pointer to function
				
				if(!exprType.isFunction() && !(exprType.isPointer() && exprType.toPointer().getBase().isFunction()))
					error(expr, "Invoked object is not a function or function pointer");
				
				FunctionType fnType;
				
				if(exprType.isPointer())
					fnType = exprType.dereference().toFunction();
				else fnType = exprType.toFunction();
				
				List<Parameter> parameters = fnType.getParameters();
				List<ExpressionNode> arguments = new ArrayList<>();

				boolean prototype = fnType.isPrototype();
				boolean variadic = fnType.isVariadic();
				
				final int n = parameters.size();
				
				while(!optional(")")) {
					
					if(!arguments.isEmpty())
						require(",");
					
					next();
					
					ExpressionNode arg = nextAssignment();
					
					if(prototype && !variadic && arguments.size() >= n)
						error(arg, "Too many arguments for function call");
					
					Type argType = arg.getType();
					
					if(!prototype || (variadic && arguments.size() >= n)) { // default argument promotion
						
						if(argType.isNumber()) {
							NumericValueType num = argType.toNumber().getNumericType();

							// promote float to double
							if(num == NumericValueType.FLOAT)
								arg = new CastExpressionNode(
									arg.getPosition(),
									arg,
									Type.DOUBLE
								);

							// promote (un)signed char/short to (un)signed int
							else switch(num) {
							case SIGNED_CHAR:
							case SIGNED_SHORT:
								arg = new CastExpressionNode(
									arg.getPosition(),
									arg,
									Type.UINT
								);
								break;
									
							case UNSIGNED_CHAR:
							case UNSIGNED_SHORT:
								arg = new CastExpressionNode(
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
							arg = new CastExpressionNode(
								arg.getPosition(),
								arg,
								parameters.get(idx).type()
							);
					}
					
					arguments.add(arg);
				}
				
				if(prototype && arguments.size() < n)
					error("Too few arguments for function call");
				
				if(exprType.isPointer()) // dereference function pointer
					expr = new UnaryExpressionNode(expr.getPosition(), expr, Punctuator.DEREFERENCE, fnType);
				
				expr = new CallExpressionNode(expr.getPosition(), expr, arguments, fnType);
				continue;
			}
			
			if(equal(".", "->")) {
				
			}
			
			if(equal("++", "--")) {
				
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
		return null;
	}

	/*
	 * § 6.3.4 Cast operators
	 * 
	 * cast-expression = ( '(' type-name ')' )* unary-expression
	 */
	public ExpressionNode nextCast() {
		return null;
	}
	
	/*
	 * § 6.3.5 Multiplicative operators
	 * 
	 * multiplicative-expression = cast-expression ( ( '*' | '/' | '%' ) cast-expression )*
	 */
	public ExpressionNode nextMultiplicative() {
		return null;
	}
	
	/*
	 * § 6.3.6 Additive operators
	 * 
	 * additive-expression = multiplicative-expression ( ( '+' | '-' ) multiplicative-expression )*
	 */
	public ExpressionNode nextAdditive() {
		return null;
	}
	
	/*
	 * § 6.3.7 Bitwise shift operators
	 * 
	 * shift-expression = additive-expression ( ( '<<' | '>>' ) additive-expression )*
	 */
	public ExpressionNode nextShift() {
		return null;
	}
	
	/*
	 * § 6.3.8 Relational operators
	 * 
	 * relational-expression = shift-expression ( ( '<' | '>' | '<=' | '>=' ) shift-expression )*
	 */
	public ExpressionNode nextRelational() {
		return null;
	}
	
	/*
	 * § 6.3.9 Equality operators
	 * 
	 * equality-expression = relational-expression ( ( '==' | '!=' ) relational-expression )*
	 */
	public ExpressionNode nextEquality() {
		return null;
	}
	
	/*
	 * § 6.3.10 Bitwise AND operator
	 * 
	 * AND-expression = equality-expression ( '&' equality-expression )*
	 */
	public ExpressionNode nextBitwiseAND() {
		return null;
	}
	
	/*
	 * § 6.3.11 Bitwise exclusive OR operator
	 * 
	 * exclusive-OR-expression = AND-expression ( '^' AND-expression )*
	 */
	public ExpressionNode nextBitwiseXOR() {
		return null;
	}
	
	/*
	 * § 6.3.12 Logical inclusive OR operator
	 * 
	 * inclusive-OR-expression = exclusive-OR-expression ( '|' exclusive-OR-expression )*
	 */
	public ExpressionNode nextBitwiseOR() {
		return null;
	}
	
	/*
	 * § 6.3.13 Logical AND operator
	 * 
	 * logical-AND-expression = inclusive-OR-expression ( '&&' inclusive-OR-expression )*
	 */
	public ExpressionNode nextLogicalAND() {
		return null;
	}
	
	/*
	 * § 6.3.14 Logical OR operator
	 * 
	 * logical-OR-expression = logical-AND-expression ( '||' logical-AND-expression )*
	 */
	public ExpressionNode nextLogicalOR() {
		return null;
	}
	
	/*
	 * § 6.3.15 Conditional operator
	 * 
	 * conditional-expression = logical-OR-expression ( '?' expression ':' conditional-expression)?
	 */
	public ExpressionNode nextConditional() {
		return null;
	}
	
	/*
	 * § 6.3.16 Assignment operators
	 * 
	 * assignment-expression = conditional-expression
	 * 						| unary-expression ( '=' | '*=' | '/=' | '%=' | '+=' | '-=' | '<<=' | '>>=' | '&=' | '^=' | '|=' ) assignment-expression
	 */
	public ExpressionNode nextAssignment() {
		return null;
	}
	
	/*
	 * § 6.3.17 Comma operator
	 * 
	 * expression = assignment-expression ( ',' assignment-expression )*
	 */
	public ExpressionNode nextExpression() {
		return null;
	}
	
	/*
	 * § 6.4 Constant expressions
	 * 
	 * constant-expression = conditional-expression
	 */
	public ExpressionNode nextConstantExpression() {
		// TODO
		return nextConditional();
	}
	
	/* For an expression ptr+off (where ptr is a pointer and off is an integer),
	 * 'off' specifies the offset in number of elements (not bytes) and must
	 * therefore be scaled accordingly (off * sizeof(ptr))
	 */
	private ExpressionNode addPointer(ExpressionNode pointer, ExpressionNode offset, Position pos) {
		
		// offset * sizeof(pointer)
		ExpressionNode scaledOffset = new BinaryExpressionNode(
			pos,
			offset,
			new NumberLiteralExpressionNode(
				pos,
				BigInteger.valueOf(pointer.getType().sizeof()),
				NumericValueType.SIZE.asType()
			),
			Punctuator.MULTIPLY,
			pointer.getType()
		);
		
		// pointer + offset * sizeof(pointer)
		return new BinaryExpressionNode(
			pos,
			pointer,
			scaledOffset,
			Punctuator.ADD,
			pointer.getType()
		);
	}
	
}
