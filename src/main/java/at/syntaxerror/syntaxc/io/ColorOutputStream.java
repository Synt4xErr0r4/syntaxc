/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import lombok.RequiredArgsConstructor;

/**
 * Color codes:
 * <ul>
 * <li>{@code &0}-{@code &9}, {@code &a}-{@code &f}, {@code &A}-{@code &F}: ANSI 3-/4-bit Colors
 * <li>{@code &l}: Bold
 * <li>{@code &r}: Reset
 * <li>{@code &+}: Push state
 * <li>{@code &-}: Pop state
 * </ul>
 *
 * @author Thomas Kasper
 */
@RequiredArgsConstructor
public class ColorOutputStream extends OutputStream {
	
	public static PrintStream ofPrintStream(PrintStream stream) {
		return new PrintStream(new ColorOutputStream(stream));
	}
	
	private static final byte PREFIX = '&';
	private static final byte BACKGROUND_MODIFIER = '#';
	
	private static final byte PUSH = '+';
	private static final byte POP = '-';
	
	private static final Map<Byte, AnsiColor> COLORS;
	
	static {
		var colors = new HashMap<Byte, AnsiColor>();
		
		for(AnsiColor color : AnsiColor.values())
			if(color != AnsiColor.RESET && color != AnsiColor.BOLD) {
				int id = color.ordinal();
				
				if(id <= 9)
					colors.put((byte) ('0' + id), color);
				else {
					colors.put((byte) ('a' + id - 0xA), color);
					colors.put((byte) ('A' + id - 0xA), color);
				}
			}
		
		colors.put((byte) 'r', AnsiColor.RESET);
		colors.put((byte) 'l', AnsiColor.BOLD);
		
		COLORS = Collections.unmodifiableMap(colors);
	}
	
	public static String escape(String string) {
		return string == null ? null : string.replace("&", "&&");
	}
	
	public static String strip(String string) {
		return string == null ? null : string.replaceAll("(?<!&)&#?[-+0-9a-fA-Frl]|&(?=&)", "");
	}
	
	private final OutputStream stream;
	private int index = 0;
	
	private Stack<AnsiState> stateStack = new Stack<>();
	private AnsiState state = new AnsiState();
	
	@Override
	public void write(int b) throws IOException {
		AnsiColor color = null;
		b &= 0xFF;
		
		switch(index) {
		case 0:
			if(b == PREFIX) {
				index = 1;
				break;
			}
			
			stream.write(b);
			break;
			
		case 1:
			index = 0;
			
			switch(b) {
			case PREFIX:
				stream.write(b);
				break;
				
			case BACKGROUND_MODIFIER:
				index = 2;
				break;
				
			case PUSH:
				stateStack.push(state);
				state = new AnsiState(state);
				break;
				
			case POP:
				if(!stateStack.empty()) {
					state = stateStack.pop();

					stream.write(AnsiColor.RESET.getForeground().getBytes(StandardCharsets.UTF_8));
					
					if(state.foreground != AnsiColor.RESET)
						stream.write(state.foreground.getForeground().getBytes(StandardCharsets.UTF_8));
					
					if(state.background != AnsiColor.RESET)
						stream.write(state.background.getBackground().getBytes(StandardCharsets.UTF_8));
					
					if(state.bold)
						stream.write(AnsiColor.BOLD.getForeground().getBytes(StandardCharsets.UTF_8));
				}
				break;
				
			default:
				color = COLORS.get((byte) b);
				
				if(color == null) {
					stream.write(PREFIX);
					stream.write(b);
				}
				else {
					if(color == AnsiColor.BOLD)
						state.bold = true;
					else if(color == AnsiColor.RESET) {
						state.bold = false;
						state.background = state.foreground = AnsiColor.RESET;
					}
					else state.foreground = color;
					
					stream.write(color.getForeground().getBytes(StandardCharsets.UTF_8));
				}
				break;
			}
			break;
		
		case 2:
			index = 0;
			
			color = COLORS.get((byte) b);
			
			if(color == null) {
				stream.write(PREFIX);
				stream.write(BACKGROUND_MODIFIER);
				stream.write(b);
			}
			else {
				if(color == AnsiColor.BOLD)
					state.bold = true;
				else if(color == AnsiColor.RESET) {
					state.bold = false;
					state.background = state.foreground = AnsiColor.RESET;
				}
				else state.background = color;
				
				stream.write(color.getBackground().getBytes(StandardCharsets.UTF_8));
			}
			break;
		}
	}
	
	private static class AnsiState {
		
		AnsiColor foreground = AnsiColor.RESET;
		AnsiColor background = AnsiColor.RESET;
		boolean bold = false;
		
		public AnsiState() {}
		
		public AnsiState(AnsiState state) {
			foreground = state.foreground;
			background = state.background;
			bold = state.bold;
		}
	
		@Override
		public String toString() {
			return "State(fg=" + foreground.name() + ", bg=" + background.name() + ", bold=" + bold + ")";
		}
		
	}
	
	@RequiredArgsConstructor
	public static enum AnsiColor {
		BLACK(30),
		RED(31),
		GREEN(32),
		YELLOW(33),
		BLUE(34),
		MAGENTA(35),
		CYAN(36),
		WHITE(37),
		BRIGHT_BLACK(90),
		BRIGHT_RED(91),
		BRIGHT_GREEN(92),
		BRIGHT_YELLOW(93),
		BRIGHT_BLUE(94),
		BRIGHT_MAGENTA(95),
		BRIGHT_CYAN(96),
		BRIGHT_WHITE(97),
		
		RESET(0),
		BOLD(1);
		
		private static String esc(int id) {
			return "\033[" + id + "m";
		}
		
		private final int id;
		
		public String getForeground() {
			return esc(id);
		}

		public String getBackground() {
			if(this == RESET || this == BOLD)
				return getForeground();
			
			return esc(id + 10);
		}

		@Override
		public String toString() {
			return getForeground();
		}
		
	}
	
}
