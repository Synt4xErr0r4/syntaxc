/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.string;

import java.io.ByteArrayOutputStream;
import java.util.Collection;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 *
 * @author Thomas Kasper
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class CString {

	public static final int RAW_BYTE = 0x80000000;
	
	public static CString of(String string) {
		return of(EncodingPrefix.NONE, string);
	}

	public static CString of(Collection<Integer> codepoints) {
		return of(EncodingPrefix.NONE, codepoints);
	}
	
	public static CString of(EncodingPrefix prefix, String string) {
		return new CString(prefix, string.codePoints().toArray(), string, null);
	}

	public static CString of(EncodingPrefix prefix, Collection<Integer> codepoints) {
		return new CString(prefix, codepoints.stream().mapToInt(i -> i).toArray());
	}
	
	private final EncodingPrefix prefix;
	private final int[] codepoints;

	@Getter(AccessLevel.NONE)
	private String strrep;

	@Getter(AccessLevel.NONE)
	private byte[] bytes;
	
	public byte[] toByteArray() {
		if(bytes != null)
			return bytes;
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		for(int ucs : codepoints) {
			if((ucs & RAW_BYTE) != 0)
				out.write(ucs & 0xFF);
			else {
				byte[] enc = prefix.charset.encode(ucs);
				
				for(byte b : enc)
					out.write(b);
			}
		}
		
		return bytes = out.toByteArray();
	}
	
	@Override
	public String toString() {
		if(strrep != null)
			return strrep;
		
		StringBuilder sb = new StringBuilder();
		
		for(int ucs : codepoints) {
			if((ucs & RAW_BYTE) != 0)
				sb.append('\uFFFD');
			else sb.append(Character.toChars(ucs));
		}
		
		return strrep = sb.toString();
	}
	
}
