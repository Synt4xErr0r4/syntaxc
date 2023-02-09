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
import java.util.List;

import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Size;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualRegisterTarget;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.ObservableList;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.type.Type;
import at.syntaxerror.syntaxc.type.TypeUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class X86MemoryTarget extends X86AssemblyTarget {

	public static X86MemoryTarget ofSegmentedDisplaced(Type type, AssemblyTarget segment, AssemblyTarget disp,
			AssemblyTarget base, AssemblyTarget index, long scale) {
		
		if(scale != 0 && scale != 1 && scale != 2 && scale != 4 && scale != 8)
			Logger.error("Illegal scale %d for memory target", scale);

		if(segment != null && !(segment instanceof X86Register))
			Logger.error("Segment is not a register");
		
		if(base != null && !(base instanceof X86Register) && !(base instanceof VirtualRegisterTarget)) {
			new Throwable().printStackTrace();
			
			Logger.error("Base is not a register");}
		
		if(index != null && !(index instanceof X86Register) && !(index instanceof VirtualRegisterTarget))
			Logger.error("Index is not a register");
		
		if(index != null && !(disp instanceof X86IntegerTarget))
			Logger.error("Displacement is not an integer");
		
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
	private AssemblyTarget segment;
	private AssemblyTarget displacement;
	private AssemblyTarget base;
	private AssemblyTarget index;
	private final long scale;
	
	@SuppressWarnings("unchecked")
	@Override
	public List<AssemblyTarget> getNestedTargets() {
		return new ObservableList<>(
			Pair.of(segment, this::setSegment),
			Pair.of(displacement, this::setDisplacement),
			Pair.of(base, this::setBase),
			Pair.of(index, this::setIndex)
		);
	}
	
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
	public AssemblyTarget resized(Type type) {
		return new X86MemoryTarget(type, X86Size.of(type), segment, displacement, base, index, scale);
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
	public boolean equals(Object obj) {
		return obj != null
			&& obj instanceof X86MemoryTarget mem
			&& TypeUtils.isEqual(type, mem.type)
			&& equals(segment, mem.segment)
			&& equals(displacement, mem.displacement)
			&& equals(base, mem.base)
			&& equals(index, mem.index)
			&& scale == mem.scale;
	}
	
	@Override
	public String toAssemblyString(boolean attSyntax) {
		StringBuilder sb = new StringBuilder();
		
		if(attSyntax) {
			/* AT&T syntax: segment:disp(base, index, scale) */
			
			if(hasSegment())
				sb.append(toAssemblyString(segment, attSyntax))
					.append(':');
			
			if(hasDisplacement())
				sb = sb.append(toAssemblyString(displacement, attSyntax));
			
			if(base != X86Register.EIP) {
				sb = sb.append('(')
					.append(toAssemblyString(base, attSyntax));
				
				if(hasIndex()) {
					sb = sb.append(',')
						.append(toAssemblyString(index, attSyntax));
					
					if(scale != 1)
						sb = sb.append(',')
							.append(scale);
				}
				
				sb = sb.append(')');
			}
		}
		else {
			/* Intel syntax: size PTR segment:[base+index*scale+disp]	(no EIP addressing mode for 32-bit) */
			
			if(size != X86Size.UNKNOWN)
				sb = sb.append(size.getPointerName())
					.append(" PTR ");
			
			if(hasSegment())
				sb.append(toAssemblyString(segment, attSyntax))
					.append(':');

			boolean hasBase = base != X86Register.EIP;
			boolean hasPredecessor = hasBase;
			
			if(hasBase)
				sb = sb.append('[')
					.append(toAssemblyString(base, attSyntax));

			if(hasIndex()) {
				sb = sb.append(
					hasPredecessor
						? toSignedString(index, attSyntax)
						: toAssemblyString(index, attSyntax)
				);
				
				hasPredecessor = true;
				
				if(scale != 1)
					sb = sb.append('*')
						.append(scale);
			}
			
			if(hasDisplacement())
				sb = sb.append(
					hasPredecessor
						? toSignedString(displacement, attSyntax)
						: toAssemblyString(displacement, attSyntax)
				);

			if(hasBase)
				sb = sb.append(']');
		}
		
		return sb.toString();
	}
	
	private static String toAssemblyString(AssemblyTarget target, boolean attSyntax) {
		if(target instanceof X86AssemblyTarget x86)
			return x86.toAssemblyString(attSyntax && !(x86 instanceof X86IntegerTarget));
		
		if(target instanceof X86Register reg)
			return reg.toAssemblyString(attSyntax);
		
		return target.toString();
	}
	
	private static String toSignedString(AssemblyTarget target, boolean attSyntax) {
		String sign = "+";
		
		if(target instanceof X86IntegerTarget integer && integer.getValue().compareTo(BigInteger.ZERO) < 0)
			sign = "";
		
		return sign + toAssemblyString(target, attSyntax);
	}
	
}
