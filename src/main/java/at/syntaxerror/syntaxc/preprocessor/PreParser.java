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

import at.syntaxerror.syntaxc.SyntaxC;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.parser.AbstractParser;
import at.syntaxerror.syntaxc.symtab.SymbolTable;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.NumericValueType;

/**
 * @author Thomas Kasper
 * 
 */
public class PreParser extends AbstractParser implements Logable {

	private Preprocessor preprocessor;
	private boolean eol;
	
	public PreParser(Preprocessor preprocessor) {
		this.preprocessor = preprocessor;
	}
	
	@Override
	public Warning getDefaultWarning() {
		return Warning.PREPROC_NONE;
	}

	@Override
	public Position getPosition() {
		return preprocessor.getPosition();
	}
	
	@Override
	public void markTokenState() {
		preprocessor.getInput().mark();
	}
	
	@Override
	public void unmarkTokenState() {
		preprocessor.getInput().unmark();
	}
	
	@Override
	public void resetTokenState() {
		preprocessor.getInput().reset();
	}

	@Override
	public Token readNextToken() {
		if(eol) return null;
		
		Token tok = preprocessor.nextToken();
		
		eol = tok == null || tok.is(TokenType.NEWLINE);
		
		if(eol) return null;
		
		if(tok.is(TokenType.IDENTIFIER, TokenType.NUMBER))
			return SyntaxC.postprocess(tok);
		
		if(tok.is(TokenType.CHARACTER))
			return Token.ofConstant(
				tok.getPosition(),
				tok.getInteger(),
				NumericValueType.SIGNED_INT
			);
		
		return tok;
	}
	
	@Override
	public SymbolTable getSymbolTable() {
		return null;
	}
	
	public boolean evaluate() {
		eol = false;
		
		// TODO
		
		return false;
	}

}
