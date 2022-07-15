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
package at.syntaxerror.syntaxc.preprocessor.macro;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import at.syntaxerror.syntaxc.io.CharStream;
import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.preprocessor.PreLexer;
import at.syntaxerror.syntaxc.preprocessor.Preprocessor;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;

/**
 * @author Thomas Kasper
 * 
 */
public record FunctionMacro(Token name, LinkedHashMap<String, Token> args, List<Token> body) implements Macro, Logable {

	@Override
	public String getName() {
		return name.getString();
	}
	
	@Override
	public Position getPosition() {
		return name.getPosition();
	}

	@Override
	public boolean isFunction() {
		return true;
	}

	@Override
	public int getArgCount() {
		return args.size();
	}

	@Override
	public LinkedHashMap<String, Token> getArgs() {
		return args;
	}

	@Override
	public List<Token> getBody() {
		return body;
	}
	
	private Token stringify(List<Token> tokens, Position pos) {
		StringBuilder sb = new StringBuilder();
		
		stripLeading(tokens);
		stripTrailing(tokens);
		
		tokens.forEach(tok -> sb.append(tok.quoted()));
		
		String string = sb.toString().strip();
		
		return Token.ofString(
			pos,
			string,
			false
		).setRaw('"' + string + '"');
	}
	
	private Token concat(Token first, Token second, Positioned self, Position pos) {
		String string = first.getRaw() + second.getRaw();
		
		PreLexer lexer = new PreLexer(CharStream.fromString(string, pos));
		
		Token concatenated = lexer.nextToken();
		
		if(lexer.next() != -1) {
			error(
				self,
				Warning.CONTINUE,
				"Concatenating »%s« and »%s« does not produce a valid preprocessing token",
				first.getRaw(),
				second.getRaw()
			);
			note(pos, "In expansion of »%s«", getName());
			terminate();
		}
		
		return concatenated;
	}
	
	private void stripLeading(List<Token> tokens) {
		while(!tokens.isEmpty() && tokens.get(0).is(TokenType.WHITESPACE))
			tokens.remove(0);
	}
	
	private void stripTrailing(List<Token> tokens) {
		int idx = tokens.size() - 1;
		
		while(!tokens.isEmpty() && tokens.get(idx).is(TokenType.WHITESPACE))
			tokens.remove(idx--);
	}
	
	@Override
	public List<Token> substitute(Preprocessor preprocessor, Token self, List<List<Token>> args) {
		List<Token> substituted = new ArrayList<>();
		
		boolean concat = false;
		boolean stringify = false;
		
		boolean empty = true;
		boolean previousEmpty;
		
		Position pos = null;
		
		List<String> keySet = new ArrayList<>(this.args.keySet());
		
		int len = body.size();
		
		List<Token> current = null;

		// § 6.8.3.1 Argument substitution
		// § 6.8.3.2 The # operator
		// § 6.8.3.3 The ## operator
		
		for(int i = 0; i < len; ++i) {
			previousEmpty = empty;
			
			Token token = body.get(i);
			
			if(token.is(Punctuator.DOUBLE_HASH)) {
				concat = true;
				pos = token.getPosition();
				
				stripTrailing(substituted);
				continue;
			}
			
			else if(token.is(Punctuator.HASH)) {
				stringify = true;
				continue;
			}
			
			else if(token.is(TokenType.WHITESPACE)) {
				if(!concat && !stringify) // skip whitespace between '##'/'#' and next token
					substituted.add(token);
				
				continue;
			}
			
			else if(token.is(TokenType.IDENTIFIER)) {
				int argIndex = keySet.indexOf(token.getString());
				
				if(argIndex != -1) { // identifier is a function parameter
					List<Token> argument = args.get(argIndex);
					
					if(stringify) {
						current = List.of(stringify(argument, token.getPosition()));
						
						stringify = false;
					}
					
					else if(concat)
						current = argument;
					
					else current = preprocessor.rescan(new ArrayList<>(argument), true, null);
				}
				
				else current = List.of(token); // identifier is not a function parameter
			}
			
			else current = List.of(token);
			
			stripLeading(current);
			empty = current.isEmpty();
			
			if(concat && !previousEmpty && !empty)
				substituted.add(concat(
					substituted.remove(substituted.size() - 1),
					current.remove(0),
					self,
					pos
				));
			
			substituted.addAll(current);

			if(concat) empty = empty && previousEmpty;
			
			concat = false;
		}
		
		return substituted;
	}

}
