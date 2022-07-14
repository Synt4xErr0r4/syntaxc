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
package at.syntaxerror.syntaxc.misc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * This class is a pipe converting special character sequences to ANSI escape codes.<br>
 * The following sequences are currently defined:
 * <ul>
 * 	<li><code>§0</code>: set foreground color to black</li>
 * 	<li><code>§1</code>: set foreground color to red</li>
 * 	<li><code>§2</code>: set foreground color to green</li>
 * 	<li><code>§3</code>: set foreground color to yellow</li>
 * 	<li><code>§4</code>: set foreground color to blue</li>
 * 	<li><code>§5</code>: set foreground color to magenta</li>
 * 	<li><code>§6</code>: set foreground color to cyan</li>
 * 	<li><code>§7</code>: set foreground color to white</li>
 * 	<li><code>§8</code>: set foreground color to bright black/gray</li>
 * 	<li><code>§9</code>: set foreground color to bright red</li>
 * 	<li><code>§a</code> or <code>§A</code>: set foreground color to bright green</li>
 * 	<li><code>§b</code> or <code>§B</code>: set foreground color to bright yellow</li>
 * 	<li><code>§c</code> or <code>§C</code>: set foreground color to bright blue</li>
 * 	<li><code>§d</code> or <code>§D</code>: set foreground color to bright magenta</li>
 * 	<li><code>§e</code> or <code>§E</code>: set foreground color to bright cyan</li>
 * 	<li><code>§f</code> or <code>§F</code>: set foreground color to bright white</li>
 * 	<li><code>§#0</code>: set background color to black</li>
 * 	<li><code>§#1</code>: set background color to red</li>
 * 	<li><code>§#2</code>: set background color to green</li>
 * 	<li><code>§#3</code>: set background color to yellow</li>
 * 	<li><code>§#4</code>: set background color to blue</li>
 * 	<li><code>§#5</code>: set background color to magenta</li>
 * 	<li><code>§#6</code>: set background color to cyan</li>
 * 	<li><code>§#7</code>: set background color to white</li>
 * 	<li><code>§#8</code>: set background color to bright black/gray</li>
 * 	<li><code>§#9</code>: set background color to bright red</li>
 * 	<li><code>§#a</code> or <code>§#A</code>: set background color to bright green</li>
 * 	<li><code>§#b</code> or <code>§#B</code>: set background color to bright yellow</li>
 * 	<li><code>§#c</code> or <code>§#C</code>: set background color to bright blue</li>
 * 	<li><code>§#d</code> or <code>§#D</code>: set background color to bright magenta</li>
 * 	<li><code>§#e</code> or <code>§#E</code>: set background color to bright cyan</li>
 * 	<li><code>§#f</code> or <code>§#F</code>: set background color to bright white</li>
 * 	<li><code>§l</code>: increase intensity (bold)</li>
 * 	<li><code>§r</code>: reset all attributes</li>
 * </ul>
 * 
 * @author Thomas Kasper
 * 
 */
public class AnsiPipe extends OutputStream {

	private static final PrintStream STDOUT = System.out;
	private static final PrintStream STDERR = System.err;
	
	// UTF-8 encoding of '§' (U+00A7)
	private static final byte[] ANSI_MARKER = { (byte) 0xC2, (byte) 0xA7 };
	
	private static final char BACKGROUND_MARKER = '#';
	
	/**
	 * Escape sequences have the format {@code ESC[ n m}
	 * (ESC is the escape character U+001B, n is the escape sequence code)
	 * 
	 * e.g. foreground color red (n = 31): ESC[31m
	 */
	private static final byte[] ANSI_PREFIX = { 0x1B, '[' };
	private static final byte ANSI_SUFFIX = 'm';
	
	private static final int STAGE_PLAIN = 0;	// plain piping mode (no sequence substitution)
	private static final int STAGE_FIRST = 1;	// first marker byte was encountered (0xC2)
	private static final int STAGE_FULL = 2;	// full marker sequence was encountered (0xC2 0xA7, '§')
	private static final int STAGE_REPEAT = 3;	// first marker byte was encountered after full marker sequence was encountered (0xC2 0xA7 0xC2)
	
	static {
		// attach ANSI pipe to stdout and stderr
		System.setOut(attachTo(STDOUT));
		System.setErr(attachTo(STDERR));
	}
	
	public static PrintStream getStdout() {
		return STDOUT;
	}
	
	public static PrintStream getStderr() {
		return STDERR;
	}
	
	/**
	 * Initializes the standard outputs to have AnsiPipes attached
	 */
	public static void init() { }
	
	/**
	 * Attaches an AnsiPipe to the specified output stream. 
	 * 
	 * @param stream the stream to attach the pipe to
	 * @return the resulting stream
	 */
	public static PrintStream attachTo(OutputStream stream) {
		return new PrintStream(new AnsiPipe(stream), true, StandardCharsets.UTF_8);
	}
	
	private final OutputStream stream;
	
	private int stage;
	
	private boolean background;
	
	private AnsiPipe(OutputStream stream) {
		this.stream = stream;
	}
	
	@Override
	public void write(int b) throws IOException {
		if(stage == STAGE_FULL) { // '§'
			if(b == BACKGROUND_MARKER) { // '§#'
				background = true;
				return;
			}
			
			if(b == ANSI_MARKER[0]) { // 0xC2 0xA7 0xC2
				stage = STAGE_REPEAT;
				return;
			}

			stage = STAGE_PLAIN; // reset stage
			
			int n;
			int off = background ? 10 : 0; // background codes are offset by 10
			
			// map character to ANSI escape code
			if(b >= '0' && b <= '7')		n = b - '0' + 30 + off; /* 30-37/40-47 */
			else if(b == '8' || b == '9')	n = b - '8' + 90 + off; /* 90-91/100-101 */
			else if(b >= 'a' && b <= 'f')	n = b - 'a' + 92 + off; /* 92-97/102-107 */
			else if(b >= 'A' && b <= 'F')	n = b - 'A' + 92 + off; /* 92-97/102-107 */
			else if(b == 'r')				n = 0; // reset ('ESC[0m')
			else if(b == 'l')				n = 1; // bold ('ESC[1m')
			else { // unrecognized sequence
				stream.write(ANSI_MARKER);
				stream.write(b);
				return;
			}

			// write 'ESC[ n m'
			stream.flush();
			stream.write(ANSI_PREFIX);
			stream.write(Integer.toString(n).getBytes(StandardCharsets.US_ASCII));
			stream.write(ANSI_SUFFIX);
			stream.flush();
			
			background = false;
		}
		
		else if(stage == STAGE_FIRST) { // 0xC2 (first marker byte)
			if(b == ANSI_MARKER[1]) // 0xC2 0xA7 ('§')
				stage = STAGE_FULL;
			
			else { // character is not '§'
				stream.write(ANSI_MARKER[0]);
				stream.write(b);
				stage = STAGE_PLAIN; // reset stage
			}
		}
		
		else if(stage == STAGE_PLAIN && b == ANSI_MARKER[0]) // 0xC2 (first marker byte)
			stage = STAGE_FIRST;
		
		else if(stage == STAGE_REPEAT) { // 0xC2 0xA7 0xC2
			if(b == ANSI_MARKER[1]) // replace '§§' with '§'
				stream.write(ANSI_MARKER);
			
			else { // unrecognized sequence
				stream.write(ANSI_MARKER);
				stream.write(ANSI_MARKER[0]);
				stream.write(b);
			}
			
			stage = STAGE_PLAIN; // reset stage
		}
		
		else stream.write(b); // write byte as usual
	}
	
}
