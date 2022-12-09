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

import at.syntaxerror.syntaxc.intermediate.Negatable;
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
public class BinaryIntermediate extends Intermediate {

	private Position position;
	
	private Operand result;
	private Operand left;
	private Operand right;
	private BinaryOperation op;
	
	public Type getType() {
		return result.getType();
	}

	@Override
	public void withResult(Operand operand) {
		result = operand;
	}
	
	@Override
	public String toStringInternal() {
		return "%s = %s %s %s;".formatted(result, left, op, right);
	}
	
	@RequiredArgsConstructor
	public static enum BinaryOperation implements Negatable<BinaryOperation> {
		ADD				("+"),
		SUBTRACT		("-"),
		MULTIPLY		("*"),
		DIVIDE			("/"),
		MODULO			("%"),
		
		BITWISE_AND		("&"),
		BITWISE_OR		("|"),
		BITWISE_XOR		("^"),
		SHIFT_LEFT		("<<"),
		SHIFT_RIGHT		(">>"),
		
		EQUAL			("==", true),
		NOT_EQUAL		("!=", true),
		LESS			("<", true),
		LESS_EQUAL		("<=", true),
		GREATER			(">", true),
		GREATER_EQUAL	(">=", true),
		LOGICAL_AND		("&&", true),
		LOGICAL_OR		("||", true),
		;

		static {
			EQUAL.negated = NOT_EQUAL;
			NOT_EQUAL.negated = EQUAL;
			LESS.negated = GREATER_EQUAL;
			GREATER.negated = LESS_EQUAL;
			LESS_EQUAL.negated = GREATER;
			GREATER_EQUAL.negated = LESS;
		}
		
		private BinaryOperation negated = this;
		
		private final String strrep;
		
		@Getter
		private final boolean conditional;
		
		private BinaryOperation(String strrep) {
			this(strrep, false);
		}
		
		public boolean isNegatable() {
			return negated != this;
		}
		
		@Override
		public BinaryOperation negate() {
			return negated;
		}
		
		@Override
		public String toString() {
			return strrep;
		}
		
	}
	
}
