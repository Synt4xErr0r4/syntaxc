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
package at.syntaxerror.syntaxc.generator.asm;

import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public class AssemblyInstruction {
	
	private final Instructions parent;
	
	protected AssemblyInstruction previous;
	protected AssemblyInstruction next;
	
	private final AssemblyInstructionKind kind;
	
	private final List<AssemblyTarget> destinations;
	private final List<AssemblyTarget> sources;
	
	private String strrep;
	
	public AssemblyInstruction(@NonNull Instructions parent, AssemblyInstructionKind kind,
			List<AssemblyTarget> destinations, List<AssemblyTarget> sources) {
		this.parent = parent;
		this.kind = kind;
		this.destinations = new ArrayList<>(destinations);
		this.sources = new ArrayList<>(sources);
	}
	
	public AssemblyInstruction(AssemblyInstruction copy) {
		this.parent = copy.getParent();
		this.kind = copy.getKind();
		this.destinations = copy.getDestinations();
		this.sources = copy.getSources();
		
		strrep = copy.toString();
	}
	
	public final void insertBefore(AssemblyInstruction insn) {
		insn.next = this;
		insn.previous = previous;

		if(previous != null)
			previous.next = insn;
		else parent.head = insn;
		
		previous = insn;
		
		++parent.count;
	}

	public final void insertAfter(AssemblyInstruction insn) {
		insn.previous = this;
		insn.next = next;
		
		if(next != null)
			next.previous = insn;
		else parent.tail = insn;
		
		next = insn;

		++parent.count;
	}
	
	@Override
	public AssemblyInstruction clone() {
		return new AssemblyInstruction(this);
	}
	
	@Override
	public String toString() {
		return strrep == null
			? "%s: %s <- %s".formatted(kind, destinations, sources)
			: strrep;
	}
	
}
