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

import java.util.Set;

import at.syntaxerror.syntaxc.io.CharStream;
import at.syntaxerror.syntaxc.preprocessor.macro.Macro;

/**
 * This class represents a code section inside of a file
 * 
 * @author Thomas Kasper
 * 
 */
public record Position(long bytenum, long position, long column, long line, long length, CharStream file,
		Set<Macro> expansions, Position expansionRoot) implements Positioned, Comparable<Position> {

	public static final long DUMMY = Long.MIN_VALUE;
	public static final long ARGUMENT = Long.MIN_VALUE + 1;
	
	public static final Position DUMMY_POSITION = new Position(DUMMY, 0, 0, 0, 0, null, Set.of(), null);
	
	public static Position argument(long length) {
		return new Position(ARGUMENT, 0, 0, 0, length, null, Set.of(), null);
	}

	public static Position dummy() {
		return DUMMY_POSITION;
	}
	
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
	
	/**
	 * Merges this and the {@code other} position into a single position, spanning across the range confined by both positions:
	 * 
	 * <code><pre>
	 * 1. -----------------------
	 * 2. -----AAAAA----BBB------
	 * 3. -----CCCCCCCCCCCC------
	 * </pre></code>
	 * 
	 * <ol>
	 * <li> Dashes ({@code -}) represent code.</li>
	 * <li> {@code A} and {@code B} represent this and the {@code other} position (order is interchangeable).</li>
	 * <li> {@code C} represents the resulting position of this function.</li>
	 * </ol> 
	 * 
	 * @param other the other position
	 * @return the position spanning across the range confined by both positions
	 */
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
			file,
			Set.of(),
			null
		);
	}

	/**
	 * See {@link #range(Position)} for more information
	 * 
	 * @param other the other position
	 * @return the position spanning across the range confined by both positions
	 */
	public Position range(Positioned other) {
		return range(other.getPosition());
	}
	
	@Override
	public int compareTo(Position o) {
		return Long.compare(bytenum, o.bytenum);
	}

}
