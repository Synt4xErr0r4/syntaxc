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

import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.parser.StatementParser;
import at.syntaxerror.syntaxc.parser.node.statement.CompoundStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.GotoStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.LabeledStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.NullStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.StatementNode;
import at.syntaxerror.syntaxc.tracking.Position;

/**
 * @author Thomas Kasper
 * 
 */
public class ControlFlowAnalyzer implements Logable {

	private final StatementParser parser;
	
	private Map<String, List<GotoStatementNode>> gotos;
	private Map<String, LabeledStatementNode> labels;
	
	private Set<String> unused;
	private Set<StatementNode> warnCandidates;
	
	private boolean removedAny;
	private boolean alreadyWarned;
	
	private Set<String> deadCandidates;
	private String activeLabel;
	
	public ControlFlowAnalyzer(StatementParser parser) {
		this.parser = parser;
		
		gotos = new HashMap<>();
		labels = new HashMap<>();
		
		unused = new HashSet<>();
		warnCandidates = new HashSet<>();
		deadCandidates = new HashSet<>();
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
	public CompoundStatementNode checkDeadCode(CompoundStatementNode root, String returnLabel) {
		gotos.clear();
		labels.clear();
		unused.clear();
		warnCandidates.clear();
		
		parser.getGotos() // group gotos by label
			.forEach(stmt ->
				gotos.computeIfAbsent(
					stmt.getLabel(),
					k -> new ArrayList<>()
				).add(stmt)
			);
		
		labels = parser.getLabels();
		
		List<StatementNode> statements = root.getStatements();
		
		ScanResult result;
		
		do {
			removedAny = false;
			alreadyWarned = false;
			activeLabel = null;
			
			deadCandidates.clear();
			
			result = scanDead(false, statements);
			
			checkUnknownLabels(); // check for unknown labels
			checkUnusedLabels(); // check for unused labels. Also matches labels where the associated goto statement is dead code
			
			statements = result.statements();
		} while(removedAny);
		
		warnCandidates
			.stream()
			.sorted()
			.forEach(stmt -> warn(stmt, Warning.DEAD_CODE, "Code is unreachable"));
		
		statements.add(new LabeledStatementNode(
			root.getPosition(),
			returnLabel,
			new NullStatementNode(root.getPosition())
		));
		
		return new CompoundStatementNode(
			root.getPosition(),
			statements
				.stream()
				.filter(stmt -> stmt != REMOVED_NODE)
				.map(stmt -> { // strip unused labels
					if(stmt instanceof LabeledStatementNode label)
						if(unused.contains(label.getLabel()))
							return label.getStatement();
					
					return stmt;
				})
				.toList()
		);
	}
	
	private void removeDeadGoto(GotoStatementNode stmt) {
		String label = stmt.getLabel();
		
		if(gotos.containsKey(label)) {
			var entries = gotos.get(label);
			
			entries.remove(stmt);
			
			if(entries.isEmpty()) // remove whole entry if all references to the label are dead
				gotos.remove(label);
		}
	}
	
	private void removeDeadLabeled(LabeledStatementNode labeled) {
		labels.remove(labeled.getLabel());
	}
	
	// scans a list of statements for dead code
	private ScanResult scanDead(boolean isDead, List<StatementNode> statements) {
		List<StatementNode> result = new ArrayList<>();
		
		for(StatementNode stmt : statements) {
			ScanResult scan = scanDeadSingle(
				isDead,
				stmt
			);
			
			isDead = scan.isDead();
			
			if(!isDead)
				alreadyWarned = false;
			
			result.addAll(scan.statements());
		}
		
		return new ScanResult(isDead, result);
	}

	// scans a statement for dead code
	private ScanResult scanDeadSingle(boolean isDead, StatementNode stmt) {
		if(stmt == REMOVED_NODE) {
			alreadyWarned = true;
			return new ScanResult(isDead, List.of(REMOVED_NODE));
		}
		
		if(stmt instanceof CompoundStatementNode compound)
			return scanDead(isDead, compound.getStatements()); // scan children
		
		else if(isDead) {
			boolean internal = false; // skip internal labels (starting with a period '.')
			
			if(stmt instanceof GotoStatementNode gotoStmt) {
				removeDeadGoto(gotoStmt);
				
				internal = gotoStmt.getLabel().startsWith(".");
			}
			
			else if(stmt instanceof LabeledStatementNode labeled) {
				
				String label = labeled.getLabel();
				
				if(!unused.contains(label)) { // used label encountered
					deadCandidates.add(label);
					activeLabel = label;
					
					return new ScanResult(false, List.of(labeled));
				}
				
				removeDeadLabeled(labeled);

				internal = label.startsWith(".");
			}
			
			if(!internal && !alreadyWarned) { // only warn at the first dead statement
				alreadyWarned = true;
				
				warnCandidates.add(stmt);
			}
			
			else warnCandidates.remove(stmt);
			
			removedAny = true;
			return new ScanResult(isDead, List.of(REMOVED_NODE));
		}
		
		else if(stmt instanceof LabeledStatementNode labeled)
			activeLabel = labeled.getLabel();
		
		else if(stmt instanceof GotoStatementNode gotoStmt) {
			
			/*
			 * measure to counteract constructions like
			 * 
			 *   return;
			 *   label:
			 *   goto label;
			 * 
			 * which would otherwise not be detected as dead code
			 */
			if(activeLabel != null &&
				deadCandidates.contains(activeLabel) &&
				gotoStmt.getLabel().equals(activeLabel)) {
				
				removeDeadGoto(gotoStmt);
				return new ScanResult(isDead, List.of(REMOVED_NODE));
			}
			
			return new ScanResult(true, List.of(stmt));
		}
		
		return new ScanResult(false, List.of(stmt));
	}
	
	private void checkUnknownLabels() {
		boolean foundUnknownLabel = false;
		
		for(var entry : gotos.entrySet()) {
			String label = entry.getKey();
			
			if(!labels.containsKey(label)) {
				foundUnknownLabel = true;
				
				for(GotoStatementNode stmt : entry.getValue())
					error(stmt, Warning.CONTINUE, "Unknown label »%s«", label);
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
			
			if(!unused.contains(label) && !gotos.containsKey(label)) {
				warn(entry.getValue(), Warning.UNUSED_LABEL, "Unused label »%s«", label);
				unused.add(label);
			}
		}
	}
	
	private static final StatementNode REMOVED_NODE = new StatementNode() {
		
		@Override
		public Position getPosition() {
			return null;
		}
		
	};
	
	private static record ScanResult(boolean isDead, List<StatementNode> statements) {
		
		/* isDead:
		 * 
		 * true if the compound obstructs execution afterwards
		 * 
		 * e.g.
		 * 
		 *   {
		 *   	...
		 *   	return;
		 *   }
		 * 
		 * but not
		 * 
		 *   {
		 *   	...
		 *   	return;
		 *   	
		 *   	label:
		 *   	...
		 *   }
		 * 
		 * unless 'label' is not referenced by a 'goto' statement
		 */
		
	}
	
}
