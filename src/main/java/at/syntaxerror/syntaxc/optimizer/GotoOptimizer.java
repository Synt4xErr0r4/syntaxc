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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxc.intermediate.representation.Intermediate;
import at.syntaxerror.syntaxc.intermediate.representation.JumpIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.LabelIntermediate;
import at.syntaxerror.syntaxc.misc.config.Optimizations;

/**
 * @author Thomas Kasper
 * 
 */
public class GotoOptimizer {

	private Map<String, String> aliases = new HashMap<>();
	private Map<String, List<Integer>> jumps = new HashMap<>();
	private List<Integer> redundants = new ArrayList<>();

	private String resolveLabel(String label) {
		return resolveLabel(label, label);
	}
	
	private String resolveLabel(final String original, String label) {
		while(aliases.containsKey(label)) {
			label = aliases.get(label);

			if(label.equals(original)) // circular reference
				return original;
		}
		
		return label;
	}
	
	/*
	 * Optimizes gotos and jumps:
	 * 
	 * - jump-to-jump
	 * - goto followed by label jumped to
	 */
	public List<Intermediate> optimize(List<Intermediate> intermediates, final String returnLabel) {
		aliases.clear();
		jumps.clear();
		redundants.clear();
		
		int index = 0;
		
		String label = null;
		String jumpLabel = null;
		
		Iterator<Intermediate> it = intermediates.iterator();
		
		while(it.hasNext()) {
			Intermediate intermediate = it.next();
			
			if(intermediate instanceof LabelIntermediate labeled) {
				if(label != null)
					aliases.put(label, labeled.getLabel());
				
				label = labeled.getLabel();
				
				if(jumpLabel != null && label.equals(jumpLabel))
					redundants.add(index - 1);
				
				jumpLabel = null;
			}
			
			else if(intermediate instanceof JumpIntermediate jump) {
				
				jumpLabel = jump.getLabel();

				jumps.computeIfAbsent(
					jumpLabel,
					l -> new ArrayList<>()
				).add(index);
				
				if(!jump.isConditional() && label != null)
					aliases.put(label, jumpLabel);
				
				label = null;
			}
			
			else label = jumpLabel = null;
			
			++index;
		}
		
		/* remove trailing 'jmp .L0; .L0:' at the end of a function after a 'return' statement */
		if(jumpLabel != null && returnLabel != null && jumpLabel.equals(returnLabel))
			intermediates.remove(index - 1);
		
		boolean hasOptimized = false;
		
		if(Optimizations.JUMP_TO_JUMP.isEnabled())
			for(var jump : jumps.entrySet()) {
				
				jumpLabel = jump.getKey();
				
				label = resolveLabel(jumpLabel);
				
				if(label.equals(jumpLabel))
					continue;
				
				for(int idx : jump.getValue()) {
					hasOptimized = true;
					
					JumpIntermediate intermediate = (JumpIntermediate) intermediates.get(idx);
					
					intermediates.set(
						idx,
						new JumpIntermediate(
							intermediate.getPosition(),
							intermediate.getCondition(),
							label
						)
					);
				}
			}
		
		if(Optimizations.GOTO.isEnabled()) {
			int offset = 0;
			
			Collections.sort(redundants);
			
			for(int redundant : redundants) {
				hasOptimized = true;
				intermediates.remove(redundant - offset++);
			}
		}
		
		return hasOptimized
			? optimize(intermediates, returnLabel)
			: intermediates;
	}
	
}
