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
import at.syntaxerror.syntaxc.generator.arch.Alignment;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.generator.arch.x86.asm.X86Assembly;
import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86InstructionKinds;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.ObjectSerializer;
import at.syntaxerror.syntaxc.generator.asm.insn.AssemblyInstruction;
import at.syntaxerror.syntaxc.misc.StringUtils;
import at.syntaxerror.syntaxc.symtab.Linkage;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.global.IntegerInitializer;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class X86ObjectSerializer extends ObjectSerializer {

	private static final Map<Integer, String> WORD_SIZES = Map.of(
		1, "byte",
		2, "word",
		4, "long",
		8, "quad"
	);
	
	private static final Map<Integer, NumericValueType> SIZE_TYPES = new HashMap<>();
	private static final List<Integer> SIZES = new ArrayList<>();
	
	private static String pointerDirective;
	
	private static void init() {
		if(!SIZES.isEmpty())
			return;
		
		NumericValueType[] types = {
			NumericValueType.UNSIGNED_CHAR,
			NumericValueType.UNSIGNED_SHORT,
			NumericValueType.UNSIGNED_INT,
			NumericValueType.UNSIGNED_LONG
		};
		
		for(NumericValueType type : types)
			SIZE_TYPES.put(type.getSize(), type);
		
		SIZES.addAll(SIZE_TYPES.keySet());
		
		Collections.sort(SIZES, Comparator.reverseOrder());
		
		SIZES.forEach(sz -> {
			if(!WORD_SIZES.containsKey(sz))
				throw new SyntaxCException("Illegal size " + sz + " does not have a corresponding assembly directive");
		});
		
		pointerDirective = "." + WORD_SIZES.get(NumericValueType.POINTER.getSize());
	}

	private final Alignment alignment;
	private final X86FloatTable floatTable;
	private final X86Assembly x86;
	private final String inputFileName;
	
	private X86SegmentKind activeSegment;
	
	{
		init();
	}
	
	private void setSegment(X86SegmentKind segment) {
		if(activeSegment == segment)
			return;
		
		activeSegment = segment;
		
		asm("." + segment);
	}
	
	private void asm(String format, Object...args) {
		asm.add(new RawAssemblyInstruction(asm, "\t" + format.formatted(args)));
	}
	
	private void label(String label) {
		asm.add(new RawAssemblyInstruction(asm, label + ":"));
	}

	@Override
	public void fileBegin() {
		asm(
			".file \"%s\"",
			StringUtils.toASCII(inputFileName, false)
				.stream()
				.reduce("", String::concat)
				.replace("\"", "\\\"")
		);
		
		if(x86.intelSyntax)
			asm(".intel_syntax noprefix");
	}

	@Override
	public void fileEnd() {
		floatTable.getFloats()
			.forEach((type, floats) ->
				floats.forEach((val, id) -> {
					
					int size = type.sizeof();
					String name = ".F" + id;
					
					setSegment(X86SegmentKind.TEXT);
					basicMetadata(name, size, type);
					label(name);
					generateInit(new IntegerInitializer(val, size));
				})
			);
	}

	private void basicMetadata(String name, int size, Type type) {
		if(size < 1)
			throw new SyntaxCException("Illegal non-positive size for variable " + name);
		
		int align = ArchitectureRegistry.getAlignment();
		
		if(align < 0)
			align = alignment.getAlignment(type);
		
		if(align != 0)
			asm(".align %d", align);
		
		asm(".size %s, %d", name, size);
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
				? X86SegmentKind.TEXT
				: initialized
					? X86SegmentKind.DATA
					: X86SegmentKind.BSS
		);
		
		asm(
			global
				? ".globl %s"
				: ".local %s",
			name
		);
		
		if(!function) {
			basicMetadata(name, size, object.getType());

			asm(".type %s, @object", name);
		}
		else asm(".type %s, @function", name);
		
		label(name);
	}

	@Override
	public void generatePostFunction(SymbolObject object) {
		asm(".size %1$s, .-%1$s", object.getName());
	}
	
	@Override
	public void stringNulTerminated(String value) {
		asm(".string \"%s\"", value.replace("\"", "\\\""));
	}

	@Override
	public void stringRaw(String value) {
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
		if(SIZES.contains(size)) {
			asm(
				".%s %s",
				WORD_SIZES.get(size),
				SIZE_TYPES.get(size)
					.mask(value)
			);
			return;
		}
		
		outer:
		while(size > 0) {
			for(int sz : SIZES)
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
	
	public static class RawAssemblyInstruction extends AssemblyInstruction {
		
		private final String strrep;
		
		public RawAssemblyInstruction(Instructions parent, String strrep) {
			super(parent, X86InstructionKinds.RAW, List.of(), List.of());
			
			this.strrep = strrep;
		}
		
		@Override
		public String toString() {
			return strrep;
		}
		
	}

}
