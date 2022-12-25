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

import at.syntaxerror.syntaxc.type.StructType;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public abstract class Alignment {

	public abstract int getAlignment(Type type);
	
	public abstract StructAlignment getMemberAlignment(StructType struct, Type type, int offset, int bitOffset, int bitWidth, boolean wasBitfield);
	
	public static long alignAt(long offset, long alignment) {
		if(alignment < 2)
			return offset;
		
		long pad = offset % alignment;
		
		return pad == 0
			? offset
			: offset + alignment - pad;
	}
	
	public static record StructAlignment(int offset, int bitOffset) {
		
		public static final StructAlignment ZERO = of(0);
		
		public static StructAlignment of(int offset) {
			return new StructAlignment(offset, 0);
		}
		
		public static StructAlignment of(int offset, int bitOffset) {
			return new StructAlignment(offset, bitOffset);
		}
		
	}
	
}
