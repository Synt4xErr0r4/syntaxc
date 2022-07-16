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
package at.syntaxerror.syntaxc.parser;

import java.util.Objects;
import java.util.Stack;

import at.syntaxerror.syntaxc.lexer.Keyword;
import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.symtab.SymbolTable;
import at.syntaxerror.syntaxc.tracking.Position;

/**
 * @author Thomas Kasper
 * 
 */
public abstract class AbstractParser implements Logable {

	protected Token previous;
	protected Token current;
	
	private Stack<Pair<Token, Token>> marked = new Stack<>();
	
	@Override
	public Position getPosition() {
		return current != null
			? current.getPosition()
			: previous != null
				? previous.getPosition()
				: null;
	}
	
	public abstract SymbolTable getSymbolTable();
	
	public abstract void markTokenState();
	public abstract void resetTokenState();
	public abstract void unmarkTokenState();

	public final void mark() {
		markTokenState();
		marked.push(Pair.of(previous, current));
	}
	
	public final void reset() {
		resetTokenState();
		
		var tokens = marked.pop();
		
		previous = tokens.getFirst();
		current = tokens.getSecond();
	}
	
	public final void unmark() {
		unmarkTokenState();
		marked.pop();
	}
	
	public abstract Token readNextToken();
	
	public Token next() {
		previous = current;
		return current = readNextToken();
	}
	
	public boolean equal(Object...any) {
		return current != null && current.is(any);
	}
	
	public boolean skip(Object...any) {
		if(equal(any)) {
			next();
			return true;
		}
		
		return false;
	}
	
	public boolean optional(Object...any) {
		mark();
		next();
		
		if(!equal(any)) {
			reset();
			return false;
		}
		
		unmark();
		return true;
	}
	
	public Token require(Object...any) {
		next();

		if(!equal(any))
			expected(any);
		
		return current;
	}
	
	public boolean peek(Object...any) {
		mark();
		next();

		boolean found = equal(any);
		
		reset();
		
		return found;
	}
	
	public void expected(Object...any) {
		error("Expected %s", toExpectationList(any));
	}
	
	private static String toExpectationList(Object...args) {
		if(args.length == 0)
			return "<empty>";
		
		if(args.length == 1)
			return toExpectationString(args[0]);
		
		StringBuilder sb = new StringBuilder();
		
		int len = args.length;
		
		for(int i = 0; i < len; ++i) {
			if(i == len - 1)
				sb.append(" or ");
			
			else if(i != 0)
				sb.append(", ");
			
			sb.append(toExpectationString(args[i]));
		}
		
		return sb.toString();
	}
	
	private static String toExpectationString(Object arg) {
		if(arg instanceof Punctuator punct)
			return '»' + punct.getName() + '«';

		if(arg instanceof Keyword kw)
			return '»' + kw.getName() + '«';
		
		if(arg instanceof TokenType type)
			return type.getName();

		return '»' + Objects.toString(arg) + '«';
	}
	
}
