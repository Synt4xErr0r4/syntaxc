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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.parser.node.Node;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.symtab.SymbolTable;

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
		expressionParser.next();
		return expressionParser.nextExpression();
	}
	
	public List<Node> parse() {
		List<Node> nodes = new ArrayList<>();
		
		return nodes;
	}

}
