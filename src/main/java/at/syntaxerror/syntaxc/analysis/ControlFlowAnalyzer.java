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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import at.syntaxerror.syntaxc.intermediate.representation.Intermediate;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.FreeIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.JumpIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.LabelIntermediate;
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.Flag;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.tracking.Position;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
public class ControlFlowAnalyzer implements Logable {
	
	private final Map<String, List<JumpIntermediate>> jumps;
	private final Map<String, LabelIntermediate> labels;
	
	private final Map<String, CFGNode> blocks;
	private final List<LinkInfo> linkInfos;
	private final List<Intermediate> code;
	
	private String returnLabel;
	
	private long counterLabel;
	
	public ControlFlowAnalyzer() {
		jumps = new HashMap<>();
		labels = new HashMap<>();
		
		blocks = new LinkedHashMap<>();
		linkInfos = new ArrayList<>();
		code = new ArrayList<>();
	}
	
	@Override
	public Position getPosition() {
		return null;
	}
	
	@Override
	public Warning getDefaultWarning() {
		return Warning.SEM_NONE;
	}
	
	/* check for dead (unreachable) code within the compound statement
	 * 
	 * also appends the a null statement labeled with the returnLabel
	 */
	public List<Intermediate> checkDeadCode(Position pos, String name, List<Intermediate> intermediates, String returnLabel) {
		this.returnLabel = returnLabel;
		
		jumps.clear();
		labels.clear();
		blocks.clear();
		linkInfos.clear();

		intermediates.add(new LabelIntermediate(pos, returnLabel));
		
		for(Intermediate intermediate : intermediates)
			if(intermediate instanceof LabelIntermediate label)
				labels.put(label.getLabel(), label);
		
			else if(intermediate instanceof JumpIntermediate jump)
				jumps.computeIfAbsent(
					jump.getLabel(),
					k -> new ArrayList<>()
				).add(jump);
		
		checkUnknownLabels(); // check for unknown labels
		checkUnusedLabels(); // check for unused labels. Also matches labels where the associated goto statement is dead code
		
		generateNodes(name, intermediates);
		checkNodes();
		
		return blocks.values()
			.stream()
			.map(
				node -> node.dead || node.code == null
					? node.free
					: node.code
			).reduce(
				new ArrayList<>(),
				(a, b) -> {
					a.addAll(b);
					return a;
				}
			);
	}
	
	public CFGNode getGraph() {
		return blocks.values()
			.iterator()
			.next();
	}
	
	private void addBlock(CFGNode node) {
		blocks.put(node.name, node);
	}

	private void linkInfo(String src, String dst) {
		linkInfo(src, dst, LinkKind.NEXT);
	}

	private void linkInfo(String src, String dst, LinkKind kind) {
		linkInfos.add(new LinkInfo(src, dst, kind));
	}
	
	private CFGNode makeBlock(String name) {
		return new CFGNode(name, new ArrayList<>(code), NodeKind.PLAIN);
	}
	
	private void generateNodes(String name, List<Intermediate> intermediates) {
		boolean wasJump = false;
		boolean wasGoto = false;
		
		String previous = null;

		for(int i = 0; i < intermediates.size(); ++i) {
			
			Intermediate intermediate = intermediates.get(i);
			
			if(!Flag.CFG_VERBOSE.isEnabled()) {
				
				if(intermediate instanceof FreeIntermediate)
					continue;
				
			}
			
			code.add(intermediate);
			
			if(intermediate instanceof LabelIntermediate label) {

				if(wasGoto || wasJump) {
					wasGoto = wasJump = false;
					
					name = label.getLabel();
					
					continue;
				}

				String current = name;
				name = label.getLabel();

				linkInfo(current, name);
				
				addBlock(makeBlock(current));
				
				previous = current;
				
				wasJump = wasGoto = false;
				
				code.clear();
				continue;
			}

			if(!(intermediate instanceof FreeIntermediate))
				wasGoto = wasJump = false;
			
			if(intermediate instanceof JumpIntermediate jump) {

				wasJump = jump.isConditional();
				wasGoto = !wasJump;

				String current = name;
				
				int nextIdx = i + 1;
				Intermediate next;
				
				do {
					next = intermediates.get(nextIdx++);
				} while(next instanceof FreeIntermediate);
				
				
				if(next instanceof LabelIntermediate label)
					name = label.getLabel();
				
				else name = ".synthetic_" + counterLabel++;
				
				if(wasJump) // this -> true
					linkInfo(current, name, LinkKind.THEN);
				
				addBlock(makeBlock(current));

				code.clear();
				
				// this -> false
				linkInfo(
					current,
					jump.getLabel(),
					wasJump
						? LinkKind.ELSE
						: LinkKind.NEXT
				);
				
				previous = current;
			}
			
		}
		
		String returnName = name;
		
		boolean doesReturn = linkInfos
			.stream()
			.anyMatch(info -> info.destination().equals(returnName));
		
		if(doesReturn) {
			
			addBlock(new CFGNode(
				name,
				List.of(),
				NodeKind.PLAIN
			));
			
			CFGNode exit = CFGNode.exit();
			
			addBlock(exit);
			
			linkInfo(previous, name);
			
			linkInfo(name, exit.name);
		}
		
		linkInfos
			.stream()
			.collect(
				Collectors.toMap( // deduplicate
					LinkInfo::toString,
					Function.identity(),
					(a, b) -> a
				)
			)
			.values()
			.forEach(info -> {
			
			CFGNode src = blocks.get(info.source());
			CFGNode dst = blocks.get(info.destination());
			
			if(src != null)
				switch(info.kind()) {
				case NEXT: src.next = dst; break;
				case THEN: src.nextThen = dst; break;
				case ELSE: src.nextElse = dst; break;
				}
			
		});
	}

