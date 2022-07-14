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

import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.preprocessor.Preprocessor;

/**
 * @author Thomas Kasper
 * 
 */
public class IfdefDirective extends IfDirective {

	private boolean desiredResult;
	
	public IfdefDirective(Preprocessor preprocessor, Token self, boolean desiredResult, boolean alreadySucceeded) {
		super(preprocessor, self, alreadySucceeded);
		this.desiredResult = desiredResult;
	}
	
	@Override
	protected boolean checkCondition() {
		Token tok = nextToken();
		
		if(tok == null)
			missingEndif(getPrevious());
		
		if(tok.is(TokenType.NEWLINE)) {
			error(getPrevious(), "Expected identifier for »%s« directive", getName());
			return false;
		}
		
		if(!tok.is(TokenType.IDENTIFIER)) {
			error(tok, "Expected identifier for »%s« directive", getName());
			return false;
		}
		
		return (resolveMacro(tok.getString()) != null) == desiredResult;
	}

}
