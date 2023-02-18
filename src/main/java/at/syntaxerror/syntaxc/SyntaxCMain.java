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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import at.syntaxerror.syntaxc.SystemUtils.OperatingSystem;
import at.syntaxerror.syntaxc.builtin.BuiltinRegistry;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.io.CharStream;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.AnsiPipe;
import at.syntaxerror.syntaxc.misc.IncludePathRegistry;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.misc.config.ConfigRegistry;
import at.syntaxerror.syntaxc.misc.config.Configurable;
import at.syntaxerror.syntaxc.misc.config.Flags;
import at.syntaxerror.syntaxc.misc.config.MachineSpecifics;
import at.syntaxerror.syntaxc.misc.config.Optimizations;
import at.syntaxerror.syntaxc.misc.config.Warnings;
import at.syntaxerror.syntaxc.options.Option;
import at.syntaxerror.syntaxc.options.OptionParser;
import at.syntaxerror.syntaxc.options.OptionResult;
import at.syntaxerror.syntaxc.preprocessor.PreLexer;
import at.syntaxerror.syntaxc.preprocessor.macro.BuiltinMacro;
import at.syntaxerror.syntaxc.tracking.Position;

/**
 * This class represents the entry point of the compiler
 * 
 * @author Thomas Kasper
 * 
 */
public class SyntaxCMain {

	static {
		Thread.setDefaultUncaughtExceptionHandler(
			(t, e) -> {
				
				if(Flags.VERY_VERBOSE.isEnabled())
					e.printStackTrace();
				
				Logger.softError(
					"%s: %s",
					e.getClass().getSimpleName(),
					e.getMessage()
				);
				
				for(StackTraceElement ste : e.getStackTrace())
					Logger.note("%s", ste);
				
				System.exit(1);
			}
		);
		
		/* make sure to load classes */
		MachineSpecifics.init();
		Flags.init();
		Warnings.init();
		Optimizations.init();
		
		BuiltinRegistry.init();
	}
	
