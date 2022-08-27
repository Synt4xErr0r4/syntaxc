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
package at.syntaxerror.syntaxc.generator.arch;

import java.nio.ByteOrder;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.SystemUtils.OperatingSystem;
import at.syntaxerror.syntaxc.generator.CodeGenerator;
import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.IEEE754Utils;
import at.syntaxerror.syntaxc.misc.IEEE754Utils.FloatingSpec;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.preprocessor.macro.BuiltinMacro;
import at.syntaxerror.syntaxc.type.NumericValueType;
import lombok.Getter;
import lombok.NonNull;

import static at.syntaxerror.syntaxc.preprocessor.macro.BuiltinMacro.defineNumber;

/**
 * @author Thomas Kasper
 * 
 */
public abstract class Architecture {

	@Getter
	private final String[] names;

	public Architecture(@NonNull String name, String...more) {
		names = new String[1 + more.length];
		names[0] = name;
		System.arraycopy(more, 0, names, 1, more.length);
	}
	
	public abstract CodeGenerator getCodeGenerator(String inputFileName);
	
	public final void onInit() {
		onInit(
			ArchitectureRegistry.getOperatingSystem(),
			ArchitectureRegistry.getBitSize(),
			ArchitectureRegistry.getEndianness()
		);
	}
	
	public void onInit(OperatingSystem system, BitSize bitSize, ByteOrder endianness) {
		/* https://agner.org/optimize/calling_conventions.pdf
		 * § 18 Predefined macros
		 * 
		 * https://sourceforge.net/p/predef/wiki/OperatingSystems/
		 * 
		 * https://sourceforge.net/p/predef/wiki/Endianness/
		 */
		
		String byteOrderName;
		
		if(endianness == ByteOrder.BIG_ENDIAN) {
			byteOrderName = "__ORDER_BIG_ENDIAN__";
			BuiltinMacro.define("__BIG_ENDIAN__");
		}
		else {
			byteOrderName = "__ORDER_LITTLE_ENDIAN__";
			BuiltinMacro.define("__LITTLE_ENDIAN__");
		}
		
		Function<Token, Token> byteOrder = self -> Token.ofIdentifier(self.getPosition(), byteOrderName);

		BuiltinMacro.defineToken("__BYTE_ORDER__", byteOrder, true);
		BuiltinMacro.defineToken("__FLOAT_WORD_ORDER__", byteOrder, true);
		
		switch(system) {
		case LINUX:
			BuiltinMacro.define("__unix__");
			BuiltinMacro.define("__linux__");
			BuiltinMacro.define("__linux");
			BuiltinMacro.define("linux");
			
			if(bitSize == BitSize.B64) {
				BuiltinMacro.define("__LP64__");
				BuiltinMacro.define("__amd64");
			}
			break;
		
		case WINDOWS:
			switch(bitSize) {
			case B16:
				BuiltinMacro.define("_WIN16");
				break;
			
			case B32:
				BuiltinMacro.define("_WIN32");
				BuiltinMacro.define("__WINDOWS__");
				break;
				
			case B64:
				BuiltinMacro.define("_WIN32");
				BuiltinMacro.define("_WIN64");
				break;
				
			default:
				break;
			}
			break;
		
		case MAC:
			BuiltinMacro.define("__APPLE__");
			BuiltinMacro.define("__DARWIN__");
			BuiltinMacro.define("__MACH__");
			break;
			
		case AIX:
			BuiltinMacro.define("_AIX");
			break;
		
		case ANDROID:
			BuiltinMacro.define("__ANDROID__");
			break;
			
		case FREEBSD:
			BuiltinMacro.define("__FreeBSD__");
			break;

		case KFREEBSD:
			BuiltinMacro.define("__FreeBSD__");
			BuiltinMacro.define("__GLIBC__");
			break;
			
		case NETBSD:
			BuiltinMacro.define("__NetBSD__");
			break;
			
		case OPENBSD:
			BuiltinMacro.define("__OpenBSD__");
			break;
			
		case GNU:
			BuiltinMacro.define("__GNU__");
			BuiltinMacro.define("__gnu_hurd__");
			break;
			
		case SOLARIS:
			BuiltinMacro.define("sun");
			BuiltinMacro.define("__sun");
			break;
			
		default:
			Logger.error("Unsupported operating system »%s«", System.getProperty("os.name"));
			break;
		}
		
		initFloat();	// <float.h>
		initLimits();	// <limits.h>
		initStddef();	// <stddef.h>
		initStdlib();	// <stdlib.h>
		initStdio();	// <stdio.h>
		initSetjmp();	// <setjmp.h>
		initSignal();	// <signal.h>
		initMath();		// <math.h>
	}
	
	private static Token asToken(Token pos, String part) {
		Punctuator punct = Punctuator.of(part);
		
		if(punct != null)
			return Token.ofPunctuator(pos.getPosition(), punct);
		
		return Token.ofIdentifier(pos.getPosition(), part);
	}
	
	protected static final void defineType(String name, String type) {
		String[] parts = type.split(" ");
		
		BuiltinMacro.defineList(
			name,
			self -> Stream
				.of(parts)
				.map(part -> asToken(self, part))
				.toList(),
			true
		);
	}

