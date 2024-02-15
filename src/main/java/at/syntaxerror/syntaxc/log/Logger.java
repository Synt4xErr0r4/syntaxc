/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.log;

import java.util.Objects;

import at.syntaxerror.syntaxc.config.warning.IWarning;
import at.syntaxerror.syntaxc.io.CharSource.Positioned;
import at.syntaxerror.syntaxc.io.CharSource.Range;
import at.syntaxerror.syntaxc.io.ColorOutputStream;
import at.syntaxerror.syntaxc.misc.Unicode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * 
 *
 * @author Thomas Kasper
 */
public class Logger {

	public static boolean hadError = false;
	public static Verbosity verbosity = Verbosity.NORMAL;
	
	public static String format(String message, Object...objects) {
		StringBuilder builder = new StringBuilder();
		
		outer:
		for(Object obj : objects) {
			int insertAt = message.indexOf('{');
			int len = message.length();
			
			String str = Objects.toString(obj);
			int insertEnd;
			
			inner:
			while(true) {
				if(insertAt == -1 || insertAt == len - 1) {
					builder.append(message);
					break outer;
				}
				
				char chr = message.charAt(insertAt + 1);
				
				if(chr == '}') {
					str = ColorOutputStream.escape(str);
					insertEnd = insertAt + 2;
					break;
				}
				
				if(insertAt < len - 2 && message.charAt(insertAt + 2) == '}') {
					insertEnd = insertAt + 3;
					
					switch(chr) {
					case 'f':
						break inner;
						
					case 'l':
						str = "&+&l" + str + "&-";
						break inner;

					case 'c':
						str = Character.toString((int) obj);
						break inner;
						
					case 'u':
						str = "&+&l" + Unicode.toString((int) obj) + "&-";
						break inner;
						
					case 'U':
						int c = (int) obj;
						
						if(c == -1)
							str = "&+&lEOF&-";
						else str = "»&+&l" + Character.toString(c)
								+ "&-« (&+&l" + Unicode.toString(c) + "&-)";
						break inner;
						
					default:
						break;
					}
				}
				
				insertAt = message.indexOf('{', insertAt + 1);
			}
			
			builder.append(message.substring(0, insertAt));
			builder.append(str);
			message = message.substring(insertEnd);
		}
		
		builder.append(message);
		
		return builder.toString();
	}
	
	public static void log(@NonNull Level level, IWarning warning, Positioned position, @NonNull String message, Object...args) {
		if(level == Level.DEBUG && verbosity != Verbosity.VERBOSE)
			return;
		
		if((level == Level.INFO || level == Level.NOTE) && verbosity == Verbosity.QUIET)
			return;
		
		if(warning != null && !warning.getConfigValue().isEnabled())
			return;
		
		if(level == Level.ERROR || level == Level.FATAL)
			hadError = true;
		
		Range range = position == null ? null : position.position();
		
		if(range == null)
			System.err.print("&b<internal>&f: ");
		else {
			var start = range.getStart();
			
			System.err.printf("&b%s&f:&b%d&f:&b%d&f: ", start.source().getName(), start.line() + 1, start.column() + 1);
		}

		System.err.printf("%s%s&f: ", level.color, level.name);
		
		System.err.print(format(message, args));
		
		if(warning != null)
			System.err.printf(" &f[&a-W%s&f]&r%n", warning.getLogName());
		else System.err.println("&r");
		
		if(range != null) {
			var start = range.getStart();
			var end = range.getEnd();
			var src = start.source();
			
			var mark = src.getPosition();
			
			long line = start.line();
			
			src.seekLine(line);
			
			int numLength = Long.toString(end.line() + 1).length();
			String padding = " ".repeat(numLength + 3);
			
			while(line <= end.line()) {
				System.err.printf("&#c&f  %" + numLength + "d &r&8| &f", line + 1);

				if(line != start.line())
					System.err.print("&9&l");
				
				int markOffset = 0;
				int markLength = 0;
				long column = 0;
				
				boolean isStart = line == start.line();
				boolean isEnd = line == end.line();
				
				while(true) {
					int c = src.next();
					
					if(c == -1 || c == '\n')
						break;
					
					int width = Unicode.wcwidth(c);
					
					if(isStart && column < start.column())
						markOffset += width;
					
					else if(!isEnd || column <= end.column()) {
						markLength += width;
						
						if(isStart && column == start.column())
							System.err.print("&9&l");
					}
					
					else if(column == end.column() + 1)
						System.err.print("&r&f");
					
					if(c == '&')
						System.err.print("&&");
					else System.err.print(Character.toChars(c));
					
					++column;
				}
				
				System.err.print("\n&#c" + padding + "&r&8| ");
				System.err.print(" ".repeat(markOffset));
				System.err.print("&9&l");
				System.err.print("^".repeat(markLength));
				System.err.println("&r");
				
				++line;
			}
			
			mark.seek();
		}
	}

	public static void warn(IWarning warning, Positioned position, String string, Object...objects) {
		log(Level.WARN, warning, position, string, objects);
	}

	public static void warn(Positioned position, String string, Object...objects) {
		log(Level.WARN, null, position, string, objects);
	}

	public static void error(Positioned position, String string, Object...objects) {
		log(Level.ERROR, null, position, string, objects);
	}

	public static enum Verbosity {
		QUIET,
		NORMAL,
		VERBOSE
	}
	
	@RequiredArgsConstructor
	public static enum Level {
		DEBUG	("&2", "debug"),
		INFO	("&c", "info"),
		NOTE	("&d", "note"),
		WARN	("&3", "warning"),
		ERROR	("&9", "error"),
		FATAL	("&1", "fatal error");
		
		private final String color;
		private final String name;
		
	}
	
}
