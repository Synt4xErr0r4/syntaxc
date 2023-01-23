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
import java.util.List;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.SystemUtils.OperatingSystem;
import at.syntaxerror.syntaxc.builtin.BuiltinRegistry;
import at.syntaxerror.syntaxc.generator.CodeGenerator;
import at.syntaxerror.syntaxc.generator.arch.Alignment;
import at.syntaxerror.syntaxc.generator.arch.Architecture;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.IEEE754Utils.FloatingSpec;
import at.syntaxerror.syntaxc.misc.config.Flags;
import at.syntaxerror.syntaxc.preprocessor.macro.BuiltinMacro;
import at.syntaxerror.syntaxc.type.NumericValueType;

/**
 * @author Thomas Kasper
 * 
 */
public class X86Architecture extends Architecture {

	private final X86Alignment alignment = new X86Alignment();
	
	public X86Architecture() {
		super("x86", "i386");
	}
	
	@Override
	public List<String> getSyntaxes() {
		return List.of("intel", "att");
	}
	
	@Override
	public CodeGenerator getCodeGenerator(String inputFileName) {
		return new X86CodeGenerator(inputFileName, this);
	}
	
	@Override
	public Alignment getAlignment() {
		return alignment;
	}
	
	@Override
	public void onInit(OperatingSystem system, BitSize bitSize, ByteOrder endianness) {
		/* https://agner.org/optimize/calling_conventions.pdf
		 * § 18 Predefined macros
		 * 
		 * https://sourceforge.net/p/predef/wiki/Architectures/
		 * § AMD64
		 * § Intel x86
		 * 
		 * https://www.uclibc.org/docs/psABI-x86_64.pdf
		 * § 3.1.2 Data Representation
		 * 
		 * http://www.sco.com/developers/devspecs/abi386-4.pdf
		 * § 3 Low-Level System Information, Machine Interface, Data Representation
		 */
		
		if(endianness != ByteOrder.LITTLE_ENDIAN)
			Logger.error(
				"Unsupported endianness for x86 architecture: %s (only %s is supported)",
				endianness,
				ByteOrder.LITTLE_ENDIAN
			);
		
		if(Flags.UNSIGNED_CHAR.isEnabled())
			NumericValueType.CHAR = NumericValueType.UNSIGNED_CHAR;

		int longDoubleBits = 0;
		
		alignment.setBitsSize(bitSize);
		
		if(bitSize == BitSize.B64) {

			checkSystemCompatability(OperatingSystem.LINUX, OperatingSystem.WINDOWS);
			
			longDoubleBits = 16;
			
			BuiltinMacro.define("__M_X64");
			BuiltinMacro.define("__M_AMD64");
			BuiltinMacro.define("__x86_64__");
			BuiltinMacro.define("__x86_64");
			BuiltinMacro.define("__amd64__");
			BuiltinMacro.define("__amd64");
			
			BuiltinMacro.define("_LP64");
			BuiltinMacro.define("__LP64__");
			
			/* https://learn.microsoft.com/en-us/cpp/c-language/c-bit-fields?view=msvc-170
			 * http://www.sco.com/developers/devspecs/abi386-4.pdf
			 * 
			 *  'int' bitfields are signed on Windows, unsigned on Linux
			 */
			
			ArchitectureRegistry.setUnsignedBitfields(
				ArchitectureRegistry.getOperatingSystem() == OperatingSystem.LINUX
			);
			
			/*
			 * https://www.uclibc.org/docs/psABI-x86_64.pdf
			 * Figure 3.34
			 * 
			 * __builtin_va_list is effectively equal to the following definition:
			 * 
			 *  typedef struct {
			 *  	unsigned int gp_offset;
			 *  	unsigned int fp_offset;
			 *  	void *overflow_arg_area;
			 *  	void *reg_save_area;
			 *  } __builtin_va_list[1];
			 *  
			 *  reg_save_area		- pointer to the register save area
			 *  overflow_arg_area	- pointer to the first stack argument
			 *  gp_offset			- offset from reg_save_area, where next general purpose register is stored.
			 *  					  when all register are exhausted, its value is set to 48
			 *  fp_offset			- offset from reg_save_area, where next floating point register is stored.
			 *  					  when all register are exhausted, its value is set to 304
			 */
			BuiltinRegistry.vaListSize = 24;
		}
		else if(bitSize == BitSize.B32) {
			
			checkSystemCompatability(OperatingSystem.LINUX);

			longDoubleBits = 12;
			
			BuiltinMacro.define("__M_IX86");
			BuiltinMacro.define("__i386__");
			BuiltinMacro.define("__i386");
			BuiltinMacro.define("i386");
			BuiltinMacro.define("__i486__");
			BuiltinMacro.define("__i586__");
			BuiltinMacro.define("__i686__");

			BuiltinMacro.define("_ILP32");
			BuiltinMacro.define("__ILP32__");
			
			// longs only have 4 bytes on 32-bit machines
			NumericValueType.SIGNED_LONG.inherit(NumericValueType.SIGNED_INT);
			NumericValueType.UNSIGNED_LONG.inherit(NumericValueType.UNSIGNED_INT);

			/* http://www.sco.com/developers/devspecs/abi386-4.pdf
			 *  'int' bitfields are unsigned on Linux */
			ArchitectureRegistry.setUnsignedBitfields(true);
			
			// __builtin_va_list consists of one pointer, holding the stack offset
			BuiltinRegistry.vaListSize = 4;
		}
		else Logger.error("Unsupported bit size for x86 architecture: %s (only 32 and 64 are supported)", bitSize);

		if(Flags.LONG_DOUBLE.isEnabled())
			NumericValueType.LDOUBLE.modify(longDoubleBits, FloatingSpec.EXTENDED);
		
		super.onInit(system, bitSize, endianness);
	}
	
	private static void checkSystemCompatability(OperatingSystem...supported) {
		OperatingSystem target = ArchitectureRegistry.getOperatingSystem();
		
		for(OperatingSystem os : supported)
			if(os == target)
				return;

		Logger.error("Unsupported operating system for x86 architecture: %s", target);
	}

}
