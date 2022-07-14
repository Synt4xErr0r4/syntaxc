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

import java.util.List;

import at.syntaxerror.syntaxc.io.CharStream;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.preprocessor.macro.Macro;
import at.syntaxerror.syntaxc.tracking.Position;

/**
 * @author Thomas Kasper
 * 
 */
public interface PreprocessorView {

	CharStream getInput();
	PreLexer getLexer();
	PreParser getParser();
	
	Token getPrevious();
	Token getCurrent();
	
	void setPrevious(Token tok);
	void setCurrent(Token tok);
	
	Token nextTokenRaw();
	Token nextToken();
	
	void mark();
	void unmark();
	
	Macro resolveMacro(String name);
	void defineMacro(Macro macro);
	void undefineMacro(Token name);

	Position skipTrailing(boolean notify, Position start);
	
	default Position skipTrailing(boolean notify) {
		return skipTrailing(notify, null);
	}
	
	List<Token> processTokens(boolean insideIfBlock, boolean skip);
	
}
