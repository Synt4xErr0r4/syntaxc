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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public class StructType extends Type {

	public static StructType forStruct() {
		return new StructType(TypeKind.STRUCT);
	}

	public static StructType forUnion() {
		return new StructType(TypeKind.UNION);
	}
	
	private boolean packed;
	
	private List<Member> members = new ArrayList<>();
	
	private StructType(TypeKind kind) {
		super(kind);
	}
	
	public void setPacked() {
		packed = true;
	}
	
	public List<Member> getMembers() {
		return Collections.unmodifiableList(members);
	}
	
	public void addMember(Token name, Type type, boolean bitfield, int bitOffset, int bitWidth) {
		int align;
		int offset;
		
		if(members.isEmpty())
			align = offset = 0;
		
		else {
			Member previous = members.get(members.size() - 1);
			
			align = 0;
			
			offset = isUnion()
				? 0
				: previous.offset + previous.type.sizeof();
		}
		
		members.add(new Member(align, offset, name, type, bitfield, bitOffset, bitWidth));
	}
	
	public void addMember(Token name, Type type) {
		addMember(name, type, false, 0, 0);
	}

	public void addMember(Token name, Type type, int bitOffset, int bitWidth) {
		addMember(name, type, true, bitOffset, bitWidth);
	}
	
	@Override
	public String toStringPrefix() {
		return isStruct()
			? "struct { ... }"
			: "union { ... }";
	}
	
	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Member implements Positioned { 

		private final int align;
		private final int offset;
		private final Token nameToken;
		private final Type type;
		private final boolean bitfield;
		private final int bitOffset;
		private final int bitWidth;
		
		@Override
		public Position getPosition() {
			return nameToken.getPosition();
		}
		
		public String getName() {
			return nameToken.getString();
		}
		
	}

}
