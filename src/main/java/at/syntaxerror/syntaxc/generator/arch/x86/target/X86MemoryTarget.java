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
package at.syntaxerror.syntaxc.generator.arch.x86.target;

import java.math.BigInteger;

import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Size;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.type.Type;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class X86MemoryTarget extends X86AssemblyTarget {
	
	public static X86MemoryTarget ofSegmentedDisplaced(Type type, AssemblyTarget segment, AssemblyTarget disp,
			AssemblyTarget base, AssemblyTarget index, long scale) {
		
		if(scale != 0 && scale != 1 && scale != 2 && scale != 4 && scale != 8)
			Logger.error("Illegal scale %d for memory target", scale);
		
		return new X86MemoryTarget(type, X86Size.of(type), segment, disp, base, index, scale);
	}

	public static X86MemoryTarget ofSegmentedDisplaced(Type type, AssemblyTarget segment, AssemblyTarget disp,
			AssemblyTarget base, AssemblyTarget index) {
		return ofSegmentedDisplaced(type, segment, disp, base, index, 1);
	}

	public static X86MemoryTarget ofSegmentedDisplaced(Type type, AssemblyTarget segment, AssemblyTarget disp,
			AssemblyTarget base) {
		return ofSegmentedDisplaced(type, segment, disp, base, null, 0);
	}
	
	
	
	public static X86MemoryTarget ofSegmented(Type type, AssemblyTarget segment, AssemblyTarget base,
			AssemblyTarget index, long scale) {
		return ofSegmentedDisplaced(type, segment, null, base, index, scale);
	}

	public static X86MemoryTarget ofSegmented(Type type, AssemblyTarget segment, AssemblyTarget base, AssemblyTarget index) {
		return ofSegmentedDisplaced(type, segment, null, base, index, 1);
	}

	public static X86MemoryTarget ofSegmented(Type type, AssemblyTarget segment, AssemblyTarget base) {
		return ofSegmentedDisplaced(type, segment, null, base, null, 0);
	}
	
	
	
	public static X86MemoryTarget ofDisplaced(Type type, AssemblyTarget disp, AssemblyTarget base,
			AssemblyTarget index, long scale) {
		return ofSegmentedDisplaced(type, null, disp, base, index, scale);
	}

	public static X86MemoryTarget ofDisplaced(Type type, AssemblyTarget disp, AssemblyTarget base, AssemblyTarget index) {
		return ofSegmentedDisplaced(type, null, disp, base, index, 1);
	}

	public static X86MemoryTarget ofDisplaced(Type type, AssemblyTarget disp, AssemblyTarget base) {
		return ofSegmentedDisplaced(type, null, disp, base, null, 0);
	}
	
	
	
	public static X86MemoryTarget of(Type type, AssemblyTarget base, AssemblyTarget index, long scale) {
		return ofSegmentedDisplaced(type, null, null, base, index, scale);
	}

	public static X86MemoryTarget of(Type type, AssemblyTarget base, AssemblyTarget index) {
		return ofSegmentedDisplaced(type, null, null, base, index, 1);
	}

	public static X86MemoryTarget of(Type type, AssemblyTarget base) {
		return ofSegmentedDisplaced(type, null, null, base, null, 0);
	}

	private final Type type;
	private final X86Size size;
	private final AssemblyTarget segment;
	private final AssemblyTarget displacement;
	private final AssemblyTarget base;
	private final AssemblyTarget index;
	private final long scale;
	
	public boolean hasSegment() {
		return segment != null;
	}

	public boolean hasDisplacement() {
		return displacement != null;
	}

	public boolean hasIndex() {
		return index != null && scale != 0;
	}
	
	@Override
	public Type getType() {
		return type;
	}

	@Override
	public boolean isMemory() {
		return true;
	}
	
	@Override
	public String toAssemblyString(boolean attSyntax) {
		StringBuilder sb = new StringBuilder();
		
		if(attSyntax) {
			/* AT&T syntax: segment:disp(base, index, scale) */

			if(hasSegment())
				sb.append(segment)
					.append(':');
			
			if(hasDisplacement())
				sb = sb.append(displacement);
			
			sb = sb.append('(')
				.append(base);
			
			if(hasIndex()) {
				sb = sb.append(',')
					.append(index);
				
				if(scale != 1)
					sb = sb.append(',')
						.append(scale);
			}
			
			sb = sb.append(')');
		}
		else {
			/* Intel syntax: size PTR segment:[base+index*scale+disp] */
			
			if(size != X86Size.UNKNOWN)
				sb.append(size.getPointerName())
					.append(" PTR ");
			
			if(hasSegment())
				sb.append(segment)
					.append(':');
			
			sb = sb.append('[')
				.append(base);
			
			if(hasIndex()) {
				sb = sb.append(toSignedString(index));
				
				if(scale != 1)
					sb = sb.append('*')
						.append(scale);
			}
			
			if(hasDisplacement())
				sb = sb.append(toSignedString(displacement));
			
			sb = sb.append(']');
		}
		
		return sb.toString();
	}
	
	private static String toSignedString(AssemblyTarget target) {
		String sign = "+";
		
		if(target instanceof X86IntegerTarget integer && integer.getValue().compareTo(BigInteger.ZERO) < 0)
			sign = "";
		
		return sign + target;
	}
	
}
