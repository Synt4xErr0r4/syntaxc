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
package at.syntaxerror.syntaxc.generator.arch.x86;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.SystemUtils.OperatingSystem;
import at.syntaxerror.syntaxc.generator.arch.Alignment;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.type.StructType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.Setter;

/**
 * @author Thomas Kasper
 * 
 */
public class X86Alignment extends Alignment {

	@Setter
	private BitSize bitsSize;

	@Override
	public int getAlignment(Type type) {
		return getAlignment(type, false);
	}
	
	private int getAlignment(Type type, boolean structMember) {
		
		if(bitsSize == BitSize.B64) {
			/*
			 * https://www.uclibc.org/docs/psABI-x86_64.pdf
			 * § 3.1.2 Data Representation
			 * 
			 * https://learn.microsoft.com/en-us/cpp/build/x64-software-conventions?view=msvc-170
			 */
			
			if(type.isArray()) {
				/*
				 * align arrays like their elements, except
				 * for local/global arrays with a size of
				 * at least 16 bytes, which are aligned at
				 * a 16-byte boundary (Linux only)
				 */
				
				if(!structMember && ArchitectureRegistry.getOperatingSystem() == OperatingSystem.LINUX
					&& type.sizeof() >= 16)
					return 16;
				
				return getAlignment(type.toArray().getBase());
			}
			
			if(type.isFunction() || type.isVaList())
				return 8;
		}
		else if(bitsSize == BitSize.B32) {
			/*
			 * http://www.sco.com/developers/devspecs/abi386-4.pdf
			 * § 3 Low-Level System Information, Machine Interface, Data Representation, Fundamental Types
			 */
			
			if(type.isFloating())
				return 4;
			
			if(type.isArray())
				return getAlignment(type.toArray().getBase());
			
			if(type.isFunction() || type.isVaList())
				return 4;
		}

		if(type.isScalar())
			return type.sizeof();
		
		if(type.isStructLike())
			return type.toStructLike()
				.getMembers()
				.stream()
				.mapToInt(mem -> getAlignment(mem.getType(), true))
				.max()
				.orElseGet(() -> 0);
		
		throw new IllegalArgumentException("Unknown type: " + type);
	}
	
	@Override
	public StructAlignment getMemberAlignment(StructType struct, Type type, int offset, int bitOffset, int bitWidth, boolean wasBitfield) {
		
		if((offset == 0 && bitOffset == 0) || struct.isUnion())
			return StructAlignment.ZERO;
		
		/*
		 * http://www.sco.com/developers/devspecs/abi386-4.pdf
		 * § 3 Low-Level System Information, Machine Interface, Data Representation, Aggregates and Unions
		 */
		
		if(type.isBitfield()) {
			/* align bitfield to next dword if
			 *  a. its bit width is zero
			 *  b. its width would exceed the dword (4-byte) boundary
			 */
			
			int off = offset % 4;
			
			if(bitWidth == 0 || off * 8 + bitOffset + bitWidth > 32) {
				
				// make sure to actually align on next dword
				if(bitOffset != 0 && off == 0)
					++offset;
				
				return StructAlignment.of((int) alignAt(offset, 4));
			}
			
			// use compact alignment otherwise
			return StructAlignment.of(offset, bitOffset);
		}
		
		return StructAlignment.of(
			(int) alignAt(
				offset,
				getAlignment(type, true)
			)
		);
	}
	
}
