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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import at.syntaxerror.syntaxc.SyntaxCException;
import at.syntaxerror.syntaxc.generator.CodeGenerator;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.generator.asm.AssemblyGenerator;
import at.syntaxerror.syntaxc.generator.asm.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.AssemblySegmentKind;
import at.syntaxerror.syntaxc.generator.asm.FunctionMetadata;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyInteger;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyLabel;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.ConstantOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.GlobalOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.LocalOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.Operand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.ReturnValueOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.TemporaryOperand;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.StringUtils;
import at.syntaxerror.syntaxc.symtab.Linkage;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public class X86CodeGenerator extends CodeGenerator implements AssemblyGenerator {

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
	
	private final String inputFileName;
	
	private AssemblySegmentKind activeSegment;
	
	public X86CodeGenerator(String inputFileName) {
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
	public AssemblyGenerator getAssembler() {
		return this;
	}
	
	@Override
	public AssemblyTarget allocate(int id, Type type) {
		return x86.allocate(id, type);
	}
	
	@Override
	public AssemblyTarget free(int id) {
		return x86.free(id);
	}
	
	private AssemblyLabel labelTarget(Type type, String name) {
		return new AssemblyLabel() {
			
			@Override
			public Type getType() {
				return type;
			}
			
			@Override
			public String getName() {
				return name;
			}
			
		};
	}
	
	private AssemblyLabel pointerLabelTarget(String name) {
		return labelTarget(
			NumericValueType.POINTER.asType(),
			name
		);
	}
	
	@Override
	public AssemblyTarget target(Operand operand) {
		if(operand == null)
			return null;
		
		if(operand instanceof GlobalOperand global) {

			String name = global.getName();
			
			// external functions: use Procedure Linkage Table (PLT)
			if(global.isExtern())
				return pointerLabelTarget(name + "@PLT");
			
			return pointerLabelTarget(name);
		}
		
		if(operand instanceof LocalOperand local) {
			
		}
		
		if(operand instanceof ReturnValueOperand retval) {
			Type type = operand.getType();
			
			if(type.isScalar()) {
				
				if(type.isFloating()) {
					
				}
				
			}

			return X86Register.RAX; // TODO	
		}
		
		if(operand instanceof TemporaryOperand temp)
			return allocate(temp.getId(), temp.getType());
		
		if(operand instanceof ConstantOperand constant) {
			Type type = constant.getType();
			
			if(type.isFloating())
				return floatTable.get(
					(BigDecimal) constant.getValue(),
					type
				);
			
			return new AssemblyInteger() {
				
				@Override
				public Type getType() {
					return type;
				}
				
				@Override
				public BigInteger getValue() {
					return (BigInteger) constant.getValue();
				}
				
			};	
		}
		
		Logger.error("Unrecognized operand");
		return null;
	}
	
	@Override
	public void begin() {
		x86 = new X86Assembly(
			ArchitectureRegistry.getArchitecture()
				.getSyntax()
				.equals("intel"),
			ArchitectureRegistry.getBitSize()
		);
		
		String file = "\t.file \""
			+ StringUtils.toASCII(inputFileName, false)
				.stream()
				.reduce("", String::concat)
				.replace("\"", "\\\"")
			+ '"';

		add(() -> file);
		
		if(x86.intelSyntax)
			add(() -> "\t.intel_syntax noprefix");
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
		
		String name = object.getName();
		
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
			
			int align = 0;
			
			if(size == 2 || size == 4 || size == 8)
				align = size;
			
			else if(size > 8)
				align = size <= 16 ? 16 : 32;
			
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
		
		add(
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
		
		add(
			x86.pop(x86.RBP),
			x86.ret()
		);
		
		asm(".size %1$s, .-%1$s", metadata.name());
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
			
			asm(".");
			
			for(int sz : sizes)
				if(sz >= size) {
					
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
	
	private void switchArithmetic(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right,
			BiFunction<AssemblyTarget, AssemblyTarget, AssemblyInstruction> opInt,
			BiFunction<AssemblyTarget, AssemblyTarget, AssemblyInstruction> opFloat,
			BiFunction<AssemblyTarget, AssemblyTarget, AssemblyInstruction> opDouble,
			BiFunction<AssemblyTarget, AssemblyTarget, AssemblyInstruction> opLongDouble) {
		
		Type type = result.getType();
		
		if(!type.isScalar())
			Logger.error("Illegal result target for scalar operation");
		
		NumericValueType num;
		
		if(type.isPointerLike())
			num = NumericValueType.POINTER;
		
		else num = type.toNumber().getNumericType();
		
		AssemblyTarget temp = left; // TODO
		
		System.out.println(temp);
		
		add(
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
		add(x86.mov(result, value));
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
		
		add(
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
	public void jump(AssemblyTarget condition, String name) {
		add(
			x86.test(condition),
			x86.je(name)
		);
	}
	
	@Override
	public void jump(String name) {
		add(x86.jmp(name));
	}
	
	@Override
	public void label(String name) {
		add(() -> name + ":");
	}
	
	private void asm(String format, Object...args) {
		final String asm = "\t" + format.formatted(args);
		
		add(() -> asm);
	}

}
