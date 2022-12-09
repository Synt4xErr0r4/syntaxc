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
import at.syntaxerror.syntaxc.optimizer.ExpressionOptimizer;
import at.syntaxerror.syntaxc.parser.SymbolHelper.DeclarationState;
import at.syntaxerror.syntaxc.parser.node.FunctionNode;
import at.syntaxerror.syntaxc.parser.node.GlobalVariableNode;
import at.syntaxerror.syntaxc.parser.node.SymbolNode;
import at.syntaxerror.syntaxc.parser.node.declaration.Declarator;
import at.syntaxerror.syntaxc.parser.node.declaration.Initializer;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.statement.CompoundStatementNode;
import at.syntaxerror.syntaxc.symtab.Linkage;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.SymbolTable;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.FunctionType;
import at.syntaxerror.syntaxc.type.FunctionType.Parameter;
import at.syntaxerror.syntaxc.type.Type;
import at.syntaxerror.syntaxc.type.TypeUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
	
	@Getter
	private SymbolHelper symbolHelper;
	
	@Getter
	private FunctionType activeFunctionType;
	
	public Parser(List<Token> tokens) {
		this.tokens = tokens;
		
		marks = new Stack<>();
		
		globalSymbolTable = new SymbolTable();
		
		symbolTables = new Stack<>();
		symbolTables.push(globalSymbolTable);
		
		declarationParser = new DeclarationParser(this);
		expressionParser = new ExpressionParser(this, declarationParser);
		statementParser = new StatementParser(this, declarationParser, expressionParser);
		
		symbolHelper = new SymbolHelper(this);
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
			return ExpressionOptimizer.optimize(expressionParser.nextAssignment());
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
	
	private List<SymbolNode> nextExternalDeclaration() {
		if(equal(";")) // skip single semicola
			return List.of();
		
		List<SymbolNode> nodes = new ArrayList<>();
		
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
			symbolHelper.warnUseless(declSpecs.getRight());
			return List.of();
		}
		
		Linkage linkage = internal ? Linkage.INTERNAL : Linkage.EXTERNAL;
		
		Declarator decl = declarationParser.nextDeclarator();
		
		Type baseType = declSpecs.getRight();
		Type type = decl.merge(baseType);
		
		if(!typedef && decl.isFunctionDeclarator() && !equal("=", ";", ",")) {
			
			enterScope();
			
			activeFunctionType = type.toFunction();
			
			if(activeFunctionType.isKAndR()) {
				
				Set<String> parameterNames = activeFunctionType
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
						symbolHelper.warnUseless(declSpecs.getRight());
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
						obj.setInitialized(true);
						
						if(getSymbolTable().addObject(obj))
							softError(obj, "Redeclaration of local variable »%s«", name);
						
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
						obj.setInitialized(true);
						
						getSymbolTable().addObject(obj);
						
					}
			
			}
			else {
				for(Parameter param : activeFunctionType.getParameters()) {
					String paramName = param.name();
					
					if(paramName == null)
						error(param, "Illegal unnamed function parameter");
					
					Type paramType = param.type();
					
					if(paramType == null)
						error(param, "Missing type for function parameter »%s«", paramName);

					SymbolObject obj = symbolHelper.registerLocal(decl, paramType, paramName);
					obj.setUnused(false);
					obj.setInitialized(true);
				}
			
				consume("{");
			}

			String name = decl.getName();
			
			CompoundStatementNode body = statementParser.nextFunctionBody(activeFunctionType.getReturnType(), name);
			
			var globalVariables = statementParser.getGlobalVariables();
			
			statementParser.getGlobalVariables()
				.forEach(obj -> nodes.add(new GlobalVariableNode(obj)));
			
			leaveScope();
			
			SymbolTable symtab = getSymbolTable();
			
			if(symtab.hasObjectInScope(name)) {
				SymbolObject obj = symtab.findObjectInScope(name);
				
				if(!TypeUtils.isEqual(type, obj.getType())) {
					softError(decl, "Incompatible types for »%s«", name);
					note(obj, "Previously declared here");
				}
				
				else if(!obj.isPrototype()) {
					softError(decl, "Redefinition of »%s«", name);
					note(obj, "Previously defined here");
				}
				
				else if(internal && obj.getFunctionData().linkage() == Linkage.EXTERNAL) {
					softError(decl, "Cannot redeclare »%s« as »static« after previous non-static declaration", name);
					note(obj, "Previously declared here");
				}
				
				obj.setInitialized(true);
				
				symtab.removeObject(obj);
			}
			
			SymbolObject obj = SymbolObject.function(
				decl,
				name,
				activeFunctionType,
				linkage,
				statementParser.getReturnLabel()
			);
			
			symtab.addObject(obj);
			
			nodes.add(new FunctionNode(obj, globalVariables, body));
			
			activeFunctionType = null;
		}
		else while(true) {
			Initializer init = null;
			
			if(skip("=")) {
				if(type.isFunction())
					error(decl, "Cannot initialize function like a variable");
				
				init = declarationParser.nextInitializer();
			}

			SymbolObject obj = symbolHelper.registerVariable(
				decl,
				type,
				decl.getName(),
				init,
				new DeclarationState(linkage, typedef, external, internal, false)
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
	
	public List<SymbolNode> parse() {
		List<SymbolNode> nodes = new ArrayList<>();
		enterScope();
		
		while(next() != null)
			nodes.addAll(nextExternalDeclaration());
		
		for(var it = nodes.iterator(); it.hasNext();) {
			SymbolObject object = it.next().getObject();
			
			if(object.isUnused()) {
				
				if(object.isFunction() && object.getFunctionData().linkage() == Linkage.INTERNAL)
					it.remove();
		
				else if(object.isGlobalVariable() && object.getVariableData().linkage() == Linkage.INTERNAL)
					it.remove();
				
			}
		}
		
		leaveScope();
		
		getSymbolTable()
			.getStringTable()
			.getEntries()
			.stream()
			.map(init -> SymbolObject.string(
				Position.dummy(),
				init
			))
			.map(StringSymbolNode::new)
			.forEach(nodes::add);
		
		return nodes;
	}
	
	@RequiredArgsConstructor
	@Getter
	private static class StringSymbolNode extends SymbolNode {
		
		private final SymbolObject object;
		
		@Override
		public Position getPosition() {
			return object.getPosition();
		}
		
	}

}
