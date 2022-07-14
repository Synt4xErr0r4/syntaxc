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
package at.syntaxerror.syntaxc.options;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxc.misc.Pair;

/**
 * @author Thomas Kasper
 * 
 */
public class OptionParser {
	
	private static final int PREFIX_WIDTH = 32;

	private final Map<String, Option> optionsName;
	private final Map<Character, Option> optionsMnemonic;
	private final List<Option> options;
	private Option fallback;

	private String command;
	private String header;
	private String footer;
	
	public OptionParser() {
		optionsName = new LinkedHashMap<>();
		optionsMnemonic = new LinkedHashMap<>();
		options = new ArrayList<>();
	}
	
	protected void add(Option option) {
		var id = option.identifiers();
		
		if(id.hasNone()) {
			if(fallback != null)
				throw new IllegalArgumentException("Duplicate fallback options");
			
			fallback = option;
		}
		
		else {
			if(id.hasSecond()) {
				if(optionsName.containsKey(id.getSecond()))
					throw new IllegalArgumentException("Duplicate option " + id.getSecond());
				
				optionsName.put(id.getSecond(), option);
			}
			
			if(id.hasFirst()) {
				if(optionsMnemonic.containsKey(id.getFirst()))
					throw new IllegalArgumentException("Duplicate option " + id.getFirst());
				
				optionsMnemonic.put(id.getFirst(), option);
			}
		}
		
		options.add(option);
	}
	
	public OptionParser withCommand(String command) {
		this.command = command;
		return this;
	}
	
	public OptionParser withHeader(String header) {
		this.header = header;
		return this;
	}
	
	public OptionParser withFooter(String footer) {
		this.footer = footer;
		return this;
	}
	
	public OptionBuilder with() {
		return new OptionBuilder(this, (char) 0, null);
	}

	public OptionBuilder with(char mnemonic) {
		return with(mnemonic, null);
	}
	
	public OptionBuilder with(String name) {
		return with((char) 0, name);
	}

	public OptionBuilder with(char mnemonic, String name) {
		return new OptionBuilder(this, mnemonic, name);
	}
	
	public Option get(String name) {
		return optionsName.get(name);
	}
	
	public Option get(char mnemonic) {
		return optionsMnemonic.get(mnemonic);
	}
	
	public OptionResult parse(String[] args) {
		OptionResult result = new OptionResult(this);
		
		int length = args.length;
		
		for(int i = 0; i < length; ++i) {
			String arg = args[i];
			
			if(arg.startsWith("--")) {
				arg = arg.substring(2);
				
				if(arg.isBlank()) {
					if(++i < length) {
						arg = args[i];
						
						if(fallback == null)
							showUsage("Illegal argument »%s«", arg);
						
						result.add(fallback, arg);
					}
					
					continue;
				}
				
				Option option = get(arg);
				
				if(option == null)
					showUsage("Unrecognized option »%s«", arg);
				
				if(option.argName() != null) {
					if(++i >= length)
						showUsage("Missing argument for option »%s«", arg);
					
					result.add(option, args[i]);
				}
				else result.add(option, null);
				
				continue;
			}
			
			if(arg.startsWith("-")) {
				arg = arg.substring(1);
				
				if(arg.isBlank()) {
					if(fallback == null)
						showUsage("Illegal argument »-«");
					
					result.add(fallback, "-");
					continue;
				}
				
				char c = arg.charAt(0);
				
				Option option = get(c);
				
				if(option == null)
					showUsage("Unrecognized option »%c«", c);
				
				if(option.argName() != null) {
					if(option.argCompact()) {
						if(arg.length() < 2)
							showUsage("Missing argument for option »%c«", c);
						
						result.add(option, arg.substring(1));
					}
					else {
						if(arg.length() > 1)
							showUsage("Illegal compact argument for option »%c«", c);
						
						if(++i >= length)
							showUsage("Missing argument for option »%c«", c);
						
						result.add(option, args[i]);
					}
				}
				else result.add(option, null);
				
				continue;
			}

			if(fallback == null)
				showUsage("Illegal argument »%s«", arg);
			
			result.add(fallback, arg);
		}
		
		return result;
	}
	
