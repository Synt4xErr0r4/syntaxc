/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxc.argp.ArgParser;
import at.syntaxerror.syntaxc.argp.Category;
import at.syntaxerror.syntaxc.argp.Description;
import at.syntaxerror.syntaxc.argp.Help;
import at.syntaxerror.syntaxc.argp.Parameter;
import at.syntaxerror.syntaxc.argp.Transformer;
import at.syntaxerror.syntaxc.argp.Value;
import at.syntaxerror.syntaxc.backend.Backends;
import at.syntaxerror.syntaxc.backend.IBackend;
import at.syntaxerror.syntaxc.config.ConfigRegistry;
import at.syntaxerror.syntaxc.config.ConfigUtils;
import at.syntaxerror.syntaxc.config.std.Standards;
import at.syntaxerror.syntaxc.frontend.Frontends;
import at.syntaxerror.syntaxc.frontend.IFrontend;
import at.syntaxerror.syntaxc.io.ColorOutputStream;
import at.syntaxerror.syntaxc.io.IFileStream;
import lombok.ToString;

/**
 * 
 *
 * @author Thomas Kasper
 */
public class Main {

	public static final PrintStream STDOUT = System.out;
	public static final PrintStream STDERR = System.err;
	
	private static void printFlags(ArgParser argp) {
		argp.printPrologue();
		
		System.out.println("Available options for »-f«:");
		System.out.println();
		
		ConfigUtils.printAll(ConfigRegistry.FLAGS.getEntries());
		
		argp.printEpilogue();
		System.exit(0);
	}

	private static void printWarnings(ArgParser argp) {
		argp.printPrologue();
		
		System.out.println("Available options for »-W«:");
		System.out.println();
		
		ConfigUtils.printAll(ConfigRegistry.WARNINGS.getEntries());

		argp.printEpilogue();
		System.exit(0);
	}

	private static void printStds(ArgParser argp) {
		argp.printPrologue();
		
		System.out.println("Available options for »--std«:");
		System.out.println();
		
		for(var entry : Standards.getStandards().entrySet())
			System.out.printf("  »%s« (%s)%n", entry.getKey(), entry.getValue().getName());

		System.out.println();
		System.out.println("When no language is specified, the default");
		System.out.println("language standard is used instead:");
		System.out.println();
		
		for(var frontend : Frontends.getFrontends())
			System.out.printf("  »%s« (%s)%n", frontend.getStandard().getName(), frontend.getName());
		
		argp.printEpilogue();
		System.exit(0);
	}

	private static void printLangs(ArgParser argp) {
		argp.printPrologue();
		
		System.out.println("Available options for »-x«:");
		System.out.println();
		
		for(var entry : Frontends.getLanguages().entrySet())
			System.out.printf("  »%s« (%s)%n", entry.getKey(), entry.getValue().getName());

		System.out.println();
		System.out.println("When no language is specified, the language");
		System.out.println("is determined by the file name extension:");
		System.out.println();
		
		for(var frontend : Frontends.getFrontends())
			System.out.printf("  ».%s« (%s)%n", String.join("«, ».", frontend.getFileExtensions().keySet()), frontend.getName());
		
		argp.printEpilogue();
		System.exit(0);
	}

	private static void printTargets(ArgParser argp) {
		argp.printPrologue();
		
		System.out.println("Available options for »-march«:");
		System.out.println();
		
		for(var entry : Backends.getNames().entrySet())
			System.out.printf("  »%s« (%s)%n", entry.getKey(), entry.getValue().getName());
		
		argp.printEpilogue();
		System.exit(0);
	}
	
