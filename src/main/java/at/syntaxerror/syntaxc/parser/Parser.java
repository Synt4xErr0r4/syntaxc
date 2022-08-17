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
import java.util.stream.Collectors;

import at.syntaxerror.syntaxc.lexer.Keyword;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.optimizer.ExpressionOptimizer;
import at.syntaxerror.syntaxc.parser.node.FunctionNode;
import at.syntaxerror.syntaxc.parser.node.GlobalVariableNode;
import at.syntaxerror.syntaxc.parser.node.Node;
import at.syntaxerror.syntaxc.parser.node.declaration.Declarator;
import at.syntaxerror.syntaxc.parser.node.declaration.Initializer;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.statement.StatementNode;
import at.syntaxerror.syntaxc.symtab.Linkage;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.SymbolTable;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.FunctionType;
import at.syntaxerror.syntaxc.type.FunctionType.Parameter;
import at.syntaxerror.syntaxc.type.Type;
import at.syntaxerror.syntaxc.type.TypeUtils;

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
	private DeclarationParser declarationParser;
	private StatementParser statementParser;
	
	public Parser(List<Token> tokens) {
		this.tokens = tokens;
		
		marks = new Stack<>();
		
		globalSymbolTable = new SymbolTable();
		
		symbolTables = new Stack<>();
		symbolTables.push(globalSymbolTable);
		
		declarationParser = new DeclarationParser(this);
		expressionParser = new ExpressionParser(this, declarationParser);
		statementParser = new StatementParser(this, declarationParser, expressionParser);
	}
	
	@Override
	protected void sync() {
		expressionParser.current = declarationParser.current = current;
		expressionParser.previous = declarationParser.previous = previous;

		declarationParser.current = declarationParser.current = current;
		declarationParser.previous = declarationParser.previous = previous;
		
		statementParser.current = statementParser.current = current;
		statementParser.previous = statementParser.previous = previous;
	}

	@Override
	public void reread() {
		if(index >= 0)
			--index;
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
	
	public void enterScope() {
		symbolTables.push(getSymbolTable().newChild());
	}
	
	public void leaveScope() {
		symbolTables.pop();
	}

	public ExpressionNode nextAssignmentExpression() {
		try {
			return expressionParser.nextAssignment();
		} finally {
			next();
		}
	}
	
	public ExpressionNode nextExpression() {
		try {
			return ExpressionOptimizer.optimize(expressionParser.nextExpression());
		} finally {
			next();
		}
	}
	
	public BigInteger nextIntegerConstantExpression() {
		try {
			return expressionParser.nextIntegerConstantExpression();
		} finally {
			next();
		}
	}
	
	public Number nextArithmeticConstantExpression() {
		try {
			return expressionParser.nextArithmeticConstantExpression();
		} finally {
			next();
		}
	}
	
	private List<Node> nextExternalDeclaration() {
		if(equal(";")) // skip single semicola
			return List.of();
		
		List<Node> nodes = new ArrayList<>();
		
		var declSpecs = declarationParser.nextDeclarationSpecifiers();
		
		Keyword storageSpec = declSpecs
			.getLeft()
			.map(Token::getKeyword)
			.orElse(null);
		
		if(storageSpec == Keyword.AUTO)
			error("»auto« is not allowed at this location");

		if(storageSpec == Keyword.REGISTER)
			error("»register« is not allowed at this location");

		boolean external = storageSpec == Keyword.EXTERN;
		boolean internal = storageSpec == Keyword.STATIC;
		boolean typedef = storageSpec == Keyword.TYPEDEF;
		
		if(external && internal)
			error("»static« and »extern« are mutually exclusive");
		
		if(typedef && (external || internal))
			error("»typedef« may not occur together with other storage-class specifiers");
		
		if(equal(";")) {
			warnUseless(declSpecs.getRight());
			return List.of();
		}
		
		Linkage linkage = internal ? Linkage.INTERNAL : Linkage.EXTERNAL;
		
		Declarator decl = declarationParser.nextDeclarator();
		
		Type baseType = declSpecs.getRight();
		Type type = decl.merge(baseType);
		
		if(!typedef && decl.isFunctionDeclarator() && !equal("=", ";")) {
			
			enterScope();
			
			FunctionType funType = type.toFunction();
			
			if(funType.isKAndR()) {
			
				Set<String> parameterNames = funType
					.getParameters()
					.stream()
					.map(Parameter::name)
					.collect(Collectors.toUnmodifiableSet());
	
				Set<String> parameterNamesFound = new HashSet<>();
				
				while(!skip("{")) {
					if(!declarationParser.isTypeName() && !equal("typedef", "extern", "static", "auto", "register"))
						error("Expected function body");
					
					declSpecs = declarationParser.nextDeclarationSpecifiers();
					
					if(!declSpecs.getLeft().isEmpty())
						error(declSpecs.getLeft().get(), "Illegal storage-class specifier for function parameter");
					
					if(equal(";")) {
						warnUseless(declSpecs.getRight());
						next();
						continue;
					}
					
					baseType = declSpecs.getRight();
					
					while(true) {
						decl = declarationParser.nextDeclarator();
						
						String name = decl.getName();
						
						if(!parameterNames.contains(name))
							error(decl, "Declaration for non-existent parameter »%s«", name);
						
						if(parameterNamesFound.contains(name))
							error(decl, "Duplicate declaration for parameter »%s«", name);
						
						if(equal("="))
							error("Illegal initializer for function parameter »%s«", name);
						
						Type paramType = decl.merge(baseType);
						
						SymbolObject obj = SymbolObject.local(decl, name, paramType);
						obj.setUnused(false);
						
						getSymbolTable().addObject(obj);
						
						parameterNamesFound.add(name);
						
						consume(",", ";");
						
						if(previous.is(";"))
							break;
					}
				}
				
				for(String name : parameterNames)
					if(!parameterNamesFound.contains(name)) {

						SymbolObject obj = SymbolObject.local(decl, name, Type.INT);
						obj.setUnused(false);
						
						getSymbolTable().addObject(obj);
						
					}
			
			}
			
			else consume("{");

			String name = decl.getName();
			
			StatementNode body = statementParser.nextFunctionBody(funType.getReturnType(), name);
			
			var declarations = statementParser.getDeclarations();
			
			statementParser.getGlobalVariables()
				.forEach(obj -> nodes.add(new GlobalVariableNode(obj)));
			
			leaveScope();
			
			SymbolTable symtab = getSymbolTable();
			
			if(symtab.hasObjectInScope(name)) {
				SymbolObject obj = symtab.findObjectInScope(name);

				if(!TypeUtils.isEqual(type, obj.getType())) {
					error(decl, Warning.CONTINUE, "Incompatible types for »%s«", name);
					note(obj, "Previously declared here");
					terminate();
				}
				
				if(!obj.isPrototype()) {
					error(decl, Warning.CONTINUE, "Redefinition of »%s«", name);
					note(obj, "Previously defined here");
					terminate();
				}
				
				if(internal && obj.getFunctionData().linkage() == Linkage.EXTERNAL) {
					error(decl, Warning.CONTINUE, "Cannot redeclare »%s« as »static« after previous non-static declaration", name);
					note(obj, "Previously declared here");
					terminate();
				}
			}
			
			SymbolObject obj = SymbolObject.function(
				decl,
				name,
				funType,
				linkage
			);
			
			symtab.addObject(obj);
			
			nodes.add(new FunctionNode(obj, declarations, body));
		}
		else while(true) {
			Initializer init = null;
			
			if(skip("=")) {
				if(type.isFunction())
					error(decl, "Cannot initialize function like a variable");
				
				init = declarationParser.nextInitializer();
			}
			
			SymbolObject obj = registerDeclaration(
				decl,
				type,
				decl.getName(),
				init,
				new DeclarationState(linkage, typedef, external, internal)
			);
			
			if(obj != null)
				nodes.add(new GlobalVariableNode(obj));
			
			if(expect(",", ";").is(";"))
				break;
			
			next();
			
			decl = declarationParser.nextDeclarator();
			type = decl.merge(baseType);
		}
		
		return nodes;
	}
	
	public void warnUseless(Type type) {
		if(!type.isEnum() && (!type.isStructLike() || type.toStructLike().isAnonymous()))
			warn(Warning.USELESS, "Useless declaration does not declare anything");
	}
	
	public SymbolObject registerDeclaration(Positioned pos, Type type, String name, Initializer initializer, DeclarationState state) {
		boolean hasInit = initializer != null;
		
		if(hasInit && (state.typedef() || state.external()))
			error("»%s« does not accept an initializer", state.typedef() ? "typedef" : "extern");
		
		SymbolTable symtab = getSymbolTable();
		
		if(symtab.hasObjectInScope(name)) {
			SymbolObject obj = symtab.findObjectInScope(name);
			
			if(obj.isTypedef() != state.typedef()) {
				error(pos, Warning.CONTINUE, "Redefinition of »%s« as another type", name);
				note(obj, "Previously declared here");
				terminate();
			}
			
			if(!TypeUtils.isEqual(type, obj.getType())) {
				error(pos, Warning.CONTINUE, "Incompatible types for »%s«", name);
				note(obj, "Previously declared here");
				terminate();
			}
			
			if(obj.getVariableData().initializer() != null && hasInit) {
				error(pos, Warning.CONTINUE, "Redefinition of »%s«", name);
				note(obj, "Previously declared here");
				terminate();
			}
			
			if(state.linkage() == Linkage.EXTERNAL)
				return null;
			
			if(state.internal() && obj.getVariableData().linkage() == Linkage.EXTERNAL) {
				error(pos, Warning.CONTINUE, "Cannot redeclare »%s« as »static« after previous non-static declaration", name);
				note(obj, "Previously declared here");
				terminate();
			}
		}
		
		SymbolObject obj;
		
		if(state.typedef())
			obj = SymbolObject.typedef(pos, name, type);
		
		else if(state.external())
			obj = SymbolObject.extern(pos, name, type);
		
		else obj = SymbolObject.global(
			pos,
			name,
			type,
			state.linkage(),
			hasInit
				? InitializerSerializer.serialize(type, initializer)
				: null
		);
		
		symtab.addObject(obj);
		
		return obj;
	}
	
	public List<Node> parse() {
		List<Node> nodes = new ArrayList<>();
		enterScope();
		
		while(next() != null)
			nodes.addAll(nextExternalDeclaration());
		
		leaveScope();
		
		return nodes;
	}
	
	public static record DeclarationState(Linkage linkage, boolean typedef, boolean external, boolean internal) {
		
	}

}
