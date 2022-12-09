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
	UNKNOWN	(null,		null,	Type.VOID);

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
		return switch(size) {
		case 1 -> X86Size.BYTE; // char
		case 2 -> X86Size.WORD; // short
		case 4 -> X86Size.DWORD; // float, int, pointer (x86)
		case 8 -> X86Size.QWORD; // double, long, pointer (x64)
		case 16 -> X86Size.TBYTE; // long double
		default -> X86Size.UNKNOWN;
		};
	}

	private final String pointerName;
	private final String suffix;
	private final Type type;
	
}
