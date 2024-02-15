/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.misc;

import lombok.experimental.UtilityClass;

/**
 * 
 *
 * @author Thomas Kasper
 */
@UtilityClass
public class ByteArrays {

	public static byte[] pack(long value, int nBytes) {
		byte[] bytes = new byte[nBytes];
		
		int nBits = 8 * (nBytes - 1);
		
		for(int i = 0; i < nBytes; ++i)
			bytes[i] = (byte) ((value >> (nBits - 8 * i)) & 0xFF);
		
		return bytes;
	}

}
