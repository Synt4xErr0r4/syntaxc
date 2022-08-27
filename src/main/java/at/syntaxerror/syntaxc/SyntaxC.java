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
import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.generator.CodeGenerator;
import at.syntaxerror.syntaxc.generator.arch.Architecture;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.generator.asm.AssemblyInstruction;
import at.syntaxerror.syntaxc.io.CharStream;
import at.syntaxerror.syntaxc.lexer.Lexer;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.parser.Parser;
import at.syntaxerror.syntaxc.parser.node.Node;
import at.syntaxerror.syntaxc.parser.tree.TreeGenerator;
import at.syntaxerror.syntaxc.preprocessor.Preprocessor;
import at.syntaxerror.syntaxc.type.NumericValueType;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class SyntaxC {
	
	@UtilityClass
	public static class Version {
		
		public static final int MAJOR = 1;
		public static final int MINOR = 0;
		public static final int PATCH = 0;

		public static final String VERSION = "%d.%d.%d".formatted(MAJOR, MINOR, PATCH);
		
	}
	
	public static boolean terminate;

	public static boolean onlyAssemble;		// -c (don't link)
	public static boolean onlyCompile;		// -S (don't assemble)
	public static boolean onlyPreprocess;	// -E (don't parse)
	
	public static OutputStream syntaxTree; // -fsyntax-tree

	public static String inputFileName;
	public static String outputFileName;

	public static void compile(CharStream input, OutputStream output) {
		Architecture architecture = ArchitectureRegistry.getArchitecture();
		
		architecture.onInit();
		
		/* Preprocess input file */
		
		List<Token> tokens = new Preprocessor(input).preprocess();
		
		checkTerminationState();
		
		/* Postprocess tokens */
		
		int len = tokens.size();
		
		if(onlyPreprocess) {

			try {
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
				if(previous.is(TokenType.STRING)) {
					postprocessed.remove(idx);
					postprocessed.add(
						Token.ofString(
							previous.getPosition().range(token),
							previous.getString() + token.getString(),
							previous.isWide() || token.isWide()
						)
					);
				}
				
				else postprocessed.add(token);
			}
			
			else if(token.is(TokenType.CHARACTER))
				postprocessed.add(Token.ofConstant(token.getPosition(), token.getInteger(), NumericValueType.SIGNED_INT));
			
			else postprocessed.add(token);
		}
		
		/* Parsing (Syntactic + Sematic Analysis) */
		
		List<Node> parsed = new Parser(postprocessed).parse();
		
		System.out.println(parsed);
		
		if(syntaxTree != null)
			TreeGenerator.generate(syntaxTree, parsed);
		
		/* Code Generation */
		
		CodeGenerator codeGen = ArchitectureRegistry.getArchitecture().getCodeGenerator(inputFileName);
		
		List<AssemblyInstruction> assembly = codeGen.generate(parsed);
		
		File asmOut = uniqueFile(outputFileName, ".s");
		
		try(PrintStream writer = new PrintStream(asmOut)) {
			
			for(AssemblyInstruction instruction : assembly)
				writer.println(instruction.toAssembly());
			
		} catch (Exception e) {
			outputFailed(e);
		}
		
		if(onlyCompile) {
			
			try {
				Files.copy(asmOut.toPath(), output);
				asmOut.delete();
			} catch (Exception e) {
				outputFailed(e);
			}
			
			return;
		}
		
		/* Assemble */

		// TODO assemble
		
		asmOut.delete();
		
		if(onlyAssemble) {
			
			
			return;
		}
		
		/* Link */
		
		
	}
	
	public static File uniqueFile(String name, String ext) {
		File file = new File(name + ext);
		
		int counter = 0;
		
		while(file.exists())
			file = new File(name + "." + counter++ + ext);
		
		return file;
	}
	
	private static void outputFailed(Exception e) {
		Logger.error("Failed to write to output file: %s", e.getMessage());
	}

	public static Token postprocess(Token token) {
		Lexer lexer = new Lexer(CharStream.fromString(token.getRaw(), token.getPosition()));
		
		Token result = lexer.nextToken();
		
		if(result == null)
			Logger.error(token, "Unrecognizable token");
		
		lexer.ensureNoTrailing(result.getType().getName());
		
		return result;
	}

	private static void checkTerminationState() {
		if(terminate)
			System.exit(1);
	}

}
