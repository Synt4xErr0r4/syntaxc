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

import static at.syntaxerror.syntaxc.preprocessor.macro.BuiltinMacro.defineNumber;

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

/**
 * @author Thomas Kasper
 * 
 */
public abstract class Architecture {

	@Getter
	private final String[] names;
	
	private String syntax;
	
	private boolean isWindows;
	
	public Architecture(@NonNull String name, String...more) {
		names = new String[1 + more.length];
		names[0] = name;
		System.arraycopy(more, 0, names, 1, more.length);
		
		var syntaxes = getSyntaxes();
		
		syntax = syntaxes.isEmpty() ? null : syntaxes.get(0);
	}
	
	public abstract CodeGenerator getCodeGenerator(String inputFileName);
	public abstract Alignment getAlignment();
	
	public List<String> getSyntaxes() {
		return List.of();
	}
	
	public String getSyntax() {
		return syntax;
	}
	
	public boolean setSyntax(String syntax) {
		if(!getSyntaxes().contains(syntax))
			return false;
		
		this.syntax = syntax;
		return true;
	}
	
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
			isWindows = true;
			
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
		initTime();		// <time.h>
		initLocale();	// <locale.h>
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
		defineNumber("__EXIT_FAILURE",	1);
		defineNumber("__EXIT_SUCCESS",	0);
		defineType("__DIV_TYPE__",		"struct { int quot ; int rem ; }");
		defineType("__LDIV_TYPE__",		"struct { long quot ; long rem ; }");
		
		if(isWindows) {
			defineType("__MB_CUR_MAX",		"___mb_cur_max_func ( )");
			defineNumber("__RAND_MAX",		0x7fff);
		}
		
		else {
			defineType("__MB_CUR_MAX",		"__ctype_get_mb_cur_max ( )");
			defineNumber("__RAND_MAX",		0x7fffffff);
		}
	}
	
	protected void initStdio() {
		defineNumber("__EOF",			-1);
		defineNumber("__SEEK_CUR",		1);
		defineNumber("__SEEK_END",		2);
		defineNumber("__SEEK_SET",		0);
		
		if(isWindows) {
			defineType("__FILE_TYPE__",		"struct { void * _Placeholder ; }");
			defineType("__stderr",			"__acrt_iob_func ( 2 )");
			defineType("__stdin",			"__acrt_iob_func ( 0 )");
			defineType("__stdout",			"__acrt_iob_func ( 1 )");
			defineType("__TMP_MAX",			"__INT_MAX__");
			defineNumber("__L_tmpnam",		260);
			defineNumber("__FILENAME_MAX",	260);
			defineNumber("__FOPEN_MAX",		20);
			defineNumber("__BUFSIZ",		512);
			defineNumber("___IOFBF",		0x0000);
			defineNumber("___IOLBF",		0x0040);
			defineNumber("___IONBF",		0x0004);
			defineType("__FPOS_TYPE__",		"long");
		}
		else {
			defineType("__FILE_TYPE__",		"struct _IO_FILE");
			defineType("__stderr",			"stderr");
			defineType("__stdin",			"stdin");
			defineType("__stdout",			"stdout");
			defineNumber("__TMP_MAX",		238328);
			defineNumber("__L_tmpnam",		20);
			defineNumber("__FILENAME_MAX",	4096);
			defineNumber("__FOPEN_MAX",		16);
			defineNumber("__BUFSIZ",		8192);
			defineNumber("___IOFBF",		0);
			defineNumber("___IOLBF",		1);
			defineNumber("___IONBF",		2);
			defineType("__FPOS_TYPE__",		"struct _G_fpos_t { long __pos ; struct { int __count ; union { unsigned int __wch ; char __wchb [ 4 ] ; } __value ; } __state ; }");
		}
	}
	
	protected void initSetjmp() {
		defineType("__JMP_BUF_TYPE__",	"struct __jmp_buf_tag { long __jmpbuf [ 8 ] ; int __mask_was_saved ; struct { unsigned long __val [ 32 ] ; } __saved_mask ; }");
	}
	
	protected void initSignal() {
		defineNumber("__SIG_DFL",	0);
		defineNumber("__SIG_ERR",	-1);
		defineNumber("__SIG_IGN",	1);
		
		defineNumber("__SIGFPE",	8);
		defineNumber("__SIGILL",	4);
		defineNumber("__SIGINT",	2);
		defineNumber("__SIGSEGV",	11);
		defineNumber("__SIGTERM",	15);
		
		if(isWindows)
			defineNumber("__SIGABRT",	22);
		else defineNumber("__SIGABRT",	6);
		
		defineType("__SIG_ATOMIC_TYPE__", "int");
	}
	
	protected void initMath() {
		defineNumber("__HUGE_VAL__", NumericValueType.DOUBLE.getMax(), NumericValueType.DOUBLE);
	}
	
	protected void initTime() {
		defineType("__CLOCK_TYPE__",		"long");
		defineType("__TIME_TYPE__",			"long");
		
		if(isWindows)
			defineNumber("__CLOCKS_PER_SEC",	1000, NumericValueType.SIGNED_LONG);
		
		else defineNumber("__CLOCKS_PER_SEC",	1000000, NumericValueType.SIGNED_LONG);
	}
	
	protected void initLocale() {
		defineNumber("__LC_ALL",		0);
		defineNumber("__LC_COLLATE",	1);
		defineNumber("__LC_CTYPE",		2);
		defineNumber("__LC_MONETARY",	3);
		defineNumber("__LC_NUMERIC",	4);
		defineNumber("__LC_TIME",		5);
	}
	
	public static Architecture unsupported(String name) {
		return new Architecture(name) {
			
			@Override
			public CodeGenerator getCodeGenerator(String inputFileName) {
				return null;
			}
			
			@Override
			public Alignment getAlignment() {
				return null;
			}
			
			@Override
			public void onInit(OperatingSystem system, BitSize bitSize, ByteOrder endianness) {
				Logger.error("Unsupported architecture %s", name);
			}
			
		};
	}
	
}
