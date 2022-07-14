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

import java.util.List;

import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.misc.Pair;

/**
 * @author Thomas Kasper
 * 
 */
public interface SubstitutionHelperView {

	SubstitutionHelper getSubstitution();

	default List<Token> substitute() {
		return substitute(false);
	}
	
	default List<Token> substitute(boolean sameLine) {
		return getSubstitution().substitute(sameLine);
	}
	
	default Pair<Boolean, List<Token>> substitute(Token current, List<Token> tokens, boolean restrict) {
		return getSubstitution().substitute(current, tokens, restrict);
	}
	
	default List<Token> rescan(List<Token> input, boolean restrict, Macro macro) {
		return getSubstitution().rescan(input, restrict, macro);
	}
	
}