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

import java.nio.ByteOrder;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.SystemUtils.OperatingSystem;
import at.syntaxerror.syntaxc.generator.CodeGenerator;
import at.syntaxerror.syntaxc.generator.arch.Architecture;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.Flag;
import at.syntaxerror.syntaxc.misc.IEEE754Utils.FloatingSpec;
import at.syntaxerror.syntaxc.preprocessor.macro.BuiltinMacro;
import at.syntaxerror.syntaxc.type.NumericValueType;

/**
 * @author Thomas Kasper
 * 
 */
public class X86Architecture extends Architecture {

	public X86Architecture() {
		super("x86", "i386");
	}
	
	@Override
	public CodeGenerator getCodeGenerator(String inputFileName) {
		return new X86CodeGenerator(inputFileName);
	}
	
	@Override
	public void onInit(OperatingSystem system, BitSize bitSize, ByteOrder endianness) {
		/* https://agner.org/optimize/calling_conventions.pdf
		 * § 18 Predefined macros
		 * 
		 * https://sourceforge.net/p/predef/wiki/Architectures/
		 * § AMD64
		 * § Intel x86
		 */
		
		if(endianness != ByteOrder.LITTLE_ENDIAN)
			Logger.error(
				"Unsupported endianness for x86 architecture: %s (only %s is supported)",
				endianness,
				ByteOrder.LITTLE_ENDIAN
			);
		
		if(Flag.LONG_DOUBLE.isEnabled())
			NumericValueType.LDOUBLE.modify(16, FloatingSpec.EXTENDED);
		
		if(Flag.UNSIGNED_CHAR.isEnabled())
			NumericValueType.CHAR = NumericValueType.UNSIGNED_CHAR;
		
		if(bitSize == BitSize.B64) {
			BuiltinMacro.define("__M_X64");
			BuiltinMacro.define("__M_AMD64");
			BuiltinMacro.define("__x86_64__");
			BuiltinMacro.define("__x86_64");
			BuiltinMacro.define("__amd64__");
			BuiltinMacro.define("__amd64");
			
			BuiltinMacro.define("_LP64");
			BuiltinMacro.define("__LP64__");
		}
		else if(bitSize == BitSize.B32) {
			BuiltinMacro.define("__M_IX86");
			BuiltinMacro.define("__i386__");
			BuiltinMacro.define("__i386");
			BuiltinMacro.define("i386");
			BuiltinMacro.define("__i486__");
			BuiltinMacro.define("__i586__");
			BuiltinMacro.define("__i686__");

			BuiltinMacro.define("_ILP32");
			BuiltinMacro.define("__ILP32__");
			
			NumericValueType.SIGNED_LONG.inherit(NumericValueType.SIGNED_INT);
			NumericValueType.UNSIGNED_LONG.inherit(NumericValueType.UNSIGNED_INT);
		}
		else Logger.error("Unsupported bit size for x86 architecture: %s (only 32 and 64 are supported)", bitSize);
		
		super.onInit(system, bitSize, endianness);
	}

}
