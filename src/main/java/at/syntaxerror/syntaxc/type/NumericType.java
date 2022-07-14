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

import java.math.BigDecimal;
import java.math.BigInteger;

import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public enum NumericType {

	SIGNED_CHAR		(true,	1), // char, signed char
	UNSIGNED_CHAR	(false,	1), // unsigned char
	
	SIGNED_SHORT	(true,	2), // short, signed short, short int, signed short int
	UNSIGNED_SHORT	(false,	2), // unsigned short, unsigned short int
	
	SIGNED_INT		(true,	4), // int, signed, signed int
	UNSIGNED_INT	(false,	4), // unsigned, unsigned int
	
	SIGNED_LONG		(true,	8), // long, signed long, long int, signed long int
	UNSIGNED_LONG	(false,	8), // unsigned long, unsigned long int
	
	POINTER			(UNSIGNED_LONG), // e.g. void*
	
	FLOAT			(4,	Float.MAX_VALUE), // float
	DOUBLE			(8,	Double.MAX_VALUE), // double
	LDOUBLE			(DOUBLE); // long double (equivalent to double by default)

	public static NumericType CHAR = SIGNED_CHAR; // char (equivalent to signed char by default)
	public static NumericType WCHAR = SIGNED_INT; // wchar_t (equivalent to signed int by default)
	
	private final boolean floating;
	private final boolean signed;
	
	private int size;
	
	private Number min;
	private Number max;

	private BigInteger mask;
	
	private NumericType(int size, double max) {
		floating = true;
		signed = false;
		
		this.size = size;
		this.min = BigDecimal.valueOf(-max);
		this.max = BigDecimal.valueOf(+max);
	}
	
	private NumericType(boolean signed, int size) {
		floating = false;
		
		this.signed = signed;
		this.size = size;
		
		resize();
	}
	
	private NumericType(NumericType type) {
		floating = type.floating;
		signed = type.signed;
		
		inhert(type);
	}
	
	public void inhert(NumericType type) {
		if(type.floating != floating)
			throw new IllegalArgumentException("Type " + this + " cannot inhert from " + (type.floating ? "floating" : "integer") + " type " + type);

		size = type.size;
		
		min = type.min;
		max = type.max;
	}
	
	public void modify(int size) {
		if(size < 1) throw new IllegalArgumentException("Illegal non-positive size for type " + this);
		if(floating) throw new IllegalArgumentException("Missing modified bounds for floating type " + this);
		
		if(size != this.size) {
			this.size = size;
			
			resize();
		}
	}
	
	public void modify(int size, BigDecimal min, BigDecimal max) {
		if(size < 1) throw new IllegalArgumentException("Illegal non-positive size for type " + this);
		if(!floating) throw new IllegalArgumentException("Cannot set floating bounds for integer type " + this);
		
		this.size = size;
		this.min = min;
		this.max = max;
	}
	
	private void resize() {
		// bitmask; (2^(8 * size))-1
		mask = BigInteger.TWO.pow(8 * size).subtract(BigInteger.ONE);
		
		if(signed) { // signed; min = -(2^(8 * size-1)), max = (2^(8 * size-1))-1
			BigInteger bound = BigInteger.TWO.pow(8 * size - 1);
			
			min = bound.negate();
			max = bound.subtract(BigInteger.ONE);
		}
		else { // unsigned; min = 0, max = (2^(8 * size))-1
			min = BigInteger.ZERO;
			max = mask;
		}
	}
	
	public boolean inUnsignedRange(BigInteger integer) {
		if(floating) throw new IllegalArgumentException("Cannot compare to floating type " + this);
		
		return integer.compareTo(BigInteger.ZERO) >= 0
			&& integer.compareTo(mask) <= 0;
	}

	public boolean inUnsignedRange(Number num) {
		return inUnsignedRange(BigInteger.valueOf(num.longValue()));
	}
	
	public boolean inRange(BigInteger integer) {
		if(floating)
			return inRange(new BigDecimal(integer));
		
		return integer.compareTo((BigInteger) min) >= 0
			&& integer.compareTo((BigInteger) max) <= 0;
	}
	
	public boolean inRange(BigDecimal floating) {
		if(!this.floating)
			return inRange(floating.toBigInteger());
		
		return floating.compareTo((BigDecimal) min) >= 0
			&& floating.compareTo((BigDecimal) max) <= 0;
	}
	
	public boolean inRange(Number num) {
		if(floating)
			return inRange(BigDecimal.valueOf(num.doubleValue()));
		else return inRange(BigInteger.valueOf(num.longValue()));
	}
	
	public BigInteger mask(BigInteger value) {
		if(floating) throw new IllegalArgumentException("Cannot convert integer to floating type " + this);
		
		if(inRange(value))
			return value;
		
		value = value.and(mask);
		
		if(signed && value.testBit(size - 1))
			value = value.not().add(BigInteger.ONE).and(mask);
		
		return value;
	}
	
	public long maskUnsigned(long value) {
		if(floating) throw new IllegalArgumentException("Cannot convert integer to floating type " + this);
		if(size > 8) throw new IllegalArgumentException("Long cannot hold integer type " + this);
		
		// effectively equal to Java's long
		if(size == 8) return value;
		
		return BigInteger.valueOf(value).and(mask).longValue();
	}
	
}
