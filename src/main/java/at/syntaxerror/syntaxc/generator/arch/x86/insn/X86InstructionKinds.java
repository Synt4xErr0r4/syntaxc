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

import at.syntaxerror.syntaxc.generator.asm.insn.AssemblyInstructionKind;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public enum X86InstructionKinds implements AssemblyInstructionKind {

	CLOBBER, /* not actually an instruction, but tells the register allocator that a register now contains undefined data */
	LABEL, /* not actually an instruction, but an assembly label */
	RAW, /* signales that the instruction contains the raw assembly string */
	
	NOP, /* do nothing */
	UD2, /* raise invalid opcode exception */
	
	RET, /* return from function */
	
	PUSH, /* push to stack */
	POP, /* pop from stack */
	
	MOV(true),		/* int = int */
	MOVSS,			/* float = float */
	MOVSD,			/* double = double */
	MOVZX(true),	/* int = zero_extend(int) */
	MOVSX(true),	/* int = sign_extend(int) */
	CMOVNE(true),	/* if(not equal) int = int */
	
	MOVDQU,		/* __m128 = __m128 */
	VMOVDQU,	/* __m256 = __m256 */
	VMOVDQU8,	/* __m512 = __m512 */
	
	FLD,		/* ST(0) = float */
	FILD,		/* ST(0) = int */
	FLDZ,		/* ST(0) = 0 */
	FLD1,		/* ST(0) = 1 */
	FSTP,		/* float = ST(0); ST(i) = ST(0) */
	FISTP,		/* int = ST(0) */
	FNSTCW,		/* int = FPUCW */
	FLDCW,		/* FPUCW = int */
	FXCH,		/* swap ST(0), ST(i) */
	FFREE,		/* free ST(i) */

	LEA(true),	/* &sym */
	
	ADD(true),	/* int + int */
	ADDSS,		/* float + float */
	ADDSD,		/* double + double */
	FADDP,		/* long double + long double */
	
	SUB(true),	/* int + int */
	SUBSS,		/* float + float */
	SUBSD,		/* double + double */
	FSUBP,		/* long double + long double */
	
	IMUL(true),	/* int + int */
	MULSS,		/* float + float */
	MULSD,		/* double + double */
	FMULP,		/* long double + long double */
	
	DIV(true),	/* int + int */
	IDIV(true),	/* int + int */
	DIVSS,		/* float + float */
	DIVSD,		/* double + double */
	FDIVP,		/* long double + long double */
	
	AND(true),  /* int & int */
	OR(true),   /* int | int */
	XOR(true),  /* int ^ int */
	PXOR, 		/* clear XMMn */
	
	SAL(true),	/* int << int (arithmetic) */
	SAR(true),	/* int >> int (arithmetic; preserve sign) */
	SHL(true),  /* int << int (logical; identical to SAL) */
	SHR(true),	/* int >>> int (logical; ignore sign) */
	
	NOT(true),	/* ~int */
	NEG(true),	/* -num */
	
	CALL(true), /* function() */
	
	CVTTSS2SI, /* (int) float */
	CVTTSD2SI, /* (int) double */
	CVTSI2SS,  /* (float) int */
	CVTSD2SS,  /* (float) double */
	CVTSI2SD,  /* (double) int */
	CVTSS2SD,  /* (double) float */
	
	CBW,  /* char -> short (sign-extend) */
	CWDE, /* short -> int (sign-extend) */
	CDQE, /* int -> long (sign-extend) */

	CWD,  /* short -> int (2-reg) */
	CDQ,  /* int -> long (2-reg) */
	CQO,  /* long -> oword (2-reg) */
	
	CMP(true),	/* int == int */
	TEST(true),	/* int == 0 */
	UCOMISS,	/* float == float */
	COMISS,		/* float == float */
	UCOMISD,	/* double == double */
	COMISD,		/* double == double */
	FUCOMIP,	/* long double == long double */
	FCOMIP,		/* long double == long double */

	/* set byte if flag is set/clear */
	SETA,
	SETAE,
	SETB,
	SETBE,
	SETC,
	SETE,
	SETG,
	SETGE,
	SETL,
	SETLE,
	SETNA,
	SETNAE,
	SETNB,
	SETNBE,
	SETNC,
	SETNE,
	SETNG,
	SETNGE,
	SETNL,
	SETNLE,
	SETNO,
	SETNP,
	SETNS,
	SETNZ,
	SETO,
	SETP,
	SETPE,
	SETPO,
	SETS,
	SETZ,

	/* jump if the flag is set/clear */
	JA,
	JAE,
	JB,
	JBE,
	JC,
	JE,
	JG,
	JGE,
	JL,
	JLE,
	JNA,
	JNAE,
	JNB,
	JNBE,
	JNC,
	JNE,
	JNG,
	JNGE,
	JNL,
	JNLE,
	JNO,
	JNP,
	JNS,
	JNZ,
	JO,
	JP,
	JPE,
	JPO,
	JS,
	JZ,
	
	JMP, /* unconditional jump */
	
	REP_MOVS(true), /* memcpy */
	REP_STOS(true), /* memset */
	
	;
	
	private final String strrep = name().toLowerCase().replace('_', ' ');

	@Getter
	private final boolean takesSuffix;

	@Getter
	private final boolean isJump = name().charAt(0) == 'J';
	
	@Getter
	private final boolean isCopy = name().contains("MOV") || name().equals("CLOBBER");
	
	@Getter
	private final boolean isAdditive = name().contains("ADD") || name().equals("SUB");
	
	private X86InstructionKinds() {
		this(false);
	}
	
	@Override
	public String toString() {
		return strrep;
	}
	
}
