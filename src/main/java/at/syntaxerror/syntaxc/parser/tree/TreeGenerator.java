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
package at.syntaxerror.syntaxc.parser.tree;

import static guru.nidi.graphviz.model.Factory.*;

import java.io.OutputStream;
import java.util.List;

import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.Flag;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Rank.RankDir;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Node;

/**
 * @author Thomas Kasper
 * 
 */
public class TreeGenerator {

	public static void generate(OutputStream out, List<? extends TreeNode> nodes) {
		new TreeGenerator(out, nodes);
	}
	
	private Graph graph;
	
	private long counter;
	
	private TreeGenerator(OutputStream out, List<? extends TreeNode> nodes) {
		graph = graph("syntaxtree")
			.directed()
			.graphAttr().with(Rank.dir(RankDir.TOP_TO_BOTTOM));
		
		Node node = node("root");
		
		for(TreeNode child : nodes)
			node = visit(node, null, child);
		
		try {
			@SuppressWarnings("preview")
			Format fmt = switch(Flag.SYNTAX_TREE.getValue().toLowerCase()) {
			case "png" -> Format.PNG;
			case "svg" -> Format.SVG;
			case null -> Format.DOT;
			default -> Format.DOT;
			};
			
			Graphviz.fromGraph(graph.with(node))
				.render(fmt)
				.toOutputStream(out);
		} catch (Exception e) {
			Logger.warn("Failed to generate syntax tree: %s", e.getMessage());
		}
	}
	
	private Node next() {
		return node("node" + counter++);
	}
	
	private Node visit(Node parent, String name, TreeNode node) {
		Node self = next();
		
		if(name != null)
			self = self.with(Label.of(name + '\n' + node.getLeafName()));
		
		else self = self.with(Label.of(node.getLeafName()));
		
		for(var child : node.getChildren())
			if(child != null && child.getRight() != null)
				self = visit(self, child.getLeft(), child.getRight());
		
		return parent.link(to(self));
	}

}
