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

import java.nio.charset.StandardCharsets;

import at.syntaxerror.syntaxc.builtin.BuiltinRegistry;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class Type {
	
	public static final Type VOID =				new Type(TypeKind.VOID);
	public static final Type VA_LIST = 			new Type(TypeKind.VA_LIST);
	
	public static final Type M128 = 			new Type(TypeKind.M128);
	public static final Type M256 = 			new Type(TypeKind.M256);
	public static final Type M512 = 			new Type(TypeKind.M512);
	
	public static final NumberType CHAR =		new NumberType(TypeKind.CHAR);
	public static final NumberType SHORT =		new NumberType(TypeKind.SHORT);
	public static final NumberType INT =		new NumberType(TypeKind.INT);
	public static final NumberType LONG =		new NumberType(TypeKind.LONG);
	public static final NumberType SCHAR =		new NumberType(TypeKind.CHAR, false);
	public static final NumberType SSHORT =		new NumberType(TypeKind.SHORT, false);
	public static final NumberType SINT =		new NumberType(TypeKind.INT, false);
	public static final NumberType SLONG =		new NumberType(TypeKind.LONG, false);
	public static final NumberType UCHAR =		new NumberType(TypeKind.CHAR, true);
	public static final NumberType USHORT =		new NumberType(TypeKind.SHORT, true);
	public static final NumberType UINT =		new NumberType(TypeKind.INT, true);
	public static final NumberType ULONG =		new NumberType(TypeKind.LONG, true);
	public static final NumberType FLOAT =		new NumberType(TypeKind.FLOAT);
	public static final NumberType DOUBLE =		new NumberType(TypeKind.DOUBLE);
	public static final NumberType LDOUBLE =	new NumberType(TypeKind.LDOUBLE);
	
	public static Type getStringType(String string, boolean wide) {
		return (wide
				? NumericValueType.WCHAR
				: NumericValueType.CHAR
			).asType()
			.arrayOf(
				string.getBytes(StandardCharsets.UTF_8)
					.length + 1
			);
	}
	
	private static long anonymousId = 0;
	
	protected static String getAnonymousName() {
		return "<anonymous_" + anonymousId++ + ">";
	}
	
	private final TypeKind kind;
	
	protected int size;
	
	@Getter
	protected boolean bitfield;
	
	protected boolean constQualifier;
	protected boolean volatileQualifier;
	
	public final TypeKind getKind() {
		return kind;
	}
	
	public final int sizeof() {
		if(kind == TypeKind.VA_LIST)
			return BuiltinRegistry.vaListSize;
		
		return size;
	}
	
	public boolean isConst() {
		return constQualifier;
	}
	
	public boolean isVolatile() {
		return volatileQualifier;
	}
	
	public final boolean isScalar() {
		return isArithmetic()
			|| isPointerLike();
	}
	
	public final boolean isArithmetic() {
		return kind == TypeKind.CHAR
			|| kind == TypeKind.SHORT
			|| kind == TypeKind.INT
			|| kind == TypeKind.LONG
			|| kind == TypeKind.FLOAT
			|| kind == TypeKind.DOUBLE
			|| kind == TypeKind.LDOUBLE
			|| kind == TypeKind.ENUM
			|| kind == TypeKind.M128
			|| kind == TypeKind.M256
			|| kind == TypeKind.M512;
	}
	
	public final boolean isInteger() {
		return kind == TypeKind.CHAR
			|| kind == TypeKind.SHORT
			|| kind == TypeKind.INT
			|| kind == TypeKind.LONG
			|| kind == TypeKind.ENUM
			|| kind == TypeKind.M128
			|| kind == TypeKind.M256
			|| kind == TypeKind.M512;
	}
	
	public final boolean isFloating() {
		return kind == TypeKind.FLOAT
			|| kind == TypeKind.DOUBLE
			|| kind == TypeKind.LDOUBLE
			|| kind == TypeKind.M128
			|| kind == TypeKind.M256
			|| kind == TypeKind.M512;
	}
	
	public final boolean isPointerLike() {
		return kind == TypeKind.POINTER
			|| kind == TypeKind.ARRAY;
	}
	
	public final boolean isPointer() {
		return kind == TypeKind.POINTER;
	}
	
	public final boolean isArray() {
		return kind == TypeKind.ARRAY;
	}
	
	public final boolean isEnum() {
		return kind == TypeKind.ENUM;
	}
	
	public final boolean isStructLike() {
		return kind == TypeKind.STRUCT
			|| kind == TypeKind.UNION;
	}
	
	public final boolean isStruct() {
		return kind == TypeKind.STRUCT;
	}
	
	public final boolean isUnion() {
		return kind == TypeKind.UNION;
	}
	
	public final boolean isFunction() {
		return kind == TypeKind.FUNCTION;
	}
	
	public final boolean isVoid() {
		return kind == TypeKind.VOID;
	}
	
	public final boolean isVaList() {
		return kind == TypeKind.VA_LIST;
	}
	
	public final boolean isM128() {
		return kind == TypeKind.M128;
	}
	
	public final boolean isM256() {
		return kind == TypeKind.M256;
	}
	
	public final boolean isM512() {
		return kind == TypeKind.M512;
	}
	
	public final boolean isString() {
		if(!isArray())
			return false;
		
		Type base = toArray().getBase();
		
		if(!base.isInteger())
			return false;
		
		NumericValueType type = base.toNumber().getNumericType();
		
		return type == NumericValueType.CHAR
			|| type == NumericValueType.WCHAR;
	}

	public boolean isIncomplete() {
		return false;
	}

	public boolean isSigned() {
		return false;
	}

	public boolean isUnsigned() {
		return !isSigned();
	}
	
	public NumberType toNumber() {
		return (NumberType) this;
	}

	public PointerLikeType toPointerLike() {
		return (PointerLikeType) this;
	}

	public PointerType toPointer() {
		return (PointerType) this;
	}
	
	public ArrayType toArray() {
		return (ArrayType) this;
	}
	
	public FunctionType toFunction() {
		return (FunctionType) this;
	}
	
	public StructType toStructLike() {
		return (StructType) this;
	}
	
	public EnumType toEnum() {
		return (EnumType) this;
	}
	
	// '&' unary operator
	public PointerType addressOf() {
		return new PointerType(this);
	}

	// 'type[expression]'
	public ArrayType arrayOf(int n) {
		return new ArrayType(this, n);
	}

	// 'type[]'
	public ArrayType arrayOf() {
		return arrayOf(ArrayType.SIZE_UNKNOWN);
	}

	// '*' unary operator
	public Type dereference() {
		return toPointerLike().getBase();
	}
	
	public Type asConst() {
		if(isConst())
			return this;
		
		Type cloned = clone();
		cloned.constQualifier = true;
		
		return cloned;
	}
	
	public Type asVolatile() {
		if(isVolatile())
			return this;
		
		Type cloned = clone();
		cloned.volatileQualifier = true;
		
		return cloned;
	}
	
	public Type inheritQualifiers(Type other) {
		if(!other.constQualifier && !other.volatileQualifier)
			return this;
		
		Type cloned = clone();
		cloned.constQualifier |= other.constQualifier;
		cloned.volatileQualifier |= other.volatileQualifier;
		
		return cloned;
	}

	public Type unqualified() {
		if(!constQualifier && !volatileQualifier)
			return this;

		Type clone = clone();
		clone.constQualifier = false;
		clone.volatileQualifier = false;
		
		return clone;
	}
	
	public Type asBitfield() {
		if(bitfield)
			return this;
		
		Type cloned = clone();
		cloned.bitfield = true;
		
		return cloned;
	}
	
	public Type normalize() {
		return this;
	}
	
	@Override
	protected Type clone() {
		return inheritProperties(new Type(kind));
	}
	
	protected <T extends Type> T inheritProperties(T clone) {
		clone.bitfield = bitfield;
		clone.constQualifier = constQualifier;
		clone.volatileQualifier = volatileQualifier;
		clone.size = clone.size;
		
		return clone;
	}
	
	protected String toStringQualifiers() {
		String quals = "";
		
		if(isConst()) quals += "const ";
		if(isVolatile()) quals += "volatile ";
		
		return quals;
	}
	
	protected String toStringPrefix() {
		return toStringQualifiers() + kind.getRawName();
	}
	
	protected String toStringSuffix() {
		return "";
	}
	
	@Override
	public String toString() {
		return toStringPrefix() + toStringSuffix();
	}

}
