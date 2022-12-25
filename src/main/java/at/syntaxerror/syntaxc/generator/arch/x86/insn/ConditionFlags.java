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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
@RequiredArgsConstructor
public enum ConditionFlags {

	ABOVE				("a",   X86InstructionKinds.SETA,	X86InstructionKinds.JA),
	ABOVE_EQUAL			("ae",  X86InstructionKinds.SETAE,	X86InstructionKinds.JAE),
	BELOW				("b",   X86InstructionKinds.SETB,	X86InstructionKinds.JB),
	BELOW_EQUAL			("be",  X86InstructionKinds.SETBE,	X86InstructionKinds.JBE),
	CARRY				("c",   X86InstructionKinds.SETC,	X86InstructionKinds.JC),
	EQUAL				("e",   X86InstructionKinds.SETE,	X86InstructionKinds.JE),
	GREATER				("g",   X86InstructionKinds.SETG,	X86InstructionKinds.JG),
	GREATER_EQUAL		("ge",  X86InstructionKinds.SETGE,	X86InstructionKinds.JGE),
	LESS				("l",   X86InstructionKinds.SETL,	X86InstructionKinds.JL),
	LESS_EQUAL			("le",  X86InstructionKinds.SETLE,	X86InstructionKinds.JLE),
	NOT_ABOVE			("na",  X86InstructionKinds.SETNA,	X86InstructionKinds.JNA),
	NOT_ABOVE_EQUAL		("nae", X86InstructionKinds.SETNAE,	X86InstructionKinds.JNAE),
	NOT_BELOW			("nb",  X86InstructionKinds.SETNB,	X86InstructionKinds.JNB),
	NOT_BELOW_EQUAL		("nbe", X86InstructionKinds.SETNBE,	X86InstructionKinds.JNBE),
	NOT_CARRY			("nc",  X86InstructionKinds.SETNC,	X86InstructionKinds.JNC),
	NOT_EQUAL			("ne",  X86InstructionKinds.SETNE,	X86InstructionKinds.JNE),
	NOT_GREATER			("ng",  X86InstructionKinds.SETNG,	X86InstructionKinds.JNG),
	NOT_GREATER_EQUAL	("nge", X86InstructionKinds.SETNGE,	X86InstructionKinds.JNGE),
	NOT_LESS			("nl",  X86InstructionKinds.SETNL,	X86InstructionKinds.JNL),
	NOT_LESS_EQUAL		("nle", X86InstructionKinds.SETNLE,	X86InstructionKinds.JNLE),
	NOT_OVERFLOW		("no",  X86InstructionKinds.SETNO,	X86InstructionKinds.JNO),
	NOT_PARITY			("np",  X86InstructionKinds.SETNP,	X86InstructionKinds.JNP),
	NOT_SIGN			("ns",  X86InstructionKinds.SETNS,	X86InstructionKinds.JNS),
	NOT_ZERO			("nz",  X86InstructionKinds.SETNZ,	X86InstructionKinds.JNZ),
	OVERFLOW			("o",   X86InstructionKinds.SETO,	X86InstructionKinds.JO),
	PARITY				("p",   X86InstructionKinds.SETP,	X86InstructionKinds.JP),
	PARITY_EVEN			("pe",  X86InstructionKinds.SETPE,	X86InstructionKinds.JPE),
	PARITY_ODD			("po",  X86InstructionKinds.SETPO,	X86InstructionKinds.JPO),
	SIGN				("s",   X86InstructionKinds.SETS,	X86InstructionKinds.JS),
	ZERO				("z",   X86InstructionKinds.SETZ,	X86InstructionKinds.JZ),
	;
	
	private static void negate(ConditionFlags a, ConditionFlags b) {
		a.negated = b;
		b.negated = a;
	}
	
	static {
		negate(ABOVE,			NOT_ABOVE);
		negate(ABOVE_EQUAL,		NOT_ABOVE_EQUAL);
		negate(BELOW,			NOT_BELOW);
		negate(BELOW_EQUAL,		NOT_BELOW_EQUAL);
		negate(CARRY,			NOT_CARRY);
		negate(EQUAL,			NOT_EQUAL);
		negate(GREATER,			NOT_GREATER);
		negate(GREATER_EQUAL,	NOT_GREATER_EQUAL);
		negate(LESS,			NOT_LESS);
		negate(LESS_EQUAL,		NOT_LESS_EQUAL);
		negate(OVERFLOW,		NOT_OVERFLOW);
		negate(PARITY,			NOT_PARITY);
		negate(PARITY_EVEN,		PARITY_ODD);
		negate(SIGN,			NOT_SIGN);
		negate(ZERO,			NOT_ZERO);
	}
	
	private final String code;
	private final X86InstructionKinds setInstruction;
	private final X86InstructionKinds jumpInstruction;
	
	private ConditionFlags negated;
	
	public ConditionFlags negate() {
		return negated;
	}
	
}
