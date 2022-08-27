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

import at.syntaxerror.syntaxc.generator.asm.target.AssemblyRegister;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
@RequiredArgsConstructor
public enum X86Register implements AssemblyRegister {

	RAX(8),
	EAX(4),
	AX(2),
	AH(1),
	AL(1),

	RBX(8),
	EBX(4),
	BX(2),
	BH(1),
	BL(1),

	RCX(8),
	ECX(4),
	CX(2),
	CH(1),
	CL(1),

	RDX(8),
	EDX(4),
	DX(2),
	DH(1),
	DL(1),

	RSI(8),
	ESI(4),
	SI(2),
	SIL(1),

	RDI(8),
	EDI(4),
	DI(2),
	DIL(1),

	RBP(8),
	EBP(4),
	BP(2),
	BPL(1),

	RSP(8),
	ESP(4),
	SP(2),
	SPL(1),

	R1X(8),
	R1D(4),
	R1W(2),
	R1B(1),

	R9X(8),
	R9D(4),
	R9W(2),
	R9B(1),

	R10X(8),
	R10D(4),
	R10W(2),
	R10B(1),

	R11X(8),
	R11D(4),
	R11W(2),
	R11B(1),

	R12X(8),
	R12D(4),
	R12W(2),
	R12B(1),

	R13X(8),
	R13D(4),
	R13W(2),
	R13B(1),

	R14X(8),
	R14D(4),
	R14W(2),
	R14B(1),

	R15X(8),
	R15D(4),
	R15W(2),
	R15B(1),
	
	FS(2),
	GS(2),
	CS(2),
	DS(2),
	ES(2),
	SS(2),

	RIP(8),
	EIP(4),
	IP(2),
	
	ST0(10),
	ST1(10),
	ST2(10),
	ST3(10),
	ST4(10),
	ST5(10),
	ST6(10),
	ST7(10),
	
	MM0(8),
	MM1(8),
	MM2(8),
	MM3(8),
	MM4(8),
	MM5(8),
	MM6(8),
	MM7(8),
	
	;
	
	private final String name = name().toLowerCase();
	private final int size;
	
	@Override
	public String toString() {
		return "%" + name;
	}
	
}
