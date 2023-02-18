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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Size;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86MemoryTarget.X86Displacement;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualStackTarget;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.type.Type;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public class X86DeferredMemoryTarget extends X86AssemblyTarget {

	private static final int MAX_REGISTER_COUNT = 2;

	public static boolean isValid(List<AssemblyTarget> parts) {
		int registerCount = 0;
		boolean hasOffset = false;
		
		for(AssemblyTarget part : parts)
			if(part instanceof X86OffsetTarget) {
				if(hasOffset)
					return false;
				
				hasOffset = true;
			}
			else registerCount += countRegisters(part);
		
		return registerCount <= MAX_REGISTER_COUNT;
	}
		
	private final Type type;
	private final List<AssemblyTarget> parts;
	private int registerCount;
	private boolean hasOffset;
	
	public X86DeferredMemoryTarget(Type type, List<AssemblyTarget> parts) {
		this.type = type;
		this.parts = new ArrayList<>(parts);
		
		for(AssemblyTarget part : parts)
			if(part instanceof X86OffsetTarget) {
				if(hasOffset)
					Logger.error("Too many offsets for deferred memory target");
				
				hasOffset = true;
			}
			else registerCount += countRegisters(part);
		
		if(registerCount > MAX_REGISTER_COUNT)
			Logger.error("Too many registers for deferred memory target");
	}
	
	public boolean accepts(AssemblyTarget target) {
		if(target instanceof X86OffsetTarget)
			return !hasOffset;
		
		return registerCount + countRegisters(target) <= MAX_REGISTER_COUNT;
	}
	
	public X86DeferredMemoryTarget with(Type type, AssemblyTarget... parts) {
		return new X86DeferredMemoryTarget(
			type,
			Stream.concat(
				this.parts.stream(),
				Stream.of(parts)
			).toList()
		);
	}
	
	public X86MemoryTarget combine() {
		List<AssemblyTarget> registers = new ArrayList<>();
		List<AssemblyTarget> labels = new ArrayList<>();

		BigInteger displacement = BigInteger.ZERO;
		
		for(AssemblyTarget part : parts) {
			
			X86MemoryTarget mem = null;
			AssemblyTarget disp = null;
			
			if(part instanceof X86DeferredMemoryTarget deferred)
				mem = deferred.combine();
			
			else if(part instanceof X86MemoryTarget m)
				mem = m;
			
			else if(part.isRegister())
				registers.add(part);
			
			else disp = part;
			
			if(mem != null) {
				registers.add(mem.getBase());
				registers.add(mem.getIndex());
				disp = mem.getDisplacement();
			}
			
			if(disp != null)
				displacement = displacement.add(processDisplacement(disp, labels));
		}
		
		registers = registers
			.stream()
			.filter(t -> t != null)
			.toList();
		
		int sz = registers.size();
		
		AssemblyTarget base = sz > 0 ? registers.get(0) : null;
		AssemblyTarget index = sz > 1 ? registers.get(1) : null;
		AssemblyTarget disp;
		
		boolean hasConstantDisplacement = displacement.compareTo(BigInteger.ZERO) != 0;
		
		if(labels.isEmpty())
			disp = hasConstantDisplacement
				? new X86IntegerTarget(Type.INT, displacement)
				: null;
		
		else {
			if(hasConstantDisplacement)
				labels.add(new X86IntegerTarget(Type.INT, displacement));
			
			disp = new X86Displacement(
				labels.toArray(AssemblyTarget[]::new)
			);
		}
		
		return X86MemoryTarget.ofDisplaced(type, disp, base, index);
	}
	
	@Override
	public List<AssemblyTarget> getNestedTargets() {
		return parts;
	}
	
	@Override
	public boolean isMemory() {
		return true;
	}
	
	@Override
	public AssemblyTarget resized(Type type) {
		return new X86DeferredMemoryTarget(type, parts);
	}

	@Override
	public String toAssemblyString(boolean attSyntax) {
		return "DEFERRED "
			+ X86Size.of(type)
				.getPointerName()
			+ " PTR ["
			+ String.join(
				"+",
				parts.stream()
					.map(AssemblyTarget::toString)
					.toList()
			)
			+ "]";
	}
	
	private static BigInteger processDisplacement(AssemblyTarget disp, List<AssemblyTarget> labels) {
		if(disp == null)
			return BigInteger.ZERO;
		
		if(disp instanceof X86LabelTarget || disp instanceof X86OffsetTarget) {
			labels.add(disp);
			return BigInteger.ZERO;
		}
		
		if(disp instanceof X86IntegerTarget integer)
			return integer.getValue();
		
		if(disp instanceof X86Displacement displacement)
			return displacement.getTargets()
				.stream()
				.map(target -> processDisplacement(target, labels))
				.reduce(BigInteger.ZERO, BigInteger::add);
		
		return BigInteger.ZERO;
	}
	
	private static int countRegisters(AssemblyTarget target) {
		if(target == null)
			return 0;
		
		if(target.isRegister())
			return 1;
		
		if(target instanceof X86DeferredMemoryTarget deferred)
			return deferred.registerCount;
		
		if(target instanceof X86MemoryTarget mem) {
			
			AssemblyTarget base = mem.getBase();

			if(base != null
				&& base instanceof X86Register reg
				&& X86Register.EIP.intersects(reg))
				return MAX_REGISTER_COUNT; // cannot use EIP/RIP in combination with other registers
			
			return countRegisters(base)
				+ countRegisters(mem.getIndex());
		}
		
		if(target instanceof VirtualStackTarget)
			return 1; // EBP/RBP
		
		return 0;
	}
	
}
