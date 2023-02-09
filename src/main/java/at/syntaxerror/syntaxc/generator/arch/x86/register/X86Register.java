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
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
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
	
	private static final Map<Integer, X86Size> TYPE_MAP = Map.of(
		1, X86Size.BYTE,
		2, X86Size.WORD,
		4, X86Size.DWORD,
		8, X86Size.QWORD,
		10, X86Size.TBYTE,
		16, X86Size.XMMWORD,
		32, X86Size.YMMWORD,
		64, X86Size.ZMMWORD
	);
	
	private static final List<List<X86Register>> XMM_REGISTERS = IntStream.range(0, 32)
			.<List<X86Register>>mapToObj(ArrayList::new)
			.toList();

	/* --- REGISTER GROUPS --- */
	
	public static final List<X86Register> GROUP_A = makeGPRWithLH("a");
	public static final List<X86Register> GROUP_B = makeGPRWithLH("b");
	public static final List<X86Register> GROUP_C = makeGPRWithLH("c");
	public static final List<X86Register> GROUP_D = makeGPRWithLH("d");
	
	public static final List<X86Register> GROUP_SI = makeGPR("si");
	public static final List<X86Register> GROUP_DI = makeGPR("di");
	public static final List<X86Register> GROUP_BP = makeGPR("bp");
	public static final List<X86Register> GROUP_SP = makeGPR("sp");
	
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
	
	public static final List<X86Register> GROUP_XMM = makeXMM('x', 16);
	public static final List<X86Register> GROUP_YMM = makeXMM('y', 32);
	public static final List<X86Register> GROUP_ZMM = makeXMM('z', 64);
	
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
	public static final X86Register XMM8 = GROUP_XMM.get(8);
	public static final X86Register XMM9 = GROUP_XMM.get(9);
	public static final X86Register XMM10 = GROUP_XMM.get(10);
	public static final X86Register XMM11 = GROUP_XMM.get(11);
	public static final X86Register XMM12 = GROUP_XMM.get(12);
	public static final X86Register XMM13 = GROUP_XMM.get(13);
	public static final X86Register XMM14 = GROUP_XMM.get(14);
	public static final X86Register XMM15 = GROUP_XMM.get(15);
	public static final X86Register XMM16 = GROUP_XMM.get(16);
	public static final X86Register XMM17 = GROUP_XMM.get(17);
	public static final X86Register XMM18 = GROUP_XMM.get(18);
	public static final X86Register XMM19 = GROUP_XMM.get(19);
	public static final X86Register XMM20 = GROUP_XMM.get(20);
	public static final X86Register XMM21 = GROUP_XMM.get(21);
	public static final X86Register XMM22 = GROUP_XMM.get(22);
	public static final X86Register XMM23 = GROUP_XMM.get(23);
	public static final X86Register XMM24 = GROUP_XMM.get(24);
	public static final X86Register XMM25 = GROUP_XMM.get(25);
	public static final X86Register XMM26 = GROUP_XMM.get(26);
	public static final X86Register XMM27 = GROUP_XMM.get(27);
	public static final X86Register XMM28 = GROUP_XMM.get(28);
	public static final X86Register XMM29 = GROUP_XMM.get(29);
	public static final X86Register XMM30 = GROUP_XMM.get(30);
	public static final X86Register XMM31 = GROUP_XMM.get(31);

	/* 256-bit integer and floating point */
	public static final X86Register YMM0 = GROUP_YMM.get(0);
	public static final X86Register YMM1 = GROUP_YMM.get(1);
	public static final X86Register YMM2 = GROUP_YMM.get(2);
	public static final X86Register YMM3 = GROUP_YMM.get(3);
	public static final X86Register YMM4 = GROUP_YMM.get(4);
	public static final X86Register YMM5 = GROUP_YMM.get(5);
	public static final X86Register YMM6 = GROUP_YMM.get(6);
	public static final X86Register YMM7 = GROUP_YMM.get(7);
	public static final X86Register YMM8 = GROUP_YMM.get(8);
	public static final X86Register YMM9 = GROUP_YMM.get(9);
	public static final X86Register YMM10 = GROUP_YMM.get(10);
	public static final X86Register YMM11 = GROUP_YMM.get(11);
	public static final X86Register YMM12 = GROUP_YMM.get(12);
	public static final X86Register YMM13 = GROUP_YMM.get(13);
	public static final X86Register YMM14 = GROUP_YMM.get(14);
	public static final X86Register YMM15 = GROUP_YMM.get(15);
	public static final X86Register YMM16 = GROUP_YMM.get(16);
	public static final X86Register YMM17 = GROUP_YMM.get(17);
	public static final X86Register YMM18 = GROUP_YMM.get(18);
	public static final X86Register YMM19 = GROUP_YMM.get(19);
	public static final X86Register YMM20 = GROUP_YMM.get(20);
	public static final X86Register YMM21 = GROUP_YMM.get(21);
	public static final X86Register YMM22 = GROUP_YMM.get(22);
	public static final X86Register YMM23 = GROUP_YMM.get(23);
	public static final X86Register YMM24 = GROUP_YMM.get(24);
	public static final X86Register YMM25 = GROUP_YMM.get(25);
	public static final X86Register YMM26 = GROUP_YMM.get(26);
	public static final X86Register YMM27 = GROUP_YMM.get(27);
	public static final X86Register YMM28 = GROUP_YMM.get(28);
	public static final X86Register YMM29 = GROUP_YMM.get(29);
	public static final X86Register YMM30 = GROUP_YMM.get(30);
	public static final X86Register YMM31 = GROUP_YMM.get(31);

	/* 512-bit integer and floating point */
	public static final X86Register ZMM0 = GROUP_ZMM.get(0);
	public static final X86Register ZMM1 = GROUP_ZMM.get(1);
	public static final X86Register ZMM2 = GROUP_ZMM.get(2);
	public static final X86Register ZMM3 = GROUP_ZMM.get(3);
	public static final X86Register ZMM4 = GROUP_ZMM.get(4);
	public static final X86Register ZMM5 = GROUP_ZMM.get(5);
	public static final X86Register ZMM6 = GROUP_ZMM.get(6);
	public static final X86Register ZMM7 = GROUP_ZMM.get(7);
	public static final X86Register ZMM8 = GROUP_ZMM.get(8);
	public static final X86Register ZMM9 = GROUP_ZMM.get(9);
	public static final X86Register ZMM10 = GROUP_ZMM.get(10);
	public static final X86Register ZMM11 = GROUP_ZMM.get(11);
	public static final X86Register ZMM12 = GROUP_ZMM.get(12);
	public static final X86Register ZMM13 = GROUP_ZMM.get(13);
	public static final X86Register ZMM14 = GROUP_ZMM.get(14);
	public static final X86Register ZMM15 = GROUP_ZMM.get(15);
	public static final X86Register ZMM16 = GROUP_ZMM.get(16);
	public static final X86Register ZMM17 = GROUP_ZMM.get(17);
	public static final X86Register ZMM18 = GROUP_ZMM.get(18);
	public static final X86Register ZMM19 = GROUP_ZMM.get(19);
	public static final X86Register ZMM20 = GROUP_ZMM.get(20);
	public static final X86Register ZMM21 = GROUP_ZMM.get(21);
	public static final X86Register ZMM22 = GROUP_ZMM.get(22);
	public static final X86Register ZMM23 = GROUP_ZMM.get(23);
	public static final X86Register ZMM24 = GROUP_ZMM.get(24);
	public static final X86Register ZMM25 = GROUP_ZMM.get(25);
	public static final X86Register ZMM26 = GROUP_ZMM.get(26);
	public static final X86Register ZMM27 = GROUP_ZMM.get(27);
	public static final X86Register ZMM28 = GROUP_ZMM.get(28);
	public static final X86Register ZMM29 = GROUP_ZMM.get(29);
	public static final X86Register ZMM30 = GROUP_ZMM.get(30);
	public static final X86Register ZMM31 = GROUP_ZMM.get(31);
	
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
			TYPE_MAP.get(size)
				.getType(),
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
		
		X86Register RxX = reg("r" + name + "x",	8, intersections);
		X86Register ExX = reg("e" + name + "x",	4, intersections);
		X86Register xX =  reg(name + "x",		2, intersections);
		X86Register xH =  reg(name + "h",		1, intersections);
		X86Register xL =  reg(name + "l",		1, intersections);
		
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
		
		X86Register Rx = reg("r" + name,	8, intersections);
		X86Register Ex = reg("e" + name,	4, intersections);
		X86Register x =  reg(name,			2, intersections);
		X86Register xL = reg(name + "l",	1, intersections);
		
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
		
		X86Register Rx =  reg("r" + name,		8, intersections);
		X86Register RxD = reg("r" + name + "d",	4, intersections);
		X86Register RxW = reg("r" + name + "w",	2, intersections);
		X86Register RxB = reg("r" + name + "n",	1, intersections);
		
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
		
		X86Register RIP = reg("rip",	8, intersections);
		X86Register EIP = reg("eip",	4, intersections);
		X86Register IP =  reg("ip",		2, intersections);
		
		intersections.addAll(List.of(RIP, EIP, IP));
		
		return Collections.unmodifiableList(intersections);
	}

	/*
	 * Creates 8 10-byte x87 register
	 */
	private static List<X86Register> makeX87() {
		return IntStream.range(0, 8)
			.mapToObj(i -> reg("st(" + i + ")", 10, List.of()))
			.toList();
	}

	/*
	 * Creates 32 n-byte ?MM register
	 */
	private static List<X86Register> makeXMM(char prefix, int bytes) {
		return IntStream.range(0, 32)
			.mapToObj(i -> {
				List<X86Register> intersections = XMM_REGISTERS.get(i);
				
				X86Register reg = reg(prefix + "mm" + i, bytes, intersections);
				
				intersections.add(reg);
				
				return reg;
			})
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
	
	public X86Register resized(X86Size size) {
		int sz = size.getType().sizeof();
		
		if(sz == this.size)
			return this;
		
		for(X86Register reg : intersections)
			if(reg.size == sz && !reg.name.endsWith("h")) // prefer ?l over ?h
				return reg;
		
		return null;
	}
	
	@Override
	public AssemblyTarget resized(Type type) {
		return resized(X86Size.of(type));
	}
	
	@Override
	public boolean intersects(RegisterTarget other) {
		return intersections.contains(other);
	}
	
	public String toAssemblyString(boolean attSyntax) {
		return (attSyntax ? "%" : "") + name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
