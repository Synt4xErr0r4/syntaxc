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

import java.util.Arrays;
import java.util.List;

import at.syntaxerror.syntaxc.generator.asm.AssemblyGenerator;
import at.syntaxerror.syntaxc.tracking.Position;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Intermediate representation of subtractions ('a-b')
 * 
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
@Getter
public class SubtractIntermediate extends Intermediate {

	private final Position position;

	private final Operand result;
	private final Operand left;
	private final Operand right;

	@Override
	public void generate(AssemblyGenerator assemblyGenerator) {
		assemblyGenerator.subtract(
			assemblyGenerator.target(result),
			assemblyGenerator.target(left),
			assemblyGenerator.target(right)
		);
	}
	
	@Override
	public List<Operand> getOperands() {
		return Arrays.asList(result, left, right);
	}
	
	@Override
	public String toStringInternal() {
		return "%s = %s - %s;".formatted(result, left, right);
	}
	
}