	public static void main(String[] args) {
		System.setOut(ColorOutputStream.ofPrintStream(STDOUT));
		System.setErr(ColorOutputStream.ofPrintStream(STDERR));
		
		Frontends.init();
		Backends.init();
		
		args = new String[] {
			"/usr/bin/syntaxc",
			"--std=c18", "-xc",
			"/opt/syntaxc/test.c"
		};

		InputArgs input = new InputArgs();
		OutputArgs output = new OutputArgs();
		ConfigArgs config = new ConfigArgs();
		MiscArgs misc = new MiscArgs();
		
		ArgParser argp = new ArgParser()
			.programName(args.length == 0 ? "syntaxc" : args[0])
			.withObject(input)
			.withObject(output)
			.withObject(config)
			.withObject(misc)
			.prologue("""
				&lSyntaxC v2.0.0&r
				(c) 2024, Thomas Kasper
				Licensed under the MIT license""")
			.epilogue("""
				Report bugs here:
				<https://github.com/Synt4xErr0r4/syntaxc/issues/>""");
		
		Frontends.getFrontends().stream()
			.map(IFrontend::getCommandLineOptions)
			.forEach(list -> list.forEach(argp::withObject));

		Backends.getBackends().stream()
			.map(IBackend::getCommandLineOptions)
			.forEach(list -> list.forEach(argp::withObject));
		
		try {
			argp.parse(Arrays.copyOfRange(args, 1, args.length));
		}
		catch (Exception e) {
			argp.fail("%s", e.getMessage());
		}
		
		if(misc.printFlags)
			printFlags(argp);
		
		if(misc.printWarnings)
			printWarnings(argp);
		
		if(misc.printStds)
			printStds(argp);
		
		if(misc.printLangs)
			printLangs(argp);
		
		if(misc.printTargets)
			printTargets(argp);
		
		IBackend backend = null;
		
		if(config.machineOptions.containsKey("arch")) {
			String backendId = config.machineOptions.remove("arch");
			
			if(backendId == null)
				argp.fail("Machine option »arch« requires a value");
			
			if(!backendId.equals("native")) {
				backend = Backends.getNames().get(backendId);
				
				if(backend == null)
					argp.fail("Unknown architecture »%s«", backendId);
			}
		}
		
		if(backend == null)
			backend = Backends.getSystemBackend();
		
		try {
			input.files.forEach(IFileStream::determineLanguage);
		}
		catch (Exception e) {
			argp.fail("%s", e.getMessage());
		}
		
		if(input.files.isEmpty())
			argp.fail("no input files specified");
		
		backend.init();
		
		var langs = Frontends.getLanguages();
		
		for(var file : input.files) {
			file.seek(0);
			
			var frontend = langs.get(file.getLanguage());
			
			var ir = frontend.process(file);
			
			// TODO code-gen, asm, link
		}
	}

	@ToString
	@Category("Output configuration")
	public static class OutputArgs {
		
		@Parameter(name = "output", mnemonic = 'o')
		@Value("path")
		@Description("""
			Specifies the output file path. If no path is provided or points to a directory,
			 the output filename is derived from the input file. Specifying »-« redirects
			 output to stdout
			""")
		public String output;
		
		@Parameter(mnemonic = 'S')
		@Description("Stops compilation after code generation (generates an assembly file)")
		public boolean outputAssembly;

		@Parameter(mnemonic = 'c')
		@Description("Stops compilation after assembling (generates an object file)")
		public boolean outputCompiled;
		
	}

	@ToString
	@Category("General configuration")
	public static class ConfigArgs {
		
		@Parameter(mnemonic = 'f')
		@Value("[no-]flag[=value]")
		@Description("Sets or unsets a compiler flag (see »--print-flags«)")
		public Map<String, String> flags = new LinkedHashMap<>();

		@Parameter(mnemonic = 'W')
		@Value("[no-]warning[=value]")
		@Description("Enables or disables a diagnostic warning (see »--print-warnings«)")
		public Map<String, String> warnings = new LinkedHashMap<>();

		@Parameter(mnemonic = 'm')
		@Value("[no-]option[=value]")
		@Description("Enables or disables a machine-dependent option (see »--print-machine«)")
		public Map<String, String> machineOptions = new LinkedHashMap<>();
		
	}

	@ToString
	@Category("Miscellaneous configuration")
	public static class MiscArgs {
		
		@Parameter(name = "print-flags")
		@Description("Prints all available flags and exits")
		public boolean printFlags;

		@Parameter(name = "print-warnings")
		@Description("Prints all available warnings and exits")
		public boolean printWarnings;

		@Parameter(name = "print-stds")
		@Description("Prints all available language standards and exits")
		public boolean printStds;

		@Parameter(name = "print-langs")
		@Description("Prints all available languages and exits")
		public boolean printLangs;

		@Parameter(name = "print-targets")
		@Description("Prints all available targets and exits")
		public boolean printTargets;
		
	}

	@ToString
	public static class InputArgs {
		
		@Parameter(name = "help", mnemonic = 'h')
		@Description("Shows this help")
		@Help
		public boolean help;

		@Parameter(name = "std")
		@Value(value = "standard")
		@Description("Specifies the language standard (see »--print-stds«)")
		@Transformer(Standards.Transformer.class)
		public Void standard;
		
		@Parameter(name = "language", mnemonic = 'x')
		@Value(value = "lang", required = false)
		@Description("""
			Specifies the language for subsequent input files (see »--print-langs«).
			 When no language is specified, it is determined by the file extension instead
			""")
		@Transformer(Frontends.Transformer.class)
		public boolean language;
		public Void language$value;
		
		@Parameter
		@Value("FILE")
		@Description("Specifies one or more input files")
		@Transformer(IFileStream.Transformer.class)
		public List<IFileStream> files = new ArrayList<>();
		
	}
	
}
