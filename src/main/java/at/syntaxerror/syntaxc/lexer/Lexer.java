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
package at.syntaxerror.syntaxc.lexer;

import java.math.BigDecimal;
import java.math.BigInteger;

import at.syntaxerror.syntaxc.io.CharStream;
import at.syntaxerror.syntaxc.misc.Flag;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.NumericValueType;

/**
 * @author Thomas Kasper
 * 
 */
public class Lexer extends CommonLexer {

	// bit masks for integer suffixes
	private static final int UNSIGNED = (1 << 0);
	private static final int LONG = (1 << 1);
	
	public Lexer(CharStream source) {
		super(false, false, source);
	}

	/* § 6.1.1 Keywords
	 * § 6.1.2 Identifiers
	 * § 6.1.5 Operators (sizeof)
	 */
	@Override
	public Token nextIdentifier() {
		Token ident = super.nextIdentifier();
		
		Keyword keyword = Keyword.of(ident.getString());
		
		if(keyword != null)
			return Token.ofKeyword(ident.getPosition(), keyword);
		
		return ident;
	}
	
	/* handles both integer and floating numbers
	 * 
	 * ((([0-9]*\.[0-9]+|[0-9]+\.)([eE][+-]?[0-9]+)?)|([0-9]+[eE][+-]?[0-9]+))[flFL]
	 * (([1-9][0-9]*)|(0[0-7]*)|(0[xX][0-9a-fA-F]+))([uU]?[lL]|[lL]?[uU])?
	 * 
	 * § 6.1.3.1 Floating constants
	 * § 6.1.3.2 Integer constants
	 */
	public Token nextNumber() {
		boolean integer = false;
		boolean floating = false;
		boolean period = false;
		int radix = 10;
		int c;
		Position octalError = null;
		
		StringBuilder sb = new StringBuilder();
		
		if(previous == '.') {
			sb.append("0.");
			floating = true;
			period = true;
		}
		else if(previous == '0') {
			c = peek();
			
			if(c == 'b') { // extension: binary literals
				radix = 2;
				integer = true;
				
				skip();
				
				if(!Flag.BINARY_LITERALS.isEnabled())
					error(Flag.BINARY_LITERALS, "Binary literals are not allowed");
				
				c = next();
				
				if(!isBinDigit(c))
					error("Illegal character in binary integer literal");

				sb.append((char) c);
				
				while(isBinDigit(peek()))
					sb.append((char) next());
			}
			else if(c == 'x') { // hexadecimal literals
				radix = 16;
				integer = true;
				
				skip();
				c = next();
				
				if(!isHexDigit(c))
					error("Illegal character in hexadecimal integer literal");
				
				sb.append((char) c);
				
				while(isHexDigit(peek()))
					sb.append((char) next());
			}
			else if(isDigit(c)) { // octal literals
				radix = 8;
				integer = isOctDigit(c);

				sb.append((char) c);
				
				while(isDigit(peek())) {
					c = next();
					
					if(!isOctDigit(c) && octalError == null) {
						octalError = getPosition();
						integer = false; // don't throw an error here yet; it might still be a valid floating constant
					}
					
					sb.append((char) c);
				}
				
			}
			else sb.append('0');
			
		}
		else { // read as many digits as possible
			sb.append((char) previous);
			
			while(isDigit(peek()))
				sb.append((char) next());
		}
		
		if(peek() == '.') { // decimal point encountered
			skip();
			
			if(integer) // decimal point cannot occur after binary/octal/hexadecimal literal
				error("Illegal decimal point in integer literal");
			
			if(period) // decimal point cannot occur multiple times
				error("Multiple decimal points in decimal literal");
			
			sb.append('.');
			
			period = true;
			floating = true;

			while(isDigit(peek())) // read as many decimal digits as possible
				sb.append((char) next());
		}
		
		c = peek();
		
		if(c == 'e' || c == 'E') { // exponent encountered
			floating = true;
			
			sb.append('e');
			
			skip();
			c = peek();
			
			if(c == '+' || c == '-') { // exponent sign encountered
				sb.append((char) c);
				skip();
				c = peek();
			}
			
			if(!isDigit(c)) // exponent requires an integer
				error("Expected digit for exponent");
			
			// read as many exponent digits as possible
			do sb.append((char) next());
			while(isDigit(peek()));
		}
		
		/* determine type based on the suffix:
		 *
		 * floating:
		 * - no suffix: double
		 * - 'f' or 'F': float
		 * - 'l' or 'L': long double
		 * 
		 * integer:
		 * - no suffix:
		 * 	 - binary/octal/hexadecimal: int, unsigned int, long int, unsigned long int
		 *   - decimal: int, long int, unsigned long int
		 * - 'u' or 'U': unsigned int or unsigned long int
		 * - 'l' or 'L': long int or unsigned long int
		 * - 'ul' or 'Ul' or 'uL' or 'UL': unsigned long int
		 */
		
		c = peek();
		
		NumericValueType type = floating ? NumericValueType.DOUBLE : null;
		
		int suffix = 0;
		
		if(c == 'f' || c == 'F') {
			skip();
			
			if(integer || !floating)
				error("Illegal suffix »%c« for integer literal", (char) c);
			
			type = NumericValueType.FLOAT;
		}
		
		else if(c == 'u' || c == 'U') {
			skip();
			
			if(floating)
				error("Illegal suffix »%c« for floating literal", (char) c);

			integer = true;
			suffix |= UNSIGNED;
			
			int n = peek();
			
			if(n == 'l' || n == 'L') {
				skip();
				suffix |= LONG;
			}
			
			else if(isIdentifierPart(n))
				error("Illegal suffix »%c%c« for integer literal", (char) c, (char) next());
		}
		
		else if(c == 'l' || c == 'L') {
			skip();
			
			if(floating)
				type = NumericValueType.LDOUBLE;
			
			else {
				integer = true;
				suffix |= LONG;
				
				int n = peek();
				
				if(n == 'u' || n == 'U') {
					skip();
					suffix |= UNSIGNED;
				}
				
				else if(isIdentifierPart(n))
					error("Illegal suffix »%c%c« for integer literal", (char) c, (char) next());
			}
		}

		else if(isIdentifierPart(c))
			error("Illegal suffix »%c« for numeric literal", (char) next());
		
		if(floating) {
			BigDecimal value;
			
			try {
				// throws an exception when there are more than 10 exponent digits
				value = new BigDecimal(sb.toString());
			} catch (Exception e) {
				value = null;
			}
			
			if(value == null || !type.inRange(value)) {
				warn(Warning.FLOAT_OVERFLOW, "Decimal literal is too big for its type");
				
				// value cannot be less than the minimum value (there are no negative constants yet)
				value = (BigDecimal) type.getMax();
			}
			
			return Token.ofConstant(getPosition(), value, type);
		}
		
		if(octalError != null)
			error(octalError, "Illegal charcter in octal integer literal");
		
		BigInteger value = new BigInteger(sb.toString(), radix);
		
		NumericValueType[] toCheck = null;
		
		switch(suffix) {
		case UNSIGNED: // unsigned int, unsigned long int
			toCheck = new NumericValueType[] {
				NumericValueType.UNSIGNED_INT
			};
			break;
		
		case LONG: // long int, unsigned long int
			toCheck = new NumericValueType[] {
				NumericValueType.SIGNED_LONG
			};
			break;
		
		case UNSIGNED | LONG: // unsigned long int
			toCheck = new NumericValueType[] { };
			break;
		
		default:
			if(radix == 10) // int, long int, unsigned long int
				toCheck = new NumericValueType[] {
					NumericValueType.SIGNED_INT,
					NumericValueType.SIGNED_LONG
				};
			
			// int, unsigned int, long int, unsigned long int
			else toCheck = new NumericValueType[] {
				NumericValueType.SIGNED_INT,
				NumericValueType.UNSIGNED_INT,
				NumericValueType.SIGNED_LONG
			};
			
			break;
		}
		
		for(NumericValueType nt : toCheck)
			if(nt.inRange(value)) {
				type = nt;
				break;
			}
		
		if(type == null) {
			type = NumericValueType.UNSIGNED_LONG;
			
			if(!type.inRange(value)) {
				warn(Warning.INT_OVERFLOW, "Integer literal is too big for its type");
				value = type.mask(value);
			}
		}

		return Token.ofConstant(getPosition(), value, type);
	}
	
	public void ensureNoTrailing(String type) {
		if(next() != -1)
			error("Trailing data for %s", type);
	}
	
	@Override
	protected Token nextToken(int c) {
		if(isDigit(c) || (c == '.' && isDigit(peek())))
			return nextNumber();
		
		return null;
	}
	
	// checks if the given codepoint is a decimal digit
	private static boolean isDigit(int c) {
		return c >= '0' && c <= '9';
	}
	
	// checks if the given codepoint is a binary digit
	private static boolean isBinDigit(int c) {
		return c == '0' || c == '1';
	}

}
