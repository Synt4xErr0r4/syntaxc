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
package at.syntaxerror.syntaxc.generator.arch.x86.register;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Size;
import at.syntaxerror.syntaxc.generator.asm.target.RegisterTarget;
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
public class X86Register implements RegisterTarget {
	
	private static final Map<Integer, Type> TYPE_MAP = Map.of(
		1, Type.UCHAR,
		2, Type.USHORT,
		4, Type.UINT,
		8, Type.ULONG,
		10, Type.LDOUBLE,
		16, Type.DOUBLE
	);

	/* --- REGISTER GROUPS --- */
	
	public static final List<X86Register> GROUP_A = makeGPRWithLH("A");
	public static final List<X86Register> GROUP_B = makeGPRWithLH("B");
	public static final List<X86Register> GROUP_C = makeGPRWithLH("C");
	public static final List<X86Register> GROUP_D = makeGPRWithLH("D");
	
	public static final List<X86Register> GROUP_SI = makeGPR("SI");
	public static final List<X86Register> GROUP_DI = makeGPR("DI");
	public static final List<X86Register> GROUP_BP = makeGPR("BP");
	public static final List<X86Register> GROUP_SP = makeGPR("SP");
	
	/* x64 only */
	public static final List<X86Register> GROUP_R8 = makeGPRNumeric("8");
	public static final List<X86Register> GROUP_R9 = makeGPRNumeric("9");
	public static final List<X86Register> GROUP_R10 = makeGPRNumeric("10");
	public static final List<X86Register> GROUP_R11 = makeGPRNumeric("11");
	public static final List<X86Register> GROUP_R12 = makeGPRNumeric("12");
	public static final List<X86Register> GROUP_R13 = makeGPRNumeric("13");
	public static final List<X86Register> GROUP_R14 = makeGPRNumeric("14");
	public static final List<X86Register> GROUP_R15 = makeGPRNumeric("15");

	public static final List<X86Register> GROUP_IP = makeIP();

	public static final List<X86Register> GROUP_ST = makeX87();
	
	public static final List<X86Register> GROUP_XMM = makeXMM();
	
	/* --- SEPARATE REGISTERS --- */
	
	public static final X86Register RAX = GROUP_A.get(0);
	public static final X86Register EAX = GROUP_A.get(1);
	public static final X86Register AX = GROUP_A.get(2);
	public static final X86Register AH = GROUP_A.get(3);
	public static final X86Register AL = GROUP_A.get(4);

	public static final X86Register RBX = GROUP_B.get(0);
	public static final X86Register EBX = GROUP_B.get(1);
	public static final X86Register BX = GROUP_B.get(2);
	public static final X86Register BH = GROUP_B.get(3);
	public static final X86Register BL = GROUP_B.get(4);

	public static final X86Register RCX = GROUP_C.get(0);
	public static final X86Register ECX = GROUP_C.get(1);
	public static final X86Register CX = GROUP_C.get(2);
	public static final X86Register CH = GROUP_C.get(3);
	public static final X86Register CL = GROUP_C.get(4);

	public static final X86Register RDX = GROUP_D.get(0);
	public static final X86Register EDX = GROUP_D.get(1);
	public static final X86Register DX = GROUP_D.get(2);
	public static final X86Register DH = GROUP_D.get(3);
	public static final X86Register DL = GROUP_D.get(4);
	
	public static final X86Register RSI = GROUP_SI.get(0);
	public static final X86Register ESI = GROUP_SI.get(1);
	public static final X86Register SI = GROUP_SI.get(2);
	public static final X86Register SIL = GROUP_SI.get(3);

	public static final X86Register RDI = GROUP_DI.get(0);
	public static final X86Register EDI = GROUP_DI.get(1);
	public static final X86Register DI = GROUP_DI.get(2);
	public static final X86Register DIL = GROUP_DI.get(3);

	public static final X86Register RBP = GROUP_BP.get(0);
	public static final X86Register EBP = GROUP_BP.get(1);
	public static final X86Register BP = GROUP_BP.get(2);
	public static final X86Register BPL = GROUP_BP.get(3);

	public static final X86Register RSP = GROUP_SP.get(0);
	public static final X86Register ESP = GROUP_SP.get(1);
	public static final X86Register SP = GROUP_SP.get(2);
	public static final X86Register SPL = GROUP_SP.get(3);

	public static final X86Register R8 = GROUP_R8.get(0);
	public static final X86Register R8D = GROUP_R8.get(1);
	public static final X86Register R8W = GROUP_R8.get(2);
	public static final X86Register R8B = GROUP_R8.get(3);

