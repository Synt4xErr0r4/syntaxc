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
package at.syntaxerror.syntaxc;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.SystemUtils.OperatingSystem;
import at.syntaxerror.syntaxc.generator.arch.Architecture;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.io.CharStream;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.AnsiPipe;
import at.syntaxerror.syntaxc.misc.Flag;
import at.syntaxerror.syntaxc.misc.IncludePathRegistry;
import at.syntaxerror.syntaxc.misc.NamedToggle;
import at.syntaxerror.syntaxc.misc.Optimization;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.misc.Warning.WarningGroup;
import at.syntaxerror.syntaxc.options.Option;
import at.syntaxerror.syntaxc.options.OptionParser;
import at.syntaxerror.syntaxc.options.OptionResult;
import at.syntaxerror.syntaxc.preprocessor.PreLexer;
import at.syntaxerror.syntaxc.preprocessor.macro.BuiltinMacro;
import at.syntaxerror.syntaxc.tracking.Position;

/**
 * @author Thomas Kasper
 * 
 */
public class SyntaxCMain {

	public static void main(String[] args) {
		Locale.setDefault(Locale.ROOT);
		AnsiPipe.init();
		
		if(Boolean.getBoolean("DEBUG"))
			args = new String[] {
				Boolean.getBoolean("DEBUG-WIN")
					? System.getenv("APPDATA") + "\\SyntaxC\\test.c"
					: "/opt/syntaxc/test/test.c",
				"-o", "-",
				"-fno-stdlib",
				"-fno-long-double",
				"-fsyntax-tree=svg",
				"-fcontrol-flow-graph=svg",
				"-fcfg-verbose",
				"-S"
			}; // XXX debugging only
		
		OptionParser parser = new OptionParser()
			.withCommand("java -jar syntaxc.jar")
			.withHeader("§9§lSyntax§1C§r §8- §fAn ANSI-C compiler written in Java")
			.withFooter("§7(c) 2022, Thomas Kasper §8<§5thomas@syntaxerror.at§8>")
			
			.with('v', "version")					.description("Show the version and other information").build()
			
			.with('h', "help")						.description("Show this help").build()
			.with('d', "doc").argument("option")	.description("Show documentation for an option").build()

			.with('S')								.description("Compile, but don't assemble").build()
			.with('c')								.description("Compile and assemble, but don't link").build()
			.with('E')								.description("Output preprocessed file").build()
			
			.with('D').compact("defn[=value]")		.description("Define a macro", CLIDoc::define).build()
			.with('U').compact("name")				.description("Undefine a predefined macro").build()
			
			.with('I').compact("path")				.description("Add a directory to the include path", CLIDoc::include).build()
			.with('m').compact("option[=value]")	.description("Configure the target architecture", CLIDoc::machine).build()
			.with('W').compact("[no-]warning")		.description("Enable or disable a specific compiler warning", CLIDoc::warning).build()
			.with('f').compact("[no-]flag[=value]")	.description("Enable or disable a specific compiler flag", CLIDoc::flag).build()
			.with('O').compact("[no-]opt[=value]")	.description("Enable or disable a specific compiler optimization", CLIDoc::optimize).build()
			
			.with('o').argument("file").description("Specify the output file").build()
			.with().argument("file").description("Specify the input file").required().build();
		
		OptionResult result = parser.parse(args);

		/*		=====================
		 * 			DOCUMENTATION
		 * 		=====================
		 */
		
		if(result.has("help"))
			parser.showHelp();
		
		if(result.has("doc")) {
			String arg = result.get("doc").get(0);
			
			Option option = parser.get(arg);
			
			if(option == null && arg.length() == 1)
				option = parser.get(arg.charAt(0));
			
			if(option == null)
				parser.showUsage("Unrecognized option »%s«", arg);
			
			parser.showDocumentation(option);
			return;
		}
		
		/*		========================
		 * 			COMPILATION MODE
		 * 		========================
		 */
		
		SyntaxC.onlyCompile = result.has('S');
		SyntaxC.onlyAssemble = result.has('c');
		SyntaxC.onlyPreprocess = result.has('E');
		
		if(SyntaxC.onlyCompile && SyntaxC.onlyAssemble)
			parser.showUsage("Options -S and -c are mutually exclusive");
		
		else if(SyntaxC.onlyAssemble && SyntaxC.onlyPreprocess)
			parser.showUsage("Options -c and -E are mutually exclusive");
		
		else if(SyntaxC.onlyPreprocess && SyntaxC.onlyCompile)
			parser.showUsage("Options -E and -S are mutually exclusive");

		/*		===================
		 * 			CLI OPTIONS
		 * 		===================
		 */
		
		result.get('m').forEach(CLIHandler::machine);
		
		result.get('W').forEach(CLIHandler::warning);
		
		result.get('f').forEach(CLIHandler::flag);
		
		result.get('O').forEach(CLIHandler::optimize);
		
		result.get('D').forEach(CLIHandler::define);
		result.get('U').forEach(CLIHandler::undef);
		
		if(Flag.NO_STDLIB.isEnabled())
			IncludePathRegistry.clear();
		
		result.get('I').forEach(IncludePathRegistry::add);
		
		if(result.has("version")) {
			CLIHandler.version();
			return;
		}

		/*		=========================
		 * 			INPUT/OUTPUT FILE
		 * 		=========================
		 */
		
		if(!result.hasUnnamed())
			parser.showUsage("Missing input file");
		
		if(result.getUnnamedCount() != 1)
			parser.showUsage("Too many input files specified");
			
		String file = result.getUnnamed().get(0);
		
		if(!file.endsWith(".c"))
			parser.showUsage("Illegal file name extension for input file");
		
		CharStream input = CharStream.fromFile(file, null);
		
		String base = file.substring(0, file.length() - 2);
		
		if(result.has('o')) {
			if(result.getCount('o') != 1)
				parser.showUsage("Too many output files specified");
			
			SyntaxC.outputFileName = result.get('o').get(0);
		}

		if(Flag.SYNTAX_TREE.isEnabled())
			SyntaxC.syntaxTree = SyntaxC.createStream(
				parser,
				base + ".syntaxtree."
					+ getGraphExtension(Flag.SYNTAX_TREE)
			);

		if(Flag.CONTROL_FLOW_GRAPH.isEnabled())
			SyntaxC.controlFlowGraph = SyntaxC.createStream(
				parser,
				base + ".cfg."
					+ getGraphExtension(Flag.CONTROL_FLOW_GRAPH)
			);
		
		SyntaxC.inputFileName = file;
		
		SyntaxC.compile(input);
	}
	
