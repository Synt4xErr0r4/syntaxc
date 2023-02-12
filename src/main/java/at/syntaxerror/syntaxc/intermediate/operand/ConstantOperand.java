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

import java.math.BigDecimal;
import java.math.BigInteger;

import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;
import at.syntaxerror.syntaxc.type.TypeUtils;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * a constant number literal operand
 * 
 * @author Thomas Kasper
 */
@Getter
public class ConstantOperand implements Operand {

	public static ConstantOperand zero(Type type) {
		return new ConstantOperand(
			type.isFloating()
				? BigDecimal.ZERO
				: BigInteger.ZERO,
			type
		);
	}
	
	private final Number value;
	private final Type type;
	
	@Getter(AccessLevel.NONE)
	private String strval;
	
	public ConstantOperand(Number value, Type type) {
		this.value = value;
		this.type = type.normalize();
	}
	
	public boolean isZero() {
		return type.isFloating()
			? ((BigDecimal) value).compareTo(BigDecimal.ZERO) == 0
			: ((BigInteger) value).compareTo(BigInteger.ZERO) == 0;
	}
	
	@Override
	public boolean equals(Operand other) {
		return other instanceof ConstantOperand constant
			&& value.equals(constant.value)
			&& TypeUtils.isEqual(type, constant.type);
	}
	
	@Override
	public String toString() {
		if(strval != null)
			return strval;
		
		NumericValueType num;
		
		if(type.isPointerLike())
			num = NumericValueType.POINTER;
		else num = type.toNumber().getNumericType();
		
		strval = value.toString();
		
		if(num.isFloating()) {
			boolean hasPeriod = strval.contains(".");
			
			String suffix = switch(num) {
			case FLOAT -> "f";
			case LDOUBLE -> "l";
			default -> "";
			};
			
			return strval += (hasPeriod ? "" : ".0") + suffix;
		}
		
		String prefix = "", suffix = "";
		
		switch(num) {
		case SIGNED_CHAR:
		case UNSIGNED_CHAR:
		case SIGNED_SHORT:
		case UNSIGNED_SHORT:
		case SIGNED_INT:
			prefix = "(" + num.getCode() + ") ";
			break;
			
		case UNSIGNED_INT:
			suffix = "u";
			break;
			
		case SIGNED_LONG:
			suffix = "l";
			break;

		case UNSIGNED_LONG:
			suffix = "ul";
			break;
			
		default:
			break;
		};
		
		return strval = prefix + strval + suffix;
	}
	
}
