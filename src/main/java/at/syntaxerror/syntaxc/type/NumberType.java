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

import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public class NumberType extends Type {

	public static Type of(NumericValueType type) {
		return switch(type) {
		case SIGNED_CHAR ->		CHAR;
		case UNSIGNED_CHAR ->	UCHAR;

		case SIGNED_SHORT ->	SHORT;
		case UNSIGNED_SHORT ->	USHORT;
		
		case SIGNED_INT ->		INT;
		case UNSIGNED_INT ->	UINT;
		
		case SIGNED_LONG ->		LONG;
		case UNSIGNED_LONG ->	ULONG;

		case FLOAT ->			FLOAT;
		case DOUBLE ->			DOUBLE;
		case LDOUBLE ->			LDOUBLE;
		};
	}

	protected boolean explicitSign;
	
	private NumericValueType numericType;

	protected NumberType(TypeKind kind, boolean unsigned) {
		super(kind);
		
		numericType = switch(kind) {
		case CHAR ->	NumericValueType.SIGNED_CHAR;
		case SHORT ->	NumericValueType.SIGNED_SHORT;
		case INT ->		NumericValueType.SIGNED_INT;
		case LONG ->	NumericValueType.SIGNED_LONG;

		case FLOAT ->	NumericValueType.FLOAT;
		case DOUBLE ->	NumericValueType.DOUBLE;
		case LDOUBLE ->	NumericValueType.LDOUBLE;
		
		default -> throw new IllegalArgumentException("Unexpected value: " + kind);
		};
		
		if(unsigned)
			numericType = numericType.getAsUnsigned();
		
		size = numericType.getSize();
		explicitSign = true;
	}
	
	protected NumberType(TypeKind kind) {
		this(kind, false);
		explicitSign = false;
	}
	
	@Override
	public boolean isSigned() {
		if(!explicitSign) {
			if(isBitfield())
				return !ArchitectureRegistry.isUnsignedBitfields();
			
			return numericType != NumericValueType.SIGNED_CHAR
				|| NumericValueType.CHAR.isSigned();
		}
		
		return numericType.isSigned();
	}
	
	@Override
	protected Type clone() {
		NumberType clone = new NumberType(getKind(), isUnsigned());
		clone.explicitSign = explicitSign;
		
		return inheritProperties(clone);
	}
	
	@Override
	public String toStringPrefix() {
		return toStringQualifiers() + numericType.getCode();
	}
	
}
