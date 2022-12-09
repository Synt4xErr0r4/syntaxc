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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.io.CharStream;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.misc.config.Warnings;
import at.syntaxerror.syntaxc.preprocessor.Preprocessor;
import at.syntaxerror.syntaxc.tracking.Position;

/**
 * @author Thomas Kasper
 * 
 */
public class LineDirective extends Directive {

	public LineDirective(Preprocessor preprocessor, Token self) {
		super(preprocessor, self);
	}

	@Override
	public void processSimple() {
		Token line = nextToken();
		
		if(line == null || line.is(TokenType.NEWLINE))
			error(getPrevious(), "Expected number for »line« preprocessing directive");
		
		Position pos = line.getPosition();
		
		Token name;
		
		if(line.is(TokenType.IDENTIFIER)) {
			List<Token> tokens = new ArrayList<>();
			
			Token tok = line;
			
			do {
				if(tok.is(TokenType.IDENTIFIER))
					tokens.addAll(substitute(tok, true));
				
				else tokens.add(tok);
				
				tok = nextToken();
			} while(tok != null && !tok.is(TokenType.NEWLINE));
			
			if(tokens.isEmpty())
				error(line, "Expected number for »line« preprocessing directive");
			
			line = tokens.remove(0);
			
			if(!tokens.isEmpty())
				name = tokens.remove(0);
			else name = null;
			
			if(!tokens.isEmpty())
				warn(
					tokens.get(tokens.size() - 1).getPosition()
						.range(tokens.get(0)),
					Warnings.TRAILING,
					"Trailing data after preprocessing directive"
				);
		}
		else name = nextToken();
		
		if(!line.is(TokenType.NUMBER) || !line.getString().matches("\\d+") || line.getString().matches("0+"))
			error(line, "Expected positive integer for »line« preprocessing directive");
		
		if(name != null && !name.is(TokenType.STRING, TokenType.NEWLINE))
			error(name, "Expected string for »line« preprocessing directive");
		
		CharStream file = pos.file();
		
		file.setOverridenLineOffset(
			pos.line(),
			new BigInteger(line.getString()).longValue()
		);

		if(name != null && !name.is(TokenType.NEWLINE))
			file.setOverridenName(name.getString());
	}

}
