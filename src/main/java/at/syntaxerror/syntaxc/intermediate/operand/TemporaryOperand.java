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
package at.syntaxerror.syntaxc.intermediate.operand;

import at.syntaxerror.syntaxc.generator.alloc0.Allocation;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.type.Type;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * temporarily allocated memory, typically using a register
 * 
 * @author Thomas Kasper
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TemporaryOperand implements Operand {

	public static final long RETURN_VALUE_ID = -1;
	
	public static TemporaryOperand forReturnValue(Type type) {
		return new TemporaryOperand(RETURN_VALUE_ID, type);
	}

	private final long id;
	private final Type type;
	
	@Setter
	private Allocation allocation;
	
	public TemporaryOperand(Type type) {
		this(SymbolObject.getNextTemporaryId(), type.normalize());
	}
	
	@Override
	public boolean equals(Operand other) {
		return other instanceof TemporaryOperand temp
			&& id == temp.id;
	}
	
	@Override
	public String toString() {
		if(id == RETURN_VALUE_ID)
			return "_RV";
		
		return "_" + id;
	}
	
}
