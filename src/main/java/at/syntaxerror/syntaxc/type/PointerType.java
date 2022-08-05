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
package at.syntaxerror.syntaxc.type;

import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public class PointerType extends PointerLikeType {

	protected PointerType(Type base) {
		super(TypeKind.POINTER, base);
		size = NumericValueType.POINTER.getSize();
	}
	
	@Override
	protected Type clone() {
		return new PointerType(getBase());
	}

	@Override
	protected String toStringQualifiers() {
		String quals = super.toStringQualifiers();
		
		return quals.isEmpty()
			? ""
			: " " + quals.strip();
	}
	
	@Override
	public String toStringPrefix() {
		return getBase().toStringPrefix() + "*" + toStringQualifiers();
	}
	
	@Override
	protected String toStringSuffix() {
		return getBase().toStringSuffix();
	}

}
