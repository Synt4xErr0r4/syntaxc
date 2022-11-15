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
package at.syntaxerror.syntaxc.generator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import at.syntaxerror.syntaxc.generator.asm.AssemblyGenerator;
import at.syntaxerror.syntaxc.generator.asm.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.FunctionMetadata;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate;
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.StringUtils;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.global.AddressInitializer;
import at.syntaxerror.syntaxc.symtab.global.GlobalVariableInitializer;
import at.syntaxerror.syntaxc.symtab.global.IntegerInitializer;
import at.syntaxerror.syntaxc.symtab.global.ListInitializer;
import at.syntaxerror.syntaxc.symtab.global.StringInitializer;
import at.syntaxerror.syntaxc.symtab.global.ZeroInitializer;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.NumericValueType;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings("preview")
public abstract class CodeGenerator implements Logable {

	@Getter
	private final List<AssemblyInstruction> instructions = new ArrayList<>();

	@Override
	public Position getPosition() {
		return null;
	}
	
	@Override
	public Warning getDefaultWarning() {
		return Warning.GEN_NONE;
	}
	
	protected final void add(AssemblyInstruction...instructions) {
		add(Arrays.asList(instructions));
	}
	
	protected final void add(List<AssemblyInstruction> instructions) {
		this.instructions.addAll(
			instructions.stream()
				.filter(i -> i != null)
				.toList()
		);
	}
	
	protected final void addAt(int index, List<AssemblyInstruction> instructions) {
		this.instructions.addAll(
			index,
			instructions.stream()
				.filter(i -> i != null)
				.toList()
		);
	}
	
	public final void generateObject(SymbolObject object) {
		if(object.isExtern())
			return;
		
		AssemblyGenerator asm = getAssembler();
		
		asm.metadata(object);
		
		if(object.isGlobalVariable()) {
			
			if(object.isInitialized())
				generateInit(asm, object.getVariableData().initializer());
			
			else asm.zero(object.getType().sizeof());
			
		}
	}
	
	public final void generateBody(List<Intermediate> intermediates, FunctionMetadata metadata) {
		AssemblyGenerator asm = getAssembler();
		
		asm.prologue(metadata);
		
		intermediates.forEach(ir -> ir.generate(asm));
		
		asm.epilogue(metadata);
	}
	
	private final int generateInit(AssemblyGenerator asm, GlobalVariableInitializer init) {
		int size;
		
		switch(init) {
		case StringInitializer strInit:
			
			StringUtils.toASCII(strInit.value(), strInit.wide())
				.stream()
				.forEach(
					strInit.withNul()
						? asm::nulString
						: asm::rawString
				);
			
			size = strInit.value()
				.getBytes(StandardCharsets.UTF_8)
				.length;
			
			if(strInit.withNul())
				size += (strInit.wide() ? NumericValueType.WCHAR : NumericValueType.CHAR).getSize();
			
			return size;
		
		case AddressInitializer addrInit:
			
			asm.pointerOffset(
				addrInit.object().getName(),
				addrInit.offset()
			);
			
			return NumericValueType.POINTER.getSize() / 8;
		
		case IntegerInitializer intInit:
			
			size = intInit.size();
			
			asm.constant(
				intInit.value(),
				size
			);
			
			return size;
			
		case ListInitializer listInit:
			
			size = 0;
			
			for(var entry : listInit.initializers())
				size += generateInit(asm, entry);
			
			return size;
			
		case ZeroInitializer zeroInit:
			size = zeroInit.size();
			
			asm.zero(size);
			
			return size;
			
		case null:
		default:
			return 0;
		}
	}
	
	public abstract AssemblyGenerator getAssembler();
	
}
