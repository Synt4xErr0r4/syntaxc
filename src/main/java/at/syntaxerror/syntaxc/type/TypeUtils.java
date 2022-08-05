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

import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class TypeUtils {
	
	public static boolean isVoidPointer(Type type) {
		return type.isPointer()
			&& type.dereference().isVoid();
	}
	
	public static Type inheritPointerQualifiers(Type a, Type b) {
		return a.dereference()
				.inheritQualifiers(b.dereference())
				.addressOf();
	}

	private static boolean checkRange(NumericValueType original, NumericValueType target) {
		int obits = original.getSize();
		int tbits = target.getSize();
		
		// if signed, one bit is used for the sign bit and can therefore not be used to store a larger value
		if(original.isSigned()) --obits;
		if(target.isSigned()) --tbits;
		
		return tbits >= obits;
	}
	
	/* § 6.2.1.1 Characters and integers */
	public static NumberType promoteInteger(Type type) {
		NumericValueType num = type.toNumber().getNumericType();
		
		switch(num) {
		case SIGNED_CHAR:
		case UNSIGNED_CHAR:
		case SIGNED_SHORT:
		case UNSIGNED_SHORT:
			// integral promotion only affects '(unsigned) char' and '(unsigned) short'
			
			// if 'int' can represent all values of the original type, the value is converted to 'int'
			if(checkRange(num, NumericValueType.SIGNED_INT))
				return Type.INT;
			
			// otherwise it is converted to 'unsigned int'
			return Type.UINT;
		
		default:
			return type.toNumber();
		}
	}

	/* § 6.2.1.5 Usual arithmetic conversions */
	public static NumberType convertUsualArithmetic(Type left, Type right) {
		NumericValueType lnum = left.toNumber().getNumericType();
		NumericValueType rnum = right.toNumber().getNumericType();
		
		// if either operand is 'long double', the other one is converted to 'long double'
		if(lnum == NumericValueType.LDOUBLE || rnum == NumericValueType.LDOUBLE)
			return Type.LDOUBLE;

		// if either operand is 'double', the other one is converted to 'double'
		if(lnum == NumericValueType.DOUBLE || rnum == NumericValueType.DOUBLE)
			return Type.DOUBLE;

		// if either operand is 'float', the other one is converted to 'float'
		if(lnum == NumericValueType.FLOAT || rnum == NumericValueType.FLOAT)
			return Type.FLOAT;
		
		lnum = promoteInteger(left).getNumericType();
		rnum = promoteInteger(right).getNumericType();

		// if either operand is 'unsigned long', the other one is converted to 'unsigned long'
		if(lnum == NumericValueType.UNSIGNED_LONG || rnum == NumericValueType.UNSIGNED_LONG)
			return Type.ULONG;

		/* if one operand is 'long' and the other is 'unsigned int',
		 * the 'unsigned int' is converted to 'long' if 'long' can represent all values of 'unsigned int'.
		 * Otherwise, both values are converted to 'unsigned long int'
		 */
		if((lnum == NumericValueType.SIGNED_LONG && rnum == NumericValueType.UNSIGNED_INT)
			|| (lnum == NumericValueType.UNSIGNED_INT && rnum == NumericValueType.SIGNED_LONG)) {
			
			if(checkRange(NumericValueType.UNSIGNED_INT, NumericValueType.SIGNED_LONG))
				return Type.LONG;
			
			return Type.ULONG;
		}

		// if either operand is 'long', the other one is converted to 'long'
		if(lnum == NumericValueType.SIGNED_LONG || rnum == NumericValueType.SIGNED_LONG)
			return Type.LONG;

		// if either operand is 'unsigned int', the other one is converted to 'unsigned int'
		if(lnum == NumericValueType.UNSIGNED_INT || rnum == NumericValueType.UNSIGNED_INT)
			return Type.UINT;
		
		// otherwise, both value are converted to 'int'
		return Type.INT;
	}
	
	public static Type toComposite(Type a, Type b) {
		if(a == b)
			return a;

		// enums, structs, and unions must pass the identify check above
		if(a.getKind() != b.getKind() || a.isEnum() || a.isStructLike())
			return null;
		
		return null;
	}
	
	public static boolean isEqual(Type a, Type b) {
		if(a == b)
			return true;
		
		// enums, structs, and unions must pass the identify check above
		if(a.getKind() != b.getKind() || a.isEnum() || a.isStructLike())
			return false;
		
		if(a.isInteger())
			return a.toNumber().isUnsigned() == b.toNumber().isUnsigned();
		
		if(a.isPointer())
			return isEqual(
				a.dereference(),
				b.dereference()
			);
		
		if(a.isFunction()) {
			FunctionType aFunction = a.toFunction();
			FunctionType bFunction = b.toFunction();
			
			if(!isEqual(
				aFunction.getReturnType(),
				bFunction.getReturnType())
			) return false;
			
			var aParams = aFunction.getParameters();
			var bParams = bFunction.getParameters();
			
			if(aParams.size() != bParams.size())
				return false;
			
			if(aFunction.isVariadic() != bFunction.isVariadic())
				return false;
			
			if(!aFunction.isKAndR() && !bFunction.isKAndR())
				for(int i = 0; i < aParams.size(); ++i)
					if(!isEqual(
						aParams.get(i).type(),
						bParams.get(i).type()
					)) return false;
			
			return true;
		}
		
		if(a.isArray()) {
			ArrayType aArray = a.toArray();
			ArrayType bArray = b.toArray();
			
			if(!isEqual(
				aArray.getBase(),
				bArray.getBase()
			)) return false;
			
			return aArray.getLength() == bArray.getLength();
		}
		
		return true;
	}

}