	@SuppressWarnings("preview")
	private static String getGraphExtension(Flag flag) {
		return switch(flag.getValue().toLowerCase()) {
		case "svg" -> "svg";
		case "png" -> "png";
		case null -> "dot";
		default -> "dot";
		};
	}
	
	private static Pair<String, String> split(String value, char delimiter) {
		int index = value.indexOf(delimiter);
		
		return index == -1
			? Pair.of(value, null)
			: Pair.of(value.substring(0, index), value.substring(index + 1));
	}
	
	/* Utility class for command line option handling */
	private static class CLIHandler {
		
		private static void version() {
			ArchitectureRegistry.getArchitecture().onInit();
			
			System.out.println("§9§lSyntax§1C§r v" + SyntaxC.Version.VERSION);
			
			System.out.println("Architecture: " + ArchitectureRegistry.getArchitecture().getNames()[0]);
			System.out.println("Bit size: " + ArchitectureRegistry.getBitSize());
			System.out.println("Target system: " + ArchitectureRegistry.getOperatingSystem());
			System.out.println("Endianness: " + (ArchitectureRegistry.getEndianness() == ByteOrder.BIG_ENDIAN ? "big" : "little"));
			
			System.out.println("Include path:");
			
			IncludePathRegistry.getIncludePath()
				.forEach(path -> System.out.println("  " + path));
			
			System.out.println("Predefined macros:");
			
			List<String> keys = new ArrayList<>(BuiltinMacro.getBuiltinMacros().keySet());
			
			Collections.sort(keys);
			
			keys.forEach(key -> {
				BuiltinMacro macro = BuiltinMacro.getBuiltinMacros().get(key);

				System.out.printf("  %s = %s\n", key, macro);
			});
		}
		
		private static void undef(String name) {
			if(BuiltinMacro.getBuiltinMacros().containsKey(name))
				Logger.warn(Warning.UNDEF, "Undefinition of non-existent macro »%s«", name);
			
			else BuiltinMacro.getBuiltinMacros().remove(name);
		}
		
		private static void define(String arg) {
			var definition = split(arg, '=');
			
			String name = definition.getLeft();
			String value = definition.getRight();
			
			if(BuiltinMacro.getBuiltinMacros().containsKey(name))
				Logger.warn(Warning.REDEF, "Redefinition of predefined macro »%s«", name);
			
			if(value == null)
				BuiltinMacro.define(name);
			
			else {
				value = value.strip();
				
				Position pos = new Position(Position.ARGUMENT, 0, 0, 0, value.length(), null);
				
				PreLexer lexer = new PreLexer(CharStream.fromString(value, pos));
				
				List<Token> tokens = new ArrayList<>();
				
				Token tok;
				
				while((tok = lexer.nextToken()) != null)
					if(!tok.is(TokenType.NEWLINE))
						tokens.add(tok);
				
				BuiltinMacro.defineList(name, self -> tokens, true);
			}
		}
		
