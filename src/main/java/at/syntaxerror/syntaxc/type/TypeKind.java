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
package at.syntaxerror.syntaxc.type;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@AllArgsConstructor
@NoArgsConstructor
public enum TypeKind {

	VOID		("void"),
	CHAR,
	SHORT,
	INT,
	LONG,
	FLOAT,
	DOUBLE,
	LDOUBLE,
	ENUM,
	POINTER,
	FUNCTION,
	ARRAY,
	STRUCT,
	UNION,
	
	/* non-standard types */
	
	// __builtin_va_list
	VA_LIST		("__builtin_va_list"),
	
	// 128, 256, and 512-bit integers, currently unused
	M128("__m128"),
	M256("__m256"),
	M512("__m512");

	@Getter(AccessLevel.PROTECTED)
	private String rawName;
	
}
