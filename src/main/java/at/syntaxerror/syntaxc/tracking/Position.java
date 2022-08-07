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
package at.syntaxerror.syntaxc.tracking;

import at.syntaxerror.syntaxc.io.CharStream;

/**
 * @author Thomas Kasper
 * 
 */
public record Position(long bytenum, long position, long column, long line, long length, CharStream file) implements Positioned, Comparable<Position> {

	public static final long ARGUMENT = Long.MIN_VALUE;
	
	/*
	 * bytenum: the offset from the start of the file in bytes
	 * position: the offset from the start of the file in UTF-8 characters
	 * column: the column in the current line (0-based)
	 * line: the line in the file (0-based)
	 * length: the number of UTF-8 characters
	 * file: the actual file itself
	 */
	
	@Override
	public Position getPosition() {
		return this;
	}
	
	public Position range(Position other) {
		if(other == this)
			return this;
		
		if(bytenum() > other.bytenum())
			return other.range(this);

		return new Position(
			bytenum,
			position,
			column,
			line,
			other.position() + other.length() - bytenum,
			file
		);
	}

	public Position range(Positioned other) {
		return range(other.getPosition());
	}
	
	@Override
	public int compareTo(Position o) {
		return Long.compare(bytenum, o.bytenum);
	}

}
