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
	
	private static void sanitizeBits(FloatingSpec spec) {
		if(spec.exponent() < 1) throw new IllegalArgumentException("Illegal non-positive exponent size");
		if(spec.mantissa() < 1) throw new IllegalArgumentException("Illegal non-positive mantissa size");
 
		if(spec.exponent() > 30) throw new IllegalArgumentException("Exponent size is too big");
		if(spec.mantissa() > 512) throw new IllegalArgumentException("Mantissa size is too big");
	}
	
	/** 
	 * convert a BigDecimal into its IEEE754 binary representation,
	 * where {@code expBits} is the number of bits for the exponent
	 * and {@code mantBits} is the number of bits for the mantissa.
	 * {@code implicit} denotes whether there is an implicit/hidden bit 
	 */
	public static BigInteger decimalToFloat(BigDecimal value, FloatingSpec spec) {
		sanitizeBits(spec);
		
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
			if(n >= exp + spec.mantissa())
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
		if(spec.implicit())
			mantissa = mantissa.clearBit(mantissa.bitLength() - 1);
		else off = 1;
		
		return BigInteger.valueOf(sign) 
			// add bias to exponent
			.shiftLeft(spec.exponent())
			.or(BigInteger.valueOf(exp + getBias(spec)))
			// align mantissa to the left
			.shiftLeft(spec.mantissa())
			.or(mantissa.shiftLeft(spec.mantissa() - n - exp - off));
	}
	
	// create a bit mask with n bits set (e.g. n=4 returns 0b1111)
	private static BigInteger mask(int n) {
		return BigInteger.ONE.shiftLeft(n).subtract(BigInteger.ONE);
	}

	// computes 2^n where n is < 0 [equivalent to 1/(2^(-n)) = 1/(1<<(-n))]
	private static BigDecimal pow2negative(int n) {
		return BigDecimal.ONE.divide(new BigDecimal(BigInteger.ONE.shiftLeft(n), MathContext.UNLIMITED));
	}
	
	public static BigDecimal floatToDecimal(BigInteger value, FloatingSpec spec) {
		sanitizeBits(spec);
		
		// extract sign (most significant bit)
		boolean sign = isNegative(value, spec);
		
		// extract mantissa
		BigInteger mantissa = getMantissa(value, spec);
		
		// extract (biased) exponent
		int exponent = getExponent(value, spec).intValue();
		
		// calculate exponent bias
		int bias = getBias(spec);
		
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
		else if(exponent == mask(spec.exponent()).intValue())
			return null; // BigDecimal does have neither infinity nor NaN, so we just return null
		
		// make exponent unbiased
		else exponent -= bias;
		
		// integer part's offset. offset is decreased by 1 when most significant bit is implicit
		int off = spec.mantissa() - exponent - (spec.implicit() ? 0 : 1);
		
		// extract integer part from mantissa
		BigInteger integer = mantissa.shiftRight(off);
		
		BigDecimal result = null;
		
		if(spec.implicit() && !subnormal) { // add implicit bit, unless subnormal
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

	public static int getBias(FloatingSpec spec) {
		return (1 << (spec.exponent() - 1)) - 1;
	}

	public static BigInteger getUnbiasedExponent(BigInteger value, FloatingSpec spec) {
		return getExponent(value, spec).subtract(BigInteger.valueOf(getBias(spec)));
	}

	public static BigInteger getExponent(BigInteger value, FloatingSpec spec) {
		return value.shiftRight(spec.mantissa()).and(mask(spec.exponent()));
	}

	public static BigInteger getMantissa(BigInteger value, FloatingSpec spec) {
		return value.and(mask(spec.mantissa()));
	}
	
	public static boolean isNegative(BigInteger value, FloatingSpec spec) {
		return value.testBit(spec.exponent() + spec.mantissa());
	}
	
	public static boolean isInfinity(BigInteger value, FloatingSpec spec) {
		int exponent = getExponent(value, spec).intValue();
		
		if(exponent != mask(spec.exponent()).intValue())
			return false;

		return getMantissa(value, spec).compareTo(BigInteger.ZERO) == 0;
	}
	
	public static boolean isPositiveInfinity(BigInteger value, FloatingSpec spec) {
		return !isNegative(value, spec) && isInfinity(value, spec);
	}
	
	public static boolean isNegativeInfinity(BigInteger value, FloatingSpec spec) {
		return isNegative(value, spec) && isInfinity(value, spec);
	}
	
	// returns positive infinity (like Double#POSITIVE_INFINITY)
	public static BigInteger getPositiveInfinity(FloatingSpec spec) {
		return BigInteger.ZERO
			.shiftLeft(spec.exponent())
			.or(mask(spec.exponent()))
			.shiftLeft(spec.mantissa());
	}

	// returns positive infinity (like Double#NEGATIVE_INFINITY)
	public static BigInteger getNegativeInfinity(FloatingSpec spec) {
		return BigInteger.ONE
			.shiftLeft(spec.exponent())
			.or(mask(spec.exponent()))
			.shiftLeft(spec.mantissa());
	}
	
	public static boolean isNaN(BigInteger value, FloatingSpec spec) {
		int exponent = getExponent(value, spec).intValue();
		
		if(exponent != mask(spec.exponent()).intValue())
			return false;
		
		return getMantissa(value, spec).compareTo(BigInteger.ZERO) != 0;
	}
	
	public static boolean isQuietNaN(BigInteger value, FloatingSpec spec) {
		return isNaN(value, spec)
			&& getMantissa(value, spec).testBit(spec.mantissa() - 1);
	}
	
	public static boolean isSignalingNaN(BigInteger value, FloatingSpec spec) {
		return isNaN(value, spec)
			&& !getMantissa(value, spec).testBit(spec.mantissa() - 1);
	}
	
	// returns qNaN (on most processors)
	public static BigInteger getQuietNaN(FloatingSpec spec) {
		return BigInteger.ZERO
			.shiftLeft(spec.exponent())
			.or(mask(spec.exponent()))
			.shiftLeft(1)
			.or(BigInteger.ONE)
			.shiftLeft(spec.mantissa() - 1)
			.or(BigInteger.ONE);
	}

	// returns sNaN (on most processors)
	public static BigInteger getSignalingNaN(FloatingSpec spec) {
		return BigInteger.ZERO
			.shiftLeft(spec.exponent())
			.or(mask(spec.exponent()))
			.shiftLeft(spec.mantissa())
			.or(BigInteger.ONE);
	}

	// returns NaN
	public static BigInteger getNaN(FloatingSpec spec) {
		return BigInteger.ZERO
			.shiftLeft(spec.exponent())
			.or(mask(spec.exponent()))
			.shiftLeft(spec.mantissa())
			.or(mask(spec.mantissa()));
	}
	
	// returns the smallest postive value for the given specs
	public static BigDecimal getMinValue(FloatingSpec spec) {
		return floatToDecimal(
			BigInteger.ONE
				.shiftLeft(spec.mantissa()),
			spec
		);
	}

	// returns the largest value for the given specs
	public static BigDecimal getMaxValue(FloatingSpec spec) {
		return floatToDecimal(
			BigInteger.ZERO
				.shiftLeft(spec.exponent())
				.or(mask(spec.exponent() - 1))
				.shiftLeft(spec.mantissa() + 1)
				.or(mask(spec.mantissa())),
			spec
		);
	}
	
	// returns the delta between 1 and the smallest number greater than 1
	public static BigDecimal getEpsilon(FloatingSpec spec) {
		sanitizeBits(spec);
		
		BigInteger rawOne = BigInteger.ZERO
			.or(mask(spec.exponent() - 1))
			.shiftLeft(spec.mantissa());
		
		if(!spec.implicit())
			rawOne = rawOne.or(BigInteger.ONE.shiftLeft(spec.mantissa() - 1));
		
		// smallest number greater than 1
		BigDecimal one = floatToDecimal(
			rawOne.or(BigInteger.ONE),
			spec
		);
		
		return one.subtract(BigDecimal.ONE);
	}
	
	// returns the smallest and largest exponent
	public static Pair<Integer, Integer> getExponentRange(FloatingSpec spec) {
		sanitizeBits(spec);
		
		int bias = getBias(spec);
		
		return Pair.of(
			2 - bias,
			(1 << spec.exponent()) - 1 - bias
		);
	}
	
	// returns the smallest and largest exponent so that 10 to the power of the exponent is a normalized number
	public static Pair<Integer, Integer> get10ExponentRange(FloatingSpec spec) {
		Pair<Integer, Integer> range = getExponentRange(spec);
		
		// 2^(e_min - 1) 	[e_min < 0]
		BigDecimal min = pow2negative(1 - range.getFirst());
		
		// (1 - 2^-p) * 2^e_max 	[e_max > 0]
		BigDecimal max = BigDecimal.ONE.subtract(pow2negative(spec.mantissa()))
			.multiply(new BigDecimal(BigInteger.ONE.shiftLeft(range.getSecond()), MathContext.UNLIMITED));
		
		return Pair.of(
			BigDecimalMath.log10(min, MathContext.DECIMAL128).round(CEIL).intValue(), // log10 and round down
			BigDecimalMath.log10(max, MathContext.DECIMAL128).round(FLOOR).intValue() // log10 and round up
		);
	}
	
	// computes the number of decimal digits that can be converted back and forth without precision loss
	public static int getDecimalDigits(FloatingSpec spec) {
		// floor( (p - 1) * log10(b) )
		return BigDecimal.valueOf(spec.mantissa() - 1).multiply(LOG10_2).round(FLOOR).intValue();
	}
	
	public static record FloatingSpec(int exponent, int mantissa, boolean implicit) {
		
		public static final FloatingSpec HALF =			new FloatingSpec(5,		10,		true); // 16-bit
		public static final FloatingSpec SINGLE =		new FloatingSpec(8,		23,		true); // float, 32-bit
		public static final FloatingSpec DOUBLE =		new FloatingSpec(11,	52,		true); // double, 64-bit
		public static final FloatingSpec QUADRUPLE =	new FloatingSpec(15,	112,	true); // 128-bit
		public static final FloatingSpec OCTUPLE =		new FloatingSpec(19,	236,	true); // 256-bit
		public static final FloatingSpec EXTENDED =		new FloatingSpec(15,	64,		false); // x86 extended precision, 80-bit
		
	}
	
}
