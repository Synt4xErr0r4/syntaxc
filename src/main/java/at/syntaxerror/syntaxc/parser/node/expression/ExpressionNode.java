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
package at.syntaxerror.syntaxc.parser.node.expression;

import at.syntaxerror.syntaxc.parser.node.Node;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public abstract class ExpressionNode extends Node {

	public abstract Type getType();

	public boolean hasConstQualifier() {
		return getType().isConst();
	}

	public boolean isLvalue() {
		return false;
	}
	
	@Override
	public String getLeafName() {
		return super.getLeafName().replaceFirst("Expression$", "");
	}

	/**
	 * Marks every symbol within the syntax tree of the expression as used
	 * 
	 * @param expr the expression
	 */
	public static void markUsed(ExpressionNode expr) {
		if(expr instanceof ArrayIndexExpressionNode index) {
			markUsed(index.getTarget());
			markUsed(index.getIndex());
		}
		
		else if(expr instanceof BinaryExpressionNode bin) {
			markUsed(bin.getLeft());
			markUsed(bin.getRight());
		}
		
		else if(expr instanceof CallExpressionNode call) {
			markUsed(call.getTarget());
			call.getParameters().forEach(ExpressionNode::markUsed);
		}
		
		else if(expr instanceof CastExpressionNode cast)
			markUsed(cast.getTarget());
		
		else if(expr instanceof ConditionalExpressionNode cond) {
			markUsed(cond.getCondition());
			markUsed(cond.getWhenTrue());
			markUsed(cond.getWhenFalse());
		}
		
		else if(expr instanceof UnaryExpressionNode unary)
			markUsed(unary.getTarget());
		
		else if(expr instanceof MemberAccessExpressionNode mem)
			markUsed(mem.getTarget());
		
		else if(expr instanceof VariableExpressionNode var)
			var.getVariable().setUnused(false);
	}

}
