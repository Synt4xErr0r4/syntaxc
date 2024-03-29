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

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import at.syntaxerror.syntaxc.analysis.ControlFlowAnalyzer;
import at.syntaxerror.syntaxc.generator.CodeGenerator;
import at.syntaxerror.syntaxc.generator.alloc.RegisterAllocator;
import at.syntaxerror.syntaxc.generator.arch.Architecture;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.generator.asm.AssemblyGenerator;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.ObjectSerializer;
import at.syntaxerror.syntaxc.generator.asm.PeepholeOptimizer;
import at.syntaxerror.syntaxc.generator.asm.PrologueEpilogueInserter;
import at.syntaxerror.syntaxc.generator.asm.insn.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.target.RegisterTarget;
import at.syntaxerror.syntaxc.intermediate.IntermediateGenerator;
import at.syntaxerror.syntaxc.intermediate.graph.ControlFlowGraphGenerator;
import at.syntaxerror.syntaxc.intermediate.graph.ControlFlowGraphGenerator.FunctionData;
import at.syntaxerror.syntaxc.io.CharStream;
import at.syntaxerror.syntaxc.lexer.Lexer;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.AnsiPipe;
import at.syntaxerror.syntaxc.optimizer.GotoOptimizer;
import at.syntaxerror.syntaxc.options.OptionParser;
import at.syntaxerror.syntaxc.parser.Parser;
import at.syntaxerror.syntaxc.parser.node.FunctionNode;
import at.syntaxerror.syntaxc.parser.node.SymbolNode;
import at.syntaxerror.syntaxc.parser.tree.SyntaxTreeGenerator;
import at.syntaxerror.syntaxc.preprocessor.Preprocessor;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.type.FunctionType;
import at.syntaxerror.syntaxc.type.NumericValueType;
import lombok.experimental.UtilityClass;

