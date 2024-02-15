/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.io;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * 
 *
 * @author Thomas Kasper
 */
@RequiredArgsConstructor
@Getter
public class CharSource {

	public static ICharset defaultCharset = Charsets.UTF8;
	
	private final ICharset charset = defaultCharset;
	private final IFileStream stream;
	
	private final List<Long> lines = new ArrayList<>(List.of(0L));
	
	private long absolute;
	private long line;
	private long column;

	private long largestAbsolute;

	public String getName() {
		return stream.getName();
	}
	
	private int nextByte() {
		int b = stream.nextByte();
		
		if(b < 0 || b > 0xFF)
			return -1;

		largestAbsolute = Math.max(largestAbsolute, ++absolute);
		return b;
	}
	
	private int nextCodepoint() {
		return charset.decode(this::nextByte);
	}
	
	public int next() {
		int c = nextCodepoint();
		
		if(c == '\r') {
			var mark = getPosition();
			
			if(nextCodepoint() != '\n')
				mark.seek();
			
			c = '\n';
		}
		
		// LF, VT, FF, CR, CR+LF, NEL, LS, PS
		if(c == '\n' || c == 0x0B || c == 0x0C || c == 0x85 || c == 0x2028 || c == 0x2029) {
			++line;
			column = 0;

			if(line == lines.size())
				lines.add(absolute);
		}
		else ++column;
		
		if(c < 0)
			return -1;
		
		return c;
	}
	
	public void seek(@NonNull Position position) {
		this.absolute = position.absolute;
		this.line = position.line;
		this.column = position.column;
		
		stream.seek(position.absolute);
	}
	
	public void seekLine(long line) {
		long abs;
		
		if(line < 0 || line >= lines.size())
			abs = largestAbsolute;
		else abs = lines.get((int) line);
		
		stream.seek(abs);
		
		this.absolute = abs;
		this.line = line;
		this.column = 0;
	}
	
	public Position getPosition() {
		return new Position(this, absolute, line, column);
	}
	
	public static interface Positioned {
		
		Range position();
		
	}
	
	@AllArgsConstructor
	@ToString
	public static class Range implements Positioned {
		
		@Override
		public Range position() {
			return this;
		}
		
		@NonNull
		@Getter
		private Position start, end;
		
		public Range setStart(@NonNull Position start) {
			if(end.absolute < start.absolute)
				return setEnd(end);
			
			this.start = start;
			return this;
		}

		public Range setEnd(@NonNull Position end) {
			if(start.absolute < end.absolute)
				return setStart(end);
			
			this.end = end;
			return this;
		}
		
	}
	
	public static record Position(CharSource source, long absolute, long line, long column) implements Positioned {

		@Override
		public Range position() {
			return ranged();
		}
		
		public Range ranged(@NonNull Position other) {
			if(other.absolute < absolute)
				return new Range(other, this);
			
			return new Range(this, other);
		}
		
		public Range ranged() {
			return new Range(this, this);
		}
		
		public void seek() {
			source.seek(this);
		}
		
	}
	
}
