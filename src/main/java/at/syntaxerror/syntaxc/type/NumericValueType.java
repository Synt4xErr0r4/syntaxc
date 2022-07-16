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

import at.syntaxerror.syntaxc.misc.IEEE754Utils;
import at.syntaxerror.syntaxc.misc.IEEE754Utils.FloatingSpec;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public enum NumericValueType {

	SIGNED_CHAR		(true,	1, "signed char"),		// char, signed char
	UNSIGNED_CHAR	(false,	1, "unsigned char"),	// unsigned char
	
	SIGNED_SHORT	(true,	2, "short"),			// short, signed short, short int, signed short int
	UNSIGNED_SHORT	(false,	2, "unsigned short"),	// unsigned short, unsigned short int
	
	SIGNED_INT		(true,	4, "int"),				// int, signed, signed int
	UNSIGNED_INT	(false,	4, "unsigned int"),		// unsigned, unsigned int
	
	SIGNED_LONG		(true,	8, "long"),				// long, signed long, long int, signed long int
	UNSIGNED_LONG	(false,	8, "unsigned long"),	// unsigned long, unsigned long int
	
	FLOAT			(4,	FloatingSpec.SINGLE, "float"),		// float
	DOUBLE			(8,	FloatingSpec.DOUBLE, "double"),		// double
	LDOUBLE			(8,	FloatingSpec.DOUBLE, "long double");	// long double (equivalent to double by default)

	public static NumericValueType CHAR = SIGNED_CHAR;		// char			(equivalent to 'signed char' by default)
	public static NumericValueType POINTER = UNSIGNED_LONG;	// e.g. void *	(equivalent to 'unsigned long' by default)
	public static NumericValueType WCHAR = SIGNED_INT;		// wchar_t		(equivalent to 'signed int' by default)
	public static NumericValueType PTRDIFF = SIGNED_INT;		// ptrdiff_t	(equivalent to 'signed int' by default)
	public static NumericValueType SIZE = UNSIGNED_INT;		// size_t		(equivalent to 'unsigned int' by default)
	
	static {
		for(NumericValueType type : values())
			if(type.floating)
				type.asSigned = type.asUnsigned = type;
			
			else {
				String name = type.name();
				
				type.asSigned = name.startsWith("UNSIGNED")
					? valueOf(name.substring(2))
					: type;
				
				type.asUnsigned = name.startsWith("SIGNED")
					? valueOf("UN" + name)
					: type;
			}
	}
	
	private final boolean floating;
	private final boolean signed;
	
	private int size;
	
	private Number min;
	private Number max;
	
	private FloatingSpec floatingSpec;

	private BigInteger mask;
	
	private final String code;
	
	private NumericValueType asSigned;
	private NumericValueType asUnsigned;
	
	private NumericValueType(int size, FloatingSpec floatingSpec, String code) {
		floating = true;
		signed = false;
		
		this.size = size;
		this.floatingSpec = floatingSpec;
		this.code = code;
		
		max = IEEE754Utils.getMaxValue(floatingSpec);
		min = ((BigDecimal) max).negate();
		
		asSigned = asUnsigned = this;
	}
	
	private NumericValueType(boolean signed, int size, String code) {
		floating = false;
		
		this.signed = signed;
		this.size = size;
		
		resize();

		this.code = code;
	}
	
	public void inherit(NumericValueType type) {
		if(type.floating != floating)
			throw new IllegalArgumentException("Type " + this + " cannot inhert from " + (type.floating ? "floating" : "integer") + " type " + type);
		
		asSigned = type.asSigned;
		asUnsigned = type.asUnsigned;
		
		size = type.size;
		
		min = type.min;
		max = type.max;

		floatingSpec = type.floatingSpec;
	}
	
	public void modify(int size) {
		if(size < 1) throw new IllegalArgumentException("Illegal non-positive size for type " + this);
		if(floating) throw new IllegalArgumentException("Missing modified bounds for floating type " + this);
		
		if(size != this.size) {
			this.size = size;
			
			resize();
		}
	}
	
	public void modify(int size, FloatingSpec floatingSpec) {
		if(size < 1) throw new IllegalArgumentException("Illegal non-positive size for type " + this);
		if(!floating) throw new IllegalArgumentException("Cannot set floating bounds for integer type " + this);
		
		this.size = size;
		
		this.floatingSpec = floatingSpec;
		
		max = IEEE754Utils.getMaxValue(floatingSpec);
		min = ((BigDecimal) max).negate();
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
	
	public Type asType() {
		return NumberType.of(this);
	}
	
}