		private static void machine(String arg) {
			var option = split(arg, '=');
			
			String name = option.getLeft();
			String value = option.getRight();
			
			if(value == null)
				value = "";
			
			switch(name) {
			case "arch":
				if(value.isBlank())
					Logger.warn("Missing value for assembler option -march");
				
				else {
					Architecture arch = ArchitectureRegistry.find(value);
					
					if(arch == null)
						Logger.warn("Unknown architecture »%s«", value);
					
					else ArchitectureRegistry.setArchitecture(arch);
				}
				
				break;

			case "8": ArchitectureRegistry.setBitSize(BitSize.B8); break;
			case "16": ArchitectureRegistry.setBitSize(BitSize.B16); break;
			case "32": ArchitectureRegistry.setBitSize(BitSize.B32); break;
			case "64": ArchitectureRegistry.setBitSize(BitSize.B64); break;
			case "128": ArchitectureRegistry.setBitSize(BitSize.B128); break;
			
			case "target":
				try {
					ArchitectureRegistry.setOperatingSystem(OperatingSystem.valueOf(value.toUpperCase()));
				} catch(Exception e) {
					Logger.warn("Unknown target system »%s«", value);
				}
				break;
				
			case "endian":
				if(value.equalsIgnoreCase("little"))
					ArchitectureRegistry.setEndianness(ByteOrder.LITTLE_ENDIAN);
				
				else if(value.equalsIgnoreCase("big"))
					ArchitectureRegistry.setEndianness(ByteOrder.BIG_ENDIAN);
				
				else if(value.isBlank())
					Logger.warn("Missing value for assembler option -morder");
				
				else Logger.warn("Unknown endianness »%s«", value);
				
				break;
			
			case "syntax":
				if(!ArchitectureRegistry.getArchitecture().setSyntax(value))
					Logger.warn("Unknown assembly syntax »%s«", value);
				
				break;
				
			default:
				Logger.warn("Unrecognized assembler option »%s«", name);
				break;
			}
		}
		
		private static void warning(String arg) {
			boolean state = true;
			
			if(arg.startsWith("no-")) {
				arg = arg.substring(3);
				state = false;
			}
			
			Warning warning = Warning.of(arg);
			
			if(warning != null) {
				warning.setEnabled(state);
				return;
			}
			
			WarningGroup group = Warning.groupOf(arg);
			
			if(group == null) {
				Logger.warn("Unrecognized warning: %s", arg);
				return;
			}
			
			group.setEnabled(state);
		}

		private static void flag(String arg) {
			var flagData = split(arg, '=');
			
			String flagName = flagData.getLeft();
			String flagValue = flagData.getRight();
			
			boolean state = true;
			
			if(flagName.startsWith("no-")) {
				flagName = flagName.substring(3);
				state = false;
			}
			
			Flag flag = Flag.of(flagName);

			if(flag == null) {
				Logger.warn("Unrecognized flag: %s", flagName);
				return;
			}
			
			if(flagValue != null && !flag.isAcceptsValue()) {
				Logger.warn("Flag »%s« does not accept a value", flagName);
				return;
			}
			else if(flagValue != null)
				flag.setValue(flagValue);
			
			flag.setEnabled(state);
		}
		
		private static void optimize(String arg) {
			var optData = split(arg, '=');
			
			String optName = optData.getLeft();
			String optValue = optData.getRight();
			
			boolean state = true;
			
			if(optName.startsWith("no-")) {
				optName = optName.substring(3);
				state = false;
			}
			
			Optimization opt = Optimization.of(optName);

			if(opt == null) {
				Logger.warn("Unrecognized optimization: %s", optName);
				return;
			}
			
			if(optValue != null && !opt.isAcceptsValue()) {
				Logger.warn("Optimization »%s« does not accept a value", optName);
				return;
			}
			else if(optValue != null)
				opt.setValue(optValue);
			
			opt.setEnabled(state);
		}
		
	}
	
	/* Utility class for command line option documentation */
	private static class CLIDoc {

		private static void define() {
			System.out.print(
				"""
				The parameter specifies the name of the macro.
				Optionally, a value can be set by separating name and value with an equals sign (§c=§f)
				
				Examples:
				a. §c-DDEBUG §f- This defines a macro called §cDEBUG §fwithout a value
				   This is effectively equal to §d#define DEBUG§f
				b. §c-DPI=3.14 §f- This defines a macro called §cPI §fwith the value §c3.14§f
				   This is effectively equal to §d#define PI 3.14§f
				
				Omitting the value §land §rthe equals sign (§c=§f) results in the macro having a value of §c1§f.
				"""
			);
		}

		private static void include() {
			
		}
		
