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

import static at.syntaxerror.syntaxc.parser.tree.TreeNode.child;

import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.misc.StringUtils;
import at.syntaxerror.syntaxc.parser.tree.TreeNode;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.global.StringInitializer;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.StructType;
import at.syntaxerror.syntaxc.type.StructType.Member;
import at.syntaxerror.syntaxc.type.Type;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
@Getter
@ToString(exclude = "position")
public class VariableExpressionNode extends ExpressionNode {

	private final Position position;
	private final SymbolObject variable;

	@Override
	public boolean isLvalue() {
		if(variable.isVariable() && getType().isStructLike())
			return checkNestedConst(getType().toStructLike());
		
		return !variable.isFunction();
	}
	
	private boolean checkNestedConst(StructType type) {
		for(Member mem : type.getMembers())
			if(mem.getType().isConst())
				return false;
		
			else if(mem.getType().isStructLike())
				return checkNestedConst(mem.getType().toStructLike());
		
		return true;
	}
	
	@Override
	public Type getType() {
		return variable.getType();
	}
	
	@Override
	public List<Pair<String, TreeNode>> getChildren() {
		List<Pair<String, TreeNode>> children = new ArrayList<>();
		
		children.add(child("name", variable.getDebugName()));
		
		children.add(child("type", getType()));
		
		if(variable.isString() && variable.getVariableData().initializer() != null)
			children.add(
				child(
					"value",
					'"' + StringUtils.quote(
						((StringInitializer) variable.getVariableData().initializer()).value()
					) + '"'
				)
			);
			
		return children;
	}

}
