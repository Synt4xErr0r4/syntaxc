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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Instruction;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionSelector;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86DeferredMemoryTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86IntegerTarget;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.PeepholeOptimizer;
import at.syntaxerror.syntaxc.generator.asm.insn.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.insn.AssemblyInstructionKind;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public class X86PeepholeOptimizer extends PeepholeOptimizer {

	private Map<AssemblyTarget, Pair<BigInteger, AssemblyInstruction>> additives = new HashMap<>();

	private void clobber(AssemblyInstruction insn) {
		clobber(insn.getDestinations());
		clobber(insn.getSources());
	}
	
	private void clobber(List<AssemblyTarget> list) {
		list.forEach(additives::remove);
		
		list.stream()
			.filter(target -> target != null)
			.map(AssemblyTarget::getNestedTargets)
			.filter(nested -> nested != null)
			.forEach(this::clobber);
	}
	
	private void processDeferredMemory(List<AssemblyTarget> targets) {
		for(int i = 0; i < targets.size(); ++i)
			if(targets.get(i) instanceof X86DeferredMemoryTarget deferred)
				targets.set(i, deferred.combine());
	}
	
	@Override
	public void optimize(Instructions asm) {
		additives.clear();
		
		boolean x32 = ArchitectureRegistry.getBitSize() == BitSize.B32;
		
		AssemblyInstruction call = null;
		
		boolean hasModified = false;
		
		for(AssemblyInstruction insn : asm) {
			
			processDeferredMemory(insn.getSources());
			processDeferredMemory(insn.getDestinations());
			
			AssemblyInstructionKind insnKind = insn.getKind();
			
			if(!(insnKind instanceof X86InstructionKinds kind))
				continue;
			
			if(kind == X86InstructionKinds.RET && call != null) {
				insn.insertAfter(new X86Instruction(
					asm,
					X86InstructionKinds.JMP,
					call.getDestinations().get(0)
				));
				
				insn.remove();
				call.remove();
				
				call = null;
				hasModified = true;
				continue;
			}
			
			if(kind != X86InstructionKinds.LABEL)
				call = kind == X86InstructionKinds.CALL ? insn : null;
			
			if(kind.isAdditive()) {
				
				AssemblyTarget src = insn.getSources().get(0);
				
				if(src instanceof X86IntegerTarget i) {
					
					if(i.getValue().compareTo(BigInteger.ZERO) == 0) {
						/* remove
						 * 
						 *  add eax, 0
						 *  sub eax, 0
						 */
						
						insn.remove();
						hasModified = true;
						continue;
					}
					
					/* group additions/subtractions */
					
					boolean subtract = kind.isSubtraction();
					
					AssemblyTarget dst = insn.getDestinations().get(0);
					
					var additive = additives.get(dst);
					
					BigInteger value = null;
					AssemblyInstruction previous = null;
					
					if(additive != null) {
						value = additive.getLeft();
						previous = additive.getRight();
					}
					
					if(value == null) {
						value = i.getValue();
						
						if(subtract)
							value = value.negate();
					}
					else {
						previous.remove();
						
						if(subtract)
							value = value.subtract(i.getValue());
						else value = value.add(i.getValue());
						
						int cmp = value.compareTo(BigInteger.ZERO);
						
						if(cmp != 0) {
							AssemblyInstruction op = new X86Instruction(
								asm,
								X86InstructionSelector.select(
									cmp < 0
										? X86InstructionKinds.SUB
										: X86InstructionKinds.ADD,
									dst.getType()
								),
								dst,
								new X86IntegerTarget(
									i.getType(),
									value.abs()
								)
							);
							
							insn.insertBefore(op);
						}
						
						insn.remove();
						
						hasModified = true;
					}
					
					additives.put(dst, Pair.of(value, insn));
				}
				
			}
			
			clobber(insn);
			
			if(kind == X86InstructionKinds.CLOBBER) {
				insn.remove();
				continue;
			}
			
			if(kind.isCopy()) {
				
				AssemblyTarget dst = insn.getDestinations().get(0);
				AssemblyTarget src = insn.getSources().get(0);
				
				if(dst instanceof X86Register reg) {
					
					if(x32 && reg == X86Register.SIL || reg == X86Register.DIL) {
						
						if(insn.getKind() == X86InstructionKinds.MOV)
							insn.setKind(X86InstructionKinds.MOVZX);
						
						insn.getDestinations().set(0, reg.resized(Type.SHORT));
					}
					
					if(reg.getType().isInteger() && isZero(src)) {
						
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

						hasModified = true;
						continue;
					}
					
					if(src == reg) {
						/* remove
						 * 	
						 * 	mov eax, eax
						 */
						insn.remove();
						hasModified = true;
						continue;
					}
					
				}
				
				if(src instanceof X86Register reg) {

					if(x32 && reg == X86Register.SIL || reg == X86Register.DIL)
						insn.getSources().set(0, reg.resized(Type.SHORT));
					
				}
				
				continue;
			}
			
			if(kind == X86InstructionKinds.CMP) {

				AssemblyTarget a = insn.getSources().get(0);
				AssemblyTarget b = insn.getSources().get(1);
				
				if(a.isRegister() && isZero(b)) {
					/* convert
					 * 
					 * 	cmp eax, 0
					 * 
					 * into
					 * 
					 * 	test eax, eax
					 */
					
					insn.insertBefore(new X86Instruction(
						asm,
						X86InstructionKinds.TEST,
						null, a, a
					));
					insn.remove();
					continue;
				}
				
			}
			
		}
		
		if(hasModified)
			optimize(asm);
	}
	
	private static boolean isZero(AssemblyTarget target) {
		return target instanceof X86IntegerTarget integer
			&& integer.getValue().compareTo(BigInteger.ZERO) == 0;
	}
	
}
