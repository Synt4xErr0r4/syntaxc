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

import at.syntaxerror.syntaxc.generator.CodeGenerator;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86Assembly;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86AssemblyGenerator;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86RegisterProvider;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public class X86CodeGenerator extends CodeGenerator {

	private X86RegisterProvider registerProvider;
	private X86AssemblyGenerator assemblyGenerator;
	
	private X86FloatTable floatTable;
	private X86Assembly x86;
	
	public X86CodeGenerator(String inputFileName) {
		floatTable = new X86FloatTable();
		
		x86 = new X86Assembly(
			ArchitectureRegistry.getArchitecture()
				.getSyntax()
				.equals("intel"),
			ArchitectureRegistry.getBitSize()
		);
		
		assemblyGenerator = new X86AssemblyGenerator(floatTable, x86);
		registerProvider = new X86RegisterProvider();
	}
	
}
