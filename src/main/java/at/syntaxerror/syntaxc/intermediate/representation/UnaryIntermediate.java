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
package at.syntaxerror.syntaxc.intermediate.representation;

import at.syntaxerror.syntaxc.intermediate.operand.Operand;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.Type;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Intermediate representation of addition ('a+b')
 * 
 * @author Thomas Kasper
 * 
 */
@AllArgsConstructor
@Getter
public class UnaryIntermediate extends Intermediate {

	private Position position;
	
	private Operand result;
	private Operand target;
	private UnaryOperation op;
	
	public Type getType() {
		return result.getType();
	}

	@Override
	public void withResult(Operand operand) {
		result = operand;
	}
	
	@Override
	public String toStringInternal() {
		return "%s = %s%s;".formatted(result, op, target);
	}
	
	@RequiredArgsConstructor
	public static enum UnaryOperation {
		ADDRESS_OF	("&"),
		INDIRECTION	("*"),
		BITWISE_NOT	("~"),
		LOGICAL_NOT	("!"),
		MINUS		("-");
		
		private final String strrep;
		
		@Override
		public String toString() {
			return strrep;
		}
		
	}
	
}
