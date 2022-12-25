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
import at.syntaxerror.syntaxc.tracking.Positioned;

/**
 * Subclasses of this class represent the intermediate representation
 * of a machine code instruction
 * 
 * @author Thomas Kasper
 * 
 */
public abstract class Intermediate implements Positioned {

	public abstract Position getPosition();
	
	public abstract void withResult(Operand operand);
	
	/**
	 * Converts this intermediate representation into C-like code
	 * 
	 * @return the C-like code
	 */
	@Override
	public final String toString() {
		String strval = toStringInternal();
		
		// remove discard operator from string representation
		if(strval.startsWith("<discard> = "))
			return strval.substring(12);
		
		return strval;
	}
	
	protected abstract String toStringInternal();
	
	protected static String toTargetString(Operand target) {
		return target == null
			? ""
			: target + " = ";
	}
	
	public static abstract class BinaryIntermediate extends Intermediate {
		
		public abstract Operand getResult();
		public abstract Operand getLeft();
		public abstract Operand getRight();
		
	}

	public static abstract class UnaryIntermediate extends Intermediate {

		public abstract Operand getTarget();
		public abstract Operand getValue();
		
	}
	
}
