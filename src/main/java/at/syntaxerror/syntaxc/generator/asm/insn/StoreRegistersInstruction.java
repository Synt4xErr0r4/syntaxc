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
package at.syntaxerror.syntaxc.generator.asm.insn;

import java.util.List;

import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.generator.asm.target.RegisterTarget;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
public class StoreRegistersInstruction extends AssemblyInstruction {

	private static int previousId = 0;
	
	@Getter
	private final int id;
	
	private final RestoreRegistersInstruction restore;
	
	public StoreRegistersInstruction(Instructions parent, RegisterTarget...targets) {
		super(parent, StoreRegistersInstructionKind.INSTANCE, List.of(), List.of(targets));
		
		id = previousId++;
		restore = new RestoreRegistersInstruction(id, parent, targets);
	}
	
	public RestoreRegistersInstruction restore() {
		return restore;
	}
	
	@Override
	public String toString() {
		return "\t; STORE { "
			+ getSources()
				.stream()
				.map(AssemblyTarget::toString)
				.reduce((a, b) -> a + ", " + b)
				.orElse("")
			+ " }";
	}
	
	public static enum StoreRegistersInstructionKind implements AssemblyInstructionKind {
		
		INSTANCE;
		
	}
	
}
