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
	private boolean alignmentCalculated;
	
	private List<Member> members = new ArrayList<>();
	
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
		alignmentCalculated = false;
		
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
		
		if(bitfield)
			type = type.asBitfield(); // mark type as bitfield
		
		members.add(new Member(
			pos.getPosition(),
			name,
			type,
			bitWidth
		));
	}
	
	public void addAnonymousMember(Positioned pos, Type type, boolean bitfield, int bitWidth) {
		addMember(pos, null, type, bitfield, bitWidth);
	}

	public void calculateAlignment() {
		if(alignmentCalculated)
			return;
		
		alignmentCalculated = true;
		
		var alignment = ArchitectureRegistry.getArchitecture().getAlignment();

		Member previous = null;
		
		for(Member member : members) {

			boolean bitfield = member.isBitfield();
			int bitWidth = member.getBitWidth();
			Type type = member.getType();
			
			int offset = 0;
			int bitOffset = 0;
			
			if(!isUnion() && previous != null) {
				
				int prevOff = previous.getOffset();

				if(previous.isBitfield()) {
					bitOffset = previous.getBitOffset() + previous.getBitWidth();
					offset = prevOff + (bitOffset >> 3);
					bitOffset &= 7;
					
					if(!bitfield) {
						if(bitOffset != 0)
							++offset; // account for partial bytes
						
						bitOffset = 0;
					}
				}
				else offset = prevOff + previous.sizeof();
				
			}
			
			var memberAlignment = alignment.getMemberAlignment(
				this,
				type,
				offset,
				bitOffset,
				bitWidth,
				previous != null && previous.isBitfield()
			);
			
			int newOffset = memberAlignment.offset();
			
			member.padding = newOffset - offset;
			
			if(bitOffset != 0 && member.padding > 0)
				--member.padding;
			
			member.offset = offset = newOffset;
			member.bitOffset = bitOffset = memberAlignment.bitOffset();
			
			if(bitfield) {
				if(bitOffset != 0)
					bitWidth -= bitOffset;
				
				if(bitWidth > 0)
					size = offset + Math.ceilDiv(bitWidth, 8);
			}
				
			else size = offset + type.sizeof();
			
			previous = member;
		}
		
		/* align struct boundary */
		
		int align = ArchitectureRegistry.getArchitecture()
			.getAlignment()
			.getAlignment(this);
		
		size = (int) Alignment.alignAt(size, align);
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
		calculateAlignment();
		
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
		calculateAlignment();
		
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
		structType.alignmentCalculated = alignmentCalculated;
		structType.size = size;
		
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

		private final Position position;
		private final String name;
		private final Type type;
		private final int bitWidth;
		
		private int offset;
		private int bitOffset;
		private int padding;
		
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
