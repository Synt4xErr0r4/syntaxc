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
import java.util.Optional;

import at.syntaxerror.syntaxc.lexer.Keyword;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.misc.Warning;
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
import at.syntaxerror.syntaxc.type.EnumType;
import at.syntaxerror.syntaxc.type.FunctionType.Parameter;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.StructType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class DeclarationParser extends AbstractParser {
	
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
	
	private final Parser parser;

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

	public Type nextSpecifierQualifierList() {
		return nextDeclarationSpecifiers(false).getRight();
	}
	
	public Pair<Optional<Token>, Type> nextDeclarationSpecifiers() {
		return nextDeclarationSpecifiers(true);
	}
	
	private Pair<Optional<Token>, Type> nextDeclarationSpecifiers(boolean withStorage) {
		Position pos = current.getPosition();
		
		Token storage = null;
		
		boolean hasConst = false;
		boolean hasVolatile = false;
		
		int specifiers = 0;
		
		Type type = null;
		
		while(true) {
			Token tok = current;
			
			// storage class specifiers
			if(skip("typedef", "extern", "static", "auto", "register")) {
				if(!withStorage)
					error(previous, "Unexpected storage-class specifier");
				
				if(storage != null)
					error(previous, "At most one storage-class specifier is allowed");
				
				storage = tok;
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
				type = nextEnumSpecifier();
			}
			
			else if(specifiers == 0 && equal(TokenType.IDENTIFIER)) {
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
		
		if(storage != null || specifiers != 0 || hasConst || hasVolatile)
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

		return Pair.of(Optional.ofNullable(storage), type);
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
			
			if(structTag != null) {
				
				if(structTag.getType().isUnion() == union) {
					type = structTag.getType().toStructLike();
					inherited = true;
				}
				else error(pos, "Declared »%s« as another type", tag);
				
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
				
				Type memberType = nextSpecifierQualifierList();
				
				while(true) {
					
					Declarator declarator = null;
					int bitWidth = 0;
					boolean bitfield = false;
					
					Positioned memberPos = null;
					
					if(!equal(":"))
						memberPos = declarator = nextDeclarator();

					if(skip(":")) {
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

						BigInteger value = parser.nextIntegerConstantExpression();
						
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
					
					consume(",", ";");
					
					if(previous.is(";"))
						break;
				}
				
			}
			
		}
		else if(tag == null)
			error(pos, "Expected declaration list for %s", name);
		
		return type;
	}
	
	private EnumType nextEnumSpecifier() {
		Position pos = current.getPosition();
		
		SymbolTable symtab = getSymbolTable();
		
		String tag = null;

		if(optional(TokenType.IDENTIFIER)) {
			tag = current.getString();
			pos = current.getPosition();
			
			SymbolTag enumTag = symtab.findTag(tag);
			
			if(enumTag != null) {
				
				if(enumTag.getType().isEnum())
					error(pos, Warning.CONTINUE, "Redeclaration of »enum %s«", tag);
				
				else error(pos, Warning.CONTINUE, "Declared »enum %s« as another type", tag);
				
				note(enumTag, "Previously declared here");
				terminate();
			}
		}
		
		EnumType type = tag == null
				? EnumType.forAnonymousEnum()
				: EnumType.forEnum(tag);
		
		if(tag != null)
			symtab.addTag(SymbolTag.of(pos, tag, type));
		
		if(optional("{")) {
			type.setComplete();
			
			var enumerators = type.getEnumerators();
			
			while(true) {
				Token tok = require(TokenType.IDENTIFIER);

				String name = tok.getString();
				
				if(enumerators.containsKey(name))
					error(tok, "Duplicate enumerator in enum");
				
				if(optional("=")) {
					next();
					type.addEnumerator(tok, parser.nextIntegerConstantExpression());
				}
				
				else {
					next();
					type.addEnumerator(tok);
				}

				symtab.addObject(SymbolObject.enumerator(tok, name, type, enumerators.get(name).value()));
				
				if(expect(",", "}").is("}")) {
					next();
					break;
				}
			}
			
		}
		else if(tag == null)
			error(pos, "Expected enumerator list for enum");
		
		return type;
	}

	public Declarator nextDeclarator() {
		List<Pointer> pointers = nextPointers();
		
		Pair<String, Declarator> nameOrNested = null;
		Position pos = null;
		
		if(skip(TokenType.IDENTIFIER)) {
			pos = previous.getPosition();
			nameOrNested = Pair.ofLeft(previous.getString());
		}
		
		else if(skip("(")) {
			Declarator nested = nextDeclarator();
			
			pos = nested.getPosition();
			nameOrNested = Pair.ofRight(nested);
			
			consume(")");
		}
		
		else error("Expected name for declarator");
		
		var postfixes = nextDeclaratorPostfixes(pos, true);
		
		// redundant parentheses
		if(nameOrNested.hasRight() && pointers.isEmpty() && postfixes.isEmpty())
			return nameOrNested.getRight();
		
		return new Declarator(pos, nameOrNested, pointers, postfixes);
	}
	
	private Declarator nextAbstractDeclarator() {
		return nextAbstractDeclarator(false);
	}
	
	private Declarator nextAbstractDeclarator(boolean optional) {
		List<Pointer> pointers = nextPointers();

		Pair<String, Declarator> nameOrNested = Pair.empty();
		Position pos = null;
		
		mark();
		
		/* abstract declarators start with either
		 * - '*' (pointer)
		 * - '(' (nested declarator or function postfix)
		 * - '[' (array postfix)
		 * 
		 * hence we check for existence of one of these tokens,
		 * because otherwise it could still be a valid function postfix
		 */
		if(skip("(") && equal("*", "(", "[")) {
			Declarator nested = nextAbstractDeclarator();
			
			pos = nested.getPosition();
			nameOrNested = Pair.ofRight(nested);
			
			consume(")");
		}
		
		else reset();

		var postfixes = nextDeclaratorPostfixes(pos, false);
		
		if(pointers.isEmpty() && postfixes.isEmpty()) {
			// redundant parentheses
			if(nameOrNested.hasRight())
				return nameOrNested.getRight();

			// report if we found neither pointers, a nested declarator, or function/array postfixes
			if(nameOrNested.hasNeither()) {
				if(optional)
					return null;
				
				error("Expected abstract declarator");
			}
		}
		
		return new Declarator(pos, nameOrNested, pointers, postfixes);
	}
	
	private Optional<Declarator> nextOptionalDeclarator() {
		mark();
		
		while(skip("*", "(", "[", "const", "volatile"))
			;
		
		if(equal(TokenType.IDENTIFIER)) {
			reset();
			
			return Optional.of(nextDeclarator());
		}
		
		reset();
		
		return Optional.ofNullable(nextAbstractDeclarator(true));
	}
	
	private List<DeclaratorPostfix> nextDeclaratorPostfixes(Positioned pos, boolean allowKAndR) {
		List<DeclaratorPostfix> postfixes = new ArrayList<>();
		
		while(true) {
			if(equal("[")) {

				int length;
				
				if(optional("]"))
					length = ArrayType.SIZE_UNKNOWN;
				
				else {
					next();
				
					BigInteger value = parser.nextIntegerConstantExpression();
					
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
					
					expect("]");
				}
				
				next();
				
				postfixes.add(new ArrayDeclaratorPostfix(length));
				continue;
			}
			
			if(equal("(")) {
				List<Parameter> parameters = new ArrayList<>();
				
				boolean kAndR = false;
				boolean variadic = false;
				
				next();
				
				if(allowKAndR && equal(TokenType.IDENTIFIER) && !isTypeName()) {
					warn(previous, Warning.K_AND_R, "Declared function using K&R syntax");
					
					kAndR = true;
					
					while(true) {
						parameters.add(new Parameter(
							getPosition(),
							current.getString(),
							null,
							Optional.empty()
						));
						
						if(require(",", ")").is(")")) {
							next();
							break;
						}
						
						require(TokenType.IDENTIFIER);
					}
					
				}
				
				else if(skip(")"))
					kAndR = true;
				
				else {
					
					while(true) {
						if(equal("...")) {
							if(parameters.isEmpty())
								error("Missing parameter name before ellipsis");

							require(")");
							next();
							
							variadic = true;
							break;
						}
						
						var declSpecs = nextDeclarationSpecifiers();

						Positioned paramPos = previous;
						
						Type type = declSpecs.getRight();
						
						if(type.isVoid()) {
							
							if(!parameters.isEmpty() || !declSpecs.getLeft().isEmpty() || type.isConst() || type.isVolatile())
								error(paramPos, "Illegal type »void« for parameter");
							
							if(!declSpecs.getLeft().isEmpty())
								error(paramPos, "Illegal storage specifiers for »void«");
							
							if(type.isConst() || type.isVolatile())
								error(paramPos, "Illegal type qualifiers for »void«");
							
							reread();
							require(")");
							next();
							break;
						}
						
						Optional<Declarator> opt = nextOptionalDeclarator();
						
						String name = null;
						
						if(opt.isPresent()) {
							Declarator declarator = opt.get();
							
							paramPos = declarator;
							
							type = declarator.merge(type);
							name = declarator.getName();
						}
						
						Optional<Keyword> storageSpec = declSpecs
							.getLeft()
							.map(Token::getKeyword);
						
						if(storageSpec.isPresent() && storageSpec.get() != Keyword.REGISTER)
							error(pos, "Illegal storage-class specifier for parameter (only »register« is permitted)");
						
						parameters.add(new Parameter(paramPos, name, type, storageSpec));

						consume(",", ")");
						
						if(previous.is(")"))
							break;
					}
					
				}

				postfixes.add(new FunctionDeclaratorPostfix(parameters, variadic, kAndR));
				continue;
			}

			break;
		}
		
		return postfixes;
	}
	
	private List<Pointer> nextPointers() {
		List<Pointer> pointers = new ArrayList<>();
		
		while(skip("*")) {
			var quals = nextQualifiers();
			pointers.add(0, new Pointer(quals.getLeft(), quals.getRight()));
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
	
	public Initializer nextInitializer() {
		if(skip("{")) {
			Position pos = previous.getPosition();
			
			List<Initializer> initializers = new ArrayList<>();
			
			while(true) {
				initializers.add(nextInitializer());
				
				consume(",", "}");

				if(previous.is("}"))
					break;
			}
			
			return new Initializer(pos, Pair.ofRight(initializers));
		}
		
		ExpressionNode value = parser.nextAssignmentExpression();
		
		return new Initializer(value.getPosition(), Pair.ofLeft(value));
	}
	
	@Override
	public boolean isTypeName() {
		if(equal("void", "char", "short", "int", "long", "float", "double",
				"signed", "unsigned", "struct", "union", "enum", "const", "volatile"))
			return true;
		
		if(equal(TokenType.IDENTIFIER))
			return Optional.ofNullable(
				getSymbolTable()
					.findObject(current.getString())
			).map(SymbolObject::isTypedef)
			.orElse(false);
		
		return false;
	}
	
	@Override
	public Type nextTypeName() {
		Type type = nextSpecifierQualifierList();
		
		Declarator decl = nextAbstractDeclarator(true);
		
		if(decl != null)
			type = decl.merge(type);
		
		return type;
	}

}
