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

import at.syntaxerror.syntaxc.SyntaxCException;
import at.syntaxerror.syntaxc.generator.CodeGenerator;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.generator.asm.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.AssemblySegmentKind;
import at.syntaxerror.syntaxc.misc.StringUtils;
import at.syntaxerror.syntaxc.type.NumericValueType;

/**
 * @author Thomas Kasper
 * 
 */
public class X86CodeGenerator extends CodeGenerator {

	private static final Map<Integer, String> WORD_SIZES = Map.of(
		1, "byte",
		2, "word",
		4, "long",
		8, "quad"
	);
	
	private final Map<Integer, NumericValueType> sizeTypes = new HashMap<>();
	private final List<Integer> sizes;
	
	private final String pointerDirective = "." + WORD_SIZES.get(NumericValueType.POINTER.getSize());

	private final X86Assembly x86 = new X86Assembly(ArchitectureRegistry.getBitSize());
	
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
	
	@Override
	public List<AssemblyInstruction> generateBegin() {
		return List.of(
			asm(
				".file \"%s\"",
				StringUtils.toASCII(inputFileName, false)
					.stream()
					.reduce("", String::concat)
					.replace("\"", "\\\"")
			)
		);
	}
	
	private List<AssemblyInstruction> setSegment(AssemblySegmentKind segment) {
		if(activeSegment == segment)
			return List.of();
		
		activeSegment = segment;
		
		return List.of(asm("." + segment));
	}
	
	@Override
	public List<AssemblyInstruction> asmVariable(String name, int size, boolean external, boolean function,
			boolean initialized, boolean readOnly) {
		
		List<AssemblyInstruction> instructions = new ArrayList<>();
		
		instructions.addAll(setSegment(
			function || readOnly
				? AssemblySegmentKind.TEXT
				: initialized
					? AssemblySegmentKind.DATA
					: AssemblySegmentKind.BSS
		));
		
		if(external)
			instructions.add(asm(".globl " + name));
		
		if(!function) {
			
			if(size < 1)
				throw new SyntaxCException("Illegal non-positive size for variable " + name);
			
			int align = 0;
			
			if(size == 2 || size == 4 || size == 8)
				align = size;
			
			else if(size > 8)
				align = size <= 16 ? 16 : 32;
			
			if(align != 0)
				instructions.add(asm(".align %d", align));
			
			instructions.add(asm(".type %s, @object", name));
			instructions.add(asm(".size %s, %d", name, size));
		}
		else instructions.add(asm(".type %s, @function", name));
		
		instructions.add(asmLabel(name));
		
		if(!function && !initialized)
			instructions.add(asmZero(size));
		
		return instructions;
	}
	
	
	@Override
	public AssemblyInstruction asmLabel(String label) {
		return () -> label + ":";
	}
	

	@Override
	public AssemblyInstruction asmZero(int n) {
		return asm(".zero %d", n);
	}

	@Override
	public AssemblyInstruction asmString(String string) {
		return asm(".ascii \"%s\"", string.replace("\"", "\\\""));
	}

	@Override
	public AssemblyInstruction asmNulString(String string) {
		return asm(".string \"%s\"", string.replace("\"", "\\\""));
	}

	@Override
	public AssemblyInstruction asmPointer(String label, BigInteger offset) {
		if(offset == null || offset.compareTo(BigInteger.ZERO) == 0)
			return asm("%s %s", pointerDirective, label);
		
		return asm("%s %s+%s", pointerDirective, label, offset);
	}
	
	@Override
	public List<AssemblyInstruction> asmConstant(BigInteger value, int size) {
		
		if(WORD_SIZES.containsKey(size))
			return List.of(
				asm(".%s %s", WORD_SIZES.get(size), sizeTypes.get(size).mask(value))
			);
		
		List<AssemblyInstruction> instructions = new ArrayList<>();
		
		outer:
		while(size > 0) {
			
			instructions.add(asm("."));
			
			for(int sz : sizes)
				if(sz >= size) {
					
					size -= sz;
					
					// this would be different on big endian systems, but x86 is always little endian
					
					instructions.addAll(asmConstant(value, sz));
					
					value = value.shiftRight(sz << 3);
					
					continue outer;
				}
			
			throw new SyntaxCException("Missing mandatory single byte type");
		}
		
		return instructions;
	}
	
	@Override
	public List<AssemblyInstruction> asmFunctionPrologue() {
		/* Stack frame setup:
		 * 
		 * x64:
		 *   pushq %rbp
		 *   movq %rsp, %rbp
		 * 
		 * x86:
		 *   pushl %ebp
		 *   movl %esp, %ebp
		 */
		
		return List.of(
			x86.push(x86.RBP),
			x86.mov(x86.RSP, x86.RBP)
		);
	}
	
	@Override
	public List<AssemblyInstruction> asmFunctionEpilogue() {
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
		
		return List.of(
			x86.pop(x86.RBP),
			x86.ret()
		);
	}
	
	protected static AssemblyInstruction asm(String format, Object...args) {
		final String asm = "\t" + format.formatted(args);
		
		return () -> asm;
	}

}
