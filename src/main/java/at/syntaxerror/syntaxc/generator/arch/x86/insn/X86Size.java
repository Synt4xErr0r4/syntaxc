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
package at.syntaxerror.syntaxc.generator.arch.x86.insn;

import at.syntaxerror.syntaxc.misc.config.Flags;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
@Getter
public enum X86Size {

	BYTE	("BYTE",	"b",	Type.CHAR),
	WORD	("WORD",	"w",	Type.SHORT),
	DWORD	("DWORD",	"l",	Type.INT),
	QWORD	("QWORD",	"q",	Type.LONG),
	TBYTE	("TBYTE",	"",		Type.LDOUBLE),
	XMMWORD	("XMMWORD",	"",		Type.VOID),
	UNKNOWN	("?WORD",	"?",	Type.VOID);

	public static X86Size ofRaw(Type type) {
		if(type.isArray())
			return of(NumericValueType.POINTER.getSize());
		
		return of(type.sizeof());
	}
	
	public static X86Size of(Type type) {
		if(!type.isScalar())
			return X86Size.UNKNOWN;

		return ofRaw(type);
	}
	
	public static X86Size of(int size) {
		if(Flags.LONG_DOUBLE.isEnabled() && size == NumericValueType.LDOUBLE.getSize())
			return TBYTE;
		
		return switch(size) {
		case 1 -> BYTE; // char
		case 2 -> WORD; // short
		case 4 -> DWORD; // float, int, pointer (x86)
		case 8 -> QWORD; // double, long, pointer (x64)
		default -> UNKNOWN;
		};
	}

	private final String pointerName;
	private final String suffix;
	private final Type type;
	
}
