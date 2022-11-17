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
package at.syntaxerror.syntaxc.preprocessor.directive;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.io.CharStream;
import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.misc.IncludePathRegistry;
import at.syntaxerror.syntaxc.preprocessor.Preprocessor;
import at.syntaxerror.syntaxc.tracking.Position;

/**
 * @author Thomas Kasper
 * 
 */
public class IncludeDirective extends Directive {

	public IncludeDirective(Preprocessor preprocessor, Token self) {
		super(preprocessor, self);
	}

	@Override
	public List<Token> process() {
		Token tok;
		
		CharStream input = getInput();
		
		while(true) {
			input.mark();
			
			// make sure strings don't get processed here
			if(input.peek(true) == '"') {
				tok = null;
				break;
			}
			
			tok = nextTokenRaw();
			
			if(tok == null || tok.is(TokenType.NEWLINE)) {
				input.unmark();
				error("Missing header name for »include« preprocessing directive");
				return null;
			}
			
			else if(tok.is(TokenType.WHITESPACE))
				input.unmark();
			
			else break;
		}
		
		Token header = null;
		
		// #include "some_file.h" or #include <some_file.h>
		if(tok == null || tok.is(Punctuator.LESS)) {
			input.reset();
			input.mark();
			
			header = getLexer().nextHeader();
			
			setPrevious(getCurrent());
			setCurrent(header);
			
			input.unmark();
			
			skipTrailing(true);
		}
		
		// #include SOME_IDENTIFIER_HERE
		else if(tok.is(TokenType.IDENTIFIER)) {
			input.unmark();
			
			List<Token> tokens = new ArrayList<>();
			
			Position trailingStart = null;
			boolean angle = false; // whether '<' was encountered
			boolean whitespace = true;
			
			outer:
			do {
				if(tok.is(TokenType.WHITESPACE)) {
					if(!whitespace)
						tokens.add(tok);
					
					whitespace = true;
				}
				else if(tok.is(TokenType.IDENTIFIER)) {
					whitespace = false;
					
					List<Token> subst = substitute(tok, true);
					
					if(!subst.isEmpty()) {
						int i;
						
						if(!angle) {
							tok = subst.get(0);
							
							if(tok.is(TokenType.STRING)) {
								if(subst.size() > 1)
									trailingStart = tok.getPosition();

								tokens.add(tok);
								break;
							}
							
							if(!tok.is(Punctuator.LESS)) {
								error("Expected header name for »include« preprocessing directive");
								return List.of();
							}
							
							angle = true;
							i = 1;
						}
						else i = 0;
						
						for(; i < subst.size(); ++i) {
							tok = subst.get(i);
							
							if(tok.is(Punctuator.GREATER)) {
								if(i + 1 != subst.size())
									trailingStart = tok.getPosition();
								
								break outer;
							}
	
							tokens.add(tok);
						}
					}
				}
				else if(tok.is(Punctuator.GREATER))
					break;
				
				else {
					whitespace = false;
					tokens.add(tok);
				}
				
				tok = nextTokenRaw();
				
				if(tok == null || tok.is(TokenType.NEWLINE))
					break;
				
			} while(true);
			
			if(tokens.isEmpty()) {
				softError("Missing file for »include« preprocessing directive");
				return List.of();
			}
			
			if(angle) {
				Position range = tokens.get(0).getPosition()
					.range(tokens.get(tokens.size() - 1));
				
				StringBuilder file = new StringBuilder();
				
				for(Token token : tokens)
					file.append(token.getRaw());
				
				header = Token.ofHeader(range, file.toString());
			}
			else header = tokens.get(0);
			
			skipTrailing(true, trailingStart);
		}
		
		else {
			input.reset();
			error(tok, "Expected header name for »include« preprocessing directive");
		}
		
		String file = header.getString();
		
		Path path = IncludePathRegistry.resolve(
			header.getPosition()
				.file()
				.getPath(),
			file
		);
		
		if(path == null)
			error(header, "File not found: %s", file);
		
		CharStream stream = CharStream.fromFile(
			path.toAbsolutePath().toString(),
			getPosition()
		);
		
		return new Preprocessor(stream, getPreprocessor()).preprocess();
	}

}
