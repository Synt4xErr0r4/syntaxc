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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86Assembly;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86AssemblyTarget;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86LabelTarget;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.insn.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.insn.AssemblyInstructionKind;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public class X86Instruction extends AssemblyInstruction {

	public X86Instruction(Instructions asm, AssemblyInstructionKind kind, AssemblyTarget destination, AssemblyTarget... sources) {
		super(
			asm,
			kind,
			destination == null
				? List.of()
				: List.of(destination),
			Arrays.asList(sources)
				.stream()
				.filter(t -> t != null)
				.toList()
		);
	}
	
	public String toAssemblyString(boolean att) {
		if(getKind() == X86InstructionKinds.CLOBBER)
			return "\t; CLOBBER { "
				+ Stream.concat(
					getDestinations().stream(),
					getSources().stream()
				)
					.map(AssemblyTarget::toString)
					.reduce((a, b) -> a + ", " + b)
					.orElse("")
				+ " }";
		
		if(getKind() == X86InstructionKinds.LABEL)
			return getDestinations().get(0) + ":";
		
		String strrep = "\t" + getKind();

		boolean reverse = false;
		boolean asterisk = false;
		
		if(att && getKind() instanceof X86InstructionKinds kind) {
			
			// indirect procedure call: 'call *eax'
			asterisk = getKind() == X86InstructionKinds.CALL && !(getDestinations().get(0) instanceof X86LabelTarget);
			
			if(kind.isTakesSuffix())
				strrep += X86InstructionSelector.getSuffix(
					Stream.concat(
						getDestinations().stream(),
						getSources().stream()
					)
						.findFirst()
						.map(AssemblyTarget::getType)
						.orElse(Type.VOID)
				);
			
			else if(kind.isX87())
				strrep += X86InstructionSelector.getX87Suffix(
					Stream.concat(
						getDestinations().stream(),
						getSources().stream()
					)
						.filter(target -> !(target instanceof X86Register reg) || !X86Register.GROUP_ST.contains(reg))
						.map(AssemblyTarget::getType)
						.findFirst()
						.orElse(Type.VOID)
				);
			
			if(kind == X86InstructionKinds.FDIVP)
				strrep = "\t" + X86InstructionKinds.FDIVRP;
			
			reverse = true;
		}
		
		String srcStr = toCommaSeparated(getSources(), att, reverse);
		String dstStr = toCommaSeparated(getDestinations(), att, reverse);
		
		if(asterisk)
			dstStr = "*" + dstStr;
		
		if(reverse) {
			String tmp = srcStr;
			srcStr = dstStr;
			dstStr = tmp;
		}
		
		if(!dstStr.isBlank() || !srcStr.isBlank())
			strrep += " " + dstStr + (dstStr.isBlank() || srcStr.isBlank() ? "" : ", ") + srcStr;
		
		return strrep;
	}
	
	@Override
	public String toString() {
		return toAssemblyString(!X86Assembly.INSTANCE.intelSyntax);
	}
	
	private static String toCommaSeparated(List<AssemblyTarget> targets, boolean att, boolean reverse) {
		return targets.stream()
			.filter(target -> target != null)
			.map(target -> toString(target, att))
			.reduce(
				reverse
					? (a, b) -> b + ", " + a
					: (a, b) -> a + ", " + b
			)
			.orElse("");
	}

	private static String toString(AssemblyTarget target, boolean att) {
		if(target == null)
			return null;
		
		if(target instanceof X86AssemblyTarget x86)
			return x86.toAssemblyString(att);

		if(target instanceof X86Register reg)
			return reg.toAssemblyString(att);
		
		return target.toString();
	}
	
}
