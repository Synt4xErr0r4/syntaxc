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

import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public class ArrayType extends PointerLikeType {

	public static final int SIZE_UNKNOWN = -1;
	
	private int length;
	
	protected ArrayType(Type base, int length) {
		super(TypeKind.ARRAY, base);
		
		setLength(length);
		
		size = base.size * length;
	}
	
	@Override
	public boolean isIncomplete() {
		return length == SIZE_UNKNOWN;
	}
	
	public void setLength(int length) {
		if(length < 0)
			length = SIZE_UNKNOWN;
		
		this.length = length;
	}
	
	@Override
	protected Type clone() {
		return new ArrayType(getBase(), length);
	}
	
	@Override
	public String toStringPrefix() {
		return getBase().toStringPrefix() + " (";
	}
	
	@Override
	protected String toStringSuffix() {
		return ")["
			+ (length == SIZE_UNKNOWN ? "" : Integer.toString(length))
			+ "]" + getBase().toStringSuffix();
	}
	
	@Override
	public String toString() {
		String prefix = toStringPrefix();
		String suffix = toStringSuffix();
		
		return prefix.substring(0, prefix.length() - 2)
			+ suffix.substring(1);
	}

}
