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
package at.syntaxerror.syntaxc.misc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import ch.obermuhlner.math.big.BigDecimalMath;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class IEEE754Utils {

	private static final BigDecimal TWO = BigDecimal.valueOf(2);

	private static MathContext CEIL = new MathContext(0, RoundingMode.CEILING);
	private static MathContext FLOOR = new MathContext(0, RoundingMode.FLOOR);
	
	private static final BigDecimal LOG10_2 = BigDecimalMath.log10(TWO, MathContext.DECIMAL128);
	
	private static void sanitizeBits(int exp, int mant) {
		if(exp < 1) throw new IllegalArgumentException("Illegal non-positive exponent size");
		if(mant < 1) throw new IllegalArgumentException("Illegal non-positive mantissa size");
 
		if(exp > 30) throw new IllegalArgumentException("Exponent size is too big");
		if(mant > 1024) throw new IllegalArgumentException("Mantissa size is too big");
	}
	
	/** 
	 * convert a BigDecimal into its IEEE754 binary representation,
	 * where {@code expBits} is the number of bits for the exponent
	 * and {@code mantBits} is the number of bits for the mantissa.
	 * {@code implicit} denotes whether there is an implicit/hidden bit 
	 */
	public static BigInteger decimalToFloat(BigDecimal value, int expBits, int mantBits, boolean implicit) {
		sanitizeBits(expBits, mantBits);
		
		if(value.compareTo(BigDecimal.ZERO) == 0)
			return BigInteger.ZERO;
		
		// determine sign bit
		int sign = value.signum() == -1 ? 1 : 0;
		
		// strip fractional part
		BigInteger mantissa = value.toBigInteger();
		
		// strip integer part
		BigDecimal fraction = value.subtract(new BigDecimal(mantissa, MathContext.UNLIMITED)).abs();
		
		// remove sign
		mantissa = mantissa.abs();
		
		// unbiased exponent, or 0 if exponent would be negative
		int exp = mantissa.bitLength();
		
		int n = 0;
		
		/* - left-shift the mantissa by 1
		 * - multiply the fraction by two
		 * - bit-or the mantissa with the integer part of result (0 or 1)
		 * - remove the integer part from the result
		 * - repeat until the fraction is zero, or all mantissa bits are used up
		 * 
		 * while(fraction != 0) {
		 * 	   mantissa <<= 1;
		 *     fraction *= 2;
		 *     mantissa |= (int) fraction;
		 *     fraction -= (int) fraction;
		 * }
		 */
		while(fraction.compareTo(BigDecimal.ZERO) != 0) {
			if(n >= exp + mantBits)
				break;
			
			++n;
			
			fraction = fraction.multiply(TWO);
			
			mantissa = mantissa.shiftLeft(1);
			
			if(fraction.intValue() == 1) {
				mantissa = mantissa.or(BigInteger.ONE);
				fraction = fraction.subtract(BigDecimal.ONE);
			}
		}
		
		// fix exponent (negative exponent)
		if(exp == 0)
			exp = mantissa.bitLength() - n - 1;
		else --exp;
		
		int off = 0;
		
		// clear most significant bit if it is implicit
		if(implicit)
			mantissa = mantissa.clearBit(mantissa.bitLength() - 1);
		else off = 1;
		
		return BigInteger.valueOf(sign) 
			// add bias to exponent
			.shiftLeft(expBits)
			.or(BigInteger.valueOf(exp + (1 << (expBits - 1)) - 1))
			// align mantissa to the left
			.shiftLeft(mantBits)
			.or(mantissa.shiftLeft(mantBits - n - exp - off));
	}

	// create a bit mask with n bits set (e.g. n=4 returns 0b1111)
	private static BigInteger mask(int n) {
		return BigInteger.ONE.shiftLeft(n).subtract(BigInteger.ONE);
	}

	// computes 2^n where n is < 0 [equivalent to 1/(2^(-n)) = 1/(1<<(-n))]
	private static BigDecimal pow2negative(int n) {
		return BigDecimal.ONE.divide(new BigDecimal(BigInteger.ONE.shiftLeft(n), MathContext.UNLIMITED));
	}
	
	public static BigDecimal floatToDecimal(BigInteger value, int expBits, int mantBits, boolean implicit) {
		sanitizeBits(expBits, mantBits);
		
		// extract sign (most significant bit)
		boolean sign = value.testBit(expBits + mantBits);
		
		// extract mantissa
		BigInteger mantissa = value.and(mask(mantBits));
		
		// extract (biased) exponent
		int exponent = value.shiftRight(mantBits).and(mask(expBits)).intValue();
		
		// calculate exponent bias
		int bias = ((1 << (expBits - 1)) - 1);
		
		boolean subnormal = false;
		
		// exponent is all 0s => signed zero, subnormals
		if(exponent == 0) {
			// mantissa == 0 => signed zero
			if(mantissa.compareTo(BigInteger.ZERO) == 0)
				return BigDecimal.ZERO; // BigDecimal doesn't have signed zeros
			
			// mantissa != 0 => subnormal
			exponent = 1 - bias;
			subnormal = true;
			
			// when a number is subnormal, the implicit bit is 0 instead of 1
		}
		
		// exponent is all 1s => infinity, NaN
		else if(exponent == mask(expBits).intValue())
			return null; // BigDecimal does have neither infinity nor NaN, so we just return null
		
		// make exponent unbiased
		else exponent -= bias;
		
		// integer part's offset. offset is decreased by 1 when most significant bit is implicit
		int off = mantBits - exponent - (implicit ? 0 : 1);
		
		// extract integer part from mantissa
		BigInteger integer = mantissa.shiftRight(off);
		
		BigDecimal result = null;
		
		if(implicit && !subnormal) { // add implicit bit, unless subnormal
			if(exponent < 0) // add 1/(2^(-exponent)) [effectively equal to 2^exponent]
				result = new BigDecimal(integer, MathContext.UNLIMITED)
					.add(pow2negative(-exponent));
			
			// add 2^exponent
			else integer = integer.or(BigInteger.ONE.shiftLeft(exponent));
		}
		
		if(result == null)
			result = new BigDecimal(integer, MathContext.UNLIMITED);

		for(int i = 0; i < off; ++i)
			if(mantissa.testBit(off - i - 1)) // if bit is set, add appropriate fraction
				result = result.add(pow2negative(i + 1));
		
		if(sign) // add sign
			result = result.negate();
		
		return result;
	}
	
	// returns the smallest postive value for the given specs
	public static BigDecimal getMinValue(int expBits, int mantBits, boolean implicit) {
		return floatToDecimal(
			BigInteger.ONE,
			expBits,
			mantBits,
			implicit
		);
	}

	// returns the largest value for the given specs
	public static BigDecimal getMaxValue(int expBits, int mantBits, boolean implicit) {
		return floatToDecimal(
			BigInteger.ZERO
				.shiftLeft(expBits)
				.or(mask(expBits - 1))
				.shiftLeft(mantBits + 1)
				.or(mask(mantBits)),
			expBits,
			mantBits,
			implicit
		);
	}
	
	// returns the delta between 1 and the smallest number greater than 1
	public static BigDecimal getEpsilon(int expBits, int mantBits, boolean implicit) {
		sanitizeBits(expBits, mantBits);
		
		BigInteger rawOne = BigInteger.ZERO
			.or(mask(expBits - 1))
			.shiftLeft(mantBits);
		
		if(!implicit)
			rawOne = rawOne.or(BigInteger.ONE.shiftLeft(mantBits - 1));
		
		// smallest number greater than 1
		BigDecimal one = floatToDecimal(
			rawOne.or(BigInteger.ONE),
			expBits,
			mantBits,
			implicit
		);
		
		return one.subtract(BigDecimal.ONE);
	}
	
	// returns the smallest and largest exponent
	public static Pair<Integer, Integer> getExponentRange(int expBits) {
		sanitizeBits(expBits, 1);
		
		int bias = ((1 << (expBits - 1)) - 1);
		
		return Pair.of(
			2 - bias,
			(1 << expBits) - 1 - bias
		);
	}
	
	// returns the smallest and largest exponent so that 10 to the power of the exponent is a normalized number
	public static Pair<Integer, Integer> get10ExponentRange(int expBits, int mantBits) {
		Pair<Integer, Integer> range = getExponentRange(expBits);
		
		// 2^(e_min - 1) 	[e_min < 0]
		BigDecimal min = pow2negative(1 - range.getFirst());
		
		// (1 - 2^-p) * 2^e_max 	[e_max > 0]
		BigDecimal max = BigDecimal.ONE.subtract(pow2negative(mantBits))
			.multiply(new BigDecimal(BigInteger.ONE.shiftLeft(range.getSecond()), MathContext.UNLIMITED));
		
		return Pair.of(
			BigDecimalMath.log10(min, MathContext.DECIMAL128).round(CEIL).intValue(), // log10 and round down
			BigDecimalMath.log10(max, MathContext.DECIMAL128).round(FLOOR).intValue() // log10 and round up
		);
	}
	
	// computes the number of decimal digits that can be converted back and forth without precision loss
	public static int getDecimalDigits(int mantBits) {
		// floor( (p - 1) * log10(b) )
		return BigDecimal.valueOf(mantBits - 1).multiply(LOG10_2).round(FLOOR).intValue();
	}
	
}
