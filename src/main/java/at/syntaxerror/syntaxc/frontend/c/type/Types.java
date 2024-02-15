/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.type;

import java.util.HashMap;
import java.util.Map;

import at.syntaxerror.ieee754.binary.Binary32;
import at.syntaxerror.ieee754.binary.Binary64;
import at.syntaxerror.ieee754.decimal.Decimal128;
import at.syntaxerror.ieee754.decimal.Decimal32;
import at.syntaxerror.ieee754.decimal.Decimal64;
import at.syntaxerror.syntaxc.frontend.c.type.NumberType.NumberClass;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

/**
 * 
 *
 * @author Thomas Kasper
 */
@UtilityClass
public class Types {

	private static final Map<Integer, IntType> BITINTS = new HashMap<>();
	
	public static CharSign charSign = CharSign.UNDEFINED;
	
	public static final VoidType VOID = new VoidType();
	
	public static final IntType BOOL = IntType.of(1, NumberClass.BOOL);
	
	public static IntType UCHAR = IntType.of(8, NumberClass.UCHAR);
	public static IntType SCHAR = IntType.of(8, NumberClass.SCHAR);
	public static IntType CHAR = IntType.of(8, NumberClass.CHAR);

	public static IntType USHORT = IntType.of(16, NumberClass.USHORT);
	public static IntType SHORT = IntType.of(16, NumberClass.SHORT);

	public static IntType UINT = IntType.of(16, NumberClass.UINT);
	public static IntType INT = IntType.of(16, NumberClass.INT);

	public static IntType ULONG = IntType.of(32, NumberClass.ULONG);
	public static IntType LONG = IntType.of(32, NumberClass.LONG);

	public static IntType ULLONG = IntType.of(64, NumberClass.ULLONG);
	public static IntType LLONG = IntType.of(64, NumberClass.LLONG);

	public static IntType WCHAR = (IntType) INT.alias("wchar_t");
	public static IntType CHAR8 = (IntType) UCHAR.alias("char8_t");
	public static IntType CHAR16 = (IntType) USHORT.alias("char16_t");
	public static IntType CHAR32 = (IntType) ULONG.alias("char32_t");
	
	public static final BinaryFloatType<?> FLOAT = BinaryFloatType.of(Binary32.FACTORY, Binary32.CODEC, NumberClass.FLOAT);
	public static final BinaryFloatType<?> DOUBLE = BinaryFloatType.of(Binary64.FACTORY, Binary64.CODEC, NumberClass.DOUBLE);
	public static BinaryFloatType<?> LDOUBLE = BinaryFloatType.of(Binary64.FACTORY, Binary64.CODEC, NumberClass.LDOUBLE);

	public static final DecimalFloatType<?> DECIMAL32 = DecimalFloatType.of(Decimal32.FACTORY, Decimal32.CODEC, NumberClass.DECIMAL32);
	public static final DecimalFloatType<?> DECIMAL64 = DecimalFloatType.of(Decimal64.FACTORY, Decimal64.CODEC, NumberClass.DECIMAL64);
	public static final DecimalFloatType<?> DECIMAL128 = DecimalFloatType.of(Decimal128.FACTORY, Decimal128.CODEC, NumberClass.DECIMAL128);
	
	public static IntType ofBitInt(int bits) {
		return BITINTS.computeIfAbsent(-bits, x -> IntType.of(bits, NumberClass.BITINT));
	}

	public static IntType ofBitUint(int bits) {
		return BITINTS.computeIfAbsent(bits, x -> IntType.of(bits, NumberClass.UBITINT));
	}
	
	public static enum CharSign {
		UNDEFINED,
		SIGNED,
		UNSIGNED
	}
	
	@RequiredArgsConstructor
	public static enum DataModel {
		LP32(16, 32),
		ILP32(32, 32),
		LLP64(32, 32),
		LP64(32, 64);
		
		private final int intBits, longBits;
		
		public void apply() {
			UINT = IntType.of(intBits, NumberClass.UINT);
			INT = IntType.of(intBits, NumberClass.INT);

			ULONG = IntType.of(longBits, NumberClass.ULONG);
			LONG = IntType.of(longBits, NumberClass.LONG);
			
			if(intBits >= 32)
				CHAR32 = (IntType) UINT.alias("char32_t");
		}
		
	}
	
}