		private static void machine() {
			System.out.print(
				"""
				The parameter specifies the option regarding the target machine.
				A value can be set by separating name and value with an equals sign (§c=§f; might not be required by every option)
				
				Examples:
				a. §c-m32 §f- This sets the bit size to 32§f
				b. §c-march=x86 §f- This sets the target architecture to x86§f
				
				The following options are currently defined:
				
				 §8- §a-march=ARCH§f         Specifies the target architecture (see below)
				 §8- §a-mtarget=TARGET§f     Specifies the target system (see below)
				 §8- §a-mendian=ENDIANNESS§f Specifies the target endianness (see below)
				 §8- §a-masm=SYNTAX§f        Specifies the assembly syntax (see below)
				 §8- §a-m8§f                 Sets the bit size to 8
				 §8- §a-m16§f                Sets the bit size to 16
				 §8- §a-m32§f                Sets the bit size to 32
				 §8- §a-m64§f                Sets the bit size to 64
				 §8- §a-m128§f               Sets the bit size to 128
				
				Supported architectures:
				
				"""
			);
			
			ArchitectureRegistry.getArchitectures()
				.forEach(arch -> {
					String[] names = arch.getNames();
					
					System.out.printf(" §8- §a%s§r", names[0]);
					
					if(names.length != 1)
						System.out.printf(" §8(aka. §a%s§8)", String.join("§8, §a", Arrays.copyOfRange(names, 1, names.length)));
					
					System.out.println(getNotice(ArchitectureRegistry.getArchitecture() == arch));
				});
			
			System.out.println("\nSupported targets:\n");

			for(OperatingSystem system : OperatingSystem.values())
				if(system != OperatingSystem.UNSPECIFIED)
					System.err.printf(
						" §8- §a%s%s§r\n",
						system,
						getNotice(ArchitectureRegistry.getOperatingSystem() == system)
					);
			
			System.out.printf(
				"""
				
				Supported endiannesses:
				
				 §8- §alittle%s
				 §8- §abig%s§r
				""",
				getNotice(ArchitectureRegistry.getEndianness() == ByteOrder.LITTLE_ENDIAN),
				getNotice(ArchitectureRegistry.getEndianness() == ByteOrder.BIG_ENDIAN)
			);
			
			System.out.println("\nSupported assembly syntaxes:\n");


			ArchitectureRegistry.getArchitectures()
				.forEach(arch -> {
					System.err.printf(
						" §8- §ffor §a%s§f:\n",
						arch.getNames()[0]
					);
						
					String defaultSyntax = arch.getSyntax();
					
					for(String syntax : arch.getSyntaxes())
						System.err.printf(
							"   §8- §a%s%s§r\n",
							syntax,
							getNotice(syntax == defaultSyntax || syntax.equals(defaultSyntax))
						);
				});
			
		}
		
		private static String getNotice(boolean flag) {
			return flag ? " §8(active)§r" : "";
		}
		
		private static void printList(Collection<? extends NamedToggle> list) {
			list.forEach(toggle -> {
				String name = toggle.getName();
				
				int len = name.length();
				
				System.out.printf(" §8- §a%s%s§f%s§r\n", name, " ".repeat(Math.max(1, 20 - len)), toggle.getDescription());
			});
			
			if(list.isEmpty())
				System.out.println(" §8<none>§r");
		}
		
		private static void warning() {
			System.out.println(
				"""
				The parameter specifies the warning to be disabled/enabled.
				
				Prefixing the name with 'no-' disables the warning:
				a. §c-Wwarning-name-here §aenables §fthe warning named '§cwarning-name-here§f'
				b. §c-Wno-warning-name-here §9disables §fthe warning named '§cwarning-name-here§f'
				
				List of warnings:
				"""
			);
			
			printList(Warning.getWarnings());
			
			System.out.println("\nList of warning groups:\n");
			
			printList(Warning.getGroups());
		}
		
		private static void flag() {
			System.out.println(
				"""
				The parameter specifies the flag to be disabled/enabled.
				
				Prefixing the name with 'no-' disables the flag:
				a. §c-fflag-name-here §aenables §fthe flag named '§cflag-name-here§f'
				b. §c-fno-flag-name-here §9disables §fthe flag named '§cflag-name-here§f'
				
				List of flags:
				"""
			);
			
			printList(Flag.getFlags());
		}
		
		private static void optimize() {
			System.out.println(
				"""
				The parameter specifies the optimization to be disabled/enabled.
				
				Prefixing the name with 'no-' disables the optimization:
				a. §c-Oopt-name-here §aenables §fthe optimization named '§copt-name-here§f'
				b. §c-Ono-opt-name-here §9disables §fthe optimization named '§copt-name-here§f'
				
				List of optimization:
				"""
			);
			
			printList(Optimization.getOptimizations());
		}
		
	}
	
}
