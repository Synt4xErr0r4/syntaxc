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
import java.util.function.Function;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.SystemUtils.OperatingSystem;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.preprocessor.macro.BuiltinMacro;
import lombok.Getter;
import lombok.NonNull;

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
