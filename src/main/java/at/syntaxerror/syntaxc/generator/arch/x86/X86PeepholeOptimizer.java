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
package at.syntaxerror.syntaxc.generator.arch.x86;

import java.math.BigInteger;

import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Instruction;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86IntegerTarget;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.PeepholeOptimizer;
import at.syntaxerror.syntaxc.generator.asm.insn.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.insn.AssemblyInstructionKind;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;

/**
 * @author Thomas Kasper
 * 
 */
public class X86PeepholeOptimizer extends PeepholeOptimizer {

	@Override
	public void optimize(Instructions asm) {
		for(AssemblyInstruction insn : asm) {
			
			AssemblyInstructionKind insnKind = insn.getKind();
			
			if(!(insnKind instanceof X86InstructionKinds kind))
				continue;
			
			if(kind == X86InstructionKinds.CLOBBER) {
				insn.remove();
				continue;
			}
			
			if(kind.isCopy()) {
				
				AssemblyTarget dst = insn.getDestinations().get(0);
				AssemblyTarget src = insn.getSources().get(0);
				
				if(dst instanceof X86Register reg) {
					
					if(reg.getType().isInteger() &&
						src instanceof X86IntegerTarget i
						&& i.getValue().compareTo(BigInteger.ZERO) == 0) {
						
						/* convert
						 * 	
						 * 	mov eax, 0
						 * 
						 * into
						 * 
						 * 	xor eax, eax
						 */
						
						insn.insertBefore(new X86Instruction(
							asm,
							X86InstructionKinds.XOR,
							reg,
							reg
						));
						insn.remove();
						
						continue;
					}
					
					if(src == reg) {
						/* remove
						 * 	
						 * 	mov eax, eax
						 */
						insn.remove();
						continue;
					}
					
				}
				
				continue;
			}
			
			if(kind.isAdditive()) {
				
				AssemblyTarget src = insn.getSources().get(0);
				
				if(src instanceof X86IntegerTarget i
					&& i.getValue().compareTo(BigInteger.ZERO) == 0) {
					
					/* remove
					 * 
					 *  add eax, 0
					 *  sub eax, 0
					 */
					
					insn.remove();
					continue;
				}
				
			}
			
		}
	}
	
}
