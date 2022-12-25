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

import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.generator.asm.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualRegisterTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualStackTarget;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public class AnnotatedAssemblyInstruction extends AssemblyInstruction {

	private List<VirtualStackTarget> freeableStacks = new ArrayList<>();
	private List<VirtualRegisterTarget> freeableRegisters = new ArrayList<>();
	
	private AssemblyInstruction instruction;
	
	public AnnotatedAssemblyInstruction(AssemblyInstruction insn) {
		super(insn);
		instruction = insn;
	}
	
	public void free(VirtualStackTarget target) {
		freeableStacks.add(target);
	}

	public void free(VirtualRegisterTarget target) {
		freeableRegisters.add(target);
	}
	
	@Override
	public String toString() {
		return "Annotated(" + instruction + ", free=" + freeableStacks + "&" + freeableRegisters + ")";
	}

}
