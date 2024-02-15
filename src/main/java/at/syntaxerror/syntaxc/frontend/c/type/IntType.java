/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.type;

import java.math.BigInteger;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

/**
 * 
 *
 * @author Thomas Kasper
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IntType extends NumberType {

	public static IntType of(int bits, @NonNull NumberClass numberClass) {
		if(!numberClass.isInteger() && !numberClass.isBool())
			throw new UnsupportedOperationException("Expected integer number class");
		
		return new IntType(bits, numberClass);
	}

	private final int bits;
	private final NumberClass numberClass;
	
	private final BigInteger minValue;
	private final BigInteger maxValue;
	
	public IntType(int bits, @NonNull NumberClass numberClass) {
		this.bits = bits;
		this.numberClass = numberClass;
		
		if(numberClass.isUnsigned()) {
			maxValue = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE);
			minValue = BigInteger.ZERO;
		}
		else {
			BigInteger max = BigInteger.ONE.shiftLeft(bits - 1);
			
			maxValue = max.subtract(BigInteger.ONE);
			minValue = max.negate();
		}
	}
	
	@Override
	public int sizeof() {
		return Math.ceilDiv(bits, 8);
	}
	
	@Override
	protected String toStringPrefix() {
		if(isAnyBitInt())
			return numberClass + "(" + bits + ")";
		
		return numberClass.toString();
	}
	
	@Override
	protected String toStringSuffix() {
		return "";
	}

	@Override
	protected Type clone() {
		return new IntType(bits, numberClass, minValue, maxValue);
	}

}
