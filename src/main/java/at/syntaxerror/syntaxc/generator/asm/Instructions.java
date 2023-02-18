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

import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;

import at.syntaxerror.syntaxc.generator.asm.insn.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.insn.AssemblyInstructionKind;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import lombok.Getter;
import lombok.Setter;

/**
 * A kind of linked list capable of holding assembly instructions
 * 
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings("deprecation")
public class Instructions implements Iterable<AssemblyInstruction> {

	@Getter
	@Setter(onMethod_ = @Deprecated) // internal use only
	private AssemblyInstruction head;
	
	@Getter
	@Setter(onMethod_ = @Deprecated) // internal use only
	private AssemblyInstruction tail;

	@Setter(onMethod_ = @Deprecated)
	private int count;
	
	@Setter
	private InstructionConstructor constructor;
	
	public void clear() {
		head = null;
		tail = null;
		count = 0;
	}
	
	public void add(AssemblyInstructionKind kind) {
		add(kind, null);
	}
	
	public void add(AssemblyInstructionKind kind, AssemblyTarget destination, AssemblyTarget...targets) {
		add(constructor.construct(this, kind, destination, targets));
	}
	
	public void add(AssemblyInstruction insn) {
		insn.setPrevious(tail);
		
		if(tail != null)
			tail.setNext(insn);
		
		tail = insn;
		
		if(head == null)
			head = insn;
		
		++count;
		insn.onAdd();
	}
	
	public AssemblyInstruction get(int i) {
		if(i < 0 || i >= count)
			return null;
		
		int mid = count / 2;
		AssemblyInstruction node;
		
		if(i > mid) {
			node = tail;
			
			for(int j = 0; j < count - i - 1 && node != null; ++j)
				node = node.getPrevious();
			
		}
		else {
			node = head;
			
			for(int j = 0; j < i && node != null; ++j)
				node = node.getNext();
			
		}
		
		return node;
	}
	
	public int size() {
		return count;
	}
	
	public Stream<AssemblyInstruction> stream() {
		return Stream.iterate(
			head,
			node -> node != null,
			AssemblyInstruction::getNext
		);
	}
	
	public Iterator<AssemblyInstruction> iterator() {
		return stream().iterator();
	}
	
	@Override
	public Spliterator<AssemblyInstruction> spliterator() {
		return stream().spliterator();
	}
	
	public static interface InstructionConstructor {
		
		AssemblyInstruction construct(
			Instructions parent,
			AssemblyInstructionKind kind,
			AssemblyTarget destination,
			AssemblyTarget...targets
		);
		
	}
	
}
