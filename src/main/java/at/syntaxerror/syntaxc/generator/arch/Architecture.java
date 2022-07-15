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
import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.IEEE754Utils;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.preprocessor.macro.BuiltinMacro;
import at.syntaxerror.syntaxc.type.NumericType;
import lombok.Getter;
import lombok.NonNull;

import static at.syntaxerror.syntaxc.preprocessor.macro.BuiltinMacro.defineNumber;

/**
 * @author Thomas Kasper
 * 
 */
public class Architecture {

	@Getter
	private final String[] names;

	public Architecture(@NonNull String name, String...more) {
		names = new String[1 + more.length];
		names[0] = name;
		System.arraycopy(more, 0, names, 1, more.length);
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
		
		initFloat();
		initLimits();
		initStddef();
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

		List<Pair<NumericType, String>> floatings = List.of(
			Pair.of(NumericType.FLOAT,		"__FLT_"),
			Pair.of(NumericType.DOUBLE,		"__DBL_"),
			Pair.of(NumericType.LDOUBLE,	"__LDBL_")
		);
		
		for(var floating : floatings) {
			NumericType type = floating.getFirst();
			String name = floating.getSecond();
			
			var exp = IEEE754Utils.getExponentRange(type.getExponent());
			var exp10 = IEEE754Utils.get10ExponentRange(type.getExponent(), type.getMantissa());
			
			defineNumber(name + "MANT_DIG__",	type.getMantissa() + (type.isImplicitBit() ? 1 : 0));
			defineNumber(name + "DIG__",		IEEE754Utils.getDecimalDigits(type.getMantissa()));
			defineNumber(name + "MIN_EXP__",	exp.getFirst());
			defineNumber(name + "MIN_10_EXP__",	exp10.getFirst());
			defineNumber(name + "MAX_EXP__",	exp.getSecond());
			defineNumber(name + "MAX_10_EXP__",	exp10.getSecond());
			defineNumber(name + "MAX__",		IEEE754Utils.getMaxValue(type.getExponent(), type.getMantissa(), type.isImplicitBit()), type);
			defineNumber(name + "EPSILON__",	IEEE754Utils.getEpsilon(type.getExponent(), type.getMantissa(), type.isImplicitBit()), type);
			defineNumber(name + "MIN__",		IEEE754Utils.getMinValue(type.getExponent(), type.getMantissa(), type.isImplicitBit()), type);
		}
	}
	
	protected void initLimits() {
		defineNumber("__CHAR_BIT__",	NumericType.CHAR.getSize() * 8);
		defineNumber("__MB_LEN_MAX__",	16); // https://www.man7.org/linux/man-pages/man3/MB_LEN_MAX.3.html

		List<Pair<NumericType, String>> integers = List.of(
			Pair.of(NumericType.SIGNED_CHAR,	"__SCHAR_"),
			Pair.of(NumericType.UNSIGNED_CHAR,	"__UCHAR_"),
			Pair.of(NumericType.SIGNED_SHORT,	"__SHRT_"),
			Pair.of(NumericType.UNSIGNED_SHORT,	"__USHRT_"),
			Pair.of(NumericType.SIGNED_INT,		"__INT_"),
			Pair.of(NumericType.UNSIGNED_INT,	"__UINT_"),
			Pair.of(NumericType.SIGNED_LONG,	"__LONG_"),
			Pair.of(NumericType.UNSIGNED_LONG,	"__ULONG_")
		);
		
		for(var integer : integers) {
			NumericType type = integer.getFirst();
			String name = integer.getSecond();
			
			if(type.isSigned())
				defineNumber(name + "MIN__", type.getMin(), type);
			
			defineNumber(name + "MAX__", type.getMax(), type);
		}
		
		defineNumber("__CHAR_MIN__",	NumericType.CHAR.getMin(),	NumericType.CHAR);
		defineNumber("__CHAR_MAX__",	NumericType.CHAR.getMax(),	NumericType.CHAR);
	}
	
	protected void initStddef() {
		defineType("__PTRDIFF_TYPE__",	NumericType.PTRDIFF.getAsSigned().getCode());
		defineType("__SIZE_TYPE__",		NumericType.SIZE.getAsUnsigned().getCode());
		defineType("__WCHAR_TYPE__",	NumericType.WCHAR.getCode());
	}
	
	public static Architecture unsupported(String name) {
		return new Architecture(name) {
			
			@Override
			public void onInit(OperatingSystem system, BitSize bitSize, ByteOrder endianness) {
				Logger.error("Unsupported architecture %s", name);
			}
			
		};
	}
	
}
