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
package at.syntaxerror.syntaxc.intermediate.graph;

import static guru.nidi.graphviz.attribute.Records.rec;
import static guru.nidi.graphviz.attribute.Records.turn;
import static guru.nidi.graphviz.model.Compass.NORTH;
import static guru.nidi.graphviz.model.Compass.SOUTH;
import static guru.nidi.graphviz.model.Factory.between;
import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;
import static guru.nidi.graphviz.model.Factory.port;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import at.syntaxerror.syntaxc.analysis.ControlFlowAnalyzer.CFGNode;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.FreeIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.JumpIntermediate;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.Flag;
import at.syntaxerror.syntaxc.misc.GraphUtils;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Records;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.Node;

/**
 * @author Thomas Kasper
 * 
 */
public class ControlFlowGraphGenerator {

	private static final String EOL = "\\l";
	
	public static void generate(OutputStream out, Map<String, FunctionData> functions) {
		new ControlFlowGraphGenerator(out, functions);
	}
	
	private Graph graph;
	
	private Map<String, Node> blocks = new HashMap<>();
	private List<LinkInfo> linkInfos = new ArrayList<>();
	
	private List<CFGNode> nodes = new ArrayList<>();
	
	private List<String> code = new ArrayList<>();
	
	private ControlFlowGraphGenerator(OutputStream out, Map<String, FunctionData> functions) {
		graph = graph("control_flow_graph")
			.graphAttr().with("overlap", "false");
		
		List<Graph> subgraphs = new ArrayList<>();
		
		for(var function : functions.entrySet()) {
			FunctionData data = function.getValue();
			
			subgraphs.add(visit(
				function.getKey(),
				data.controlFlowGraph(),
				data.hasReturnValue()
			));
		}
		
		try {
			Graphviz.fromGraph(graph.with(subgraphs))
				.render(GraphUtils.getGraphFormat(Flag.CONTROL_FLOW_GRAPH))
				.toOutputStream(out);
		} catch (Exception e) {
			Logger.warn("Failed to generate control flow graph: %s", e.getMessage());
		}
	}
	
	private void addBlock(String name, Node node) {
		blocks.put(name, node);
	}
	
	private void editBlock(String name, Function<Node, Node> edit) {
		blocks.computeIfPresent(name, (x, node) -> edit.apply(node));
	}

	private void addLabel(String name) {
		code.add(rec(name + ":" + EOL));
	}

	private void linkInfo(String src, String dst, Color color) {
		linkInfos.add(new LinkInfo(src, dst, color));
	}
	
	private void linkBlock(String src, String dst, Color color) {
		editBlock(src, blk -> {
			Link link = between(
				port(src + ".port", SOUTH),
				node(dst).port(NORTH)
			).with("constraint", "true");
			
			if(color != null)
				link = link.with(color);
			
			return blk.link(link);
		});
	}
	
	private void link(CFGNode src, CFGNode dst, Color color) {
		if(dst != null)
			linkInfo(src.name, dst.name, color);
	}
	
	private void next(CFGNode node, boolean hasReturnValue) {
		if(node == null || nodes.contains(node))
			return;
		
		nodes.add(node);
		
		if(node == CFGNode.ENTRY) {
			next(node.next, hasReturnValue);
			return;
		}
		
		if(node == CFGNode.EXIT) {
			if(hasReturnValue)
				code.add(rec("return " + SymbolObject.RETURN_VALUE_NAME + ";" + EOL));
			
			else code.add(rec("return;" + EOL));
			
			addBlock(
				CFGNode.EXIT_NAME,
				makeDiamond(
					CFGNode.EXIT_NAME,
					"EXIT"
				)
			);
			return;
		}
		
		code.clear();
		
		addLabel(node.name);
		
		String labelTrue = node.nextThen == null
			? null
			: node.nextThen.name;
		
		for(Intermediate intermediate : node.code) {
			
			if(!Flag.CFG_VERBOSE.isEnabled()) {
				
				if(intermediate instanceof FreeIntermediate)
					continue;
				
			}

			if(intermediate instanceof JumpIntermediate jump)
				code.add(rec(
					jump.toString(labelTrue)
						.replace("\n", EOL)
						+ EOL
				));
			
			else code.add(rec(intermediate.toString()));
		}
		
		addBlock(node.name, makeBlock(node.name));
		
		link(node, node.next, null);
		link(node, node.nextThen, Color.GREEN);
		link(node, node.nextElse, Color.RED);
		
		next(node.next, hasReturnValue);
		next(node.nextThen, hasReturnValue);
		next(node.nextElse, hasReturnValue);
	}
	
	private Graph visit(String name, CFGNode graph, boolean hasReturnValue) {
		Graph g = graph(name)
			.directed()
			.cluster()
			.graphAttr().with(Label.of(name));
		
		blocks.clear();
		linkInfos.clear();
		
		code.clear();
		addLabel(name);
		
		addBlock(
			CFGNode.ENTRY_NAME,
			makeDiamond(
				CFGNode.ENTRY_NAME,
				"ENTRY"
			)
		);
		
		link(CFGNode.ENTRY, graph, null);
		next(graph, hasReturnValue);
		
		linkInfos.forEach(info -> linkBlock(
			info.source(),
			info.destination(),
			info.color()
		));
		
		return g.with(
			blocks.values()
				.toArray(Node[]::new)
		);
	}
	
	private Node makeDiamond(String name, String label) {
		return node(name)
			.with(Shape.M_DIAMOND)
			.with(Style.FILLED)
			.with(Color.WHITE.fill())
			.with(Label.of(label));
	}
	
	private Node makeBlock(String name) {
		if(code.isEmpty())
			code.add(rec(name + ".port", ""));
		
		else {
			int i = code.size() - 1;
			
			code.set(i, "<" + name + ".port>" + code.get(i));
		}
		
		return node(name)
			.with(Records.of(
				turn(code.toArray(String[]::new))
			))
			.with(Style.FILLED)
			.with(Color.LIGHTGRAY.fill());
	}
	
	private static record LinkInfo(String source, String destination, Color color) {
		
	}
	
	public static record FunctionData(List<Intermediate> intermediate, CFGNode controlFlowGraph, boolean hasReturnValue) {
		
	}
	
}
