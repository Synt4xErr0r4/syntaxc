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
package at.syntaxerror.syntaxc.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
public abstract class CharStream implements Logable, Closeable {
	
	public static CharStream fromFile(String file, Position includedFrom) {
		return new FileCharStream(file, includedFrom);
	}
	
	public static CharStream fromString(String string, Position position) {
		return new StringCharStream(string, position);
	}
	
	@Getter
	private final String name;

	@Getter
	private final Path path;
	
	@Getter
	private final Position includedFrom;
	
	private Stack<Mark> marks;
	private Set<Long> warnedTrigraphs;
	
	protected long bytenum;
	protected long position;
	protected long column;
	protected long line;
	
	@Getter
	private String overriddenName;
	
	@Getter
	private long overridenLineOffset = 1;
	
	protected CharStream(String name, Path path, Position includedFrom) {
		this.name = name;
		this.path = path;
		this.includedFrom = includedFrom;
		
		marks = new Stack<>();
		warnedTrigraphs = new HashSet<>();
		
		setOverridenName(name);
	}
	
	public void setOverridenName(String name) {
		overriddenName = name;
	}
	
	public void setOverridenLineOffset(long at, long to) {
		overridenLineOffset = to - at - 1;
	}
	
	protected abstract void seek(long position);
	protected abstract long tell();
	
	public SectionData getSection(long bytenum, long length, long offset) {
		StringBuilder sb = new StringBuilder();
		
		++offset;
		
		mark();
		
		// traverse until start of line is found or 'offset' chars are passed
		outer:
		for(long pre = 0; pre < offset; ++pre) {
			seek(this.bytenum = bytenum);
			
			int c = nextByte();

			// traverse to the start of the UTF-8 character
			while((c & 0xC0) == 0x80) {
				if(bytenum == 0) {
					sb.append('\n');
					bytenum = 0;
					break outer;
				}
				
				seek(this.bytenum = --bytenum);
				c = nextByte();
			}
			
			seek(this.bytenum = bytenum);
			c = nextSimple(false);
			
			if(c == '\n') {
				sb.append('\n');
				++bytenum;
				--length;
				break;
			}
			
			if(bytenum == 0) {
				sb.append('\n');
				break;
			}
			
			--bytenum;
			++length;
		}

		seek(this.bytenum = bytenum);
		
		List<Long> lengths = new ArrayList<>();
		long line = 0;
		
		long i;
		
		// read pre-offset + actual section
		for(i = 0; i < length; ++i) {
			int c = nextSimple(false);
			
			if(c == '\n') {
				lengths.add(line);
				line = 0;
			}
			else ++line;
			
			if(c == -1) {
				sb.append('\n');
				break;
			}
			
			sb.append(Character.toChars(c));
		}

		// read post-offset when previous read completed normally
		if(i == length) {
			long post;
			
			for(post = 0; post < offset; ++post) {
				int c = nextSimple(false);
				
				if(c == -1 || c == '\n') {
					sb.append('\n');
					break;
				}
				
				sb.append(Character.toChars(c));
			}
			
			if(post == offset && nextSimple(false) == -1)
				sb.append('\n');
		}
		
		reset();
		
		return new SectionData(sb.toString(), lengths);
	}
	
	public static record SectionData(String section, List<Long> lengths) { }
	
	public void mark() {
		marks.push(new Mark(bytenum, position, column, line));
	}
	
	public void reset() {
		Mark mark = marks.pop();

		bytenum = mark.bytenum();
		position = mark.pos();
		column = mark.col();
		line = mark.line();
		
		seek(bytenum);
	}
	
	public void unmark() {
		marks.pop();
	}
	
	@Override
	public Position getPosition() {
		Mark mark = marks.peek();
		return new Position(mark.bytenum(), mark.pos(), mark.col(), mark.line(), position - mark.pos(), this);
	}
	
	public Position getCharPosition() {
		return new Position(bytenum, position, column, line, 1, this);
	}

	private void encodingError(String message) {
		error((Positioned) null, message + " (in line %d at character %d)", line + 1, column + 1);
	}
	
	protected abstract int nextByte();
	
	private int nextRaw() {
		int chr = nextByte();
		
		++bytenum;
		
		if(chr < 0)
			return -1;
		
		/* U+0000 - U+007F (ASCII, 0xxxxxxx) */
		if(chr <= 0x7F)
			return chr;
		
		int n = 0;
		int min = 0;
		
		/* U+0080 - U+07FF (110xxxxx 10xxxxxx) */
		if((chr & 0xE0) == 0xC0) {
			n = 1;
			chr &= 0x1F;
			min = 0x0080;
		}

		/* U+0800 - U+FFFF (1110xxxx 10xxxxxx 10xxxxxx) */
		else if((chr & 0xF0) == 0xE0) {
			n = 2;
			chr &= 0x0F;
			min = 0x0800;
		}

		/* U+10000 - U+10FFFF (11110xxx 10xxxxxx 10xxxxxx 10xxxxxx) */
		else if((chr & 0xF8) == 0xF0) {
			n = 3;
			chr &= 0x07;
			min = 0x10000;
		}
		
		else encodingError("Illegal UTF-8 character");
		
		// read n continuation bytes (10xxxxxx)
		for(int i = 0; i < n; ++i) {
			int c = nextByte();

			++bytenum;
			
			if(c == -1)
				encodingError("Unexpected end-of-file in UTF-8 character");
			
			if((c & 0xC0) != 0x80)
				encodingError("Illegal UTF-8 character continuation");
			
			chr = (chr << 6) | (c & 0x3F);
		}
		
		if(chr < min)
			encodingError("Overlong encoded UTF-8 character");
		
		if(chr <= 0xFFFF && Character.isSurrogate((char) chr))
			encodingError("Illegal surrogate character");
		
		return chr;
	}
	
