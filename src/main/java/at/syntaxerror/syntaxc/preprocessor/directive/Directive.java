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
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.preprocessor.Preprocessor;
import at.syntaxerror.syntaxc.preprocessor.PreprocessorView;
import at.syntaxerror.syntaxc.preprocessor.macro.SubstitutionHelperView;
import at.syntaxerror.syntaxc.tracking.Position;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Delegate;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
@Getter
@Accessors(chain = true)
public abstract class Directive implements Logable, PreprocessorView, SubstitutionHelperView {
	
	@Delegate(types = { PreprocessorView.class, SubstitutionHelperView.class })
	private final Preprocessor preprocessor;
	private final Token self;
	
	@Setter
	private String name;
	
	@Override
	public Position getPosition() {
		return self.getPosition();
	}

	public void skip() { }

	public void processSimple() { }
	
	public List<Token> process() {
		processSimple();
		return List.of();
	}
	
	@FunctionalInterface
	public static interface DirectiveConstructor {
		
		Directive construct(Preprocessor preprocessor, Token self);
		
	}

	@FunctionalInterface
	public static interface ElseDirectiveConstructor {
		
		Directive construct(Preprocessor preprocessor, Token self, boolean alreadySucceeded);
		
	}
	
}
