/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.string;

import java.math.BigInteger;
import java.util.function.Supplier;

import at.syntaxerror.syntaxc.frontend.c.type.IntType;
import at.syntaxerror.syntaxc.frontend.c.type.Types;
import at.syntaxerror.syntaxc.io.Charsets;
import at.syntaxerror.syntaxc.io.ICharset;
import lombok.AllArgsConstructor;

/**
 * 
 *
 * @author Thomas Kasper
 */
@AllArgsConstructor
public enum EncodingPrefix {

	NONE(Charsets.UTF8,		() -> Types.INT,	() -> Types.UINT),
	WIDE(Charsets.UTF32LE,	() -> Types.WCHAR,	() -> Types.WCHAR),
	UTF8(Charsets.UTF8,		() -> Types.CHAR8,	() -> Types.CHAR8),
	UTF16(Charsets.UTF16LE,	() -> Types.CHAR16,	() -> Types.CHAR16),
	UTF32(Charsets.UTF32LE,	() -> Types.CHAR32,	() -> Types.CHAR32);
	
	public ICharset charset;
	public final Supplier<IntType> type;
	public final Supplier<IntType> compareType;
	
	public IntType getType() {
		return type.get();
	}
	
	public boolean inRange(BigInteger value) {
		return compareType.get().getMaxValue().compareTo(value) >= 0;
	}
	
	public BigInteger truncate(BigInteger value) {
		value = value.and(compareType.get().getMaxValue());
		
		var type = getType();
		
		if(!type.isUnsigned() && type.getMaxValue().compareTo(value) < 0)
			value = value.subtract(compareType.get().getMaxValue()).subtract(BigInteger.ONE);
		
		return value;
	}
	
}