/**
 * This class is responsible for connecting all the various compilation steps and
 * writing the result to the according output stream 
 * 
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class SyntaxC {
	
	/**
	 * This class contains information about the current version of SyntaxC
	 * 
	 * @author Thomas Kasper
	 *
	 */
	@UtilityClass
	public static class Version {
		
		public static final int MAJOR = 1;
		public static final int MINOR = 0;
		public static final int PATCH = 0;

		public static final String VERSION = "%d.%d.%d".formatted(MAJOR, MINOR, PATCH);
		
	}
	
	public static boolean terminate; // true if the compilation should be terminated after the current stage has completed

	public static boolean onlyAssemble;		// -c (don't link)
	public static boolean onlyCompile;		// -S (don't assemble)
	public static boolean onlyPreprocess;	// -E (don't parse)
	
	public static OutputStream syntaxTree;		 // -fsyntax-tree
	public static OutputStream controlFlowGraph; // -fcontrol-flow-graph
	
	public static String inputFileName;
	public static String outputFileName;

	/**
	 * Compiles the given stream of characters into the output format
	 * specified by {@link #onlyAssemble}, {@link #onlyCompile}, and
	 * {@link #onlyPreprocess}. Also generates a {@link #syntaxTree attributed syntax tree}
	 * and {@link #controlFlowGraph control flow graph}, if requested.
	 * 
	 * @param input the input file contents
	 */
	public static void compile(CharStream input) {
		Architecture architecture = ArchitectureRegistry.getArchitecture();
		
		architecture.onInit();
		
		/* Preprocess input file */
		
		List<Token> tokens = new Preprocessor(input).preprocess();
		
		checkTerminationState();
		
		/* Postprocess tokens */
		
		int len = tokens.size();
		
		if(onlyPreprocess) {

			try(OutputStream output = constructOutput(".preproc.c")) {
				for(int i = 0; i < len; ++i) {
					Token token = tokens.get(i);
					
					if(token.is(TokenType.NEWLINE))
						output.write('\n');
					
					else output.write(token.getRaw().getBytes(StandardCharsets.UTF_8));
				}
			} catch (Exception e) {
				outputFailed(e);
			}
			
			return;
		}
		
		List<Token> postprocessed = new ArrayList<>();
		
		for(int i = 0; i < len; ++i) {
			Token token = tokens.get(i);
			
			if(token.is(TokenType.UNPARSEABLE)) {
				String raw = token.getRaw();
				int cp = raw.codePoints().toArray()[0];
				
				Logger.error(token.getPosition(), "Unexpected character »%s« (U+%04X)", raw, cp);
			}
			
			if(token.is(TokenType.NEWLINE, TokenType.WHITESPACE))
				continue;
			
			if(token.is(TokenType.IDENTIFIER, TokenType.NUMBER))
				postprocessed.add(postprocess(token));
			
			else if(token.is(TokenType.STRING) && !postprocessed.isEmpty()) {
				int idx = postprocessed.size() - 1;
				
				Token previous = postprocessed.get(idx);
				
				// concatenate adjacent strings
				if(previous.is(TokenType.STRING))
					postprocessed.set(
						idx,
						Token.ofString(
							previous.getPosition().range(token),
							previous.getString() + token.getString(),
							previous.isWide() || token.isWide()
						)
					);
				
				else postprocessed.add(token);
			}
			
			else if(token.is(TokenType.CHARACTER))
				postprocessed.add(
					Token.ofConstant(
						token.getPosition(),
						token.getInteger(),
						NumericValueType.SIGNED_INT
					)
				);
			
			else postprocessed.add(token);
		}

		checkTerminationState();
		
		/* Parsing (Syntactic + Sematic Analysis) */
		
		List<SymbolNode> parsed = new Parser(postprocessed).parse();

		checkTerminationState();
		
		if(syntaxTree != null)
			SyntaxTreeGenerator.generate(syntaxTree, parsed);

		/* Intermediate Representation */
		
		IntermediateGenerator intermediateGenerator = new IntermediateGenerator();
		
		Map<String, FunctionData> intermediate = new HashMap<>();
		
		List<SymbolObject> symbols = new ArrayList<>();
		
		ControlFlowAnalyzer analyzer = new ControlFlowAnalyzer();
		GotoOptimizer gotoOptimizer = new GotoOptimizer();
		
		for(SymbolNode node : parsed) {
			symbols.add(node.getObject());
			
			if(node instanceof FunctionNode function) {

				SymbolObject object = function.getObject();
				String name = object.getName();
				
				final String returnLabel = object.getFunctionData().returnLabel();
				
				var intermediates = intermediateGenerator.toIntermediateRepresentation(
					function.getBody().getStatements()
				);
				
				intermediates = gotoOptimizer.optimize(
					intermediates,
					returnLabel
				);

				intermediates = analyzer.checkDeadCode(
					function.getPosition(),
					name,
					intermediates,
					returnLabel
				);

				intermediates = gotoOptimizer.optimize(intermediates, returnLabel);
				
				intermediate.put(
					name,
					new FunctionData(
						function.getParameters(),
						intermediates,
						analyzer.getGraph(),
						!object.getType()
							.toFunction()
							.getReturnType()
							.isVoid()
					)
				);
			}
		}

		checkTerminationState();
		
		if(controlFlowGraph != null)
			ControlFlowGraphGenerator.generate(controlFlowGraph, intermediate);
		
		/*** Code Generation ***/
		
		CodeGenerator codeGen = ArchitectureRegistry.getArchitecture()
			.getCodeGenerator(inputFileName);
		
		/* Instruction Selection */
		
		AssemblyGenerator asmGen = codeGen.getAssemblyGenerator();
		
		List<AssemblyInstruction> instructions = new ArrayList<>();
		
		ObjectSerializer serial = codeGen.getObjectSerializer();
		PeepholeOptimizer peephole = codeGen.getPeepholeOptimizer();
		
		serial.fileBegin();
		
		for(SymbolObject sym : symbols) {
			if(sym.isPrototype() || sym.isTypedef() || sym.isExtern())
				continue;
			
			serial.metadata(sym);
			
			if(sym.isGlobalVariable()) {
				
				if(sym.isInitialized())
					serial.generateInit(sym.getVariableData().initializer());
				
				else serial.zero(sym.getType().sizeof());
				
			}
			
			serial.transfer(instructions);
			
			if(sym.isFunction()) {
				FunctionType type = sym.getType().toFunction();
				
				Instructions insns = new Instructions();
				
				FunctionData data = intermediate.get(sym.getName());
				
				asmGen.onEntry(insns, type, data.parameters());
				asmGen.generate(data.intermediate());
				asmGen.onLeave(type);
				
				RegisterAllocator alloc = asmGen.getRegisterAllocator(insns);
				
				alloc.allocate();
				
				List<RegisterTarget> registers = alloc.getAssignedRegisters();
				
				PrologueEpilogueInserter inserter = asmGen.getPrologueEpilogueInserter();

				Instructions prologue = new Instructions();
				long stackSize = alloc.getStackSize();
				
				inserter.insertPrologue(prologue, stackSize, registers);
				inserter.insertEpilogue(insns, stackSize, registers);
				
				AssemblyInstruction head = insns.getHead();
				prologue.stream()
					.map(AssemblyInstruction::clone)
					.forEach(head::insertBefore);
				
				peephole.optimize(insns);

				insns.forEach(instructions::add);
				
				serial.generatePostFunction(sym);
			}
		}
		
		serial.fileEnd();
		serial.transfer(instructions);
		
		checkTerminationState();
		
		File asmOut = uniqueFile(inputFileName, ".syntaxctmp.s");
		
		try(PrintStream writer = new PrintStream(asmOut)) {
			
			for(AssemblyInstruction instruction : instructions)
				writer.println(instruction.toString());
			
		} catch (Exception e) {
			outputFailed(e);
		}
		
		if(onlyCompile) {
			
			try {
				Files.copy(asmOut.toPath(), constructOutput(".s"));
				asmOut.delete();
			} catch (Exception e) {
				outputFailed(e);
			}
			
			return;
		}

		if(onlyAssemble) /* Assemble */
			codeGen.getAssemblerLinker().assemble(asmOut, getOutputFileName(".o"));
			
		/* Link */
		else codeGen.getAssemblerLinker().assembleAndLink(
				asmOut,
				outputFileName == null
					? Paths.get(inputFileName)
						.getParent()
						.resolve("a.out")
						.toAbsolutePath()
						.toString()
					: outputFileName
			);
		
		asmOut.delete();
	}
	
	public static Token postprocess(Token token) {
		Lexer lexer = new Lexer(CharStream.fromString(token.getRaw(), token.getPosition()));
		
		Token result = lexer.nextToken();
		
		if(result == null)
			Logger.error(token, "Unrecognizable token");
		
		lexer.ensureNoTrailing(result.getType().getName());
		
		return result;
	}
	
	/**
	 * Creates a file with the name and the file name extension.
	 * The file is guaranteed to not exist yet, which is accomplished
	 * by inserting periods followed by a number inbetween the name
	 * and the extension
	 * 
	 * @param name the file name
	 * @param ext the file name extension
	 * @return the unique, non-existent file
	 */
	public static File uniqueFile(String name, String ext) {
		if(name.equals("-"))
			name = inputFileName + ".stdout";
		
		File file = new File(name + ext);
		
		int counter = 0;
		
		while(file.exists())
			file = new File(name + "." + counter++ + ext);
		
		return file;
	}
	
	/**
	 * Creates an output stream from a given file path.
	 * Parent directories are created if they don't exist yet.
	 * Existing file contents are truncated.
	 * 
	 * @param parser
	 * @param file
	 * @return
	 */
	public static OutputStream createStream(OptionParser parser, String file) {
		try {
			Path path = Paths.get(file).toAbsolutePath();
			
			Files.createDirectories(path.getParent());
			
			return Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (Exception e) {
			if(parser == null)
				outputFailed(e);
			else parser.showUsage("Failed to open output file: %s", e.getMessage());
			
			return null;
		}
	}
	
	/**
	 * Constructs the output stream for the {@link #outputFileName output file}.
	 * If the {@link #outputFileName} is a single dash ({@code -}), the output
	 * stream is identical to the standard console output stream (stdout).
	 * 
	 * @param extension the file name extension of the output file
	 * @return the output stream
	 */
	private static OutputStream constructOutput(String extension) {
		if(outputFileName != null && outputFileName.equals("-"))
			return AnsiPipe.getStdout();
		
		return createStream(
			null,
			getOutputFileName(extension)
		);
	}
	
	private static String getOutputFileName(String extension) {
		return Objects.requireNonNullElseGet(
			outputFileName,
			() -> inputFileName.substring(0, inputFileName.length() - 2)
				+ extension
		);
	}
	
	/**
	 * Shows an error message and terminates the application
	 * 
	 * @param e the occured exception
	 */
	private static void outputFailed(Exception e) {
		e.printStackTrace();
		Logger.error("Failed to write to output file: %s", e.getMessage());
		System.exit(1);
	}

	/**
	 * Terminates the application, if previously requested
	 */
	public static void checkTerminationState() {
		if(terminate)
			System.exit(1);
	}

}
