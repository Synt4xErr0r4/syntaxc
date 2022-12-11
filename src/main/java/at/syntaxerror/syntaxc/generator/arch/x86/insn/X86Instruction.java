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
package at.syntaxerror.syntaxc.generator.arch.x86.insn;

import java.util.List;
import java.util.stream.Stream;

import at.syntaxerror.syntaxc.generator.arch.x86.target.X86AssemblyTarget;
import at.syntaxerror.syntaxc.generator.asm.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.AssemblyInstructionKind;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;

/**
 * @author Thomas Kasper
 * 
 */
public class X86Instruction extends AssemblyInstruction {

	public X86Instruction(Instructions asm, AssemblyInstructionKind kind, AssemblyTarget destination, AssemblyTarget... sources) {
		super(asm, kind, destination, List.of(sources));
	}
	
	public String toAssemblyString(boolean att) {
		if(getKind() == X86InstructionKinds.CLOBBER)
			return "; CLOBBER { "
				+ Stream.concat(
					Stream.of(getDestination()),
					getSources().stream()
				)
					.map(AssemblyTarget::toString)
					.reduce((a, b) -> a + ", " + b)
					.orElse("")
				+ " }";
		
		if(att) {
			// TODO
			
			return "?";
		}
		
		String strrep = getKind().toString();
		
		AssemblyTarget dest = getDestination();
		
		if(dest != null) {
			strrep += " " + toString(dest, att);
			
			var src = getSources();
			
			if(!src.isEmpty())
				strrep += ", "
					+ src.stream()
						.map(target -> toString(target, att))
						.reduce((a, b) -> a + ", " + b)
						.orElse("");
		}
		
		return strrep;
	}
	
	@Override
	public String toString() {
		return toAssemblyString(false);
	}

	private static String toString(AssemblyTarget target, boolean att) {
		if(target == null)
			return null;
		
		if(target instanceof X86AssemblyTarget x86)
			return x86.toAssemblyString(att);
		
		return target.toString();
	}
	
}
