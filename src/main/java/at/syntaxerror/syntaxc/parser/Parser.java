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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import at.syntaxerror.syntaxc.lexer.Keyword;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.parser.node.Node;
import at.syntaxerror.syntaxc.parser.node.declaration.Declarator;
import at.syntaxerror.syntaxc.parser.node.declaration.DeclaratorPostfix;
import at.syntaxerror.syntaxc.parser.node.declaration.Initializer;
import at.syntaxerror.syntaxc.parser.node.declaration.Pointer;
import at.syntaxerror.syntaxc.parser.node.declaration.postfix.ArrayDeclaratorPostfix;
import at.syntaxerror.syntaxc.parser.node.declaration.postfix.FunctionDeclaratorPostfix;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.SymbolTable;
import at.syntaxerror.syntaxc.symtab.SymbolTag;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.ArrayType;
import at.syntaxerror.syntaxc.type.FunctionType.Parameter;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.StructType;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public class Parser extends AbstractParser {

	private List<Token> tokens;
	private int index;
	
	private Stack<Integer> marks;
	
	private SymbolTable globalSymbolTable;
	
	private Stack<SymbolTable> symbolTables;
	
	private ExpressionParser expressionParser;
	
	public Parser(List<Token> tokens) {
		this.tokens = tokens;
		
		marks = new Stack<>();
		
		globalSymbolTable = new SymbolTable();
		
		symbolTables = new Stack<>();
		symbolTables.push(globalSymbolTable);
		
		expressionParser = new ExpressionParser(this);
	}
	
	@Override
	public void markTokenState() {
		marks.push(index);
	}
	
	@Override
	public void resetTokenState() {
		index = marks.pop();
	}
	
	@Override
	public void unmarkTokenState() {
		marks.pop();
	}
	
	@Override
	public Token readNextToken() {
		if(index < 0 || index >= tokens.size())
			return null;
		
		return tokens.get(index++);
	}
	
	@Override
	public SymbolTable getSymbolTable() {
		return symbolTables.peek();
	}
	
	private void enterScope() {
		symbolTables.push(getSymbolTable().newChild());
	}
	
	private void leaveScope() {
		symbolTables.pop();
	}
	
	private ExpressionNode nextExpression() {
		try {
			expressionParser.next();
			return expressionParser.nextExpression();
		} finally {
			next();
		}
	}
	
	private BigInteger nextIntegerConstantExpression() {
		try {
			expressionParser.next();
			return expressionParser.nextIntegerConstantExpression();
		} finally {
			next();
		}
	}
	
	private Number nextArithmeticConstantExpression() {
		try {
			expressionParser.next();
			return expressionParser.nextArithmeticConstantExpression();
		} finally {
			next();
		}
	}
	
	private void nextDeclaration() {
		var declSpec = nextDeclarationSpecifiers(true);
		
		System.out.println(declSpec);
	}
	
	private static final int VOID =			(1 << 0);
	private static final int CHAR =			(1 << 1);
	private static final int SHORT =		(1 << 2);
	private static final int INT =			(1 << 3);
	private static final int LONG =			(1 << 4);
	private static final int FLOAT =		(1 << 5);
	private static final int DOUBLE =		(1 << 6);
	private static final int SIGNED =		(1 << 7);
	private static final int UNSIGNED =		(1 << 8);
	private static final int INHERIT =		(1 << 9);
	private static final int DUPLICATE =	(1 << 10); // set when one of the specifiers above occurs multiple times
	
	private Pair<Set<Keyword>, Type> nextDeclarationSpecifiers(boolean withStorage) {
		Position pos = current.getPosition();
		
		Set<Keyword> storage = new HashSet<>();
		
		boolean hasConst = false;
		boolean hasVolatile = false;
		
		int specifiers = 0;
		
		Type type = null;
		
		while(true) {
			Token tok = current;
			
			// storage class specifiers
			if(withStorage && skip("typedef", "extern", "static", "auto", "register")) {
				storage.add(tok.getKeyword());
				continue;
			}

			// type qualifiers
			
			if(skip("const")) {
				if(hasConst)
					warn(tok, Warning.DUPLICATE_QUALIFIER, "Duplicate type qualifier »const«");
				
				hasConst = true;
				continue;
			}
			
			if(skip("volatile")) {
				if(hasVolatile)
					warn(tok, Warning.DUPLICATE_QUALIFIER, "Duplicate type qualifier »volatile«");
				
				hasVolatile = true;
				continue;
			}
			
			// type specifiers
			int spec = 0;
			
				 if(skip("void"))		spec = VOID;
			else if(skip("char"))		spec = CHAR;
			else if(skip("short"))		spec = SHORT;
			else if(skip("int"))		spec = INT;
			else if(skip("long"))		spec = LONG;
			else if(skip("float"))		spec = FLOAT;
			else if(skip("double"))		spec = DOUBLE;
			else if(skip("signed"))		spec = SIGNED;
			else if(skip("unsigned"))	spec = UNSIGNED;
			
			else if(equal("struct", "union")) {
				spec = INHERIT;
				type = nextStructSpecifier();
			}

			else if(equal("enum")) {
				spec = INHERIT;
			}
			
			else if(equal(TokenType.IDENTIFIER)) {
				SymbolObject obj = getSymbolTable().findObject(tok.getString());
				
				if(obj != null && obj.isTypedef()) {
					next();

					spec = INHERIT;
					type = obj.getType();
				}
			}
			
			if(spec == 0)
				break;
			
			if((specifiers & spec) != 0) // specifier was already set
				specifiers |= DUPLICATE;
			else specifiers |= spec;
		}
		
		if(!storage.isEmpty() || specifiers != 0 || hasConst || hasVolatile)
			pos = pos.range(previous);
		
		switch(specifiers) {
		case VOID:
			type = Type.VOID;
			break;
			
		case CHAR:
			type = NumericValueType.CHAR.asType();
			break;
			
		case SIGNED | CHAR:
			type = Type.CHAR;
			break;
			
		case UNSIGNED | CHAR:
			type = Type.UCHAR;
			break;
			
		case SHORT:
		case SHORT | INT:
		case SIGNED | SHORT:
		case SIGNED | SHORT | INT:
			type = Type.SHORT;
			break;
			
		case UNSIGNED | SHORT:
		case UNSIGNED | SHORT | INT:
			type = Type.USHORT;
			break;
			
		case 0:
		case SIGNED:
			warn(pos, Warning.IMPLICIT_INT, "Type is implicitly »signed int«");
		case INT:
		case SIGNED | INT:
			type = Type.INT;
			break;

		case UNSIGNED:
			warn(pos, Warning.IMPLICIT_INT, "Type is implicitly »unsigned int«");
		case UNSIGNED | INT:
			type = Type.UINT;
			break;
			
		case LONG:
		case LONG | INT:
		case SIGNED | LONG:
		case SIGNED | LONG | INT:
			type = Type.LONG;
			break;

		case UNSIGNED | LONG:
		case UNSIGNED | LONG | INT:
			type = Type.ULONG;
			break;
			
		case FLOAT:
			type = Type.FLOAT;
			break;
			
		case DOUBLE:
			type = Type.DOUBLE;
			break;
			
		case LONG | DOUBLE:
			type = Type.LDOUBLE;
			break;
			
		case INHERIT: // inherit from struct/union/enum/typedef name
			break;
		
		default:
			error("Illegal type specifier combination");
			break;
		}

		if(hasConst) type = type.asConst();
		if(hasVolatile) type = type.asVolatile();

		return Pair.of(storage, type);
	}
	
	private StructType nextStructSpecifier() {
		boolean union = current.is("union");

		String name = union ? "union" : "struct";
		Position pos = current.getPosition();
		
		next();
		
		String tag = null;
		StructType type = null;
		boolean inherited = false;
		
		if(equal(TokenType.IDENTIFIER)) {
			tag = current.getString();
			pos = current.getPosition();
			
			next();

			SymbolTag structTag = getSymbolTable().findTag(tag);
			
			if(structTag != null && structTag.getType().isStructLike()) {
				type = structTag.getType().toStructLike();
				
				if(union != type.isUnion())
					error(pos, "Declared »%s« as another type", tag);
				
				inherited = true;
			}
		}
		
		if(type == null)
			type = tag == null
				? union
					? StructType.forAnonymousUnion()
					: StructType.forAnonymousStruct()
				: union
					? StructType.forUnion(tag)
					: StructType.forStruct(tag);
		
		if(tag != null)
			getSymbolTable().addTag(SymbolTag.of(pos, tag, type));
		
		if(skip("{")) {
		
			if(inherited && !type.isIncomplete())
				error(pos, "Redeclaration of »%s«", tag);
		
			type.setComplete();
			
			while(!skip("}")) {
				
				Type memberType = nextDeclarationSpecifiers(false).getSecond();
				
				while(true) {
					
					Declarator declarator = null;
					int bitWidth = 0;
					boolean bitfield = false;
					
					Positioned memberPos = null;
					
					if(!equal(":"))
						memberPos = declarator = nextDeclarator();

					if(equal(":")) {
						memberPos = current;
						
						do {
							if(memberType.isInteger()) {
								
								NumericValueType valType = memberType.toNumber().getNumericType();
								
								if(valType == NumericValueType.SIGNED_INT || valType == NumericValueType.UNSIGNED_INT)
									break;
								
							}
							
							error(memberPos, "Expected »int« or »unsigned int« for bit-field");
						}
						while(false);
						
						BigInteger value = nextIntegerConstantExpression();
						
						if(value.compareTo(BigInteger.ZERO) < 0)
							error(memberPos, "Illegal negative width for bit-field");
						
						if(value.compareTo(BigInteger.valueOf(NumericValueType.UNSIGNED_INT.getSize() * 8)) > 0)
							error(memberPos, "Width for bit-field is too big for its type");
						
						bitfield = true;
						bitWidth = value.intValue();
					}

					if(declarator == null)
						type.addAnonymousMember(memberPos, memberType, bitfield, bitWidth);
					else type.addMember(memberPos, declarator.getName(), memberType, bitfield, bitWidth);
					
					if(!skip(",", ";"))
						expected(",", ";");
					
					if(previous.is(";"))
						break;
				}
				
			}
			
			System.out.println(type.getMembers());
			
		}
		else if(type == null)
			error(pos, "Expected declaration list for %s", name);
		
		return type;
	}
	
	private Pair<Declarator, Initializer> nextInitDeclarator() {
		Declarator decl = nextDeclarator();
		
		if(skip("="))
			return Pair.of(decl, nextInitializer());
		
		return Pair.ofFirst(decl);
	}
	
	private Declarator nextDeclarator() {
		List<Pointer> pointers = nextPointers();
		
		Pair<String, Declarator> nameOrNested = null;
		Position pos = null;
		
		if(skip(TokenType.IDENTIFIER)) {
			pos = previous.getPosition();
			nameOrNested = Pair.ofFirst(previous.getString());
		}
		
		else if(skip("(")) {
			Declarator nested = nextDeclarator();
			
			pos = nested.getPosition();
			nameOrNested = Pair.ofSecond(nested);
		}
		
		else error("Expected name for declarator");
		
		List<DeclaratorPostfix> postfixes = new ArrayList<>();
		
		while(true) {
			if(optional("[")) {

				int length;
				
				if(optional("]"))
					length = ArrayType.SIZE_UNKNOWN;
				
				else {
				
					BigInteger value = nextIntegerConstantExpression();
					
					if(value.compareTo(BigInteger.ZERO) < 0)
						error(pos, "Illegal negative length for array");
					
					/* implementation limit
					 * 
					 * a char array of that size (2^31-1) would take up 2GiB
					 * of space, so this should never cause any problems
					 */
					if(value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0)
						error(pos, "Length for array is too large");
					
					length = value.intValue();
					
				}
				
				postfixes.add(new ArrayDeclaratorPostfix(length));
				continue;
			}
			
			if(optional("(")) {
				List<Parameter> parameters = new ArrayList<>();
				
				boolean kAndR = false;
				boolean variadic = false;
				
				next();
				
				if(equal(TokenType.IDENTIFIER)) {
					warn(previous, Warning.K_AND_R, "Declared function using K&R syntax");
					
					kAndR = true;
					
					while(true) {
						parameters.add(new Parameter(getPosition(), current.getString(), null, Set.of()));
						
						if(require(",", ")").is(")"))
							break;
					}
					
				}
				
				else {
					
					while(!skip(")")) {
						var declSpecs = nextDeclarationSpecifiers(true);
						
						
						
					}
					
				}
				
				postfixes.add(new FunctionDeclaratorPostfix(parameters, variadic, kAndR));
				continue;
			}

			break;
		}
		
		return new Declarator(pos, nameOrNested, pointers, postfixes);
	}
	
	private List<Pointer> nextPointers() {
		List<Pointer> pointers = new ArrayList<>();
		
		while(skip("*")) {
			var quals = nextQualifiers();
			pointers.add(0, new Pointer(quals.getFirst(), quals.getSecond()));
		}
		
		return pointers;
	}
	
	private Pair<Boolean, Boolean> nextQualifiers() {
		boolean hasConst = false;
		boolean hasVolatile = false;
		
		while(true) {
			Token tok = current;
			
			if(skip("const")) {
				if(hasConst)
					warn(tok, Warning.DUPLICATE_QUALIFIER, "Duplicate type qualifier »const«");
				
				hasConst = true;
				continue;
			}
			
			if(skip("volatile")) {
				if(hasVolatile)
					warn(tok, Warning.DUPLICATE_QUALIFIER, "Duplicate type qualifier »volatile«");
				
				hasVolatile = true;
				continue;
			}
			
			break;
		}
		
		return Pair.of(hasConst, hasVolatile);
	}
	
	private Initializer nextInitializer() {
		return null;
	}
	
	public List<Node> parse() {
		List<Node> nodes = new ArrayList<>();
		enterScope();
		
		getSymbolTable()
			.addObject(SymbolObject.local(null, "a", Type.CHAR.arrayOf(2).arrayOf(3)));

		next();
		nextDeclaration();
		
		leaveScope();
		
		return nodes;
	}

}
