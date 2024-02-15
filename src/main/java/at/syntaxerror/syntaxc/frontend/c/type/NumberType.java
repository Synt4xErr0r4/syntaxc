/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.type;

import at.syntaxerror.syntaxc.frontend.c.CStandard;
import at.syntaxerror.syntaxc.frontend.c.type.NumberType.NumberClass;
import at.syntaxerror.syntaxc.frontend.c.type.Types.CharSign;
import lombok.RequiredArgsConstructor;

/**
 * 
 *
 * @author Thomas Kasper
 */
public abstract class NumberType extends Type implements NumberClassHolder {

	@RequiredArgsConstructor
	public static enum NumberClass implements NumberClassHolder {
		
		BOOL		(null),
		
		UCHAR		("unsigned char"),
		SCHAR		("signed char"),
		CHAR		("char"),
		SHORT		("short int"),
		USHORT		("short unsigned int"),
		INT			("int"),
		UINT		("unsigned int"),
		LONG		("long int"),
		ULONG		("long unsigned int"),
		LLONG		("long long int"),
		ULLONG		("long long unsigned int"),
		
		FLOAT		("float"),
		DOUBLE		("double"),
		LDOUBLE		("long double"),
		
		DECIMAL32	("_Decimal32"),
		DECIMAL64	("_Decimal64"),
		DECIMAL128	("_Decimal128"),
		
		BITINT		("_BitInt"),
		UBITINT		("unsigned _BitInt");
		
		private final String name;

		@Override
		public NumberClass getNumberClass() {
			return this;
		}
		
		public String getName() {
			if(isBool()) {
				if(CStandard.supportsTrueFalse())
					return "bool";
				
				return "_Bool";
			}
			
			return name;
		}
		
		@Override
		public String toString() {
			return name;
		}
		
	}
	
}
interface NumberClassHolder {
	
	NumberClass getNumberClass();
	
	default boolean isBool() {
		return getNumberClass() == NumberClass.BOOL;
	}
	
	default boolean isInteger() {
		NumberClass cls = getNumberClass();
		
		return cls == NumberClass.CHAR
			|| cls == NumberClass.SCHAR
			|| cls == NumberClass.UCHAR
			|| cls == NumberClass.SHORT
			|| cls == NumberClass.USHORT
			|| cls == NumberClass.INT
			|| cls == NumberClass.UINT
			|| cls == NumberClass.LONG
			|| cls == NumberClass.ULONG
			|| cls == NumberClass.LLONG
			|| cls == NumberClass.ULLONG
			|| cls == NumberClass.BITINT
			|| cls == NumberClass.UBITINT;
	}
	
	default boolean isFloating() {
		NumberClass cls = getNumberClass();
		
		return cls == NumberClass.FLOAT
			|| cls == NumberClass.DOUBLE
			|| cls == NumberClass.LDOUBLE
			|| cls == NumberClass.DECIMAL32
			|| cls == NumberClass.DECIMAL64
			|| cls == NumberClass.DECIMAL128;
	}
	
	default boolean isBinary() {
		NumberClass cls = getNumberClass();
		
		return cls == NumberClass.FLOAT
			|| cls == NumberClass.DOUBLE
			|| cls == NumberClass.LDOUBLE;
	}

	default boolean isDecimal() {
		NumberClass cls = getNumberClass();
		
		return cls == NumberClass.DECIMAL32
			|| cls == NumberClass.DECIMAL64
			|| cls == NumberClass.DECIMAL128;
	}
	
	default boolean isUnsigned() {
		NumberClass cls = getNumberClass();
		
		if(cls == NumberClass.CHAR)
			return Types.charSign == CharSign.UNSIGNED;
		
		return cls == NumberClass.UCHAR
			|| cls == NumberClass.USHORT
			|| cls == NumberClass.UINT
			|| cls == NumberClass.ULONG
			|| cls == NumberClass.ULLONG
			|| cls == NumberClass.UBITINT;
	}
	
	default boolean isSigned() {
		NumberClass cls = getNumberClass();
		
		if(cls == NumberClass.CHAR)
			return Types.charSign == CharSign.SIGNED;
		
		return cls == NumberClass.CHAR
			|| cls == NumberClass.SHORT
			|| cls == NumberClass.INT
			|| cls == NumberClass.LONG
			|| cls == NumberClass.LLONG
			|| cls == NumberClass.BITINT;
	}
	
	default boolean isAnyChar() {
		NumberClass cls = getNumberClass();
		
		return cls == NumberClass.CHAR
			|| cls == NumberClass.SCHAR
			|| cls == NumberClass.UCHAR;
	}
	
	default boolean isChar() {
		return getNumberClass() == NumberClass.CHAR;
	}
	
	default boolean isSChar() {
		return getNumberClass() == NumberClass.SCHAR;
	}

	default boolean isUChar() {
		return getNumberClass() == NumberClass.UCHAR;
	}
	
	default boolean isAnyShort() {
		NumberClass cls = getNumberClass();
		
		return cls == NumberClass.SHORT
			|| cls == NumberClass.USHORT;
	}
	
	default boolean isShort() {
		return getNumberClass() == NumberClass.SHORT;
	}

	default boolean isUShort() {
		return getNumberClass() == NumberClass.USHORT;
	}
	
	default boolean isAnyInt() {
		NumberClass cls = getNumberClass();
		
		return cls == NumberClass.INT
			|| cls == NumberClass.UINT;
	}
	
	default boolean isInt() {
		return getNumberClass() == NumberClass.INT;
	}
	
	default boolean isUInt() {
		return getNumberClass() == NumberClass.UINT;
	}
	
	default boolean isAnyLong() {
		NumberClass cls = getNumberClass();
		
		return cls == NumberClass.LONG
			|| cls == NumberClass.ULONG;
	}
	
	default boolean isLong() {
		return getNumberClass() == NumberClass.LONG;
	}
	
	default boolean isULong() {
		return getNumberClass() == NumberClass.ULONG;
	}
	
	default boolean isAnyLongLong() {
		NumberClass cls = getNumberClass();
		
		return cls == NumberClass.LLONG
			|| cls == NumberClass.ULLONG;
	}
	
	default boolean isLongLong() {
		return getNumberClass() == NumberClass.LLONG;
	}
	
	default boolean isULongLong() {
		return getNumberClass() == NumberClass.ULLONG;
	}
	
	default boolean isAnyBitInt() {
		NumberClass cls = getNumberClass();
		
		return cls == NumberClass.BITINT
			|| cls == NumberClass.UBITINT;
	}
	
	default boolean isBitInt() {
		return getNumberClass() == NumberClass.BITINT;
	}
	
	default boolean isUBitInt() {
		return getNumberClass() == NumberClass.UBITINT;
	}

	default boolean isFloat() {
		return getNumberClass() == NumberClass.FLOAT;
	}

	default boolean isDouble() {
		return getNumberClass() == NumberClass.DOUBLE;
	}
	
	default boolean isLongDouble() {
		return getNumberClass() == NumberClass.LDOUBLE;
	}

	default boolean isDecimal32() {
		return getNumberClass() == NumberClass.DECIMAL32;
	}

	default boolean isDecimal64() {
		return getNumberClass() == NumberClass.DECIMAL64;
	}
	
	default boolean isDecimal128() {
		return getNumberClass() == NumberClass.DECIMAL128;
	}
	
}
