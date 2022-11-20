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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import at.syntaxerror.syntaxc.SyntaxCException;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.generator.asm.AssemblyGenerator;
import at.syntaxerror.syntaxc.generator.asm.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.AssemblySegmentKind;
import at.syntaxerror.syntaxc.generator.asm.FunctionMetadata;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.AlignmentUtils;
import at.syntaxerror.syntaxc.misc.StringUtils;
import at.syntaxerror.syntaxc.symtab.Linkage;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public class X86AssemblyGenerator extends AssemblyGenerator {

	private static final Map<Integer, String> WORD_SIZES = Map.of(
		1, "byte",
		2, "word",
		4, "long",
		8, "quad"
	);
	
	private final Map<Integer, NumericValueType> sizeTypes = new HashMap<>();
	private final List<Integer> sizes;
	
	private final String pointerDirective = "." + WORD_SIZES.get(NumericValueType.POINTER.getSize());

	private final X86FloatTable floatTable = new X86FloatTable();
	
	private X86Assembly x86;
	
	private AssemblySegmentKind activeSegment;
	
	private final X86CodeGenerator gen;
	private final String inputFileName;
	
	public X86AssemblyGenerator(X86CodeGenerator gen, String inputFileName) {
		this.gen = gen;
		this.inputFileName = inputFileName;
		
		NumericValueType[] types = {
			NumericValueType.SIGNED_CHAR,
			NumericValueType.SIGNED_SHORT,
			NumericValueType.SIGNED_INT,
			NumericValueType.SIGNED_LONG
		};
		
		for(NumericValueType type : types)
			sizeTypes.put(type.getSize(), type);
		
		sizes = new ArrayList<>(sizeTypes.keySet());
		
		Collections.sort(sizes, Comparator.reverseOrder());
		
		sizes.forEach(sz -> {
			if(!WORD_SIZES.containsKey(sz))
				throw new SyntaxCException("Illegal size " + sz + " does not have a corresponding assembly directive");
		});
	}
	
	private void setSegment(AssemblySegmentKind segment) {
		if(activeSegment == segment)
			return;
		
		activeSegment = segment;
		
		asm("." + segment);
	}

	@Override
	public AssemblyTarget getReturnValueTarget(Type type) {
		
		return x86.RAX;
	}

	@Override
	public void begin() {
		x86 = new X86Assembly(
			ArchitectureRegistry.getArchitecture()
				.getSyntax()
				.equals("intel"),
			ArchitectureRegistry.getBitSize()
		);
		
		gen.getRegisterProvider().setX86(x86);
		
		String file = "\t.file \""
			+ StringUtils.toASCII(inputFileName, false)
				.stream()
				.reduce("", String::concat)
				.replace("\"", "\\\"")
			+ '"';

		gen.add(() -> file);
		
		if(x86.intelSyntax)
			gen.add(() -> "\t.intel_syntax noprefix");
	}

	@Override
	public void end() {
		
	}

	@Override
	public void metadata(SymbolObject object) {
		boolean function = object.isFunction();
		boolean readOnly = object.getType().isConst();
		boolean initialized = object.isInitialized();
		boolean global = object.getLinkage() == Linkage.EXTERNAL;
		
		int size = object.getType().sizeof();
		
		String name = object.getFullName();
		
		setSegment(
			function || readOnly
				? AssemblySegmentKind.TEXT
				: initialized
					? AssemblySegmentKind.DATA
					: AssemblySegmentKind.BSS
		);
		
		asm(
			global
				? ".globl %s"
				: ".local %s",
			name
		);
		
		if(!function) {
			
			if(size < 1)
				throw new SyntaxCException("Illegal non-positive size for variable " + name);
			
			int align = ArchitectureRegistry.getAlignment();
			
			if(align < 0)
				align = AlignmentUtils.align(size);
			
			if(align != 0)
				asm(".align %d", align);
			
			asm(".type %s, @object", name);
			asm(".size %s, %d", name, size);
		}
		else asm(".type %s, @function", name);
		
		label(name);	
	}

	@Override
	public void prologue(FunctionMetadata metadata) {
		/* Stack frame setup:
		 * 
		 * x64:
		 *   pushq %rbp
		 *   movq %rsp, %rbp
		 *   
		 *   push rbp
		 *   mov rbp, rsp
		 * 
		 * x86:
		 *   pushl %ebp
		 *   movl %esp, %ebp
		 *   
		 *   push ebp
		 *   mov ebp, esp
		 */
		
		// TODO subq $STACK_OFFSET, %rsp
		
		gen.add(
			x86.push(x86.RBP),
			x86.mov(x86.RBP, x86.RSP)
		);
	}

	@Override
	public void epilogue(FunctionMetadata metadata) {
		/* Stack frame restore:
		 * 
		 * x64:
		 *   popq %rbp
		 *   ret
		 *   
		 * x86:
		 *   popl %ebp
		 *   ret
		 */
		
		// TODO addq $STACK_OFFSET, %rsp
		
		gen.add(
			x86.pop(x86.RBP),
			x86.ret()
		);
		
		asm(".size %1$s, .-%1$s", metadata.object().getFullName());
	}

	@Override
	public void nulString(String value) {
		asm(".string \"%s\"", value.replace("\"", "\\\""));
		
	}

	@Override
	public void rawString(String value) {
		asm(".ascii \"%s\"", value.replace("\"", "\\\""));
	}

	@Override
	public void pointerOffset(String label, BigInteger offset) {
		if(offset == null || offset.compareTo(BigInteger.ZERO) == 0)
			asm("%s %s", pointerDirective, label);
		
		else asm("%s %s+%s", pointerDirective, label, offset);
	}

	@Override
	public void constant(BigInteger value, int size) {
		if(WORD_SIZES.containsKey(size)) {
			asm(
				".%s %s",
				WORD_SIZES.get(size),
				sizeTypes.get(size)
					.mask(value)
			);
			return;
		}
		
		outer:
		while(size > 0) {
			for(int sz : sizes)
				if(size >= sz) {
					
					size -= sz;
					
					// this would be different on big endian systems, but x86 is always little endian
					
					constant(value, sz);
					
					value = value.shiftRight(sz << 3);
					
					continue outer;
				}
			
			throw new SyntaxCException("Missing mandatory single byte type");
		}
	}

	@Override
	public void zero(int size) {
		asm(".zero %d", size);
	}

	@Override
	public void memcpy(AssemblyTarget dst, AssemblyTarget src, int dstOffset, int srcOffset, int length) {
		// if(!dst.isSD()) ;
		// if(!src.isSI()) ;
		
		gen.add(
			x86.mov(X86Register.EDI, dst), // TODO
			x86.mov(X86Register.ESI, src), // TODO
			x86.mov(X86Register.ECX, length)
		);
		asm("rep movsb");
	}

	@Override
	public void memset(AssemblyTarget dst, int offset, int length, int value) {
		gen.add(
			x86.mov(X86Register.EDI, dst), // TODO
			x86.mov(X86Register.ECX, length),
			x86.mov(X86Register.AL, value)
		);
		asm("rep stosb");
	}
	
	private void switchArithmetic(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right,
			BiFunction<AssemblyTarget, AssemblyTarget, AssemblyInstruction> opInt,
			BiFunction<AssemblyTarget, AssemblyTarget, AssemblyInstruction> opFloat,
			BiFunction<AssemblyTarget, AssemblyTarget, AssemblyInstruction> opDouble,
			BiFunction<AssemblyTarget, AssemblyTarget, AssemblyInstruction> opLongDouble) {
		
		if(opInt != opFloat) // XXX
			return;
		
		Type type = result.getType();
		
		if(!type.isScalar())
			Logger.error("Illegal result target for scalar operation");
		
		NumericValueType num;
		
		if(type.isPointerLike())
			num = NumericValueType.POINTER;
		
		else num = type.toNumber().getNumericType();
		
		AssemblyTarget temp = left; // TODO
		
		gen.add(
			x86.mov(temp, left),
			
			(switch(num) {
			case FLOAT -> opFloat;
			case DOUBLE -> opDouble;
			case LDOUBLE -> opLongDouble;
			default -> opInt;
			}).apply(temp, right),
			
			x86.mov(result, temp)
		);
	}
	
	@Override
	public void add(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		switchArithmetic(
			result, left, right,
			x86::add,
			x86::addss,
			x86::addsd,
			(dst, src) -> {
				
				return () -> "nop ; ldouble + ldouble";
			}
		);
	}
	
	@Override
	public void subtract(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void multiply(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void divide(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void modulo(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void bitwiseAnd(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void bitwiseOr(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void bitwiseXor(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void shiftLeft(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void shiftRight(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void logicalAnd(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void logicalOr(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void equal(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void notEqual(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void greater(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void greaterEqual(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void less(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void lessEqual(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void assign(AssemblyTarget result, AssemblyTarget value) {
		gen.add(x86.mov(result, value));
	}
	
	@Override
	public void bitwiseNot(AssemblyTarget result, AssemblyTarget value) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void minus(AssemblyTarget result, AssemblyTarget value) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addressOf(AssemblyTarget result, AssemblyTarget value) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void indirection(AssemblyTarget result, AssemblyTarget value) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void call(AssemblyTarget result, AssemblyTarget target, List<AssemblyTarget> args) {
		/*
		 * TODO support different calling conventions
		 * 
		 * currently supported: System V AMD64 ABI
		 * 
		 *   arguments:
		 *    - ints 1-6:   RDI, RSI, RDX, RCX, R8, R9
		 *    - floats 1-8: XMM0-7
		 *    - additional: stack
		 *    - structs:
		 *      - > 
		 *   
		 *   return value:
		 *    - ints: RAX [+RDX]
		 *    - float/double: XMM0, XMM1
		 *    - long double: ST0, ST1
		 * 
		 *   callee saved:
		 *    - R12-R15
		 *    - RBX
		 *    - RBP
		 *   
		 *   caller saved:
		 *    - all other registers
		 *   
		 *   variadic:
		 *    - number of floats stored in AL
		 * 
		 */
		
		gen.add(
			x86.mov(X86Register.EAX, 0),
			x86.call(target)
		);
	}
	
	@Override
	public void cast(AssemblyTarget result, AssemblyTarget value, boolean resultFloat, boolean valueFloat) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void member(AssemblyTarget result, AssemblyTarget value, int offset, int bitOffset, int bitWidth) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void arrayIndex(AssemblyTarget result, AssemblyTarget target, AssemblyTarget index) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void jump(AssemblyTarget condition, String name) {
		gen.add(
			x86.test(condition),
			x86.je(name)
		);
	}
	
	@Override
	public void jump(String name) {
		gen.add(x86.jmp(name));
	}
	
	@Override
	public void label(String name) {
		gen.add(() -> name + ":");
	}
	
	private void asm(String format, Object...args) {
		final String asm = "\t" + format.formatted(args);
		
		gen.add(() -> asm);
	}

}
