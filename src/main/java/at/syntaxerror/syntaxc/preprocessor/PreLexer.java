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

import at.syntaxerror.syntaxc.io.CharStream;
import at.syntaxerror.syntaxc.lexer.CommonLexer;
import at.syntaxerror.syntaxc.lexer.Token;

/**
 * This class represents the lexer used for pre-processing
 * 
 * @author Thomas Kasper
 * 
 */
public class PreLexer extends CommonLexer {

	public PreLexer(CharStream input) {
		super(true, true, input);
	}

	/* § 6.1.7 Header names */
	public Token nextHeader() {
		int delim = next();
		
		if(delim == '<')
			delim = '>';
		
		StringBuilder sb = new StringBuilder();
		
		while(true) {
			int c = next();
			
			if(c == -1)
				error("Unclosed header name");
			
			if(c == '\n')
				error("Illegal new-line character in header name");
			
			if(c == delim)
				break;
			
			sb.append(Character.toChars(c));
		}
		
		return Token.ofHeader(getPosition(), sb.toString());
	}
	
	/* § 6.1.8 Preprocessing numbers */
	public Token nextNumber() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(Character.toChars(previous));
		
		while(true) {
			int c = peek();
			
			if(c == 'e' || c == 'E') {
				sb.append((char) next());
				
				c = peek();
				
				if(c == '+' || c == '-')
					sb.append((char) next());
			}
			
			else if(isDigit(c) || isIdentifierPart(c) || c == '.')
				sb.append(Character.toChars(next()));
			
			else break;
		}
		
		return Token.ofNumber(getPosition(), sb.toString());
	}

	@Override
	protected Token nextToken(int c) {
		if(isDigit(c) || (c == '.' && isDigit(peek())))
			return nextNumber();
		
		return Token.ofUnparseable(getPosition(), c);
	}

}
