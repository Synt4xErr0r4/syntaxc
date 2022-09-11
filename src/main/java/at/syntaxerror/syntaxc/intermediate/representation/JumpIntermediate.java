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

import java.util.Optional;

import at.syntaxerror.syntaxc.generator.asm.AssemblyGenerator;
import at.syntaxerror.syntaxc.tracking.Position;
import lombok.Getter;

/**
 * Intermediate representation of (un-)conditional jumps
 * 
 * <p>unconditional jumps are equivalent to {@code goto label;}
 * <p>conditional jumps are equivalent to {@code if(!condition) goto label;}
 * 
 * @author Thomas Kasper
 * 
 */
@Getter
public class JumpIntermediate extends Intermediate {

	private final Position position;

	private final Optional<Operand> condition;
	private final String label;
	
	public JumpIntermediate(Position position, String label) {
		this(position, Optional.empty(), label);
	}
	
	public JumpIntermediate(Position position, Operand condition, String label) {
		this(position, Optional.of(condition), label);
	}
	
	public JumpIntermediate(Position position, Optional<Operand> condition, String label) {
		this.position = position;
		this.condition = condition;
		this.label = label;
	}
	
	public boolean isConditional() {
		return condition.isPresent();
	}

	@Override
	public void generate(AssemblyGenerator assemblyGenerator) {
		if(isConditional())
			assemblyGenerator.jump(
				assemblyGenerator.target(condition.get()),
				label
			);
		
		else assemblyGenerator.jump(label);
	}
	
	@Override
	public String toString() {
		return toString("<undefined>");
	}
	
	public String toString(String labelTrue) {
		if(condition.isPresent())
			return "if(%s)\n     goto %s;\nelse\n     goto %s;".formatted(
				condition.get(),
				labelTrue,
				label
			);
		
		return "goto %s;".formatted(label);
	}
	
}
