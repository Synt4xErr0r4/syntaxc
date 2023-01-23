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

import java.math.BigInteger;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.arch.x86.target.X86IntegerTarget;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.config.Flags;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class X86FPUHelper {

	public static boolean useFPU(Type type) {
		return (ArchitectureRegistry.getBitSize() == BitSize.B32 && type.isFloating())
			|| (Flags.LONG_DOUBLE.isEnabled() && isLongDouble(type));
	}
	
	public static boolean isLongDouble(Type type) {
		return type.isFloating()
			&& type.toNumber().getNumericType() == NumericValueType.LDOUBLE;
	}
	
	public static boolean isDouble(Type type) {
		return type.toNumber().getNumericType() == NumericValueType.DOUBLE;
	}
	
	public static boolean isFloat(Type type) {
		return type.toNumber().getNumericType() == NumericValueType.FLOAT;
	}
	
	private final Instructions asm;
	private final X86AssemblyGenerator gen;
	
	private int stackCount;
	
	public boolean isFPUStackEmpty() {
		return stackCount < 1;
	}
	
	public void fpuStackPop() {
		if(--stackCount < 0)
			Logger.warn("FPU stack underflow detected. This is a bug.");
	}
	
	public void fpuStackPush() {
		if(++stackCount > 8)
			Logger.warn("FPU stack overflow detected. This is a bug.");
	}
	
	public boolean isST(AssemblyTarget target) {
		return X86Register.GROUP_ST.contains(target);
	}
	
	public void fld(AssemblyTarget target) {
		if(target == X86Register.ST0)
			return;
		
		fpuStackPush();
		
		if(isST(target)) {
			asm.add(X86InstructionKinds.FLD, target);
			return;
		}
		
		if(target instanceof X86IntegerTarget lit) {
			
			BigInteger val = lit.getValue();
			
			if(val.compareTo(BigInteger.ZERO) == 0) {
				asm.add(X86InstructionKinds.FLDZ);
				return;
			}

			if(val.compareTo(BigInteger.ZERO) == 1) {
				asm.add(X86InstructionKinds.FLD1);
				return;
			}
			
		}
		
		asm.add(
			target.getType().isFloating()
				? X86InstructionKinds.FLD
				: X86InstructionKinds.FILD,
			gen.toMemory(target)
		);
	}
	
	public void fpop() {
		asm.add(X86InstructionKinds.FSTP, X86Register.ST0);
		fpuStackPop();
	}

	public void fstp(AssemblyTarget target) {
		if(target == X86Register.ST0)
			return;
		
		fpuStackPop();
		
		if(isST(target))
			asm.add(X86InstructionKinds.FSTP, target);
		
		else asm.add(
			target.getType().isFloating()
				? X86InstructionKinds.FSTP
				: X86InstructionKinds.FISTP,
			gen.toMemory(target.minimum(Type.SHORT))
		);
	}
	
}
