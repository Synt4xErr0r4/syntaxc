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
package at.syntaxerror.syntaxc.generator.arch.x86.asm;

import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Size;
import at.syntaxerror.syntaxc.type.StructType.Member;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class X86BitfieldHelper {

	/*
	 * If the bitfield lies within the dword boundary,
	 * only one memory read/write operation is performed:
	 * 
	 * 	affected bytes	| memory access
	 * 	----------------+--------------
	 * 	1 byte			| BYTE
	 * 	2 bytes			| WORD/DWORD (when crossing word boundary)
	 * 	3 bytes			| DWORD
	 * 	4 bytes			| DWORD
	 * 
	 * However, if the bitfield crosses the dword boundary,
	 * the memory access is split up and the following
	 * operations are performed: (this can only happen on 64-bit)
	 * 
	 * 	affected bytes		|
	 *  (1st/2nd dword)		| memory accesses
	 *  --------------------+----------------
	 *  1 byte + 1 bytes	| BYTE + BYTE
	 *  1 byte + 2 bytes	| BYTE + WORD
	 *  1 byte + 3 bytes	| BYTE + DWORD
	 *  1 byte + 4 bytes	| BYTE + DWORD
	 *  2 bytes + 1 bytes	| SHORT + BYTE
	 *  2 bytes + 2 bytes	| SHORT + WORD
	 *  2 bytes + 3 bytes	| SHORT + DWORD
	 *  3 bytes + 1 bytes	| DWORD + BYTE
	 *  3 bytes + 2 bytes	| DWORD + WORD
	 *  4 bytes + 1 byte	| DWORD + BYTE
	 */
	public static BitfieldSegment[] determineBitfieldSegments(Member member) {
		int offset = member.getOffset();
		int bitOffset = member.getBitOffset();
		int bitWidth = member.getBitWidth();
		
		int numBytes = Math.ceilDiv(bitOffset + bitWidth, 8);
		int alignOffset = offset % 4;
		
		if((alignOffset + numBytes) > 4) {
			int segmentBytes = 4 - alignOffset;
			int segmentWidth = 8 * segmentBytes - bitOffset;
			
			return new BitfieldSegment[] {
				new BitfieldSegment(
					false,
					offset,
					alignOffset,
					segmentBytes,
					0,
					bitOffset,
					segmentWidth
				),
				new BitfieldSegment(
					true,
					offset + segmentBytes,
					0,
					numBytes - segmentBytes,
					segmentWidth,
					0,
					bitWidth - segmentWidth
				)
			};
		}
		
		return new BitfieldSegment[] {
			new BitfieldSegment(
				true,
				offset,
				alignOffset,
				numBytes,
				0,
				bitOffset,
				bitWidth
			)
		};
	}
	
	public static record BitfieldSegment(boolean isLast, int offset, int alignOffset, int numBytes,
			int relativeOffset, int bitOffset, int bitWidth) {

		/*
		 * mem-size = the number of bytes read from/written to memory
		 * mem-offset = the number of bytes read/written before the actual data
		 * 
		 * mem-size:
		 * 	BYTE if n=1
		 * 	WORD if n=2 and alignOffset<>1
		 * 	DWORD otherwise
		 * 
		 * mem-offset:
		 * 	1 if (n=3 or n=2) and alignOffset=1		(in this case, a DWORD is read from/written to one byte before the actual data)
		 * 	0 otherwise
		 */
		
		public X86Size memSize() {
			int n = numBytes();
			
			if(n == 1)
				return X86Size.BYTE;
			
			if(n == 2 && alignOffset() != 1)
				return X86Size.WORD;
			
			return X86Size.DWORD;
		}
		
		public int memOffset() {
			int n = numBytes();
			
			if((n == 3 || n == 2) && alignOffset() == 1)
				return 1;
			
			return 0;
		}
		
		public int bitMask() {
			return (1 << bitWidth()) - 1;
		}
		
	}
	
}
