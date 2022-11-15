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
package at.syntaxerror.syntaxc.logger;

import at.syntaxerror.syntaxc.SyntaxC;
import at.syntaxerror.syntaxc.misc.Flag;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.preprocessor.macro.Macro;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class Logger {
	
	// how many characters are to be displayed before and after the erroneous section
	private static final int OFFSET = 20;
	
	private static String sanitizeSection(String section) {
		char[] chars = section.toCharArray();
		
		for(int i = 0; i < chars.length; ++i) {
			char c = chars[i];
			
			if((c >= 0x0000 && c < 0x0020) || c == 0x007F) {
				switch(c) {
		        case '\b':		c = '←'; break;
		        case '\t':		c = ' '; break; // replace horizontal tab with space
		        case '\n':		c = '↵'; break;
		        case '\u000B':	c = '↹'; break; // vertical tab
		        case '\f':		c = '↡'; break;
		        case '\r':		c = '↵'; break;
		        case '\u007F':	c = '\u2421'; break;
		        default:		c = (char) (0x2400 | c); break; /* U+2400 to U+2421 */
		        }
				
				chars[i] = c;
			}
		}
		
		return String.valueOf(chars);
	}
	
	private static int stringWidth(String section) {
		return section
			.codePoints()
			.map(WCWidth::wcwidth)
			.sum();
	}

	/* when true, calling log(...) with LogLevel.ERROR will not terminate the compilation process.
	 * Termination takes instead place before the next compilation phase starts */
	public static boolean recoverNextError = false;
	
	private static void logImpl(@NonNull LogLevel level, Positioned pos, Warning warning, boolean recover, String message, Object...args) {
		if(warning == null)
			warning = Warning.NONE;
		
		if(!warning.isEnabled())
			return;
		
		Position position = pos == null ? null : pos.getPosition();
		
		if(position == null) // print '<builtin>: '
			System.out.print("§b<builtin>§7: ");
		
		else if(position.bytenum() == Position.ARGUMENT)
			System.out.print("§b<argument>§7: ");
		
		else System.out.printf( // print 'file:line:column: '
			"§b%s§7:§b%d§7:§b%d§7: ",
			position.file().getName(),
			position.line() + 1,
			position.column() + 1
		);

	    System.out.print(level.getPrefix()); // print color prefix, e.g. red for error
		
	    if(warning.getStage() != null)
	    	System.out.print(warning.getStage().getName() + " "); // print stage name, e.g. 'Lexical'
	    
	    System.out.printf("%s: §7", level.getName()); // print level name, e.g. 'ERROR'
		
	    // print actual (formatted) error message
		if(message == null)
			System.out.print("null");
		else System.out.print(message.formatted(args));

	    if(warning.getName() != null)
	        System.out.printf(" §8[§a-W%s§8]", warning.getName()); // print ' [-Wwarning]' (indicates how to disable the warning)
		
	    System.out.println("§r"); // reset color
	    
	    // print code section where error occured
	    if(position != null && position.file() != null) {
	    	// get erroneous section, including a certain portion of the code before and after that
	    	var sectionData = position.file().getSection(position.bytenum(), position.length(), OFFSET);
	    	
	    	String section = sectionData.section();
	    	
	    	if(!section.isBlank()) {
	    		
		    	// true when the section starts/ends exactly on a line break/file start/end of file
		    	boolean newlineStart = section.startsWith("\n");
		    	boolean newlineEnd = section.endsWith("\n");
		    	
		    	section = section.substring(1, section.length() - 1);

		    	int nLines = sectionData.lengths().size();
		    	
		    	long line = position.line();
	
		    	// calculate how many chars the line number will occupy (how many digits it consists of)
		    	int lineWidth = (int) (Math.log10((double) line + nLines + 1) + 1);
		    	
		    	long offset = 0;
		    	
		    	for(int l = 0; l < nLines + 1; ++l) {
		    		int currentLineWidth = (int) (Math.log10((double) line + l + 1) + 1);
		    		
		    		System.out.printf("§#c§f  %s%d §r§8| §f", " ".repeat(lineWidth - currentLineWidth), line + l + 1);
		    		
		    		if(l == 0 && !newlineStart) // indicate that there is code on this line before the displayed section
		    			System.out.print("...");
		    		
		    		String part;
		    		
		    		if(l == nLines)
		    			part = section;
		    		
		    		else {
		    			int off = sectionData.lengths().get(l).intValue();
		    			
		    			part = section.substring(0, off);
		    			section = section.substring(off + 1);
		    		}
		    		
		    		System.out.print(part = sanitizeSection(part));
		    		
		    		if(l == nLines && !newlineEnd) // indicate that there is code on this line after the displayed section
		    			System.out.println("...");
		    		else System.out.println();
		    		
		    		int markerDisplayOffset = 0;
		    		int markerDisplayWidth = part.length();
		    		
		    		int markerCharWidth = markerDisplayWidth;
		    		
		    		if(l == 0) {
		    			if(newlineStart) {
		    				markerDisplayOffset = (int) position.column();
		    				markerDisplayWidth -= markerDisplayOffset;
		    			}
		    			else {
		    				part = "..." + part;
		    				markerDisplayOffset = 3 + OFFSET;
		    				markerDisplayWidth -= OFFSET;
		    			}
		    			
		    			markerCharWidth = markerDisplayWidth;
		    			
		    			markerDisplayWidth = stringWidth(part.substring(markerDisplayOffset, markerDisplayOffset + markerDisplayWidth));
		    			markerDisplayOffset = stringWidth(part.substring(0, markerDisplayOffset));
		    			
		    			if(l == nLines)
		    				markerDisplayWidth = (int) position.length();
		    		}
		    		else if(l == nLines)
		    			markerDisplayWidth = stringWidth(part.substring(0, markerCharWidth = (int) (position.length() - offset)));
		    		
		    		else markerDisplayWidth = stringWidth(part);
		    		
		    		System.out.printf(
		    			"§#c  %s §r§8| §f%s§9%s§r\n",
		    			" ".repeat(lineWidth),
		    			" ".repeat(markerDisplayOffset),
		    			"^".repeat(markerDisplayWidth)
		    		);
		    		
		    		offset += markerCharWidth + 1; // + 1 for new line character
		    	}
	    	
	    	}
	    }
	    
	    System.out.println();
	    
	    if(Flag.VERBOSE.isEnabled() && position != null && level != LogLevel.NOTE) {
	    	var expansions = position.expansions();
	    	var file = position.file();
	    	
		    if(expansions != null && !expansions.isEmpty()) {
		    	if(Flag.VERY_VERBOSE.isEnabled()) {
		    		for(Macro macro : expansions)
			    		if(macro != null)
			    			note(macro, "In expansion of »%s«", macro.getName());
		    	}
		    	
	    		else {
	    			var it = expansions.iterator();
	    			Macro macro;
	    			
	    			do macro = it.next();
	    			while(it.hasNext());
	    			
	    			note(macro, "In expansion of »%s«", macro.getName());
	    		}
		    }
		    
		    if(file != null) {
		    	Position parent = file.getIncludedFrom();
		    	
		    	if(parent != null)
		    		note(parent, "Included from here");
		    }
	    }
	    
		if(level == LogLevel.ERROR) {
			if(recoverNextError || recover) {
				SyntaxC.terminate = true;
				recoverNextError = false;
			}
				
			else System.exit(1);
		}
	}
	
	public static void logRecover(@NonNull LogLevel level, Positioned pos, Warning warning, String message, Object...args) {
		logImpl(level, pos, warning, true, message, args);
	}
	

	public static void log(@NonNull LogLevel level, Positioned pos, Warning warning, String message, Object...args) {
		logImpl(level, pos, warning, false, message, args);
	}

	public static void log(LogLevel level, Positioned position, String message, Object...args) {
		log(level, position, null, message, args);
	}

	public static void log(LogLevel level, Warning warning, String message, Object...args) {
		log(level, null, warning, message, args);
	}
	
	public static void log(LogLevel level, String message, Object...args) {
		log(level, null, null, message, args);
	}
	

	public static void note(Positioned position, Warning warning, String message, Object...args) {
		log(LogLevel.NOTE, position, warning, message, args);
	}
	
	public static void note(Positioned position, String message, Object...args) {
		log(LogLevel.NOTE, position, message, args);
	}

	public static void note(Warning warning, String message, Object...args) {
		log(LogLevel.NOTE, warning, message, args);
	}
	
	public static void note(String message, Object...args) {
		log(LogLevel.NOTE, message, args);
	}
	

	public static void info(Positioned position, Warning warning, String message, Object...args) {
		log(LogLevel.INFO, position, warning, message, args);
	}
	
	public static void info(Positioned position, String message, Object...args) {
		log(LogLevel.INFO, position, message, args);
	}

	public static void info(Warning warning, String message, Object...args) {
		log(LogLevel.INFO, warning, message, args);
	}
	
	public static void info(String message, Object...args) {
		log(LogLevel.INFO, message, args);
	}
	

	public static void warn(Positioned position, Warning warning, String message, Object...args) {
		log(LogLevel.WARN, position, warning, message, args);
	}
	
	public static void warn(Positioned position, String message, Object...args) {
		log(LogLevel.WARN, position, message, args);
	}

	public static void warn(Warning warning, String message, Object...args) {
		log(LogLevel.WARN, warning, message, args);
	}
	
	public static void warn(String message, Object...args) {
		log(LogLevel.WARN, message, args);
	}
	

	public static void error(Positioned position, Warning warning, String message, Object...args) {
		log(LogLevel.ERROR, position, warning, message, args);
	}
	
	public static void error(Positioned position, String message, Object...args) {
		log(LogLevel.ERROR, position, message, args);
	}

	public static void error(Warning warning, String message, Object...args) {
		log(LogLevel.ERROR, warning, message, args);
	}
	
	public static void error(String message, Object...args) {
		log(LogLevel.ERROR, message, args);
	}

	
	public static void error(Positioned position, Flag flag, String message, Object...args) {
		log(LogLevel.ERROR, position, message + formatFlag(flag), args);
	}

	public static void error(Flag flag, String message, Object...args) {
		error(null, flag, message, args);
	}
	

	public static void softError(Positioned position, Warning warning, String message, Object...args) {
		logRecover(LogLevel.ERROR, position, warning, message, args);
	}
	
	public static void softError(Positioned position, String message, Object...args) {
		logRecover(LogLevel.ERROR, position, null, message, args);
	}

	public static void softError(Warning warning, String message, Object...args) {
		logRecover(LogLevel.ERROR, null, warning, message, args);
	}
	
	public static void softError(String message, Object...args) {
		logRecover(LogLevel.ERROR, null, null, message, args);
	}

	
	public static void softError(Positioned position, Flag flag, String message, Object...args) {
		logRecover(LogLevel.ERROR, position, null, message + formatFlag(flag), args);
	}

	public static void softError(Flag flag, String message, Object...args) {
		softError(null, flag, message, args);
	}
	
	private static String formatFlag(Flag flag) {
		if(flag == null)
			return "";

		String name = flag.getName();
		
		if(!flag.isEnabled())
			name = "no-" + name;
		
		return " §8[§a-f" + name + "§8]";
	}
	
}
