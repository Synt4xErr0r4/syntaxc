/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.io;

import java.util.function.Supplier;

/**
 * 
 *
 * @author Thomas Kasper
 */
public enum Charsets implements ICharset {
	
	UTF8 {
	
		@Override
		public int decode(Supplier<Integer> next) {
			int val = read(next, true);
			
			if(val < 0x80)
				return val;
			
			int n;
			
			if((val & 0xE0) == 0xC0) {
				n = 1;
				val &= 0x1F;
			}
			else if((val & 0xF0) == 0xE0) {
				n = 2;
				val &= 0x0F;
			}
			else if((val & 0xF8) == 0xF0) {
				n = 3;
				val &= 0x07;
			}
			else throw new MalformedFileException("Illegal leading UTF-8 byte");
			
			for(; n > 0; --n) {
				int c = read(next, false);
				
				if((c & 0xC0) != 0x80)
					throw new MalformedFileException("Illegal UTF-8 continuation byte");
				
				val = (val << 6) | (c & 0x3F);
			}

			if(val >= 0x10FFFF)
				throw new MalformedFileException("Unicode codepoint out of range");
			
			return (int) val;
		}

		@Override
		public byte[] encode(int codepoint) {
			if(codepoint < 0x80)
				return new byte[] { (byte) codepoint };
			
			byte[] buf;
			
			if(codepoint < 0x800) {
				buf = new byte[2];
				buf[0] = (byte) (((codepoint >> 6) & 0x1F) | 0xC0);
			}
			else if(codepoint < 0x10000) {
				buf = new byte[3];
				buf[0] = (byte) (((codepoint >> 12) & 0x0F) | 0xE0);
			}
			else {
				buf = new byte[4];
				buf[0] = (byte) (((codepoint >> 18) & 0x07) | 0xF0);
			}
			
			for(int i = 0; i < buf.length - 1; ++i)
				buf[buf.length - 1 - i] = (byte) (((codepoint >> (6 * i)) & 0x3F) | 0x80);
			
			return buf;
		}
		
	},
	
	UTF16LE {
	
		@Override
		public int decode(Supplier<Integer> next) {
			long val = read(next, 2, false);
			
			if(val >= Character.MIN_HIGH_SURROGATE && val <= Character.MAX_HIGH_SURROGATE) {
				long low = read(next, 2, false);
				
				if(val < Character.MIN_LOW_SURROGATE || val > Character.MAX_LOW_SURROGATE)
					throw new MalformedFileException("Missing low surrogate character");
				
				val = ((val - 0xD800) << 10) + low - 0xDC00 + 0x10000;
			}
			
			if(val >= 0x10FFFF)
				throw new MalformedFileException("Unicode codepoint out of range");
			
			return (int) val;
		}

		@Override
		public byte[] encode(int codepoint) {
			byte[] buf;
			
			if(codepoint > 0xFFFF) {
				buf = new byte[4];
				
				codepoint -= 0x10000;

				int hi = (codepoint >> 10) + 0xD800;
				int lo = (codepoint & 0x3FF) + 0xDC00;
				
				buf[0] = (byte) ((hi >> 8) & 0xFF);
				buf[1] = (byte) (hi & 0xFF);
				buf[2] = (byte) ((lo >> 8) & 0xFF);
				buf[3] = (byte) (lo & 0xFF);
			}
			else {
				buf = new byte[2];
				
				buf[0] = (byte) ((codepoint >> 8) & 0xFF);
				buf[1] = (byte) (codepoint & 0xFF);
			}

			return buf;
		}
		
	},
	
	UTF16BE {
	
		@Override
		public int decode(Supplier<Integer> next) {
			long val = read(next, 2, true);
			
			if(val >= Character.MIN_HIGH_SURROGATE && val <= Character.MAX_HIGH_SURROGATE) {
				long low = read(next, 2, true);
				
				if(val < Character.MIN_LOW_SURROGATE || val > Character.MAX_LOW_SURROGATE)
					throw new MalformedFileException("Missing low surrogate character");
				
				val = ((val - 0xD800) << 10) + low - 0xDC00 + 0x10000;
			}
			
			if(val >= 0x10FFFF)
				throw new MalformedFileException("Unicode codepoint out of range");
			
			return (int) val;
		}

		@Override
		public byte[] encode(int codepoint) {
			return reverse(UTF16LE.encode(codepoint));
		}
		
	},
	
	UTF32LE {
	
		@Override
		public int decode(Supplier<Integer> next) {
			long val = read(next, 4, false);
			
			if(val >= 0x10FFFF)
				throw new MalformedFileException("Unicode codepoint out of range");
			
			return (int) val;
		}

		@Override
		public byte[] encode(int codepoint) {
			byte[] buf = new byte[4];
			
			buf[0] = (byte) ((codepoint >> 24) & 0xFF);
			buf[1] = (byte) ((codepoint >> 16) & 0xFF);
			buf[2] = (byte) ((codepoint >> 8) & 0xFF);
			buf[3] = (byte) (codepoint & 0xFF);
			
			return buf;
		}
		
	},
	
	UTF32BE {
	
		@Override
		public int decode(Supplier<Integer> next) {
			long val = read(next, 4, true);
			
			if(val >= 0x10FFFF)
				throw new MalformedFileException("Unicode codepoint out of range");
			
			return (int) val;
		}

		@Override
		public byte[] encode(int codepoint) {
			return reverse(UTF32LE.encode(codepoint));
		}
		
	};
	
	private static int read(Supplier<Integer> next, boolean allowEOF) {
		int c = next.get();
		
		if(c < 0 || c > 0xFF) {
			if(allowEOF)
				return -1;
			
			throw new MalformedFileException("Malformed file");
		}
		
		return c;
	}
	
	private static long read(Supplier<Integer> next, int n, boolean reverse) {
		long val = 0;
		
		for(int i = 0; i < n; ++i) {
			long c = read(next, i == 0);
			
			if(c == -1)
				return -1;
			
			int off = 8 * (reverse ? n - i - 1 : i);
			
			val |= c << off;
		}
		
		return val;
	}
	
	private static byte[] reverse(byte[] buf) {
		for(int i = 0; i < buf.length / 2; ++i) {
			byte b = buf[i];
			buf[i] = buf[buf.length - 1 - i];
			buf[buf.length - 1 - i] = b;
		}
		
		return buf;
	}
	
}
