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
import java.util.List;

import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.preprocessor.Preprocessor;
import at.syntaxerror.syntaxc.preprocessor.PreprocessorView;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class SubstitutionHelper implements PreprocessorView, Logable {

	@Delegate(types = { PreprocessorView.class, Logable.class })
	private final Preprocessor preprocessor;
	
	private List<Token> inserted = new ArrayList<>();
	private Token currentOverride;
	private Counter counter = new Counter(null, 0, false, false, false);
	
	private int nextTokenPos() {
		final Object[] predicate = counter.sameLine
			? new Object[] { TokenType.WHITESPACE }
			: new Object[] { TokenType.WHITESPACE, TokenType.NEWLINE };
		
		for(int i = 0; i < inserted.size(); ++i)
			if(!inserted.get(i).is(predicate))
				return i;
		
		return -1;
	}
	
	private Token nextTokenNonNewline() {
		if(counter.sameLine)
			return preprocessor.nextToken();
		
		while(preprocessor.nextToken() != null && preprocessor.getCurrent().is(TokenType.NEWLINE))
			;
		
		return preprocessor.getCurrent();
	}
	
	@Override
	public Token nextToken() {
		if(!inserted.isEmpty()) {
			int i = nextTokenPos();
			
			if(i != -1) {
				counter.consume(i + 1);
				
				inserted.subList(0, i).clear();
				
				return inserted.remove(0);
			}
		}

		if(counter.restrict)
			return null;
		
		return nextTokenNonNewline();
	}
	
	@Override
	public Token nextTokenRaw() {
		if(!inserted.isEmpty()) {
			counter.consume(1);
			return inserted.remove(0);
		}
		
		if(counter.restrict)
			return null;
		
		return preprocessor.nextTokenRaw();
	}
	
	public Token peekToken() {
		if(!inserted.isEmpty()) {
			int i = nextTokenPos();
			
			if(i != -1)
				return inserted.get(i);
		}

		if(counter.restrict)
			return null;
		
		getInput().mark();
		
		Token tok = nextTokenNonNewline();
		
		getInput().reset();
		
		return tok;
	}
	
	@Override
	public Token getCurrent() {
		return currentOverride == null
			? preprocessor.getCurrent()
			: currentOverride;
	}

	public List<Token> rescan(List<Token> input, boolean restrict, Macro macro) {
		List<Token> rescanned = new ArrayList<>();

		boolean found = false;
		
		while(!input.isEmpty()) {
			Token token = input.remove(0);
			
			if(token.is(TokenType.NEWLINE))
				rescanned.add(Token.ofWhitespace(token.getPosition()));
			
			else if(token.is(TokenType.IDENTIFIER)) {
				var result = substitute(token, input, restrict);
				
				if(result.getFirst())
					found = true;
				
				rescanned.addAll(result.getSecond());
			}
			
			else rescanned.add(token);
		}
		
		if(macro != null)
			rescanned.forEach(tok -> tok.inExpansionOf(macro));
		
		return found
			? rescan(rescanned, restrict, macro)
			: rescanned;
	}
	
	private List<Token> substituteFunction(Macro macro) {
		Token self = getCurrent();
		
		List<List<Token>> args = new ArrayList<>();
		
		Token tok = peekToken();

		if(tok == null || !tok.is(Punctuator.LPAREN)) { // identifier is not a macro
			counter.found = false;
			return new ArrayList<Token>(List.of(self));
		}

		tok = nextToken(); // skip '('
		
		List<Token> activeList = new ArrayList<>();
		
		int parentheses = 0; // nested parentheses counter
		
		while(true) {
			tok = nextTokenRaw();
			
			if(tok == null || (counter.sameLine && tok.is(TokenType.NEWLINE)))
				error("Unclosed macro function call");
			
			if(tok.is(Punctuator.RPAREN)) {
				if(parentheses > 0)
					--parentheses;
				
				else {
					args.add(activeList);
					break;
				}
			}
			
			else if(tok.is(Punctuator.LPAREN))
				++parentheses;
			
			else if(tok.is(Punctuator.COMMA)) {
				if(parentheses == 0) {
					args.add(activeList);
					activeList = new ArrayList<>();
					
					if(args.size() > macro.getArgCount())
						error("Too many arguments for macro call");
					
					continue;
				}
			}
			
			activeList.add(tok);
		}

		if(args.size() < macro.getArgCount())
			error("Too few arguments for macro call (expected %s, got %d)", macro.getArgCount(), args.size());
		
		return macro.substitute(preprocessor, self, args);
	}
	
	public List<Token> substitute(boolean sameLine) {
		counter.found = false;
		counter.sameLine = sameLine;
		
		Token current = getCurrent();
		
		Macro macro = resolveMacro(current.getString());
		
		// identifier is not a macro or was already expanded
		if(macro == null || current.isInExpansionOf(macro))
			return List.of(current);
		
		List<Token> replacement;
		
		counter.found = true;
		
		if(macro.isFunction())
			replacement = substituteFunction(macro);
		else replacement = macro.substitute(preprocessor, current, List.of());

		if(!counter.found)
			return replacement;
		
		replacement.forEach(tok -> tok.inExpansionOf(macro));
		
		// § 6.8.3.4 Rescanning and further replacement
		
		return rescan(replacement, false, macro);
	}

	public Pair<Boolean, List<Token>> substitute(Token current, List<Token> tokens, boolean restrict) {
		this.currentOverride = current;
		inserted.addAll(0, tokens);
		
		int avail = tokens.size();
		
		counter = new Counter(counter, avail, false, restrict, counter.sameLine);
		
		List<Token> result = substitute(counter.sameLine);
		
		List<Token> section = inserted.subList(0, counter.available);
		
		tokens.clear();
		tokens.addAll(section);
		
		section.clear();
		
		boolean found = counter.found;
		
		counter = counter.parent;
		this.currentOverride = null;
		
		return Pair.of(found, result);
	}
	
	@AllArgsConstructor
	private class Counter {
		
		private final Counter parent;
		private int available;
		private boolean found;
		private boolean restrict;
		private boolean sameLine;
		
		public void consume(int n) {
			if(n > available) {
				n -= available;
				available = 0;
				
				parent.consume(n);
			}
			else available -= n;
		}
		
	}
	
}