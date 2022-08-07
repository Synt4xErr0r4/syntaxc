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
package at.syntaxerror.syntaxc.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.statement.CompoundStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.ExpressionStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.JumpStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.LabeledStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.StatementNode;
import at.syntaxerror.syntaxc.tracking.Position;

/**
 * @author Thomas Kasper
 * 
 */
public class DataFlowAnalyzer implements Logable {

	private Map<String, Set<String>> jumpInitialized;
	
	public DataFlowAnalyzer() {
		jumpInitialized = new HashMap<>();
	}
	
	@Override
	public Position getPosition() {
		return null;
	}
	
	@Override
	public Warning getDefaultWarning() {
		return Warning.SEM_NONE;
	}
	
	/*
	 * checks if all variables are initialized
	 * also checks if values of expressions (without side effects) are actually used
	 */
	public void checkInitialized(CompoundStatementNode root) {
		jumpInitialized.clear();
		
		root.getStatements().forEach(stmt -> scanInitialized(stmt, new HashSet<>()));
	}

	private void scanInitialized(StatementNode stmt, Set<String> initialized) {
		
		if(stmt instanceof ExpressionStatementNode node) {
			if(scanInitialized(node.getExpression(), initialized))
				warn(node, Warning.UNUSED_VALUE, "Result of expression is unused");
		}
		
		else if(stmt instanceof LabeledStatementNode node) {
			String label = node.getLabel();
			
			if(jumpInitialized.containsKey(label)) {
				initialized.clear();
				initialized.addAll(jumpInitialized.get(label));
			}
			
			scanInitialized(node.getStatement(), initialized);
		}
		
		else if(stmt instanceof JumpStatementNode node) {
			scanInitialized(node.getCondition(), initialized);
			
			jumpInitialized.put(node.getJumpLabel(), new HashSet<>(initialized));
		}
	}

	private boolean scanInitialized(ExpressionNode stmt, Set<String> initialized) {
		
		// TODO
		return false;
	}
	
}
