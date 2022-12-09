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
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Intermediate representation of memory copies
 * 
 * @author Thomas Kasper
 * 
 */
@AllArgsConstructor
@Getter
public class MemcpyIntermediate extends Intermediate {

	private Position position;

	private Operand source;
	private Operand destination;
	private int sourceOffset;
	private int destinationOffset;
	private int length;

	@Override
	public void withResult(Operand operand) { }
	
	@Override
	public String toStringInternal() {
		return "/*synthetic*/ memcpy((void *) %s, (void *) %s, %d)".formatted(
			getTargetString(
				destination,
				destinationOffset
			),
			getTargetString(
				source,
				sourceOffset
			),
			length
		);
	}
	
	protected static String getTargetString(Operand target, int offset) {
		return offset == 0
			? target.toString()
			: offset > 0
				? target + "+" + offset
				: target + "-" + -offset;
	}
	
}
