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

import java.util.HashMap;
import java.util.Map;

import at.syntaxerror.syntaxc.misc.config.Flags;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.experimental.UtilityClass;

import static at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds.*;

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
	
	private static void register(X86InstructionKinds...insns) {
		for(X86InstructionKinds insn : insns)
			MAPPINGS.put(insn, insns);
	}
	
	static {
		//		 sint	uint	float		double		long double
		register(ADD,	ADD,	ADDSS,		ADDSD,		FADDP);
		register(SUB,	SUB,	SUBSS,		SUBSD,		FSUBP);
		register(IMUL,	IMUL,	MULSS,		MULSD,		FMULP);
		register(IDIV,	DIV,	DIVSS,		DIVSD,		FDIVP);
		register(CMP,	CMP,	UCOMISS,	UCOMISD,	FUCOMIP);
		register(MOV,	MOV,	MOVSS,		MOVSD,		FSTP);
	}
	
	public static X86InstructionKinds select(X86InstructionKinds base, Type type) {
		if(!type.isScalar())
			return null;
		
		if(!MAPPINGS.containsKey(base))
			return base;
		
		NumericValueType num;
		
		if(type.isEnum())
			num = type.toEnum().getNumericType();
			
		else if(type.isPointerLike())
			num = NumericValueType.POINTER;
			
		else num = type.toNumber().getNumericType();
		
		int index;
		
		switch(num) {
		case FLOAT:		index = INDEX_F32; break;
		case DOUBLE:	index = INDEX_F64; break;
		case LDOUBLE:
			if(!Flags.LONG_DOUBLE.isEnabled())
				index = INDEX_F64;
			else index = INDEX_F80;
			break;
		default:
			if(num.isSigned())
				index = INDEX_SINT;
			else index = INDEX_UINT;
			break;
		}
		
		return MAPPINGS.get(base)[index];
	}
	
}
