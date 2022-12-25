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
package at.syntaxerror.syntaxc.parser.node;

import static at.syntaxerror.syntaxc.parser.tree.SyntaxTreeNode.child;
import java.util.List;

import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.parser.node.statement.CompoundStatementNode;
import at.syntaxerror.syntaxc.parser.tree.SyntaxTreeNode;
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
@ToString
public class FunctionNode extends SymbolNode {

	private final SymbolObject object;
	private final List<SymbolObject> declarations;
	private final List<SymbolObject> parameters;
	private final CompoundStatementNode body;
	
	@Override
	public Position getPosition() {
		return object.getPosition();
	}
	
	@Override
	public List<Pair<String, SyntaxTreeNode>> getChildren() {
		return List.of(
			child(
				"declarations",
				declarations.stream()
					.filter(sym -> !sym.isSyntaxTreeIgnore())
					.map(sym -> new TreeListNode(
						List.of(
							child("name", sym.getDebugName()),
							child("type", sym.getType())
						)
					))
					.toList()
			),
			child("name", object.getName()),
			child("type", object.getType()),
			child("body", body)
		);
	}
	
}
