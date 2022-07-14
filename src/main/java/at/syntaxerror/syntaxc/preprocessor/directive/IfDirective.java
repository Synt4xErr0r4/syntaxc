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

import java.util.List;

import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.preprocessor.Preprocessor;
import at.syntaxerror.syntaxc.tracking.Positioned;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
public class IfDirective extends Directive {
	
	@Getter
	protected boolean alreadySucceeded;
	
	public IfDirective(Preprocessor preprocessor, Token self, boolean previousSucceeded) {
		super(preprocessor, self);
		this.alreadySucceeded = previousSucceeded;
	}

	protected void missingEndif(Positioned pos) {
		error(pos, "Missing »endif« for »%s« preprocessing directive", getName());
	}
	
	protected boolean checkCondition() {
		return getParser().evaluate();
	}
	
	/*
	 * success = if-block condition
	 * alreadySucceeded = one of the previous if-blocks has already succeeded (or the block is being skipped)
	 * 
	 * success && !alreadySucceeded => return current block
	 * !success && alreadySucceeded => return empty block
	 * !success && !alreadySucceeded => return next block (recursively; until one of the two options above apply)
	 * success && alreadySucceeded => illegal state
	 */
	private List<Token> processBlock(boolean success) {
		List<Token> result = processTokens(true, !success);
		
		if(getCurrent() == null)
			missingEndif(getPrevious());
		
		Directive directive = Directives.findElse(
			getPreprocessor(),
			getCurrent(),
			success || alreadySucceeded
		);
		
		if(directive == null)
			missingEndif(getCurrent());
		
		List<Token> directiveResult = directive.process();
		
		return success
			? result
			: alreadySucceeded
				? List.of()
				: directiveResult;
	}
	
	@Override
	public void skip() {
		alreadySucceeded = true;
		processBlock(false);
	}
	
	@Override
	public List<Token> process() {
		return processBlock(!alreadySucceeded && checkCondition());
	}

}
