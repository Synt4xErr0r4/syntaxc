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
 * This class represents an array type, e.g. {@code int[42]}
 * 
 * @author Thomas Kasper
 * 
 */
@Getter
public class ArrayType extends PointerLikeType {

	/**
	 * Length value indicating that the length is not known
	 */
	public static final int SIZE_UNKNOWN = -1;
	
	private int length;
	
	/**
	 * Constructs a new array type of the given length for the given base type
	 * 
	 * @param base the base type
	 * @param length the length, or {@link #SIZE_UNKNOWN}
	 */
	protected ArrayType(Type base, int length) {
		super(TypeKind.ARRAY, base);
		
		setLength(length);
		
		size = base.size * length;
	}
	
	@Override
	public boolean isIncomplete() {
		return length == SIZE_UNKNOWN;
	}
	
	/**
	 * Sets the length of the array.
	 * If {@link length} is less than {@code 0},
	 * it is assumed that the length is unknown
	 * 
	 * @param length the length of the array
	 */
	public void setLength(int length) {
		if(length < 0)
			length = SIZE_UNKNOWN;
		
		this.length = length;
		
		size = getBase().size * length;
	}
	
	@Override
	protected Type clone() {
		return inheritProperties(new ArrayType(getBase(), length));
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
