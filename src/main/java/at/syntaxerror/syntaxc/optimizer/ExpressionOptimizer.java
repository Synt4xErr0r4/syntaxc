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
package at.syntaxerror.syntaxc.optimizer;

import java.math.BigInteger;

import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.misc.Optimization;
import at.syntaxerror.syntaxc.parser.ConstantExpressionEvaluator;
import at.syntaxerror.syntaxc.parser.node.expression.ArrayIndexExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.BinaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CallExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CastExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ConditionalExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.MemberAccessExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.NumberLiteralExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.UnaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.global.GlobalVariableInitializer;
import at.syntaxerror.syntaxc.symtab.global.IntegerInitializer;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.TypeUtils;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class ExpressionOptimizer {

	private static ExpressionNode eval(ExpressionNode expr) {
		if(!ConstantExpressionEvaluator.isConstant(expr))
			return expr;
		
		return new NumberLiteralExpressionNode(
			expr.getPosition(),
			ConstantExpressionEvaluator.evalArithmetic(expr),
			expr.getType()
		);
	}
	
	private static boolean isNumber(ExpressionNode...nodes) {
		for(ExpressionNode node : nodes)
			if(!(node instanceof NumberLiteralExpressionNode))
				return false;
		
		return true;
	}
	
	public static ExpressionNode optimize(ExpressionNode expr) {
		Position pos = expr.getPosition();
		boolean eval = false;

		if(expr instanceof VariableExpressionNode var) {
			
			if(Optimization.CONST_FOLDING.isEnabled() && var.hasConstQualifier()) {
				SymbolObject obj = var.getVariable();
				
				if(obj.isGlobalVariable() && obj.getType().isArithmetic()) {
					GlobalVariableInitializer init = obj.getVariableData().initializer();
					
					if(init != null && init instanceof IntegerInitializer intInit)
						return new NumberLiteralExpressionNode(
							var.getPosition(),
							intInit.value(),
							var.getType()
						);
				}
			}
			
		}
		
		else if(expr instanceof BinaryExpressionNode binary) {
			ExpressionNode left = optimize(binary.getLeft());
			ExpressionNode right = optimize(binary.getRight());
			
			expr = new BinaryExpressionNode(
				pos,
				left,
				right,
				binary.getOperation(),
				binary.getType()
			);

			eval = binary.getOperation() != Punctuator.ASSIGN
				&& isNumber(left, right);
		}

		else if(expr instanceof UnaryExpressionNode unary) {
			ExpressionNode target = optimize(unary.getTarget());
			
			expr = new UnaryExpressionNode(
				pos,
				target,
				unary.getOperation(),
				unary.getType()
			);

			eval = isNumber(target);
		}
		
		else if(expr instanceof CastExpressionNode cast) {
			ExpressionNode target = optimize(cast.getTarget());

			if(TypeUtils.isCompatible(target.getType(), cast.getType()))
				expr = target;
				
			else {
				expr = new CastExpressionNode(
					pos,
					target,
					cast.getType()
				);
				
				eval = isNumber(target) && cast.getType().isArithmetic();
			}
		}
		
		else if(expr instanceof ConditionalExpressionNode cond) {
			ExpressionNode condition = optimize(cond.getCondition());
			
			ExpressionNode whenTrue = optimize(cond.getWhenTrue());
			ExpressionNode whenFalse = optimize(cond.getWhenFalse());
			
			if(isNumber(condition))
				return ((BigInteger) ((NumberLiteralExpressionNode) condition).getLiteral())
						.compareTo(BigInteger.ZERO) == 0
					? whenFalse
					: whenTrue;
			
			expr = new ConditionalExpressionNode(
				pos,
				condition,
				whenTrue,
				whenFalse,
				cond.getType()
			);
		}
		
		else if(expr instanceof CallExpressionNode call)
			expr = new CallExpressionNode(
				call.getPosition(),
				optimize(call.getTarget()),
				call.getParameters()
					.stream()
					.map(ExpressionOptimizer::optimize)
					.toList(),
				call.getFunctionType()
			);
		
		else if(expr instanceof MemberAccessExpressionNode mem)
			expr = new MemberAccessExpressionNode(
				mem.getPosition(),
				optimize(mem.getTarget()),
				mem.getMember(),
				mem.getType()
			);
		
		else if(expr instanceof ArrayIndexExpressionNode idx)
			expr = new ArrayIndexExpressionNode(
				idx.getPosition(),
				optimize(idx.getTarget()),
				optimize(idx.getIndex()),
				idx.isSwapped(),
				idx.isArray(),
				idx.getType()
			);
		
		return eval ? eval(expr) : expr;
	}
	
}
