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

import at.syntaxerror.syntaxc.intermediate.representation.BinaryIntermediate.BinaryOperation;
import at.syntaxerror.syntaxc.type.Type;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * operand used to optimize jump intermediates by reusing
 * already set CPU flags instead of recalculating them
 * 
 * @author Thomas Kasper
 */
@RequiredArgsConstructor
@Getter
public class ConditionOperand implements Operand {
	
	private final BinaryOperation operation;
	private final Type type;
	
	@Override
	public boolean equals(Operand other) {
		return false;
	}
	
	@Override
	public String toString() {
		return "<condition>";
	}

}