	private void checkNodes() {
		List<CFGNode> traversed = new ArrayList<>();
		
		traverse(traversed, getGraph());
		
		blocks.values()
			.stream()
			.filter(node -> !traversed.contains(node))
			.forEach(this::warnDead);
	}
	
	private void traverse(final List<CFGNode> traversed, CFGNode node) {
		if(node == null || traversed.contains(node))
			return;
		
		traversed.add(node);
		
		traverse(traversed, node.next);
		traverse(traversed, node.nextThen);
		traverse(traversed, node.nextElse);
	}
	
	private void warnDead(CFGNode block) {
		if(block == null || block.code == null)
			return;
		
		block.dead = true;
		
		Intermediate dead = null;
		
		var iter = block.code.iterator();
		
		while(iter.hasNext()) {
			Intermediate intermediate = iter.next();
			
			if(intermediate instanceof FreeIntermediate free) {
				block.free.add(free);
				continue;
			}
			
			if(dead != null)
				continue;
			
			String label = null;
			
			if(intermediate instanceof LabelIntermediate lbl)
				label = lbl.getLabel();
		
			else if(intermediate instanceof JumpIntermediate jump)
				label = jump.getLabel();
			
			if(label == null || !label.startsWith("."))
				dead = intermediate;
		}
		
		if(dead != null)
			warn(dead, Warning.DEAD_CODE, "Code is unreachable");
	}
	
	private void checkUnknownLabels() {
		boolean foundUnknownLabel = false;

		for(var entry : jumps.entrySet()) {
			String label = entry.getKey();
			
			if(label.equals(returnLabel))
				continue;
			
			if(!labels.containsKey(label)) {
				foundUnknownLabel = true;
				
				for(JumpIntermediate pos : entry.getValue())
					softError(pos, "Unknown label »%s«", label);
			}
		}
		
		if(foundUnknownLabel) // terminate if a labels is not known
			terminate();
	}
	
	private void checkUnusedLabels() {
		for(var entry : labels.entrySet()) {
			String label = entry.getKey();
			
			if(label.startsWith(".")) // skip internal labels
				continue;
			
			if(!jumps.containsKey(label))
				warn(entry.getValue(), Warning.UNUSED_LABEL, "Unused label »%s«", label);
		}
	}
	
	@RequiredArgsConstructor
	public static class CFGNode {
		
		private static final String ENTRY_NAME = ".entry";
		private static final String EXIT_NAME = ".exit";
		
		private static int id = 0;
		
		public static CFGNode entry() {
			return new CFGNode(ENTRY_NAME + "." + id++, null, NodeKind.ENTRY);
		}

		public static CFGNode exit() {
			return new CFGNode(EXIT_NAME + "." + id++, null, NodeKind.EXIT);
		}
		
		public final String name;
		public final List<Intermediate> code;
		public final NodeKind kind;
		
		private final List<Intermediate> free = new ArrayList<>();
		
		public boolean dead;
		
		public CFGNode next;

		public CFGNode nextThen;
		public CFGNode nextElse;
		
		public boolean isEntry() {
			return kind == NodeKind.ENTRY;
		}

		public boolean isExit() {
			return kind == NodeKind.EXIT;
		}
		
		@Override
		public String toString() {
			if(isEntry())
				return "Node[entry]";
			
			if(isExit())
				return "Node[exit]";
			
			if(nextThen != null || nextElse != null)
				return "Node[%s -> %s & %s; %s]".formatted(
					name,
					getName(nextThen),
					getName(nextElse),
					code
				);
			
			return "Node[%s -> %s; %s]".formatted(
				name,
				getName(next),
				code
			);
		}
		
		private static String getName(CFGNode node) {
			return node == null
				? null
				: node.name;
		}
		
	}
	
	private static enum NodeKind {
		PLAIN,
		ENTRY,
		EXIT
	}
	
	private static record LinkInfo(String source, String destination, LinkKind kind) {
		
	}
	
	private static enum LinkKind {
		NEXT,
		THEN,
		ELSE
	}
	
}
