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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class StringUtils {
	
	// § 6.8.3.2 The # operator
	public static String quote(String str) {
		return str
			.replace("\\", "\\\\")
			.replace("\"", "\\\"");
	}
	
	public static List<String> toASCII(String str, boolean wide) {
		byte[] bytes;
		
		if(wide) {
			int[] codepoints = str.codePoints().toArray();
			
			bytes = new byte[codepoints.length * 4];
			
			for(int i = 0; i < codepoints.length; ++i) {
				int c = codepoints[i];
				
				bytes[4 * i + 0] = (byte) ((c >> 0) & 0xFF);
				bytes[4 * i + 1] = (byte) ((c >> 8) & 0xFF);
				bytes[4 * i + 2] = (byte) ((c >> 16) & 0xFF);
				bytes[4 * i + 3] = (byte) ((c >> 24) & 0xFF);
			}
		}
		else bytes = str.getBytes(StandardCharsets.UTF_8);
		
		List<String> parts = new ArrayList<>();
		StringBuilder part = new StringBuilder();
		
		for(byte b : bytes) {
			if(b == 0) {
				parts.add(part.toString());
				part = new StringBuilder();
				continue;
			}
			
			if(b < ' ' || b >= 0x7F)
				part.append('\\').append(Integer.toOctalString(b));
			
			else part.append((char) b);
		}

		parts.add(part.toString());
		return parts;
	}

}
