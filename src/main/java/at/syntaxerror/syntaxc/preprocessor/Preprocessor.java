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
package at.syntaxerror.syntaxc.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

import at.syntaxerror.syntaxc.SyntaxCException;
import at.syntaxerror.syntaxc.io.CharStream;
import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.config.Warnings;
import at.syntaxerror.syntaxc.preprocessor.directive.Directive;
import at.syntaxerror.syntaxc.preprocessor.directive.Directives;
import at.syntaxerror.syntaxc.preprocessor.macro.BuiltinMacro;
import at.syntaxerror.syntaxc.preprocessor.macro.FunctionMacro;
import at.syntaxerror.syntaxc.preprocessor.macro.Macro;
import at.syntaxerror.syntaxc.preprocessor.macro.SubstitutionHelper;
import at.syntaxerror.syntaxc.preprocessor.macro.SubstitutionHelperView;
import at.syntaxerror.syntaxc.tracking.Position;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Thomas Kasper
 * 
 */
public class Preprocessor implements Logable, PreprocessorView, SubstitutionHelperView {

	private static final Map<String, Macro> PREDEFINED_MACROS = new HashMap<>();
	
	private static final Set<String> BUILTIN = BuiltinMacro.getBuiltinMacros().keySet();
	
	static {
		/*
		 * Defines a macro to disable/remove GCC attributes (such as '__attribute__((packed))').
		 * 
		 * effectively equal to:
		 * 
		 * 	#define __attribute__(attr)
		 */
		PREDEFINED_MACROS.put(
			"__attribute__",
			new FunctionMacro(
				Token.ofIdentifier(Position.dummy(), "__attribute__"),
				new LinkedHashMap<>(
					Map.of(
						"attr",
						Token.ofIdentifier(Position.dummy(), "attr")
					)
				),
				List.of()
			)
		);
	}
	
	private final @Getter CharStream input;
	private final @Getter PreLexer lexer;
	private final @Getter PreParser parser;
	
	private final @Getter SubstitutionHelper substitution;
	
	private @Getter @Setter Token current, previous;
	
	private Map<String, Macro> macros = new HashMap<>();
	
	private Stack<Position> marks = new Stack<>();
	
	public Preprocessor(CharStream input) {
		substitution = new SubstitutionHelper(this);
		
		lexer = new PreLexer(this.input = input);
		parser = new PreParser(this);
		
		macros.putAll(BuiltinMacro.getBuiltinMacros());
		macros.putAll(PREDEFINED_MACROS);
	}
	
	public Preprocessor(CharStream input, Preprocessor parent) {
		this(input);
		macros = parent.macros;
	}
	
	@Override
	public Warnings getDefaultWarning() {
		return Warnings.PREPROC_NONE;
	}
	
	@Override
	public void mark() {
		marks.push(current.getPosition());
	}
	
	@Override
	public void unmark() {
		marks.pop();
	}
	
	@Override
	public Position getPosition() {
		Token tok = current == null ? previous : current;
		
		if(tok == null)
			return marks.peek();
		
		return marks.peek().range(tok);
	}
	
	@Override
	public Token nextTokenRaw() {
		Token tok = lexer.nextToken(); 
		
		if(tok != null)
			previous = current;
		
		return current = tok;
	}
	
	@Override
	public Token nextToken() {
		while(nextTokenRaw() != null && current.is(TokenType.WHITESPACE))
			;
		
		return current;
	}
	
	@Override
	public Macro resolveMacro(String name) {
		return macros.get(name);
	}
	