	private int nextSimple(boolean continuation) {
		int next = nextRaw();
		
		if(next == -1)
			return -1;
		
		if(next == '\n') { // LF
			++line;
			column = 0;
		}
		else if(next == '\r') { // CR/CRLF
			mark();
			
			if(nextRaw() == '\n')
				unmark();
			else reset();
			
			++line;
			column = 0;
			
			next = '\n';
		}
		else if(continuation && next == '\\') { // line continuation
			
			mark();
			
			if(nextSimple(false) == '\n') {
				unmark();
				next = nextSimple();
			}
			else {
				reset();
				++column;
			}
		}
		
		else ++column;
		
		++position;
		
		return next;
	}
	
	public int nextSimple() {
		return nextSimple(true);
	}

	public int nextTrigraphs() {
		mark();
		int c = nextSimple();
		
		if(c == '?') {
			mark();
			
			if(nextSimple() == '?') {
				boolean trigraph = true;
				
				int n = nextSimple();
				char r = 0;
				
				switch(n) {
				case '=':  r = '#';  break;
				case '(':  r = '[';  break;
				case '/':  r = '\\'; break;
				case ')':  r = ']';  break;
				case '\'': r = '^';  break;
				case '<':  r = '{';  break;
				case '!':  r = '|';  break;
				case '>':  r = '}';  break;
				case '-':  r = '~';  break;
				default:
					trigraph = false;
					break;
				}
				
				if(trigraph) {
					unmark();
					
					if(!warnedTrigraphs.contains(bytenum)) {
						warn(Warning.TRIGRAPHS, "Converted trigraph »??%c« into »%c«", (char) n, r);
						warnedTrigraphs.add(bytenum);
					}
					
					unmark();
					return r;
				}
			}
			
			reset();
		}
		unmark();
		
		return c;
	}
	
	public int next(boolean trigraphs) {
		return trigraphs
			? nextTrigraphs()
			: nextSimple();
	}
	
	public int peek(boolean trigraphs) {
		mark();
		
		int c = next(trigraphs);
		
		reset();
		
		return c;
	}
	
	public void skip(boolean trigraphs) {
		skip(1, trigraphs);
	}

	public void skip(int n, boolean trigraphs) {
		for(int i = 0; i < n; ++i)
			if(next(trigraphs) == -1)
				return;
	}
	
	@Override
	public String toString() {
		return "CharStream(" + name + ")";
	}
	
	private static record Mark(long bytenum, long pos, long col, long line) { }
	
}

class FileCharStream extends CharStream {

	private RandomAccessFile handle;
	
	protected FileCharStream(String file, Position includedFrom) {
		super(file, Paths.get(file), includedFrom);
		
		try {
			handle = new RandomAccessFile(file, "r");
		} catch (Exception e) {
			Logger.error("Failed to open file");
		}

		mark();
		
		if(nextSimple() == 0xFEFF) // skip byte order mark, if present
			unmark();
		else reset();
	}

	@Override
	protected long tell() {
		try {
			return handle.getFilePointer();
		} catch (Exception e) {
			Logger.error("Failed to get position in file: %s", e.getMessage());
			return -1;
		}
	}
	
	@Override
	protected void seek(long position) {
		try {
			handle.seek(position);
		} catch (Exception e) {
			Logger.error("Failed to set position in file: %s", e.getMessage());
		}
	}
	
	@Override
	protected int nextByte() {
		try {
			return handle.read();
		} catch (Exception e) {
			Logger.error("Failed to read from file: %s", e.getMessage());
			return -1;
		}
	}
	
	@Override
	public void close() throws IOException {
		handle.close();
	}
	
}

class StringCharStream extends CharStream {

	private final byte[] bytes;
	private final long length;
	
	private int offset;

	private Position selfPosition;
	
	protected StringCharStream(String string, Position pos) {
		super(
			pos.bytenum() == Position.ARGUMENT
				? "<argument>"
				: pos.file().getName(),
			Paths.get(""),
			null
		);
		
		selfPosition = pos;
		
		bytenum = pos.bytenum();
		position = pos.position();
		column = pos.column();
		line = pos.line();
		
		bytes = string.getBytes(StandardCharsets.UTF_8);
		length = bytes.length;
	}
	
	@Override
	public SectionData getSection(long bytenum, long length, long offset) {
		return selfPosition.file().getSection(bytenum, length, offset);
	}
	
	@Override
	public Position getPosition() {
		return selfPosition;
	}
	
	@Override
	public void close() throws IOException { }
	
	@Override
	protected long tell() {
		return selfPosition.bytenum() + offset;
	}
	
	@Override
	protected void seek(long position) {
		offset = (int) (position - selfPosition.bytenum());
	}
	
	@Override
	protected int nextByte() {
		if(offset < 0 || offset >= length)
			return -1;
		
		return bytes[offset++] & 0xFF;
	}
	
}
