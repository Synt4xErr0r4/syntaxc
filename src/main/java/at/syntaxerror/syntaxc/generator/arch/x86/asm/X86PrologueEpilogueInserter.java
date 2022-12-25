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
package at.syntaxerror.syntaxc.generator.arch.x86.asm;

import at.syntaxerror.syntaxc.generator.arch.Alignment;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Instruction;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.PrologueEpilogueInserter;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class X86PrologueEpilogueInserter extends PrologueEpilogueInserter {

	private final X86Assembly x86;
	
	@Override
	public void insertPrologue(Instructions asm, long stackSize) {
		/*
		 * stack frame setup
		 * 
		 * push rbp
		 * mov rbp, rsp
		 * sub rsp, <size>
		 */
		
		if(stackSize != 0) {
			asm.setConstructor(X86Instruction::new);
			
			stackSize = Alignment.alignAt(stackSize, 16); // align stack to 16 bytes
			
			asm.add(X86InstructionKinds.PUSH, x86.RBP);
			asm.add(X86InstructionKinds.MOV, x86.RBP, x86.RSP);
			asm.add(X86InstructionKinds.SUB, x86.RSP, X86OperandHelper.constant(stackSize));
		}
	}
	
	@Override
	public void insertEpilogue(Instructions asm, long stackSize) {
		/*
		 * restore stack frame and return
		 * 
		 * mov rsp, rbp
		 * pop rbp
		 * ret
		 */

		asm.setConstructor(X86Instruction::new);
		
		if(stackSize != 0) {
			stackSize = Alignment.alignAt(stackSize, 16); // align stack to 16 bytes
			
			asm.add(X86InstructionKinds.MOV, x86.RSP, x86.RBP);
			asm.add(X86InstructionKinds.POP, x86.RBP);
		}

		asm.add(X86InstructionKinds.RET);
	}
	
}
