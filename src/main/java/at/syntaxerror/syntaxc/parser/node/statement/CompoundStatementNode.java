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
package at.syntaxerror.syntaxc.parser.node.statement;

import static at.syntaxerror.syntaxc.parser.tree.TreeNode.child;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.parser.tree.TreeNode;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.tracking.Position;
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
public class CompoundStatementNode extends StatementNode {
	
	private final Position position;
	private final List<SymbolObject> declarations;
	private final List<StatementNode> statements;
	
	@Override
	protected Set<String> checkGotos() {
		return statements.stream()
			.map(StatementNode::getGotos)
			.reduce(new HashSet<>(), COMBINE);
	}
	
	@Override
	protected Set<String> checkLabels() {
		return statements.stream()
			.map(StatementNode::getLabels)
			.reduce(new HashSet<>(), COMBINE);
	}
	
	@Override
	public boolean checkInterrupt() {
		return statements.stream()
			.anyMatch(StatementNode::doesInterrupt);
	}
	
	@Override
	public List<Pair<String, TreeNode>> getChildren() {
		return List.of(
			child(
				"declarations",
				declarations.stream()
					.map(sym -> new TreeListNode(
						List.of(
							child(
								"name",
								sym.getName()
								 + (sym.isTemporaryVariable()
									? " (tmp)"
									: "")
							),
							child("type", sym.getType())
						)
					))
					.toList()
			),
			child("statements", statements)
		);
	}
	
}
