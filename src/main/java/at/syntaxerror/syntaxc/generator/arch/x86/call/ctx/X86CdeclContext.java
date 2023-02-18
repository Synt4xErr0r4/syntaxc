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
package at.syntaxerror.syntaxc.generator.arch.x86.call.ctx;

import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Instruction;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.type.FunctionType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * @author Thomas Kasper
 * 
 */
@FieldDefaults(level = AccessLevel.PUBLIC)
public class X86CdeclContext {

	long structPointerOffset;
	long allocatedStackSpace;
	long alignmentOffset;
	FunctionType callee;
	Type returnType;
	AssemblyTarget returnValue;
	X86Instruction stackSetup;
	
	int argN;
	int paramCount;
	
}
