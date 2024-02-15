/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.type;

import at.syntaxerror.ieee754.FloatingFactory;
import at.syntaxerror.ieee754.binary.Binary;
import at.syntaxerror.ieee754.binary.BinaryCodec;
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
public final class BinaryFloatType<T extends Binary<T>> extends FloatType<T> {

	public static <T extends Binary<T>> BinaryFloatType<T> of(@NonNull FloatingFactory<T> factory, @NonNull BinaryCodec<T> codec, @NonNull NumberClass numberClass) {
		if(!numberClass.isBinary())
			throw new UnsupportedOperationException("Expected binary number class");
		
		return new BinaryFloatType<T>(factory, codec, numberClass, FloatCategory.REAL);
	}
	
	private final FloatingFactory<T> factory;
	private final BinaryCodec<T> codec;
	private final NumberClass numberClass;
	private final FloatCategory category;
	
	@Override
	public int sizeof() {
		int bits = 1 + codec.getExponentBits() + codec.getSignificandBits() + (codec.isImplicit() ? 0 : 1);
		bits = Math.ceilDiv(bits, 8);
		
		if(category == FloatCategory.COMPLEX)
			bits *= 2;
		
		return bits;
	}
	
	public BinaryFloatType<T> asImaginary() {
		if(isDecimal())
			throw new UnsupportedOperationException("Imaginary decimal floating-point types are not supported");

		if(category == FloatCategory.IMAGINARY)
			return this;
		
		return clone(FloatCategory.IMAGINARY);
	}
	
	public BinaryFloatType<T> asComplex() {
		if(isDecimal())
			throw new UnsupportedOperationException("Complex decimal floating-point types are not supported");
		
		if(category == FloatCategory.COMPLEX)
			return this;
		
		return clone(FloatCategory.COMPLEX);
	}

	public BinaryFloatType<T> asReal() {
		if(category == FloatCategory.REAL)
			return this;
		
		return clone(FloatCategory.REAL);
	}
	
	@Override
	protected String toStringPrefix() {
		return numberClass.toString() + category.suffix;
	}
	
	@Override
	protected String toStringSuffix() {
		return "";
	}

	@Override
	protected BinaryFloatType<T> clone() {
		return clone(category);
	}
	
	private BinaryFloatType<T> clone(FloatCategory category) {
		return new BinaryFloatType<>(factory, codec, numberClass, category);
	}
	
	@RequiredArgsConstructor
	public static enum FloatCategory {
		
		REAL		(""),
		COMPLEX		(" _Complex"),
		IMAGINARY	(" _Imaginary");
		
		private final String suffix;
		
	}
	
}