	public static final X86Register R9 = GROUP_R9.get(0);
	public static final X86Register R9D = GROUP_R9.get(1);
	public static final X86Register R9W = GROUP_R9.get(2);
	public static final X86Register R9B = GROUP_R9.get(3);

	public static final X86Register R10 = GROUP_R10.get(0);
	public static final X86Register R10D = GROUP_R10.get(1);
	public static final X86Register R10W = GROUP_R10.get(2);
	public static final X86Register R10B = GROUP_R10.get(3);

	public static final X86Register R11 = GROUP_R11.get(0);
	public static final X86Register R11D = GROUP_R11.get(1);
	public static final X86Register R11W = GROUP_R11.get(2);
	public static final X86Register R11B = GROUP_R11.get(3);

	public static final X86Register R12 = GROUP_R12.get(0);
	public static final X86Register R12D = GROUP_R12.get(1);
	public static final X86Register R12W = GROUP_R12.get(2);
	public static final X86Register R12B = GROUP_R12.get(3);

	public static final X86Register R13 = GROUP_R13.get(0);
	public static final X86Register R13D = GROUP_R13.get(1);
	public static final X86Register R13W = GROUP_R13.get(2);
	public static final X86Register R13B = GROUP_R13.get(3);

	public static final X86Register R14 = GROUP_R14.get(0);
	public static final X86Register R14D = GROUP_R14.get(1);
	public static final X86Register R14W = GROUP_R14.get(2);
	public static final X86Register R14B = GROUP_R14.get(3);

	public static final X86Register R15 = GROUP_R15.get(0);
	public static final X86Register R15D = GROUP_R15.get(1);
	public static final X86Register R15W = GROUP_R15.get(2);
	public static final X86Register R15B = GROUP_R15.get(3);

	public static final X86Register RIP = GROUP_IP.get(0);
	public static final X86Register EIP = GROUP_IP.get(1);
	public static final X86Register IP = GROUP_IP.get(2);
	
	public static final X86Register FS = makeSegment("FS");
	public static final X86Register GS = makeSegment("GS");
	public static final X86Register CS = makeSegment("CS");
	public static final X86Register DS = makeSegment("DS");
	public static final X86Register ES = makeSegment("ES");
	public static final X86Register SS = makeSegment("SS");

	/* 80-bit floating point */
	public static final X86Register ST0 = GROUP_ST.get(0);
	public static final X86Register ST1 = GROUP_ST.get(1);
	public static final X86Register ST2 = GROUP_ST.get(2);
	public static final X86Register ST3 = GROUP_ST.get(3);
	public static final X86Register ST4 = GROUP_ST.get(4);
	public static final X86Register ST5 = GROUP_ST.get(5);
	public static final X86Register ST6 = GROUP_ST.get(6);
	public static final X86Register ST7 = GROUP_ST.get(7);

	/* 128-bit integer and floating point */
	public static final X86Register XMM0 = GROUP_XMM.get(0);
	public static final X86Register XMM1 = GROUP_XMM.get(1);
	public static final X86Register XMM2 = GROUP_XMM.get(2);
	public static final X86Register XMM3 = GROUP_XMM.get(3);
	public static final X86Register XMM4 = GROUP_XMM.get(4);
	public static final X86Register XMM5 = GROUP_XMM.get(5);
	public static final X86Register XMM6 = GROUP_XMM.get(6);
	public static final X86Register XMM7 = GROUP_XMM.get(7);
	
	public static void disable(X86Register...registers) {
		for(X86Register register : registers)
			register.disable();
	}

	@SuppressWarnings("unchecked")
	public static void disable(List<X86Register>...registers) {
		for(List<X86Register> list : registers)
			for(X86Register register : list)
				register.disable();
	}
	
	/*
	 * Creates a register
	 */
	private static X86Register reg(String name, int size, List<X86Register> intersections) {
		return new X86Register(
			size,
			TYPE_MAP.get(size),
			name,
			intersections
		);
	}

	/* 
	 * Creates 5 registers using the following layout:
	 * 
	 * +---------------------------------------+
	 * |                  R?X                  |
	 * +-------------------+-------------------+
	 *                     |        E?X        |
	 *                     +---------+---------+
	 *                               |   ?X    |
	 *                               +----+----+
	 *                               | ?H | ?L |
	 *                               +----+----+
	 * 0. R?X = 64-bit
	 * 1. E?X = 32-bit
	 * 2. ?X = 16-bit
	 * 3. ?H = 8-bit
	 * 4. ?L = 8-bit                             
	 */
	private static List<X86Register> makeGPRWithLH(String name) {
		List<X86Register> intersections = new ArrayList<>();
		
		X86Register RxX = reg("R" + name + "X",	8, intersections);
		X86Register ExX = reg("E" + name + "X",	4, intersections);
		X86Register xX =  reg(name + "X",		2, intersections);
		X86Register xH =  reg(name + "H",		1, intersections);
		X86Register xL =  reg(name + "L",		1, intersections);
		
		intersections.addAll(List.of(RxX, ExX, xX, xH, xL));
		
		return Collections.unmodifiableList(intersections);
	}
	
