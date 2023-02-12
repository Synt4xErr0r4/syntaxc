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
package at.syntaxerror.syntaxc.generator.arch.x86.insn;

import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.ADD;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.ADDSD;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.ADDSS;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.CMP;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.COMISD;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.COMISS;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.DIV;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.DIVSD;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.DIVSS;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.FADDP;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.FCOMIP;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.FDIVP;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.FMULP;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.FSTP;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.FSUBP;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.FUCOMIP;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.IDIV;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.IMUL;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.MOV;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.MOVDQU;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.MOVSD;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.MOVSS;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.MULSD;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.MULSS;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.SUB;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.SUBSD;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.SUBSS;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.UCOMISD;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.UCOMISS;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.UD2;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.VMOVDQU;
import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.VMOVDQU8;

import java.util.HashMap;
import java.util.Map;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86FPUHelper;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class X86InstructionSelector {

	private static final Map<X86InstructionKinds, X86InstructionKinds[]> MAPPINGS = new HashMap<>();
	
	private static final int INDEX_SINT = 0;
	private static final int INDEX_UINT = 1;
	private static final int INDEX_F32 = 2;
	private static final int INDEX_F64 = 3;
	private static final int INDEX_F80 = 4;
	private static final int INDEX_M128 = 5;
	private static final int INDEX_M256 = 6;
	private static final int INDEX_M512 = 7;
	
	private static void register(X86InstructionKinds...insns) {
		for(X86InstructionKinds insn : insns)
			MAPPINGS.put(insn, insns);
	}
	
	static {
		//		 sint	uint	float		double		long double		__m128		__m256		__m512
		register(ADD,	ADD,	ADDSS,		ADDSD,		FADDP,			UD2,		UD2,		UD2);
		register(SUB,	SUB,	SUBSS,		SUBSD,		FSUBP,			UD2,		UD2,		UD2);
		register(IMUL,	IMUL,	MULSS,		MULSD,		FMULP,			UD2,		UD2,		UD2);
		register(IDIV,	DIV,	DIVSS,		DIVSD,		FDIVP,			UD2,		UD2,		UD2);
		register(CMP,	CMP,	UCOMISS,	UCOMISD,	FUCOMIP,		UD2,		UD2,		UD2);
		register(CMP,	CMP,	COMISS,		COMISD,		FCOMIP,			UD2,		UD2,		UD2);
		register(MOV,	MOV,	MOVSS,		MOVSD,		FSTP,			MOVDQU,		VMOVDQU,	VMOVDQU8);
	}
	
	public static String getSuffix(Type type) {
		if(type.isArray())
			type = Type.VOID.addressOf();
		
		if(!type.isScalar())
			return "";
		
		switch(type.sizeof()) {
		case 1: return "b";
		case 2: return "w";
		case 4: return "l";
		case 8: return "q";
		default: return "";
		}
	}
	
	public static String getX87Suffix(Type type) {
		if(type.isArray())
			type = Type.VOID.addressOf();
		
		if(!type.isScalar())
			return "";
		
		if(type.isFloating())
			switch(type.sizeof()) {
			case 4: return "s";
			case 8: return "l";
			case 10: return "t";
			default: return "";
			}
		
		switch(type.sizeof()) {
		case 1: return "b";
		case 2: return "w";
		case 4: return "l";
		case 8: return "q";
		default: return "";
		}
	}
	
	public static X86InstructionKinds select(X86InstructionKinds base, Type type) {
		if(!MAPPINGS.containsKey(base))
			return base;
		
		int index;
		
		if(X86FPUHelper.useFPU(type))
			index = INDEX_F80;
		
		else if(type.isVaList())
			index = ArchitectureRegistry.getBitSize() == BitSize.B32
				? INDEX_UINT
				: INDEX_M256;
		
		else if(type.isM128())
			index = INDEX_M128;

		else if(type.isM256())
			index = INDEX_M256;

		else if(type.isM512())
			index = INDEX_M512;
		
		else if(!type.isScalar())
			return null;
		
		else {
			NumericValueType num;
			
			if(type.isEnum())
				num = type.toEnum().getNumericType();
				
			else if(type.isPointerLike())
				num = NumericValueType.POINTER;
				
			else num = type.toNumber().getNumericType();
			
			switch(num) {
			case FLOAT:		index = INDEX_F32; break;
			case DOUBLE:	index = INDEX_F64; break;
			default:
				if(num.isSigned())
					index = INDEX_SINT;
				else index = INDEX_UINT;
				break;
			}
		}
		
		return MAPPINGS.get(base)[index];
	}
	
}