	@Override
	public void defineMacro(Macro macro) {
		String name = macro.getName();
		Position pos = macro.getPosition();
		
		if(name.equals("defined")) {
			softError(pos, "»defined« cannot be used a macro name");
			return;
		}
		
		if(BUILTIN.contains(name))
			warn(pos, Warnings.BUILTIN_REDEF, "Redefinition of builtin macro");
		
		else if(macros.containsKey(name) && Warnings.REDEF.isEnabled()) {
			Macro old = macros.get(name);
			
			boolean equal = old.isFunction() == macro.isFunction()
				&& Objects.equals(old.getArgs(), macro.getArgs())
				&& Objects.equals(old.getBody(), macro.getBody());

			if(!equal) {
				warn(pos, Warnings.REDEF, "Redefinition of existent macro");
				note(old, Warnings.REDEF, "Previously defined here");
			}
		}
		
		macros.put(name, macro);
	}
	
	@Override
	public void undefineMacro(Token nameToken) {
		String name = nameToken.getString();
		Position pos = nameToken.getPosition();
		
		if(BUILTIN.contains(name))
			warn(pos, Warnings.BUILTIN_UNDEF, "Undefinition of builtin macro");
		
		else if(!macros.containsKey(name))
			warn(pos, Warnings.UNDEF, "Undefinition of non-existent macro");
		
		macros.remove(name);
	}
	
	// skip all trailing tokens until new-line or end-of-file is reached
	@Override
	public Position skipTrailing(boolean notify, Position start) {
		if(start == null)
			start = getPosition();
		
		Token tok = nextTokenRaw();
		
		if(tok == null || tok.is(TokenType.NEWLINE))
			return start;
		
		start = getPosition();
		Position end = start;
		
		boolean nonWhitespace = false;
		
		while(true) {
			tok = nextTokenRaw();

			if(tok == null || tok.is(TokenType.NEWLINE))
				break;
			
			if(tok.is(TokenType.WHITESPACE))
				continue;
			
			nonWhitespace = true;
			end = tok.getPosition();
		}
		
		end = start.range(end);
		
		if(notify && nonWhitespace)
			warn(end, Warnings.TRAILING, "Trailing data after preprocessing directive");
		
		return end;
	}
	
	@Override
	public List<Token> processTokens(boolean insideIfBlock, boolean skip) {
		List<Token> tokens = new ArrayList<>();

		boolean newline = true;
		
		while(true) {
			Token tok = nextTokenRaw();
			
			if(tok == null) // no more tokens left
				break;

			mark();
			
			if(tok.is(TokenType.NEWLINE)) {
				newline = true;
				tokens.add(tok);
			}
			
			else if(tok.is(Punctuator.HASH)) { // preprocessor directive
				if(!newline) {
					if(skip) {
						unmark();
						continue;
					}
					
					error("Unexpected »#« at this position");
				}
				
				tok = nextToken();
				
				if(tok.is(TokenType.NEWLINE)) {
					if(!skip)
						warn(previous, Warnings.EMPTY_DIRECTIVE, "Empty preprocessing directive");
					
					unmark();
					continue;
				}
				
				if(!tok.is(TokenType.IDENTIFIER)) {
					if(!skip)
						softError(tok, "Expected identifier for preprocessing directive");
					
					skipTrailing(false);
					unmark();
					continue;
				}
				
				if(insideIfBlock && Directives.findElse(this, tok, skip) != null) {
					unmark();
					return tokens;
				}
				
				Directive directive = Directives.find(this, tok);
				
				if(directive == null) {
					if(!skip) {
						warn("Unknown preprocessing directive");
						skipTrailing(true);
					}
				}
				else if(skip)
					directive.skip();
				
				else tokens.addAll(directive.process());
			}
			else {
				if(!tok.is(TokenType.WHITESPACE))
					newline = false;
				
				if(!skip) {
					if(tok.is(TokenType.IDENTIFIER)) // substitute macro, don't preserve whitespace
						tokens.addAll(substitute(tok));
					
					else tokens.add(tok);
				}
			}
			
			unmark();
		}
		
		return tokens;
	}
	
	public List<Token> preprocess() {
		List<Token> result = processTokens(false, false);
		
		if(!marks.isEmpty())
			throw new SyntaxCException("Stack not emptied properly");
		
		return result;
	}
	
}
