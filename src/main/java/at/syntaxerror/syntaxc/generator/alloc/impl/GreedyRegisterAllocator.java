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
package at.syntaxerror.syntaxc.generator.alloc.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import at.syntaxerror.syntaxc.generator.alloc.RegisterAllocator;
import at.syntaxerror.syntaxc.generator.arch.Alignment;
import at.syntaxerror.syntaxc.generator.asm.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualRegisterTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualStackTarget;
import lombok.RequiredArgsConstructor;

/**
 * The default register allocation implementation. This is functionally based on LLVM's RegAllocGreedy, as described
 * in <a href="https://youtu.be/IK8TMJf3G6U">Matthias Braun's talk at the 2018 LLVM Developer's meeting</a>. Also
 * explained in <a href="https://llvm.org/devmtg/2018-04/slides/Yatsina-LLVM%20Greedy%20Register%20Allocator.pdf"
 * >this document</a> and <a href="https://blog.llvm.org/2011/09/greedy-register-allocation-in-llvm-30.html">here</a>.
 * 
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public abstract class GreedyRegisterAllocator extends RegisterAllocator {
	
	/*
	 * 	FIXME basically the whole code here is just to test the stack allocator
	 */
	
	private final Instructions asm;
	private final Alignment alignment;
	
	private Instructions annotated;
	
	private StackAllocator allocator;
	
	@Override
	public long getStackSize() {
		return allocator.getStackSize();
	}
	
	@Override
	public void allocate() {
		annotated = annotate(asm);
		
		allocateRegisters();
		allocateMemory();
	}
	
	private Instructions annotate(Instructions asm) {
		Instructions annotated = new Instructions();
		
		Map<Long, VirtualStackTarget> stacks = new HashMap<>();
		Map<Long, VirtualRegisterTarget> registers = new HashMap<>();
		
		Map<Long, AnnotatedAssemblyInstruction> stackLastUsed = new HashMap<>();
		Map<Long, AnnotatedAssemblyInstruction> registerLastUsed = new HashMap<>();
		
		for(AssemblyInstruction insn : asm) {
			AnnotatedAssemblyInstruction annotatedInsn;
			
			if(insn instanceof AnnotatedAssemblyInstruction anno)
				annotatedInsn = anno;
			
			else annotatedInsn = new AnnotatedAssemblyInstruction(insn);

			annotatedInsn.getFreeableStacks().clear();
			annotatedInsn.getFreeableRegisters().clear();
			
			Stream.concat(
				insn.getDestinations().stream(),
				insn.getSources().stream()
			).forEach(target -> {
				
				if(target instanceof VirtualStackTarget stack) {
					long id = stack.getId();
					
					stacks.putIfAbsent(id, stack);
					stackLastUsed.put(id, annotatedInsn);
				}

				if(target instanceof VirtualRegisterTarget register) {
					long id = register.getId();
					
					registers.putIfAbsent(id, register);
					registerLastUsed.put(id, annotatedInsn);
				}
				
			});
			
			annotated.add(annotatedInsn);
		}
		
		stackLastUsed.forEach(
			(target, insn) -> insn.getFreeableStacks()
				.add(stacks.get(target))
			);
		
		registerLastUsed.forEach(
			(target, insn) -> insn.getFreeableRegisters()
				.add(registers.get(target))
			);
		
		return annotated;
	}
	
	private void allocateRegisters() {
		
	}
	
	private void allocateMemory() {
		allocator = new StackAllocator(alignment);
		
		int index;
		
		for(AssemblyInstruction insn : annotate(annotated)) {
			List<AssemblyTarget> dsts = insn.getDestinations();
			List<AssemblyTarget> srcs = insn.getSources();
			
			index = 0;
			
			for(AssemblyTarget target : dsts)
				allocateMemory(target, dsts, index++);

			index = 0;
			
			for(AssemblyTarget target : srcs)
				allocateMemory(target, srcs, index++);
			
			((AnnotatedAssemblyInstruction) insn).getFreeableStacks()
				.forEach(allocator::free);
		}
	}
	
	private void allocateMemory(AssemblyTarget target, List<AssemblyTarget> targets, int index) {
		if(target instanceof VirtualStackTarget mem)
			targets.set(
				index,
				resolveVirtualMemory(
					allocator.allocate(mem),
					mem.getType()
				)
			);
	}
	
	/**
	 * try to assign a physical register to the virtual register
	 */
	private boolean assign() {
		
		return false;
	}

	/**
	 * try to revert previous allocations
	 */
	private boolean evict() {
		
		return false;
	}

	/**
	 * try live range splitting
	 */
	private boolean split() {
		
		return false;
	}

	/**
	 * try to spill and reload
	 */
	private boolean spill() {
		
		return false;
	}
	
	/**
	 * fail if register could not be allocated
	 */
	private void error() {
		
	}
	
}
