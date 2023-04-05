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

import at.syntaxerror.syntaxc.type.Type;
import at.syntaxerror.syntaxc.type.TypeUtils;
import lombok.Getter;

/**
 * operand representing an index into an array
 * 
 * @author Thomas Kasper
 */
@Getter
public class IndexOperand implements Operand {

	private final Operand target;
	private final Operand offset;
	private final Type type;
	
	public IndexOperand(Operand target, Operand offset, Type type) {
		this.target = target;
		this.offset = offset;
		this.type = type.normalize();
	}
	
	public IndexOperand(Operand target, Type type) {
		this(target, ConstantOperand.zero(type), type);
	}
	
	@Override
	public Operand withType(Type type) {
		return new IndexOperand(target, offset, type);
	}

	@Override
	public boolean isMemory() {
		return true;
	}
	
	@Override
	public boolean equals(Operand other) {
		return other instanceof IndexOperand index
			&& target.equals(index.getTarget())
			&& this.offset == index.getOffset()
			&& TypeUtils.isEqual(type, index.type);
	}
	
	@Override
	public String toString() {
		return offset instanceof ConstantOperand constant && constant.isZero()
			? "*(%s)".formatted(target)
			: "(%s)[%s]".formatted(target, offset);
	}
	
}