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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import at.syntaxerror.syntaxc.analysis.DataFlowAnalyzer;
import at.syntaxerror.syntaxc.lexer.Keyword;
import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.misc.Flag;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.parser.SymbolHelper.DeclarationState;
import at.syntaxerror.syntaxc.parser.node.declaration.Declarator;
import at.syntaxerror.syntaxc.parser.node.declaration.Initializer;
import at.syntaxerror.syntaxc.parser.node.expression.BinaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.NumberLiteralExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.parser.node.statement.CompoundStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.ExpressionStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.GotoStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.JumpStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.LabeledStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.NullStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.StatementNode;
import at.syntaxerror.syntaxc.symtab.Linkage;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.SymbolTable;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.Type;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class StatementParser extends AbstractParser {

	private final Parser parser;
	private final DeclarationParser declarationParser;
	private final ExpressionParser expressionParser;
	
	private final DataFlowAnalyzer dataFlowAnalyzer = new DataFlowAnalyzer();
	
	private final Stack<ScopeType> scopeType = new Stack<>();

	private final Stack<Map<BigInteger, LabeledStatementNode>> cases = new Stack<>();
	
	private final @Getter Map<String, LabeledStatementNode> labels = new HashMap<>();
	private final @Getter List<GotoStatementNode> gotos = new ArrayList<>();
	private final @Getter List<JumpStatementNode> jumps = new ArrayList<>();
	
	private final Stack<String> labelContinue = new Stack<>();
	private final Stack<String> labelBreak = new Stack<>();
	
	private final List<SymbolObject> allVariables = new ArrayList<>();
	
	private long loopId;

	private Type expectedType;
	private VariableExpressionNode returnValueField;
	
	@Getter
	private String returnLabel;
	
	@Getter
	private List<SymbolObject> globalVariables = new ArrayList<>();

	private List<SymbolObject> declarations = new ArrayList<>();
	
	private String nextLabel() {
		return ".L" + loopId++;
	}
	
	private void enterLoop() {
		labelContinue.push(nextLabel());
		labelBreak.push(nextLabel());
		scopeType.push(ScopeType.LOOP);
	}
	
	private void leaveLoop() {
		labelContinue.pop();
		labelBreak.pop();
		scopeType.pop();
	}
	
	private void enterSwitch(Map<BigInteger, LabeledStatementNode> caseMap) {
		labelBreak.push(nextLabel());
		scopeType.push(ScopeType.SWITCH);
		cases.push(caseMap);
	}
	
	private void leaveSwitch() {
		labelBreak.pop();
		scopeType.pop();
		cases.pop();
	}
	
	public ScopeType getScopeType() {
		return scopeType.peek();
	}
	
	public String getLabelContinue() {
		return labelContinue.peek();
	}

	public String getLabelBreak() {
		return labelBreak.peek();
	}
	
	public Map<BigInteger, LabeledStatementNode> getCases() {
		return cases.peek();
	}
	
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
	
	private LabeledStatementNode newLabel(Positioned pos, String label, StatementNode stmt) {
		LabeledStatementNode labeled = new LabeledStatementNode(
			pos.getPosition(),
			label,
			stmt
		);
		
		if(labels.containsKey(label))
			error(pos, Warning.SEM_NONE, "Duplicate label »%s« in current function", label);
		
		labels.put(label, labeled);
		
		return labeled;
	}
	
	private GotoStatementNode newGoto(Positioned pos, String label) {
		GotoStatementNode stmt = new GotoStatementNode(pos.getPosition(), label);
		
		gotos.add(stmt);
		
		return stmt;
	}
	
	private JumpStatementNode newJump(Positioned pos, ExpressionNode condition, String label) {
		JumpStatementNode stmt = new JumpStatementNode(pos.getPosition(), condition, label);
		
		jumps.add(stmt);
		
		return stmt;
	}
	
	private StatementNode nextStatement() {
		
		if(equal("case", "default")) {
			if(getScopeType() != ScopeType.SWITCH)
				error("Unexpected »%s« label outside of »switch« statement", current.getKeyword().getName());
			
			return nextLabeled(true);
		}
		
		if(equal(TokenType.IDENTIFIER) && peek(":"))
			return nextLabeled(false);
		
		if(skip("{"))
			return nextCompound(true);
		
		if(skip(";"))
			return new NullStatementNode(previous.getPosition());
		
		if(skip("if"))
			return nextIf();
		
		if(skip("switch"))
			return nextSwitch();
		
		if(skip("while"))
			return nextWhile();
		
		if(skip("do"))
			return nextDoWhile();
		
		if(skip("for"))
			return nextFor();
		
		if(skip("goto")) {
			Position pos = previous.getPosition();
			String label = current.getString();
			
			consume(TokenType.IDENTIFIER);
			consume(";");
			
			GotoStatementNode gotoStmt = newGoto(pos, label);
			
			return gotoStmt;
		}
		
		if(skip("continue", "break")) {
			Position pos = previous.getPosition();
			String label = previous.is("continue")
				? getLabelContinue()
				: getLabelBreak();
			
			consume(";");
			
			return newGoto(pos, label);
		}
		
		if(skip("return")) {
			Position pos = previous.getPosition();
			ExpressionNode returnValue = null;
			
			if(!skip(";")) {
				returnValue = parser.nextExpression();
				consume(";");
			}
			
			var statements = new ArrayList<StatementNode>();
			
			if(returnValue == null) {
				
				if(!expectedType.isVoid())
					warn(pos, Warning.RETURN_VOID, "Expected value for »return« inside function returning »%s«", expectedType);
				
			}
			else {
				
				if(expectedType.isVoid())
					warn(pos, Warning.RETURN_VALUE, "Unexpected value for »return« inside function returning »void«");
				
				else statements.add(new ExpressionStatementNode(
					expressionParser.getChecker().checkAssignment(
						pos,
						returnValueField,
						returnValue,
						true
					)
				));
				
			}
			
			statements.add(newGoto(pos, returnLabel));
			
			return new CompoundStatementNode(pos, statements);
		}
		
		ExpressionNode expr = parser.nextExpression();
		consume(";");
		
		return new ExpressionStatementNode(expr);
	}
	
	private StatementNode nextLabeled(boolean switchLabel) {
		Position pos = getPosition();
		
		if(switchLabel) {
			boolean isCase = equal("case");
			next();
			
			BigInteger constant = isCase
				? parser.nextIntegerConstantExpression()
				: null;
			
			var cases = getCases();
			
			if(cases.containsKey(constant))
				error(pos, Warning.SEM_NONE, "Duplicate label for »switch« statement");
			
			consume(":");
			
			LabeledStatementNode stmt = newLabel(
				pos,
				nextLabel(),
				nextStatement()
			);
			
			cases.put(constant, stmt);
			
			return stmt;
		}
		else {
			String label = current.getString();
			
			require(":");
			next();
			
			return newLabel(
				pos,
				label,
				nextStatement()
			);
		}
	}
	
	private Optional<Boolean> checkBooleanValue(ExpressionNode expr) {
		if(expr instanceof NumberLiteralExpressionNode lit && lit.getType().isInteger())
			return Optional.of(
				((BigInteger) lit.getLiteral())
					.compareTo(BigInteger.ZERO) != 0
			);
			
		return Optional.empty();
	}
	
	private Pair<ExpressionNode, StatementNode> nextExpressionStatement() {
		consume("(");
		
		ExpressionNode expr = parser.nextExpression();
		
		if(!expr.getType().isScalar())
			error(expr, "Expected scalar type for condition");
		
		consume(")");
		
		return Pair.of(expr, nextStatement());
	}
	
	/*
	 * The statement
	 * 
	 *   if(condition)
	 *   	thenBody;
	 *   else elseBody;
	 *   
	 * is equivalent to
	 * 
	 *   if(!condition) goto elseBlock;
	 *   
	 *   thenBody;
	 *   goto end;
	 *   
	 *   elseBlock:
	 *   elseBody;
	 *   
	 *   end:
	 *   ;
	 * 
	 * and the statement
	 * 
	 *   if(condition)
	 *   	thenBody;
	 * 
	 * is equivalent to
	 * 
	 * 	 if(!condition) goto end;
	 * 	 
	 *   thenBody;
	 * 	 
	 *   end:
	 *   ;
	 */
	private StatementNode nextIf() {
		Position pos = previous.getPosition();
		
		var exprStmt = nextExpressionStatement();
		
		ExpressionNode condition = exprStmt.getLeft();
		
		StatementNode stmtThen = exprStmt.getRight();
		StatementNode stmtElse = null;
		
		Position elsePos = getPosition();
		
		if(skip("else"))
			stmtElse = nextStatement();
		
		var val = checkBooleanValue(condition);
		
		if(val.isPresent()) {
			if(!val.get()) { // if(0) ... [else ...]
				warn(pos, Warning.DEAD_CODE, "»if« block is unreachable because condition always evaluates to false");
				
				return stmtElse == null
					? new NullStatementNode(pos)
					: stmtElse;
			}
			
			if(stmtElse != null) // if(1) ... else ...
				warn(elsePos, Warning.DEAD_CODE, "»else« block is unreachable because condition always evaluates to true");

			return stmtThen;
		}

		List<StatementNode> statements = new ArrayList<>();
		
		String labelEnd = nextLabel();
		String labelThen = nextLabel();
		
		if(stmtElse != null) { // if(condition) ... else ...
			
			String labelElse = nextLabel();
			
			statements.add(newJump( // if(!condition) goto elseBlock;
				pos,
				condition,
				labelElse
			));

			statements.add(newLabel( // thenBlock: thenBody;
				pos,
				labelThen,
				stmtThen
			));
			statements.add(newGoto(pos, labelEnd)); // goto end;
			
			statements.add(newLabel( // elseBlock: elseBody;
				pos,
				labelElse,
				stmtElse
			));
		}
		else { // if(condition) ...
			statements.add(newJump( // if(!condition) goto end;
				pos,
				condition,
				labelEnd
			));

			statements.add(newLabel( // thenBlock: thenBody;
				pos,
				labelThen,
				stmtThen
			));
		}

		statements.add(newLabel( // end: ;
			pos,
			labelEnd,
			new NullStatementNode(pos)
		));
		
		return new CompoundStatementNode(pos, statements);
	}
	
	/*
	 * The statement
	 * 
	 *   switch(value) {
	 *   case X:
	 *     body_x;
	 *     break;
	 *   case Y:
	 *     body_y;
	 *   case Z:
	 *     body_z;
	 *   default:
	 *     body_default;
	 *   }
	 *  
	 * is equivalent to
	 * 
	 * 	 typeof(value) tmp;
	 * 	 tmp = value;
	 * 
	 *   if(tmp == X) goto case_X;
	 *   if(tmp == Y) goto case_X;
	 *   if(tmp == Z) goto case_X;
	 *   goto case_default;
	 * 
	 *   case_X:
	 *   body_x;
	 *   goto brk;
	 *   
	 *   case_Y:
	 *   body_y;
	 *   
	 *   case_Z:
	 *   body_z;
	 *   
	 *   case_default:
	 * 	 body_default;
	 * 
	 *   brk:
	 *   ;
	 * 
	 */
	private StatementNode nextSwitch() {
		Position pos = previous.getPosition();
		
		var caseMap = new HashMap<BigInteger, LabeledStatementNode>();

		enterSwitch(caseMap);
		
		consume("(");
		
		ExpressionNode value = parser.nextExpression();
		
		if(!value.getType().isInteger())
			error(value, "Expected integer for »switch« statement");
		
		consume(")");
		
		CompoundStatementNode body;
		StatementNode rawBody = nextStatement();
		
		if(rawBody instanceof CompoundStatementNode compound)
			body = compound;
		
		else body = new CompoundStatementNode(rawBody.getPosition(), List.of(rawBody));
		
		List<StatementNode> statements = new ArrayList<>();

		SymbolObject valueCache = SymbolObject.temporary(
			pos.getPosition(),
			value.getType()
		);
		
		VariableExpressionNode valueVar = new VariableExpressionNode(pos, valueCache);
		
		statements.add( // typeof(value) tmp = value;
			new ExpressionStatementNode(
				new BinaryExpressionNode(
					pos,
					valueVar,
					value,
					Punctuator.ASSIGN,
					value.getType()
				)
			)
		);
		
		for(var switchCase : caseMap.entrySet()) {
			
			if(switchCase.getKey() == null)
				continue;
			
			BigInteger caseValue = switchCase.getKey();
			LabeledStatementNode caseLabel = switchCase.getValue();
			
			Position casePos = caseLabel.getPosition();
			
			// tmp == X
			ExpressionNode condition = new BinaryExpressionNode(
				casePos,
				valueVar,
				new NumberLiteralExpressionNode(
					pos,
					caseValue,
					valueVar.getType()
				),
				Punctuator.EQUAL,
				Type.INT
			);
			
			statements.add( // if(tmp == X) goto case_X;
				newJump(
					pos,
					condition,
					caseLabel.getLabel()
				)
			);
		}
		
		if(caseMap.containsKey(null)) {
			// goto case_default;
			LabeledStatementNode labelDefault = caseMap.get(null);
			
			statements.add(newGoto(pos, labelDefault.getLabel()));
		}
		
		// goto brk;
		else statements.add(newGoto(pos, getLabelBreak()));

		statements.add(body);
		
		Position unreachablePos = null;
		
		if(!body.getStatements().isEmpty()) {
			
			StatementNode first = body.getStatements().get(0);
			
			if(!(first instanceof LabeledStatementNode))
				unreachablePos = first.getPosition();
			
		}
		
		if(unreachablePos != null)
			warn(unreachablePos, Warning.DEAD_CODE, "Code before first switch label is unreachable");
		
		statements.add( // brk: ;
			newLabel(
				pos,
				getLabelBreak(),
				new NullStatementNode(pos)
			)
		);
		
		leaveSwitch();
		
		return new CompoundStatementNode(
			pos,
			statements
		);
	}

	/*
	 * The statement
	 * 
	 *   while(condition) body_statement;
	 * 
	 * is effectively equal to
	 * 
	 *   cont:
	 *   if(!condition) goto brk;
	 *   
	 *   body_statement;
	 *   goto cont;
	 *   
	 *   brk:
	 *   ;
	 * 
	 * or to
	 * 
	 *   cont:
	 *   body_statement;
	 *   goto cont;
	 *   
	 *   brk:
	 *   ;
	 * 
	 * if condition evaluates to true
	 * 
	 */
	private StatementNode nextWhile() {
		Position pos = previous.getPosition();
		
		enterLoop();
		
		var exprStmt = nextExpressionStatement();
		
		ExpressionNode condition = exprStmt.getLeft();
		StatementNode body = exprStmt.getRight();
		
		List<StatementNode> statements = new ArrayList<>();

		var val = checkBooleanValue(condition);
		
		if(val.isPresent()) {
			if(!val.get()) {
				warn(pos, Warning.DEAD_CODE, "»while« block is unreachable because condition always evaluates to false");
				
				return new NullStatementNode(pos);
			}
			else statements.add(
				newLabel( // cont: body_statement;
					pos,
					getLabelContinue(),
					body
				)
			);
		}
		
		else {
			statements.add(newLabel( // if(!condition) goto brk;
				pos,
				getLabelContinue(),
				newJump( // if(!condition) goto brk;
					pos,
					condition,
					getLabelBreak()
				)
			));
			
			statements.add(body);
		}
		
		statements.add( // goto cont;
			newGoto(pos, getLabelContinue())
		);
		
		statements.add(
			newLabel( // brk: ;
				pos,
				getLabelBreak(),
				new NullStatementNode(pos)
			)
		);
		
		leaveLoop();
		return new CompoundStatementNode(pos, statements);
	}
	
	/*
	 * The statement
	 * 
	 *   do body_statement; while(condition)
	 * 
	 * is effectively equal to
	 * 
	 *   cont:
	 *   body_statement;
	 *   
	 *   if(condition) goto cont;
	 *   brk:
	 *   ;
	 * 
	 * or to
	 * 
	 *   cont:
	 *   body_statement;
	 *   goto cont;
	 *   
	 *   brk:
	 *   ;
	 * 
	 * if condition evaluates to true, or to
	 * 
	 *   body_statement;
	 *   cont:
	 *   brk:
	 *   ;
	 * 
	 * if condition evaluates to false
	 * 
	 */
	private StatementNode nextDoWhile() {
		Position pos = previous.getPosition();
		
		enterLoop();
		
		StatementNode body = nextStatement();
		
		consume("while");
		consume("(");
		
		ExpressionNode condition = parser.nextExpression();
		
		consume(")");
		consume(";");
		
		List<StatementNode> statements = new ArrayList<>();

		var val = checkBooleanValue(condition);
		
		if(val.isPresent()) {
			if(val.get()) { // do ... while(1)
				
				statements.add(
					newLabel( // cont: body_statement;
						pos,
						getLabelContinue(),
						body
					)
				);
				
				statements.add( // goto cont;
					newGoto(pos, getLabelContinue())
				);
			}
			else { // do ... while(0)
				statements.add(body);
				
				statements.add(
					newLabel( // cont: ;
						pos,
						getLabelContinue(),
						new NullStatementNode(pos)
					)
				);
			}
		}
		else {
			statements.add(
				newLabel( // cont: body_statement;
					pos,
					getLabelContinue(),
					body
				)
			);

			statements.add( // if(condition) goto cont;
				newJump(
					pos,
					condition,
					getLabelContinue()
				)
			);
		}

		statements.add(
			newLabel( // brk: ;
				pos,
				getLabelBreak(),
				new NullStatementNode(pos)
			)
		);
		
		leaveLoop();
		return new CompoundStatementNode(pos, statements);
	}

	/*
	 * The statement
	 * 
	 *   for(initialize; condition; operation)
	 *     body_statement;
	 * 
	 * is effectively equal to
	 * 
	 * 	 initialize;
	 *   goto cond;
	 * 
	 * 	 body:
	 * 	 body_statement;
	 *   
	 *   cont:
	 *   operation;
	 *   
	 *   cond:
	 *   if(condition) goto body;
	 *   
	 *   brk:
	 *   ;
	 *   
	 * or to
	 * 
	 * 	 initialize;
	 * 
	 *   body:
	 * 	 body_statement;
	 * 
	 *   cont:
	 *   operation;
	 *   
	 *   goto body;
	 *   
	 *   brk:
	 *   ;
	 * 
	 * if condition evaluates to true, or to
	 * 
	 * 	 initialize;
	 * 
	 * if condition evaluates to false
	 * 
	 */
	private StatementNode nextFor() {
		Position pos = previous.getPosition();
		
		enterLoop();
		
		consume("(");
		
		ExpressionNode initialize = null, condition = null, operation = null;
		
		if(!equal(";")) {
			initialize = parser.nextExpression();
			consume(";");
		}

		if(!equal(";")) {
			condition = parser.nextExpression();
			consume(";");
		}

		if(!equal(")")) {
			operation = parser.nextExpression();
			consume(")");
		}

		StatementNode body = nextStatement();
		
		List<StatementNode> statements = new ArrayList<>();
		
		boolean infiniteLoop = condition == null;

		var val = infiniteLoop
			? Optional.<Boolean>empty()
			: checkBooleanValue(condition);
		
		if(val.isPresent()) {
			if(!val.get()) { // for(...; 0; ...) ... 
				warn(pos, Warning.DEAD_CODE, "»for« block is unreachable because condition always evaluates to false");
				
				return initialize == null
					? new NullStatementNode(pos)
					: new ExpressionStatementNode(initialize); // initialize;
			}
			
			// for(...; 1; ...) ...
			else infiniteLoop = true;
		}
		
		if(initialize != null)
			statements.add(new ExpressionStatementNode(initialize)); // initialize;
		
		String labelCond = null;
		String labelBody = nextLabel();
		
		if(!infiniteLoop) {
			labelCond = nextLabel();
			statements.add(newGoto(pos, labelCond)); // goto cond;
		}
		
		statements.add(newLabel( // body: body_statement;
			pos,
			labelBody,
			body
		));
		
		statements.add(newLabel( // cont: operation;
			pos,
			getLabelContinue(),
			new ExpressionStatementNode(operation)
		));

		if(!infiniteLoop)
			statements.add(newLabel( // cond: if(condition) goto body;
				pos,
				labelCond,
				newJump(
					pos,
					condition,
					labelBody
				)
			));
		else statements.add(newGoto(pos, labelBody)); // goto body;

		statements.add(
			newLabel( // brk: ;
				pos,
				getLabelBreak(),
				new NullStatementNode(pos)
			)
		);
		
		leaveLoop();
		return new CompoundStatementNode(pos, statements);
	}

	private CompoundStatementNode nextCompound(boolean skipBrace) {
		Position pos = previous.getPosition();
		
		List<StatementNode> statements = new ArrayList<>();
		
		parser.enterScope();
		
		while(declarationParser.isTypeName()) {
			var declSpec = declarationParser.nextDeclarationSpecifiers();
			
			var optSpec = declSpec.getLeft();
			
			boolean internal = false;
			boolean external = false;
			
			if(optSpec.isPresent()) {
				Keyword spec = optSpec.get().getKeyword();
				
				switch(spec) {
				case TYPEDEF:
					error(optSpec.get(), "Illegal location for »typedef« specifier");
					break;
					
				case EXTERN:
					external = true;
					break;
					
				case STATIC:
					internal = true;
					break;
				
				case REGISTER:
					warn(optSpec.get(), Warning.REGISTER, "The »register« specifier is not supported");
					break;

				case AUTO:
				default:
					break;
				}
			}
			
			Type baseType = declSpec.getRight();
			
			if(skip(";")) {
				parser.getSymbolHelper()
					.warnUseless(baseType);
				continue;
			}
			
			while(true) {
				Declarator decl = declarationParser.nextDeclarator();
				
				Position declPos = decl.getPosition();
				
				Type type = decl.merge(baseType);
				
				Initializer init = null;
				
				if(skip("="))
					init = declarationParser.nextInitializer();
				
				if(type.isFunction() && internal)
					error(decl, "Illegal »static« specifier for block-scope function");
				
				SymbolObject obj = parser.getSymbolHelper().registerVariable(
					declPos,
					type,
					decl.getName(),
					internal
						? init
						: null,
					new DeclarationState(
						external
							? Linkage.EXTERNAL
							: Linkage.INTERNAL,
						false,
						external,
						internal
					)
				);
				
				allVariables.add(obj);
				
				if(internal)
					globalVariables.add(obj);
				
				else if(!external)
					declarations.add(obj);
				
				if(init != null && !internal)
					statements.addAll(
						InitializerSerializer.toAssignment(
							expressionParser,
							new VariableExpressionNode(declPos, obj),
							type,
							init
						)
					);
				
				consume(",", ";");
				
				if(previous.is(";"))
					break;
			}
		}
		
		while(!equal("}"))
			statements.add(nextStatement());

		if(skipBrace)
			next();
		
		parser.leaveScope();
		
		return new CompoundStatementNode(pos, statements);
	}
	
	public CompoundStatementNode nextFunctionBody(Type expectedReturnType, String funcName) {
		expectedType = expectedReturnType;
		
		scopeType.clear();
		cases.clear();
		
		labels.clear();
		gotos.clear();
		jumps.clear();
		
		labelBreak.clear();
		labelContinue.clear();
		
		globalVariables.clear();
		declarations.clear();
		
		allVariables.clear();
		
		scopeType.push(ScopeType.FUNCTION);
		
		Position pos = getPosition();
		
		boolean hasFunc = Flag.FUNC.isEnabled();
		SymbolObject __func__ = null;
		
		if(hasFunc) {
			__func__ = SymbolObject.global(
				pos,
				"__func__",
				Type.getStringType(funcName, false)
					.asConst(),
				Linkage.INTERNAL,
				getSymbolTable()
					.getStringTable()
					.addDistinct(funcName, false)
			);
			
			__func__.setInitialized(true);
			__func__.setSyntaxTreeIgnore(true);
			
			parser.enterScope();
			
			getSymbolTable().addObject(__func__);
			globalVariables.add(__func__);
		}
		
		returnValueField = new VariableExpressionNode(
			getPosition(),
			SymbolObject.returns(getPosition(), expectedReturnType)
		);
		
		returnLabel = nextLabel();
		
		CompoundStatementNode body = nextCompound(false);
		
		postInitVariables();
		
		dataFlowAnalyzer.checkInitialized(body);
		
		if(hasFunc) {
			parser.leaveScope();
			
			if(__func__.isUnused()) {
				getSymbolTable().removeObject(__func__);
				globalVariables.remove(__func__);
			}
		}
		
		checkVariableUsage();
		
		return body;
	}
	
	private void postInitVariables() {
		int length = declarations.size();
		
		int offset = 0;
		
		for(int i = length - 1; i >= 0; --i) {
			SymbolObject decl = declarations.get(i);
			
			if(decl.isFunction())
				continue;

			offset += decl.getType().sizeof();
			
			decl.setOffset(offset);
		}
		
		globalVariables.forEach(SymbolObject::setLocalStatic);
	}
	
	private void checkVariableUsage() {
		allVariables.forEach(obj -> {
			
			if(obj.isUnused())
				warn(obj, Warning.UNUSED, "Variable »%s« is declared, but not used", obj.getName());
			
		});
	}
	
	private static enum ScopeType {

		FUNCTION,
		SWITCH,
		LOOP
		
	}
	
}
