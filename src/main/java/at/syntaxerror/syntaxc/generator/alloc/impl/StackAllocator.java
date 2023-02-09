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
package at.syntaxerror.syntaxc.generator.alloc.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import at.syntaxerror.syntaxc.generator.arch.Alignment;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualStackTarget;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.type.Type;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class StackAllocator {

	private final Alignment alignment;
	
	private long maxGrowth;
	
	private Block blocks = new Block(0, Long.MAX_VALUE);
	
	private Map<VirtualStackTarget, Long> addressById = new HashMap<>();
	
	public long getStackSize() {
		return maxGrowth;
	}
	
	public long allocate(VirtualStackTarget target) {
		return addressById.computeIfAbsent(
			target,
			t -> allocate(t.getType())
		);
	}
	
	public void free(VirtualStackTarget target) {
		if(addressById.containsKey(target))
			free(addressById.remove(target));
	}
	
	private long allocate(Type type) {
		Block bestBlock = null;
		BlockMatch bestMatch = null;
		
		long align = alignment.getAlignment(type);
		long size = type.sizeof();

		for(Block block : blocks) {
			BlockMatch match = block.satisfies(align, size);

			// skip block if it is too small or already allocated
			if(match == BlockMatch.TOO_SMALL || match == BlockMatch.ALLOCATED)
				continue;
			
			// quick exit if block fits perfectly
			if(match == BlockMatch.PERFECT) {
				bestBlock = block;
				break;
			}
			
			if(bestBlock == null) {
				bestBlock = block;
				bestMatch = match;
				continue;
			}
			
			// prefer padding on either left or right over padding on both sides
			if(bestMatch == BlockMatch.PADDING_BOTH && match == BlockMatch.PADDING_SINGLE) {
				bestBlock = block;
				bestMatch = match;
			}
		}
		
		if(bestBlock == null)
			Logger.error("Failed to allocate memory (no block found)");
		
		bestBlock.allocate(align, size);
		
		maxGrowth = Math.max(maxGrowth, bestBlock.end + 1);
		
		return bestBlock.start;
	}
	
	private void free(long address) {
		for(Block block : blocks)
			if(block.start == address) {
				block.free();
				break;
			}
	}
	
	@ToString(exclude = { "previous" })
	private static class Block implements Iterable<Block> {
		
		private long start;
		private long end;

		private long size;
		
		private boolean isFree;
		
		private Block previous;
		private Block next;
		
		public Block(long start, long end) {
			this.start = start;
			this.end = end;
			
			size = end - start;
			isFree = true;
		}
		
		public void free() {
			/* free block by merging with previous/next block, if possible */
			isFree = true;
			
			if(previous != null && previous.isFree) {
				start = previous.start;
				previous = previous.previous;
				
				if(previous != null)
					previous.next = this;
			}

			if(next != null && next.isFree) {
				end = next.end;
				next = next.next;
				
				if(next != null)
					next.previous = this;
			}
			
			size = end - start;
		}
		
		public void allocate(long align, long size) {
			long start = Alignment.alignAt(this.start, align);
			long end = start + size - 1;
			
			// alignment padding
			if(start != this.start) {
				Block pad = new Block(this.start, start - 1);
				
				if(previous != null)
					previous.next = pad;
				
				pad.previous = previous;
				pad.next = this;
				
				previous = pad;
			}
			
			// size padding
			if(end != this.end) {
				Block pad = new Block(end + 1, this.end);
				
				if(next != null)
					next.previous = pad;
				
				pad.previous = this;
				pad.next = next;
				
				next = pad;
			}
			
			this.start = start;
			this.end = end;
			this.size = size;
			
			isFree = false;
		}
		
		public BlockMatch satisfies(long align, long size) {
			if(!isFree)
				return BlockMatch.ALLOCATED;
			
			long offset = Alignment.alignAt(start, align) - start;
			
			size = offset + size;
			
			if(size > this.size)
				return BlockMatch.TOO_SMALL;

			// block either fits perfectly or needs alignment padding
			if(size == this.size)
				return offset == 0
					? BlockMatch.PERFECT
					: BlockMatch.PADDING_SINGLE;

			// block is too large and possibly needs alignment padding
			return offset == 0
				? BlockMatch.PADDING_SINGLE
				: BlockMatch.PADDING_BOTH;
		}
		
		@Override
		public Iterator<Block> iterator() {
			if(previous != null)
				return previous.iterator();
			
			return Stream.iterate(
				this,
				idx -> idx != null,
				idx -> idx.next
			).iterator();
		}
		
	}
	
	private static enum BlockMatch {
		ALLOCATED,
		TOO_SMALL,
		PADDING_SINGLE,
		PADDING_BOTH,
		PERFECT
	}
	
}
