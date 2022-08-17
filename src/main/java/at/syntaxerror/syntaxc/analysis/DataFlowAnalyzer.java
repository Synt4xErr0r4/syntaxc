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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.parser.node.expression.BinaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CallExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CastExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ConditionalExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.MemberAccessExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.UnaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.parser.node.statement.CompoundStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.ExpressionStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.GotoStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.JumpStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.LabeledStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.StatementNode;
import at.syntaxerror.syntaxc.tracking.Position;

/**
 * @author Thomas Kasper
 * 
 */
public class DataFlowAnalyzer implements Logable {

	private Map<String, BranchList> jumpInitialized;
	
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
		
		BranchList list = new BranchList(
			new ArrayList<>(
				List.of(
					new Branch(new HashSet<>())
				)
			)
		);
		
		scanInitialized(root.getStatements(), list);
	}

	private void scanInitialized(List<StatementNode> stmts, BranchList branches) {
		stmts.forEach(stmt -> scanInitializedSingle(stmt, branches));
	}

	private void scanInitializedSingle(StatementNode stmt, BranchList branches) {
		
		if(stmt instanceof ExpressionStatementNode node) {
			if(!scanInitialized(node.getExpression(), branches))
				warn(node, Warning.UNUSED_VALUE, "Result of expression is unused");
		}
		
		else if(stmt instanceof LabeledStatementNode node) {
			String label = node.getLabel();
			
			if(jumpInitialized.containsKey(label))
				branches.merge(jumpInitialized.get(label));
			
			scanInitializedSingle(node.getStatement(), branches);
		}
		
		else if(stmt instanceof JumpStatementNode node) {
			scanInitialized(node.getCondition(), branches);
			
			String label = node.getJumpLabel();
			
			if(jumpInitialized.containsKey(label))
				jumpInitialized.get(label).merge(branches);
			
			else jumpInitialized.put(label, branches.deepCopy());
		}
		
		else if(stmt instanceof GotoStatementNode node) {
			String label = node.getLabel();
			
			if(jumpInitialized.containsKey(label))
				jumpInitialized.get(label).merge(branches);
			
			else jumpInitialized.put(label, branches.deepCopy());
			
			branches.clear();
		}
		
		else if(stmt instanceof CompoundStatementNode node)
			scanInitialized(node.getStatements(), branches);
	}

	private boolean scanInitialized(ExpressionNode stmt, BranchList branches) {
		return scanInitialized(stmt, branches, false, false);
	}
	
	private boolean scanInitialized(ExpressionNode stmt, BranchList branches, boolean insideInit, boolean funCall) {
		
		if(stmt instanceof VariableExpressionNode var) {
			
			if(!var.getVariable().isUserDefined())
				return false;
			
			String name = var.getVariable().getName();
			
			if(!branches.has(name))
				warn(
					var,
					Warning.UNINITIALIZED,
					"»%s« might not have been initialized yet",
					name
				);
			
			var.getVariable().setUnused(false);
			
			return false;
		}
		
		else if(stmt instanceof BinaryExpressionNode binOp) {
			
			ExpressionNode left = binOp.getLeft();
			ExpressionNode right = binOp.getRight();
			
			if(binOp.getOperation() == Punctuator.ASSIGN) {
				scanInitialized(right, branches, false, funCall);
				
				if(left instanceof VariableExpressionNode var)
					branches.add(var.getVariable().getName());
				
				else scanInitialized(left, branches, true, funCall);
				
				return true;
			}
			else {
				boolean hasEffectLeft = scanInitialized(left, branches, insideInit, funCall);
				boolean hasEffectRight = scanInitialized(right, branches, insideInit, funCall);
				
				return hasEffectLeft || hasEffectRight;
			}
			
		}
		
		else if(stmt instanceof UnaryExpressionNode unOp) {
			
			ExpressionNode target = unOp.getTarget();
			
			if(unOp.getOperation() == Punctuator.ADDRESS_OF && target instanceof VariableExpressionNode var) {
				
				if(insideInit || funCall)
					branches.add(var.getVariable().getName());
				
				return false;
			}

			boolean hasEffect = scanInitialized(target, branches, insideInit, funCall);
			
			return hasEffect || unOp.getOperation() == Punctuator.INDIRECTION;
		}
		
		else if(stmt instanceof ConditionalExpressionNode cond) {

			boolean hasEffectCond = scanInitialized(cond.getCondition(), branches, insideInit, funCall);

			BranchList branchesTrue = branches.deepCopy();
			BranchList branchesFalse = branches.deepCopy();
			
			boolean hasEffectTrue = scanInitialized(cond.getWhenTrue(), branchesTrue, insideInit, funCall);
			boolean hasEffectFalse = scanInitialized(cond.getWhenFalse(), branchesFalse, insideInit, funCall);
			
			branchesTrue.conjunction(branchesFalse)
				.forEach(branches::add);
			
			return hasEffectCond || hasEffectTrue || hasEffectFalse;
		}
		
		else if(stmt instanceof CallExpressionNode call) {
			call.getParameters().forEach(expr -> scanInitialized(expr, branches, false, true));
			
			return true;
		}
		
		else if(stmt instanceof MemberAccessExpressionNode mem)
			return scanInitialized(mem.getTarget(), branches, insideInit, funCall);

		else if(stmt instanceof CastExpressionNode mem)
			return scanInitialized(mem.getTarget(), branches, insideInit, funCall);
		
		return false;
	}

	private static record BranchList(List<Branch> branches) {
		
		public void clear() {
			branches().clear();
		}
		
		public void add(String name) {
			branches().forEach(branch -> branch.initialized().add(name));
		}
		
		public boolean has(String name) {
			return !branches().isEmpty()
				&& branches()
					.stream()
					.allMatch(branch -> branch.initialized().contains(name));
		}
		
		public void merge(BranchList other) {
			branches().addAll(other.deepCopy().branches());
		}
		
		public Set<String> conjunction(BranchList other) {
			Set<String> init = new HashSet<>();
			
			branches().stream()
				.map(Branch::initialized)
				.forEach(init::addAll);
			
			init.retainAll(
				branches().stream()
					.map(Branch::initialized)
					.reduce(
						new HashSet<>(),
						(a, b) -> {
							a.addAll(b);
							return a;
						}
					)
			);
			
			return init;
		}
		
		public BranchList deepCopy() {
			return new BranchList(
				branches.stream()
					.map(Branch::copy)
					.collect(Collectors.toCollection(ArrayList::new))
			);
		}
		
	}
	
	private static record Branch(Set<String> initialized) {
		
		public Branch copy() {
			return new Branch(new HashSet<>(initialized));
		}
		
	}
	
}
