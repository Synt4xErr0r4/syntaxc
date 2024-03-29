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
package at.syntaxerror.syntaxc;

/**
 * This class represents the standard exception raised by SyntaxC
 * 
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings("serial")
public class SyntaxCException extends RuntimeException {

	/**
	 * Constructs a new SyntaxCException
	 */
	public SyntaxCException() {
		super();
	}

	/**
	 * Constructs a new SyntaxCException with a message
	 * 
	 * @param message the message
	 */
	public SyntaxCException(String message) {
		super(message);
	}

	/**
	 * Constructs a new SyntaxCException with a cause
	 * 
	 * @param cause the cause
	 */
	public SyntaxCException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new SyntaxCException with a message and cause
	 * 
	 * @param message the message
	 * @param cause the cause
	 */
	public SyntaxCException(String message, Throwable cause) {
		super(message, cause);
	}

}
