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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class Type {
	
	public static final Type VOID =				new Type(TypeKind.VOID);
	
	public static final NumberType CHAR =		new NumberType(TypeKind.CHAR);
	public static final NumberType SHORT =		new NumberType(TypeKind.SHORT);
	public static final NumberType INT =		new NumberType(TypeKind.INT);
	public static final NumberType LONG =		new NumberType(TypeKind.LONG);
	public static final NumberType UCHAR =		new NumberType(TypeKind.CHAR);
	public static final NumberType USHORT =		new NumberType(TypeKind.SHORT);
	public static final NumberType UINT =		new NumberType(TypeKind.INT);
	public static final NumberType ULONG =		new NumberType(TypeKind.LONG);
	public static final NumberType FLOAT =		new NumberType(TypeKind.FLOAT);
	public static final NumberType DOUBLE =		new NumberType(TypeKind.DOUBLE);
	public static final NumberType LDOUBLE =	new NumberType(TypeKind.LDOUBLE);
	
	public static Type getStringType(String string) {
		return CHAR.arrayOf(string.getBytes(StandardCharsets.UTF_8).length + 1);
	}
	
	private final TypeKind kind;
	
	protected int size;
	protected int align;
	
	public final TypeKind getKind() {
		return kind;
	}
	
	public final int sizeof() {
		return size;
	}
	
	public int getAlignment() {
		return align;
	}
	
	public final boolean isNumber() {
		return kind == TypeKind.CHAR
			|| kind == TypeKind.SHORT
			|| kind == TypeKind.INT
			|| kind == TypeKind.LONG
			|| kind == TypeKind.FLOAT
			|| kind == TypeKind.DOUBLE
			|| kind == TypeKind.LDOUBLE
			|| kind == TypeKind.ENUM;
	}
	
	public final boolean isInteger() {
		return kind == TypeKind.CHAR
			|| kind == TypeKind.SHORT
			|| kind == TypeKind.INT
			|| kind == TypeKind.LONG
			|| kind == TypeKind.ENUM;
	}
	
	public final boolean isFloating() {
		return kind == TypeKind.FLOAT
			|| kind == TypeKind.DOUBLE
			|| kind == TypeKind.LDOUBLE;
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

	public boolean isIncomplete() {
		return false;
	}
	
	public boolean isCompatible(Type other) {
		if(this == other)
			return true;
		
		if(kind != other.kind || isEnum() || isStructLike()) // enums, structs, and unions must pass the identify check above
			return false;
		
		if(isInteger())
			return toNumber().isUnsigned() == other.toNumber().isUnsigned();
		
		if(isFloating())
			return true;
		
		if(isPointer())
			return toPointer().getBase().isCompatible(other.toPointer().getBase());
		
		if(isFunction()) {
			FunctionType thisFunction = toFunction();
			FunctionType otherFunction = other.toFunction();
			
			if(!thisFunction.getReturnType().isCompatible(otherFunction.getReturnType()))
				return false;
			
			var thisParams = thisFunction.getParameters();
			var otherParams = otherFunction.getParameters();
			
			if(thisParams.size() != otherParams.size())
				return false;
			
			for(int i = 0; i < thisParams.size(); ++i)
				if(!thisParams.get(i).type().isCompatible(otherParams.get(i).type()))
					return false;
			
			return true;
		}
		
		if(isArray()) {
			ArrayType thisArray = toArray();
			ArrayType otherArray = other.toArray();
			
			if(!thisArray.getBase().isCompatible(otherArray.getBase()))
				return false;
			
			return thisArray.getLength() < 0
				&& otherArray.getLength() < 0
				&& thisArray.getLength() == otherArray.getLength();
		}
		
		return false;
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
	
	public StructType toStruct() {
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

	// 'type[expression]'
	public Type dereference() {
		return toPointerLike().getBase();
	}
	
	protected String toStringPrefix() {
		return "void";
	}
	
	protected String toStringSuffix() {
		return "";
	}
	
	@Override
	public String toString() {
		return toStringPrefix() + toStringSuffix();
	}

}