	protected void initFloat() {
		/* § 5.2.4.2.2 Characteristics of floating types <float.h>
		 * 
		 * IEEE 754
		 */
		
		defineNumber("__FLT_RADIX__",	2); // binary
		defineNumber("__FLT_ROUNDS__",	1); // round to nearest

		List<Pair<NumericValueType, String>> floatings = List.of(
			Pair.of(NumericValueType.FLOAT,		"__FLT_"),
			Pair.of(NumericValueType.DOUBLE,	"__DBL_"),
			Pair.of(NumericValueType.LDOUBLE,	"__LDBL_")
		);
		
		for(var floating : floatings) {
			NumericValueType type = floating.getLeft();
			String name = floating.getRight();
			
			FloatingSpec spec = type.getFloatingSpec();
			
			var exp = IEEE754Utils.getExponentRange(spec);
			var exp10 = IEEE754Utils.get10ExponentRange(spec);
			
			defineNumber(name + "MANT_DIG__",	spec.mantissa() + (spec.implicit() ? 1 : 0));
			defineNumber(name + "DIG__",		IEEE754Utils.getDecimalDigits(spec));
			defineNumber(name + "MIN_EXP__",	exp.getLeft());
			defineNumber(name + "MIN_10_EXP__",	exp10.getLeft());
			defineNumber(name + "MAX_EXP__",	exp.getRight());
			defineNumber(name + "MAX_10_EXP__",	exp10.getRight());
			defineNumber(name + "MAX__",		IEEE754Utils.getMaxValue(spec), type);
			defineNumber(name + "EPSILON__",	IEEE754Utils.getEpsilon(spec), type);
			defineNumber(name + "MIN__",		IEEE754Utils.getMinValue(spec), type);
		}
	}
	
	protected void initLimits() {
		defineNumber("__CHAR_BIT__",	NumericValueType.CHAR.getSize() * 8);
		defineNumber("__MB_LEN_MAX__",	16); // https://www.man7.org/linux/man-pages/man3/MB_LEN_MAX.3.html

		List<Pair<NumericValueType, String>> integers = List.of(
			Pair.of(NumericValueType.SIGNED_CHAR,		"__SCHAR_"),
			Pair.of(NumericValueType.UNSIGNED_CHAR,		"__UCHAR_"),
			Pair.of(NumericValueType.SIGNED_SHORT,		"__SHRT_"),
			Pair.of(NumericValueType.UNSIGNED_SHORT,	"__USHRT_"),
			Pair.of(NumericValueType.SIGNED_INT,		"__INT_"),
			Pair.of(NumericValueType.UNSIGNED_INT,		"__UINT_"),
			Pair.of(NumericValueType.SIGNED_LONG,		"__LONG_"),
			Pair.of(NumericValueType.UNSIGNED_LONG,		"__ULONG_")
		);
		
		for(var integer : integers) {
			NumericValueType type = integer.getLeft();
			String name = integer.getRight();
			
			if(type.isSigned())
				defineNumber(name + "MIN__", type.getMin(), type);
			
			defineNumber(name + "MAX__", type.getMax(), type);
		}
		
		defineNumber("__CHAR_MIN__",	NumericValueType.CHAR.getMin(),	NumericValueType.CHAR);
		defineNumber("__CHAR_MAX__",	NumericValueType.CHAR.getMax(),	NumericValueType.CHAR);
	}
	
	protected void initStddef() {
		defineType("__PTRDIFF_TYPE__",	NumericValueType.PTRDIFF.getAsSigned().getCode());
		defineType("__SIZE_TYPE__",		NumericValueType.SIZE.getAsUnsigned().getCode());
		defineType("__WCHAR_TYPE__",	NumericValueType.WCHAR.getCode());
	}
	
	protected void initStdlib() {
		defineType("__DIV_TYPE__",		"");
		defineType("__LDIV_TYPE__",		"");
	}
	
	protected void initStdio() {
		defineType("__FILE_TYPE__",		"");
		defineType("__FPOS_TYPE__",		"");
		defineType("___IOFBF",			"");
		defineType("___IOLBF",			"");
		defineType("___IONBF",			"");
		defineType("__BUFSIZ",			"");
		defineType("__EOF",				"");
		defineType("__FILENAME_MAX",	"");
		defineType("__FOPEN_MAX",		"");
		defineType("__L_tmpnam__",		"");
		defineType("__SEEK_CUR",		"");
		defineType("__SEEK_END",		"");
		defineType("__SEEK_SET",		"");
		defineType("__TMP_MAX",			"");
		defineType("__stderr",			"");
		defineType("__stdin",			"");
		defineType("__stdout",			"");
	}
	
	protected void initSetjmp() {
		defineType("__JMP_BUF_TYPE__",	"struct __jmp_buf_tag");
	}
	
	protected void initSignal() {
		defineType("__JMP_BUF_TYPE__",	"");
	}
	
	protected void initMath() {
		defineNumber("__HUGE_VAL__",	NumericValueType.DOUBLE.getMax(), NumericValueType.DOUBLE);
	}
	
	public static Architecture unsupported(String name) {
		return new Architecture(name) {
			
			@Override
			public CodeGenerator getCodeGenerator(String inputFileName) {
				return null;
			}
			
			@Override
			public void onInit(OperatingSystem system, BitSize bitSize, ByteOrder endianness) {
				Logger.error("Unsupported architecture %s", name);
			}
			
		};
	}
	
}
