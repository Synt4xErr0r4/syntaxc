/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.type;

import at.syntaxerror.ieee754.FloatingFactory;
import at.syntaxerror.ieee754.decimal.Decimal;
import at.syntaxerror.ieee754.decimal.DecimalCodec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * 
 *
 * @author Thomas Kasper
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class DecimalFloatType<T extends Decimal<T>> extends FloatType<T> {

	public static <T extends Decimal<T>> DecimalFloatType<T> of(@NonNull FloatingFactory<T> factory, @NonNull DecimalCodec<T> codec, @NonNull NumberClass numberClass) {
		if(!numberClass.isDecimal())
			throw new UnsupportedOperationException("Expected decimal number class");
		
		return new DecimalFloatType<>(factory, codec, numberClass);
	}

	private final FloatingFactory<T> factory;
	private final DecimalCodec<T> codec;
	private final NumberClass numberClass;
	
	@Override
	public int sizeof() {
		int bits = 1 + codec.getCombinationBits() + codec.getSignificandBits();
		return Math.ceilDiv(bits, 8);
	}
	
	@Override
	protected String toStringPrefix() {
		return numberClass.toString();
	}
	
	@Override
	protected String toStringSuffix() {
		return "";
	}

	@Override
	protected DecimalFloatType<T> clone() {
		return new DecimalFloatType<>(factory, codec, numberClass);
	}
	
}