	/* 
	 * Creates 4 registers using the following layout:
	 * 
	 * +-----------------------------------------------+
	 * |                      R??                      |
	 * +-----------------------+-----------------------+
	 *                         |          E??          |
	 *                         +-----------+-----------+
	 *                                     |    ??     |
	 *                                     +-----+-----+
	 *                                           | ??L |
	 *                                           +-----+
	 * 0. R?? = 64-bit
	 * 1. E?? = 32-bit
	 * 2. ?? = 16-bit
	 * 3. ??L = 8-bit                             
	 */
	private static List<X86Register> makeGPR(String name) {
		List<X86Register> intersections = new ArrayList<>();
		
		X86Register Rx = reg("R" + name,	8, intersections);
		X86Register Ex = reg("E" + name,	4, intersections);
		X86Register x =  reg(name,			2, intersections);
		X86Register xL = reg(name + "L",	1, intersections);
		
		intersections.addAll(List.of(Rx, Ex, x, xL));
		
		return Collections.unmodifiableList(intersections);
	}
	
	/* 
	 * Creates 4 registers using the following layout:
	 * 
	 * +-------------------------------------------------------+
	 * |                          R??                          |
	 * +---------------------------+---------------------------+
	 *                             |           R??D            |
	 *                             +-------------+-------------+
	 *                                           |    R??W     |
	 *                                           +------+------+
	 *                                                  | R??B |
	 *                                                  +------+
	 * 0. R?? = 64-bit
	 * 1. R??D = 32-bit
	 * 2. R??W = 16-bit
	 * 3. R??B = 8-bit                             
	 */
	private static List<X86Register> makeGPRNumeric(String name) {
		List<X86Register> intersections = new ArrayList<>();
		
		X86Register Rx =  reg("R" + name,		8, intersections);
		X86Register RxD = reg("R" + name + "D",	4, intersections);
		X86Register RxW = reg("R" + name + "W",	2, intersections);
		X86Register RxB = reg("R" + name + "B",	1, intersections);
		
		intersections.addAll(List.of(Rx, RxD, RxW, RxB));
		
		return Collections.unmodifiableList(intersections);
	}

	/*
	 * Creates a 2-byte segment register
	 */
	private static X86Register makeSegment(String name) {
		return reg(name, 2, List.of());
	}

	/*
	 * Creates the instruction pointers using the following layout:
	 * 
	 * +-------------------+
	 * |        RIP        |
	 * +---------+---------+
	 *           |   EIP   |
	 *           +----+----+
	 *                | IP |
	 *                +----+
	 * 
	 * 0. RIP = 64-bit
	 * 1. EIP = 32-bit
	 * 2. IP = 8-bit
	 * 
	 */
	private static List<X86Register> makeIP() {
		List<X86Register> intersections = new ArrayList<>();
		
		X86Register RIP = reg("RIP",	8, intersections);
		X86Register EIP = reg("EIP",	4, intersections);
		X86Register IP =  reg("IP",		2, intersections);
		
		intersections.addAll(List.of(RIP, EIP, IP));
		
		return Collections.unmodifiableList(intersections);
	}

	/*
	 * Creates 8 10-byte x87 register
	 */
	private static List<X86Register> makeX87() {
		final int size = Type.LDOUBLE.sizeof();
		
		return IntStream.range(0, 8)
				.mapToObj(i -> reg("ST" + i, size, List.of()))
				.toList();
	}

	/*
	 * Creates 8 16-byte XMM register
	 */
	private static List<X86Register> makeXMM() {
		return IntStream.range(0, 8)
			.mapToObj(i -> reg("XMM" + i, 16, List.of()))
			.toList();
	}
	
	private final int size;
	private final Type type;
	private final String name;
	private final List<X86Register> intersections;
	
	private boolean usable = true;
	
	private void disable() {
		usable = false;
	}
	
	public X86Register getFor(X86Size size) {
		int sz = size.getType().sizeof();
		
		for(X86Register reg : intersections)
			if(reg.size == sz && !reg.name.endsWith("H")) // prefer ?L over ?H
				return reg;
		
		return null;
	}
	
	@Override
	public boolean intersects(RegisterTarget other) {
		return intersections.contains(other);
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