	public static void main(String[] args) {
		Locale.setDefault(Locale.ROOT);
		AnsiPipe.init();
		
		if(Boolean.getBoolean("DEBUG"))
			args = new String[] {
				"-S",
				"-m32", 
				"-fno-long-double",
				"-Wno-all",
				"-o", "-", "/opt/syntaxc/test/test.c",
//				"-o", "-", "/opt/syntaxc/test/benchmark/syntaxbench/aes256.c",
				"-I/opt/syntaxc/test/benchmark/ansibench-master/coremark/include",
				"-DCORE_DEBUG=0",
				"-DMEM_METHOD=MEM_STATIC",
				"-DCALLGRIND_RUN=0",
				"-DCOMPILER_REQUIRES_SORT_RETURN",
				"-DUSE_CLOCK=1",
				"-DMICA=0",
				"-fcontrol-flow-graph=svg",
				"-fsyntax-tree=svg",
				"-Ono-goto",
				"-Ono-jump-to-jump"
			};
		
		/*
		 * TODO:
		 * 
		 * - System V calling convention
		 * - Microsoft x64 calling convention
		 * - register allocator spilling
		 * - fix __LINE__ macro
		 */
		
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
			
			.with('D').compact("macro[=defn]")		.description("Define a macro", CLIDoc::define).build()
			.with('U').compact("name")				.description("Undefine a predefined macro").build()
			
			.with('I').compact("dir")				.description("Add a directory to the include path", CLIDoc::include).build()
			.with('m').compact("option[=value]")	.description("Configure the target architecture", CLIDoc::machine).build()
			.with('W').compact("[no-]warning")		.description("Enable or disable a specific compiler warning", CLIDoc::warning).build()
			.with('f').compact("[no-]flag[=value]")	.description("Enable or disable a specific compiler flag", CLIDoc::flag).build()
			.with('O').compact("[no-]opt[=value]")	.description("Enable or disable a specific compiler optimization", CLIDoc::optimize).build()
			
			.with("regen-stdlib")					.description("Regenerates the standard library definitions", CLIDoc::regenStdlib).build()
			.with("remove-stdlib")					.description("Removes the standard library definitions").build()
			
			.with('o').argument("file")				.description("Specify the output file").build()
			.with().argument("file")				.description("Specify the input file").required().build();
		
		OptionResult result = parser.parse(args);

		/*		=====================
		 * 			DOCUMENTATION
		 * 		=====================
		 */
		
		if(result.has("help"))
			parser.showHelp();
		
		if(result.has("doc")) {
			String arg = result.get("doc").get(0);
			
			Option option = parser.get(arg); // recognize option name, e.g. 'version'
			
			if(option == null && arg.length() == 1) // recognize mnemonic, e.g. 'v'
				option = parser.get(arg.charAt(0));
			
			if(option == null)
				parser.showUsage("Unrecognized option »%s«", arg);
			
			parser.showDocumentation(option);
			return;
		}
		
		if(result.has("regen-stdlib")) {
			try {
				IncludePathRegistry.install();
			} catch (Exception e) {
				System.out.printf("§cFailed to install the standard library: %s\n", e.getMessage());
				System.exit(1);
			}
			
			System.out.println("§aSuccessfully installed the standard library.");
			return;
		}
		
		if(result.has("remove-stdlib")) {
			try {
				IncludePathRegistry.uninstall();
			} catch (Exception e) {
				System.out.printf("§cFailed to uninstall the standard library: %s\n", e.getMessage());
				System.exit(1);
			}
			
			System.out.println("§aSuccessfully uninstalled the standard library.");
			return;
		}
		
		/*		========================
		 * 			COMPILATION MODE
		 * 		========================
		 * 
		 * -S only produces assembly code
		 * -c only produces an unlinked object file
		 * -E only produces pre-processed C code
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
		
		// architecture/machine settings
		result.get('m').forEach(ConfigRegistry::enableMachineSpecific);
		
		// enable/disable warnings
		result.get('W').forEach(ConfigRegistry::enableWarning);
		
		// enable/disable flags
		result.get('f').forEach(ConfigRegistry::enableFlag);
		
		// enable/disable optimizations
		result.get('O').forEach(ConfigRegistry::enableOptimization);
		
		// (un)define macros
		result.get('D').forEach(CLIHandler::define);
		result.get('U').forEach(CLIHandler::undef);
		
		if(!Flags.STDLIB.isEnabled())
			IncludePathRegistry.clear();
		
		// include path
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

		if(Flags.SYNTAX_TREE.isEnabled())
			SyntaxC.syntaxTree = SyntaxC.createStream(
				parser,
				base + ".syntaxtree."
					+ getGraphExtension(Flags.SYNTAX_TREE)
			);

		if(Flags.CONTROL_FLOW_GRAPH.isEnabled())
			SyntaxC.controlFlowGraph = SyntaxC.createStream(
				parser,
				base + ".cfg."
					+ getGraphExtension(Flags.CONTROL_FLOW_GRAPH)
			);
		
		if(Flags.ALIGN.isEnabled())
			try {
				int alignment = Integer.parseInt(Flags.ALIGN.getValue());
				
				if((alignment & 3) != 0 || alignment < 0)
					throw new IllegalArgumentException();

				ArchitectureRegistry.setAlignment(alignment);
			} catch (Exception e) {
				parser.showUsage("Illegal alignment specified");
			}
		
		SyntaxC.inputFileName = file;
		
		SyntaxC.compile(input);
	}
	
	/**
	 * Returns the file name extension for the requested graph, depending
	 * on the value of the flag.
	 * Supported extensions are {@code svg}, {@code png} and {@code dot}
	 * 
	 * @param flags the flag of the requested graph
	 * @return the file name extension
	 */
	private static String getGraphExtension(Flags flags) {
		return switch(flags.getValue().toLowerCase()) {
		case "svg" -> "svg";
		case "png" -> "png";
		default -> "dot";
		};
	}
	
	/**
	 * Splits a string at the first occurence of a specified delimiter and
	 * returns the two parts as a pair. If the string does not contain the
	 * delimiter, the pair contains the original string and {@code null}
	 * 
	 * @param value
	 * @param delimiter
	 * @return
	 */
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
				Logger.warn(Warnings.UNDEF, "Undefinition of non-existent macro »%s«", name);
			
			else BuiltinMacro.getBuiltinMacros().remove(name);
		}
		
