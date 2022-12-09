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

import at.syntaxerror.syntaxc.generator.arch.Alignment;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.logger.Logger;
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
		return forStruct(null);
	}

	public static StructType forAnonymousUnion() {
		return forUnion(null);
	}
	
	public static StructType forStruct(String name) {
		return new StructType(TypeKind.STRUCT, name);
	}

	public static StructType forUnion(String name) {
		return new StructType(TypeKind.UNION, name);
	}
	
	private final String name;
	private final boolean anonymous;
	
	private boolean incomplete = true;
	private boolean inherited;
	
	private List<Member> members = new ArrayList<>();
	
	private Member previous;
	
	private StructType(TypeKind kind, String name) {
		super(kind);
		
		anonymous = name == null;
		
		this.name = anonymous ? getAnonymousName() : name;
	}
	
	public StructType asInherited() {
		StructType cloned = (StructType) clone();
		
		cloned.inherited = true;
		
		return cloned;
	}
	
	public void setComplete() {
		incomplete = false;
		inherited = false;
	}
	
	public List<Member> getMembers() {
		return Collections.unmodifiableList(members);
	}
	
	public void addMember(Positioned pos, String name, Type type, boolean bitfield, int bitWidth) {
		if(type.isIncomplete())
			incomplete = true;

		// only 'int', 'signed int', and 'unsigned int' are eligible for bitfields
		if(bitfield && !type.isInteger())
			Logger.softError(pos, "Illegal bitfield struct/union member of type »%s«", type);
		
		if(bitfield && bitWidth == 0 && name != null)
			Logger.softError(pos, "Bitfield with width 0 must not have a name");
		
		if(name != null)
			members.stream()
				.map(Member::getName)
				.filter(memName -> memName != null && memName.equals(name))
				.findFirst()
				.ifPresent(x -> Logger.softError(pos, "Duplicate member named »%s«", name));
		
		int padding = 0;
		int offset = 0;
		int bitOffset = 0;
		int align;
		
		if(!isUnion() && previous != null) {

			if(previous.isBitfield()) {
				bitOffset = previous.bitOffset + previous.bitWidth;
				offset = previous.offset + (bitOffset >> 3);
				bitOffset &= 7;
				
				if(!bitfield && bitOffset != 0)
					++offset; // account for partial bytes
			}
			else offset = previous.offset + previous.sizeof();
			
		}
		
		if(bitfield)
			type = type.asBitfield(); // mark type as bitfield
		
		var alignment = ArchitectureRegistry.getArchitecture()
			.getAlignment()
			.getMemberAlignment(
				this,
				type,
				offset,
				bitOffset,
				bitWidth
			);
		
		System.out.print(name + " => " + offset + "~" + alignment.offset() + ":" + type.sizeof() + " [" 
			+ ArchitectureRegistry.getArchitecture()
				.getAlignment()
				.getAlignment(type)
			+ "] / " + bitOffset + "~" + alignment.bitOffset() + ":" + bitWidth);
		
		offset = alignment.offset();
		bitOffset = alignment.bitOffset();
		
		members.add(previous = new Member(
			padding,
			offset,
			pos.getPosition(),
			name,
			type,
			bitOffset,
			bitWidth
		));
		
		int sz = size;

		if(bitfield) {
			if(bitOffset != 0)
				bitWidth -= bitOffset;
			
			if(bitWidth > 0)
				size = offset + Math.ceilDiv(bitWidth, 8);
		}
			
		else size = offset + type.sizeof();
		
		/* align struct to byte/word/... boundary */
		
		int sz0 = size;
		
		align = ArchitectureRegistry.getArchitecture()
			.getAlignment()
			.getAlignment(this);
		
		size = Alignment.alignAt(size, align);
		
		System.out.println(" // " + toStringPrefix() + " => " + sz + "~" + sz0 + "~" + size + " [" + align + "]");
	}
	
	public void addAnonymousMember(Positioned pos, Type type, boolean bitfield, int bitWidth) {
		addMember(pos, null, type, bitfield, bitWidth);
	}

	/**
	 * Searches the member associated with a given name.
	 * The search is also performed on nested structures/unions.
	 * Returns {@code null} if the member does not exist.
	 * 
	 * @param name the member's name
	 * @return the member
	 */
	public Member getMember(String name) {
		for(Member member : members) {
			String memName = member.getName();
			
			// name is null for anonymous members
			if(memName != null && memName.equals(name))
				return member;
			
			Type type = member.getType();
			
			if(type.isStructLike()) { // nested struct/union, search recursively
				member = type.toStructLike().getMember(name);
				
				if(member != null)
					return member;
			}
		}
		
		return null;
	}
	
	/**
	 * Finds the offset of a given member.
	 * The search is also performed on nested structures/unions.
	 * Returns {@code -1} if the member
	 * does not exist, {@code -2} if the member is a bitfield.
	 * 
	 * @param name the member's name
	 * @return the member's offset
	 */
	public int offsetof(String name) {
		for(Member member : members) {
			String memName = member.getName();
			
			int base = member.getOffset();
			
			// name is null for anonymous members
			if(memName != null) {
				if(memName.equals(name)) {
					if(member.isBitfield())
						return -2;
					
					return base;
				}
				
				continue;
			}
			
			// only check recursively if struct/union is unnamed
			
			Type type = member.getType();
			
			if(type.isStructLike()) { // nested struct/union, search recursively
				StructType struct = type.toStructLike();
				
				if(struct.isInherited()) // only search in structs/unions defined within the parent struct/union
					continue;
				
				int mem = struct.offsetof(name);
				
				if(mem == -2)
					return -2;
				
				if(mem != -1)
					return base + mem;
			}
		}
		
		return -1;
	}
	
	@Override
	protected Type clone() {
		StructType structType = new StructType(getKind(), name);
		
		structType.incomplete = incomplete;
		structType.inherited = inherited;
		structType.members = members;
		structType.previous = previous;
		
		return inheritProperties(structType);
	}
	
	@Override
	public String toStringPrefix() {
		return toStringQualifiers() + (isStruct() ? "struct " : "union ") + name;
	}
	
	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	@ToString(exclude = "position")
	public static class Member implements Positioned { 

		private final int padding;
		private final int offset;
		private final Position position;
		private final String name;
		private final Type type;
		private final int bitOffset;
		private final int bitWidth;
		
		public boolean isBitfield() {
			return type.isBitfield();
		}
		
		public int sizeof() {
			return isBitfield()
				? Math.ceilDiv(bitWidth, 8)
				: type.sizeof();
		}
		
	}

}
