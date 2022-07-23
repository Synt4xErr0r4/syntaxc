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

import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public class StructType extends Type {

	public static StructType forAnonymousStruct() {
		return forStruct(getAnonymousName());
	}

	public static StructType forAnonymousUnion() {
		return forUnion(getAnonymousName());
	}
	
	public static StructType forStruct(String name) {
		return new StructType(TypeKind.STRUCT, name);
	}

	public static StructType forUnion(String name) {
		return new StructType(TypeKind.UNION, name);
	}
	
	private final String name;
	
	private boolean incomplete = true;
	private boolean packed;
	
	private List<Member> members = new ArrayList<>();
	
	private Member previous;
	
	private StructType(TypeKind kind, String name) {
		super(kind);
		this.name = name;
	}
	
	public void setComplete() {
		incomplete = false;
	}
	
	public void setPacked() {
		packed = true;
	}
	
	public List<Member> getMembers() {
		return Collections.unmodifiableList(members);
	}
	
	public void addMember(Positioned pos, String name, Type type, boolean bitfield, int bitWidth) {
		setComplete();
		
		int align;
		int offset;
		int bitOffset = 0;
		
		if(previous == null)
			align = offset = 0;
		
		else {
			align = 0;
			
			if(bitfield && previous.type.isBitfield()) {
				if(previous.bitWidth == 0) {
					bitOffset = 0;
					offset = previous.offset + 1;
				}
				else {
					bitOffset = previous.bitOffset + previous.bitWidth;
					offset = previous.offset + (bitOffset >> 3);
					bitOffset &= 7;
				}
			}
			else offset = isUnion()
				? 0
				: previous.offset + previous.type.sizeof();
		}
		
		members.add(previous = new Member(
			align,
			offset,
			pos.getPosition(),
			name,
			bitfield
				? type.asBitfield()
				: type,
			bitOffset,
			bitWidth
		));
	}
	
	public void addAnonymousMember(Positioned pos, Type type, boolean bitfield, int bitWidth) {
		addMember(pos, null, type, bitfield, bitWidth);
	}
	
	
	public Member getMember(String name) {
		for(Member member : members)
			if(member.getName() == null) { // anonymous member
				Type type = member.getType();
				
				if(type.isStructLike()) { // anonymous struct/union
					member = type.toStructLike().getMember(name);
					
					if(member != null)
						return member;
				}
				
			}
			else if(name.equals(member.getName()))
				return member;
		
		return null;
	}
	
	@Override
	protected Type clone() {
		StructType structType = new StructType(getKind(), name);
		
		structType.incomplete = incomplete;
		structType.packed = packed;
		structType.members = members;
		structType.previous = previous;
		
		return structType;
	}
	
	@Override
	public String toStringPrefix() {
		return toStringQualifiers() + (isStruct() ? "struct " : "union ") + name;
	}
	
	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	@ToString(exclude = "position")
	public static class Member implements Positioned { 

		private final int align;
		private final int offset;
		private final Position position;
		private final String name;
		private final Type type;
		private final int bitOffset;
		private final int bitWidth;
		
	}

}
