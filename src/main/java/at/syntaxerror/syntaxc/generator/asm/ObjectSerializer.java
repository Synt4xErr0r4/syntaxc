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
package at.syntaxerror.syntaxc.generator.asm;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import at.syntaxerror.syntaxc.misc.StringUtils;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.global.AddressInitializer;
import at.syntaxerror.syntaxc.symtab.global.GlobalVariableInitializer;
import at.syntaxerror.syntaxc.symtab.global.IntegerInitializer;
import at.syntaxerror.syntaxc.symtab.global.ListInitializer;
import at.syntaxerror.syntaxc.symtab.global.StringInitializer;
import at.syntaxerror.syntaxc.symtab.global.ZeroInitializer;
import at.syntaxerror.syntaxc.type.NumericValueType;

/**
 * @author Thomas Kasper
 * 
 */
public abstract class ObjectSerializer {

	protected final Instructions asm = new Instructions();

	public abstract void fileBegin();
	public abstract void fileEnd();
	
	public abstract void metadata(SymbolObject object);
	
	public abstract void stringNulTerminated(String value);
	public abstract void stringRaw(String value);
	public abstract void pointerOffset(String label, BigInteger offset);
	public abstract void constant(BigInteger value, int size);
	public abstract void zero(int size);
	
	public final void transfer(List<AssemblyInstruction> insns) {
		asm.forEach(insns::add);
		asm.clear();
	}
	
	@SuppressWarnings("preview")
	public final int generateInit(GlobalVariableInitializer init) {
		int size;
		
		switch(init) {
		case StringInitializer strInit:
			
			StringUtils.toASCII(strInit.value(), strInit.wide())
				.stream()
				.forEach(
					strInit.withNul()
						? this::stringNulTerminated
						: this::stringRaw
				);
			
			size = strInit.value()
				.getBytes(StandardCharsets.UTF_8)
				.length;
			
			if(strInit.withNul())
				size += (strInit.wide() ? NumericValueType.WCHAR : NumericValueType.CHAR).getSize();
			
			return size;
		
		case AddressInitializer addrInit:
			
			pointerOffset(
				addrInit.object().getName(),
				addrInit.offset()
			);
			
			return NumericValueType.POINTER.getSize() / 8;
		
		case IntegerInitializer intInit:
			
			size = intInit.size();
			
			constant(
				intInit.value(),
				size
			);
			
			return size;
			
		case ListInitializer listInit:
			
			size = 0;
			
			for(var entry : listInit.initializers())
				size += generateInit(entry);
			
			return size;
			
		case ZeroInitializer zeroInit:
			size = zeroInit.size();
			
			zero(size);
			
			return size;
			
		case null:
		default:
			return 0;
		}
	}
	
	public void generatePostFunction(SymbolObject object) { }
	
}