	private void printUsage() {
		System.out.print("§9Usage: §c");
		
		if(command == null)
			System.out.print("java -jar program.jar");
		else System.out.print(command);
		
		for(Option option : options) {
			boolean required = option.required();

			var id = option.identifiers();
			String argName = option.argName();
			
			if(required)
				System.out.print(' ');
			else System.out.print(" §8[");

			if(id.hasFirst()) {
				System.out.printf("§7-§a%c", id.getFirst());
				
				if(!option.argCompact() && argName != null)
					System.out.print(' ');
			}
			
			else if(id.hasSecond()) {
				System.out.printf("§7--§a%s", id.getSecond());

				if(argName != null)
					System.out.print(' ');
			}
			
			else {
				System.out.printf("§b%s", argName);
				argName = null;
			}
			
			if(argName != null)
				System.out.printf("§7<§b%s§7>", argName);
			
			if(!required)
				System.out.print("§8]");
		}
		
		System.out.println();
	}
	
	public void showUsage(String message, Object...args) {
		System.out.println(header);
		System.out.println();
		
		System.out.print("§9");
		System.out.printf(message, args);
		System.out.println("\n");
		
		printUsage();
		
		System.out.println();
		System.out.println(footer);
		
		System.exit(1);
	}
	
	private void printDescription(int width, String description) {
		if(description == null)
			System.out.println();
		
		else {
			if(width + 1 > PREFIX_WIDTH) {
				System.out.println();
				System.out.print(" ".repeat(PREFIX_WIDTH));
			}
			else System.out.print(" ".repeat(PREFIX_WIDTH - width));
			
			System.out.print("§f");
			System.out.println(description);
		}
	}

	private Pair<String, Integer> getPrefix(Option option, boolean print) {
		var id = option.identifiers();
		String argName = option.argName();
		
		String prefix = null;
		int width = 0;
		
		if(argName != null && option.argCompact()) {
			if(id.hasBoth()) {
				prefix = "  §7-§e%c§7<§b%s§7>".formatted(id.getFirst(), argName);
				
				if(print) {
					System.out.print(prefix);
					
					printDescription(6 + argName.length(), "Equivalent to --%s <%s>".formatted(id.getSecond(), argName));
					
					prefix = "";
				}
				else prefix += "§8, ";
				
				prefix += " §7--§e%s §7<§b%s§7>".formatted(id.getSecond(), argName);
				width = 6 + id.getSecond().length() + argName.length();
			}
			
			/*   -X<abc> */
			else if(id.hasFirst()) {
				prefix = "  §7-§e%c§7<§b%s§7>".formatted(id.getFirst(), argName);
				width = 6 + argName.length();
			}
		}
		
		if(prefix == null) {
			if(id.hasBoth()) {
				prefix = "  §7-§e%c§8, §7--§e%s".formatted(id.getFirst(), id.getSecond());
				width = 8 + id.getSecond().length();
			}
			
			else if(id.hasFirst()) {
				prefix = "  §7-§e%c§8".formatted(id.getFirst());
				width = 4;
			}
			
			else if(id.hasSecond()) {
				prefix = " §7--§e%s§8".formatted(id.getSecond());
				width = 3 + id.getSecond().length();
			}
			
			else {
				prefix = "  §b" + argName;
				width = 2 + argName.length();
				argName = null;
			}
			
			if(argName != null) {
				prefix += " §7<§b%s§7>".formatted(argName);
				width += 3 + argName.length();
			}
		}
		
		return Pair.of(prefix, width);
	}
	
	public void showHelp() {
		System.out.println(header);
		System.out.println();

		printUsage();
		
		System.out.println();
		System.out.println("§cOptions:");

		for(Option option : options) {
			Pair<String, Integer> prefix = getPrefix(option, true);
			
			System.out.print(prefix.getFirst());
			printDescription(prefix.getSecond(), option.description());
		}
		
		System.out.println();
		System.out.println(footer);

		System.exit(0);
	}

	public void showDocumentation(Option option) {
		System.out.println(header);
		System.out.println();
		
		if(option.documentation() == null) {
			System.out.print("§9No documentation for ");
			
			var id = option.identifiers();
			
			if(id.hasSecond())
				System.out.println("--" + id.getSecond());
			
			else if(id.hasFirst())
				System.out.println("-" + id.getFirst());
			
			else System.out.println("unnamed option");
		}
		else {
			System.out.printf("§9Documentation for %s\n\n", getPrefix(option, false).getFirst().stripLeading());
			
			option.documentation().run();
		}
		
		System.out.println();
		System.out.println(footer);

		System.exit(0);
	}
	
}
