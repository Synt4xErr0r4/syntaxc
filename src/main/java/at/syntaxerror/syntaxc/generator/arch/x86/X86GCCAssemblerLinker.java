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

import java.io.File;
import java.io.IOException;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.generator.AssemblerLinker;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.logger.Logger;

/**
 * @author Thomas Kasper
 * 
 */
public class X86GCCAssemblerLinker extends AssemblerLinker {

	private void invokeGCC(File input, String output, String... additionalOptions) throws Exception {
		
		String[] options;
		int additional = additionalOptions.length;
		
		if(output == null)
			options = new String[3 + additional];
		
		else {
			options = new String[5 + additional];
			options[3] = "-o";
			options[4] = new File(output).getAbsolutePath();
		}
		
		options[0] = "gcc";
		options[1] = input.getAbsolutePath();
		options[2] = ArchitectureRegistry.getBitSize() == BitSize.B32 ? "-m32" : "-m64";
		
		System.arraycopy(additionalOptions, 0, options, options.length - additional, additional);
		
		Process proc = new ProcessBuilder(options)
			.inheritIO()
			.start();
		
		int exitCode = proc.onExit().get().exitValue();
		
		if(exitCode != 0)
			throw new IOException("GCC failed with exit code " + exitCode);
	}
	
	@Override
	public void assemble(File input, String output) {
		try {
			invokeGCC(input, output, "-c");
		} catch (Exception e) {
			Logger.error("Failed to assemble file: %s", e.getMessage());
		}
	}
	
	@Override
	public void assembleAndLink(File input, String output) {
		try {
			invokeGCC(input, output);
		} catch (Exception e) {
			Logger.error("Failed to assemble and link file: %s", e.getMessage());
		}
	}
	
}
