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

import java.util.List;
import java.util.stream.Stream;

import at.syntaxerror.syntaxc.generator.asm.AssemblyGenerator;
import at.syntaxerror.syntaxc.tracking.Position;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Intermediate representation of function calls ('fun(a, b, c)')
 * 
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
@Getter
public class CallIntermediate extends Intermediate {

	private final Position position;

	private final Operand target;
	private final Operand function;
	private final List<Operand> arguments;

	@Override
	public void generate(AssemblyGenerator assemblyGenerator) {
		assemblyGenerator.call(
			assemblyGenerator.target(target),
			assemblyGenerator.target(function),
			arguments.stream()
				.map(assemblyGenerator::target)
				.toList()
		);
	}
	
	@Override
	public List<Operand> getOperands() {
		return Stream.concat(
			Stream.of(target, function),
			arguments.stream()
		).toList();
	}
	
	@Override
	public String toStringInternal() {
		return (target == null ? "" : target + " = ")
			+ "%s(%s);".formatted(
				function,
				arguments.stream()
					.map(Operand::toString)
					.reduce((a, b) -> a + ", " + b)
					.orElse("")
			);
	}
	
}
