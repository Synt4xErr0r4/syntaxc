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

import java.util.LinkedHashMap;
import java.util.List;

import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.preprocessor.Preprocessor;
import at.syntaxerror.syntaxc.tracking.Positioned;

/**
 * This class represents a macro used by the pre-processor
 * 
 * @author Thomas Kasper
 * 
 */
public interface Macro extends Positioned {
	
	/**
	 * Returns the name of the macro
	 * 
	 * @return the name of the macro
	 */
	String getName();
	
	/**
	 * Returns whether the macro designates a function
	 * 
	 * @return whether the macro designates a function
	 */
	boolean isFunction();
	
	/**
	 * Returns the number of arguments for the function-like macro
	 * 
	 * @return the number of arguments for the function-like macro
	 */
	int getArgCount();
	
	/**
	 * Returns the arguments for this macro, if it is a function-like macro
	 * 
	 * @return the arguments for this macro
	 */
	LinkedHashMap<String, Token> getArgs();
	
	/**
	 * Returns the substitution list defined for this macro
	 * 
	 * @return the substitution list defined for this macro
	 */
	List<Token> getBody();
	
	/**
	 * Substitutes the token {@code self} with the subsitution list defined for this macro,
	 * optionally accepting {@code args} if this macro is function-like.
	 * 
	 * @param preprocessor the preprocessor
	 * @param self the token to be substituted
	 * @param args the (optional) function call arguments
	 * @return the resulting tokens
	 */
	List<Token> substitute(Preprocessor preprocessor, Token self, List<List<Token>> args);
	
}