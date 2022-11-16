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
package at.syntaxerror.syntaxc.intermediate.representation;

import at.syntaxerror.syntaxc.generator.asm.AssemblyGenerator;
import at.syntaxerror.syntaxc.tracking.Position;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Intermediate representation of member accessors ('a.b')
 * 
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
@Getter
public class MemberIntermediate extends Intermediate {

	private final Position position;

	private final Operand result;
	private final Operand struct;
	private final Operand member;
	
	private final int bitOffset;
	private final int bitWidth;

	@Override
	public void generate(AssemblyGenerator assemblyGenerator) {
		assemblyGenerator.member(
			assemblyGenerator.target(result),
			assemblyGenerator.target(struct),
			((LocalOperand) member).getOffset(),
			bitOffset,
			bitWidth
		);
	}
	
	@Override
	public String toStringInternal() {
		return "%s = %s.%s;".formatted(result, struct, member);
	}
	
}