		private static void define(String arg) {
			var definition = split(arg, '=');
			
			String name = definition.getLeft();
			String value = definition.getRight();
			
			if(name.isBlank()) {
				Logger.warn("Empty name for predefined macro");
				return;
			}

			if(!name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
				Logger.warn("Illegal characters in name for predefined macro");
				return;
			}
			
			if(BuiltinMacro.getBuiltinMacros().containsKey(name))
				Logger.warn(Warnings.REDEF, "Redefinition of predefined macro »%s«", name);
			
			if(value == null)
				BuiltinMacro.define(name);
			
			else {
				value = value.strip();
				
				// generate tokens from input
				
				Position pos = Position.argument(value.length());
				
				PreLexer lexer = new PreLexer(CharStream.fromString(value, pos));
				
				List<Token> tokens = new ArrayList<>();
				
				Token tok;
				
				while((tok = lexer.nextToken()) != null)
					if(!tok.is(TokenType.NEWLINE))
						tokens.add(tok);
				
				BuiltinMacro.defineList(name, self -> tokens, true);
			}
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
			System.out.println(
				"""
				The parameter specifies the path to be added to the include path.
				
				The include path currently consists of the following paths:
				"""
			);
			
			IncludePathRegistry.getIncludePath()
				.forEach(path -> {
					System.out.printf(" §8- §a%s", path.toAbsolutePath().toString());
					
					if(!Files.exists(path) || !Files.isDirectory(path))
						System.out.print(" §8[§9missing§8]");
					
					System.out.println();
				});
			
			System.out.println("\n§fThe include path can be cleared by specifying §c-fno-stdlib§f.");

			Path stdlib = IncludePathRegistry.getSyntaxCLibraryPath();
			
			if(stdlib != null)
				System.out.printf(
					"""
					§9SyntaxC §fprovides all of ANSI-C's standard library definitions (§lnot §r§fan actual implementation), which can be installed to their according path (§a%s§f) by specifying §c--regen-stdlib§f.
					The standard library is currently %s§f.
					""",
					stdlib.toAbsolutePath().toString(),
					Files.exists(stdlib) && Files.isDirectory(stdlib)
						? "§ainstalled"
						: "§9not installed"
				);
		}
		
		private static void regenStdlib() {
			Path stdlib = IncludePathRegistry.getSyntaxCLibraryPath();
			
			System.out.printf(
				"""
				§fInstalls the default ANSI-C standard library definitions to the file system (at §a%s§f).
				This standard library provides all §ldefinitions §r§f(§lnot §r§fimplementations) defined by the ANSI-C standard (ANSI/ISO 9899-1990, Chapter 7).
				The standard library is currently %s§f.
				""",
				stdlib.toAbsolutePath().toString(),
				Files.exists(stdlib) && Files.isDirectory(stdlib)
					? "§ainstalled"
					: "§9not installed"
			);
		}
		
		private static void printList(List<Configurable> list) {
			list.forEach(toggle -> {
				String name = toggle.getName();
				
				int len = name.length();
				
				System.out.printf(" §8- §a%s%s§f%s§r\n", name, " ".repeat(Math.max(1, 20 - len)), toggle.getDescription());
			});
			
			if(list.isEmpty())
				System.out.println(" §8<none>§r");
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
				
				"""
			);

			printList(ConfigRegistry.getMachineSpecifics());

			System.out.println("\nSupported architectures:\n");
			
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
			
			printList(ConfigRegistry.getWarnings());
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
			
			printList(ConfigRegistry.getFlags());
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
			
			printList(ConfigRegistry.getOptimizations());
		}
		
	}
	
}
