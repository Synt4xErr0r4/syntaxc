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

import java.math.BigInteger;
import java.util.Set;

import at.syntaxerror.syntaxc.io.CharStream;
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.NumericValueType;
import lombok.RequiredArgsConstructor;

/**
 * This class represents the lexer used for pre-processing and lexing C code
 * 
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public abstract class CommonLexer implements Logable {
	
	private static final Set<Integer> PUNCTUATOR_START;
	
	static {
		PUNCTUATOR_START = Set.copyOf(
			Punctuator.getNames()
				.stream()
				.map(s -> (int) s.charAt(0))
				.toList()
		);
	}

	private final boolean whitespaces; // whether whitespaces should be considered separate tokens
	private final boolean trigraphs; // whether trigraphs should be parsed
	private final CharStream source;
	
	protected int previous;
	
	@Override
	public Warning getDefaultWarning() {
		return Warning.LEX_NONE;
	}
	
	@Override
	public final Position getPosition() {
		return source.getPosition();
	}
	
	public final int next() {
		return previous = source.next(trigraphs);
	}
	
	protected final int peek() {
		return source.peek(trigraphs);
	}
	
	protected final void skip() {
		source.skip(trigraphs);
	}
	
	protected final void skip(int n) {
		source.skip(n, trigraphs);
	}
	
	/* [a-zA-Z_][0-9a-zA-Z_]*
	 * 
	 * § 6.1.2 Identifiers
	 */
	public Token nextIdentifier() {
		StringBuilder sb = new StringBuilder();
		
		sb.append((char) previous);
		
		while(isIdentifierPart(peek()))
			sb.append((char) next());
		
		return Token.ofIdentifier(getPosition(), sb.toString());
	}
	
	/* § 6.1.3.4 Character constants */
	private int nextEscapeSequence(boolean wide, String where, StringBuilder raw) {
		NumericValueType type = wide ? NumericValueType.WCHAR : NumericValueType.CHAR;
		
		source.mark();
		
		skip(); // skip backslash
		raw.append('\\');
		
		int c = next();
		
		if(isOctDigit(c)) {
			raw.append((char) c);
			
			int value = c - '0';
			
			if(isOctDigit(c = peek())) {
				raw.append((char) c);
				
				skip();
				value = (value << 3) | (c - '0');
				
				if(isOctDigit(c = peek())) {
					raw.append((char) c);
					
					skip();
					value = (value << 3) | (c - '0');
				}
			}
			
			if(!type.inUnsignedRange(value)) {
				value = (int) type.maskUnsigned(value);
				warn(Warning.CHAR_OCT_OVERFLOW, "Octal escape sequence is too big for its type in %s", where);
			}

			if(value < 0 || value > 0x10FFFF) { // in case 'type' is greater than 4 bytes
				value = Math.min(0x10FFFF, value & 0x1FFFFF);
				warn(Warning.CODEPOINT, "Codepoint out of range");
			}

			source.unmark();
			return value;
		}
		else if(c == 'x') {
			if(!isHexDigit(c = next()))
				error("Expected hexadecimal digit for escape sequence in %s", where);

			raw.append('x').append((char) c);

			BigInteger value = BigInteger.valueOf(hexToDec(c));
			
			while(isHexDigit(c = peek())) {
				raw.append((char) c);
				
				skip();
				value = value.shiftLeft(4).or(BigInteger.valueOf(hexToDec(c)));
			}
			
			int charValue;
			
			if(!type.inUnsignedRange(value)) {
				charValue = (int) type.maskUnsigned(value.longValue());
				warn(Warning.CHAR_OCT_OVERFLOW, "Hexadecimal escape sequence is too big for its type in %s", where);
			}
			else charValue = value.intValue();
			
			if(charValue < 0 || charValue > 0x10FFFF) { // in case 'type' is greater than 4 bytes
				charValue = Math.min(0x10FFFF, charValue & 0x1FFFFF);
				warn(Warning.CODEPOINT, "Codepoint out of range");
			}

			source.unmark();
			return charValue;
		}
		else {
			raw.append((char) c);
			
			switch(c) {
			case '\'':
			case '"':
			case '?':
			case '\\':
				break; // these escape sequences simply represent themselves
			
			case 'a': c = 0x07; break; // alert/bell
			case 'b': c = '\b'; break; // backspace
			case 'f': c = '\f'; break; // form feed
			case 'n': c = '\n'; break; // new line
			case 'r': c = '\r'; break; // carriage return
			case 't': c = '\t'; break; // horizontal tab
			case 'v': c = 0x0B; break; // vertical tab
			
			default:
				softError("Unrecognized escape sequence in %s", where);
				break;
			}
		}

		source.unmark();
		return c;
	}

	/* ([^\n\\]|\\(['"?\\abfnrtv]|[0-7]{1,3}|x[0-9a-fA-F]+))*'
	 * 
	 * § 6.1.3.4 Character constants
	 */
	public Token nextCharacter(boolean wide) {
		StringBuilder raw = new StringBuilder();
		NumericValueType type;
		
		if(wide) {
			type = NumericValueType.WCHAR;
			raw.append("L'");
		}
		else {
			type = NumericValueType.CHAR;
			raw.append("'");
		}
		
		BigInteger value = BigInteger.ZERO;
		
		int count = 0;
		
		while(true) {
			int c = peek();
			
			if(c == -1)
				error("Unclosed character literal");
			
			if(c == '\n')
				error("Illegal new-line character in character literal");
			
			if(c == '\'') {
				raw.append("'");
				skip();
				break;
			}
			
			if(c == '\\')
				c = nextEscapeSequence(wide, "character literal", raw);
			
			else {
				raw.append(Character.toChars(c));
				skip();
			}
			
			value = value.shiftLeft(type.getSize() * 8)
				.or(BigInteger.valueOf(c));
			
			++count;
		}
		
		if(count == 0)
			error("Empty character literal");
		
		if(!NumericValueType.SIGNED_INT.inRange(value)) {
			warn(Warning.CHAR_OVERFLOW, "Character literal is too big for its type");
			value = NumericValueType.SIGNED_INT.mask(value);
		}
		
		else if(count > 1)
			warn(Warning.MULTICHAR, "Multiple characters in character literal");
		
		return Token.ofCharacter(getPosition(), value, wide)
			.setRaw(raw.toString());
	}
	
	/* ([^\n\\]|\\(['"?\\abfnrtv]|[0-7]{1,3}|x[0-9a-fA-F]+))*"
	 *
	 * § 6.1.4 String literals
	 */
	public Token nextString(boolean wide) {
		StringBuilder raw = new StringBuilder();
		StringBuilder sb = new StringBuilder();
		
		if(wide) raw.append("L\"");
		else raw.append('"');
		
		while(true) {
			int c = peek();
			
			if(c == -1)
				error("Unclosed string literal");
			
			if(c == '\n')
				error("Illegal new-line character in string literal");
			
			if(c == '"') {
				raw.append('"');
				skip();
				break;
			}
			
			if(c == '\\')
				sb.append(Character.toChars(nextEscapeSequence(wide, "string literal", raw)));
			
			else {
				skip();
				
				char[] chars = Character.toChars(c);
				
				raw.append(chars);
				sb.append(chars);
			}
		}
		
		return Token.ofString(getPosition(), sb.toString(), wide).setRaw(raw.toString());
	}
	
	/* § 6.1.5 Operators
	 * § 6.1.6 Punctuators
	 */
	public Token nextPunctuator() {
		source.mark();
		
		// peek a maximum of 3 chars
		
		StringBuilder sb = new StringBuilder();
		
		int i = 3;
		
		do sb.append(Character.toChars(previous));
		while(--i > 0 && next() != -1);
		
		String buffer = sb.toString();
		
		source.reset();
		
		for(String name : Punctuator.getNames())
			if(buffer.startsWith(name)) {
				skip(name.length() - 1);
				return Token.ofPunctuator(getPosition(), Punctuator.of(name));
			}
		
		return null;
	}
	
	/* § 6.1 Lexical elements
	 * § 6.1.9 Comments
	 */
	public Token nextWhitespace() {
		int c = previous;
		
		source.mark();
		
		while(true) {
			if(isWhitespace(c)) {
				source.unmark();
				source.mark();
				
				c = next();
				continue;
			}
			
			// multiline comments (starting with /* and ending with */, according to § 6.1.9)
			if(c == '/' && peek() == '*') {
				skip();
				
				while(true) {
					c = next();
					
					if(c == -1) {
						source.unmark();
						error("Unclosed comment");
					}
					
					if(c == '*' && peek() == '/') {
						skip();
						break;
					}
				}
				
				source.unmark();
				source.mark();
				
				c = next();
				continue;
			}
			
			source.reset();
			break;
		}
		
		return Token.ofWhitespace(getPosition());
	}
	
	protected abstract Token nextToken(int c);
	
	public final Token nextToken() {
		boolean successful = true;
		
		try {
			source.mark();
			
			int c = next();
			
			if(c == -1)
				return null;

			// "..."
			if(c == '"')
				return nextString(false);
			
			// L"..."
			if(c == 'L' && peek() == '"') {
				skip();
				return nextString(true);
			}

			// '...'
			if(c == '\'')
				return nextCharacter(false);

			// L'...'
			if(c == 'L' && peek() == '\'') {
				skip();
				return nextCharacter(true);
			}
			
			// new-line
			if(c == '\n') {
				Token tok = Token.ofNewline(getPosition());
				
				if(whitespaces)
					return tok;
				
				return nextToken();
			}

			// whitespaces and comments
			if(isWhitespace(c) || (c == '/' && peek() == '*')) {
				Token tok = nextWhitespace();
				
				if(whitespaces)
					return tok;
				
				return nextToken();
			}
			
			// identifiers
			if(isIdentifierStart(c))
				return nextIdentifier();
			
			// punctuators
			if(PUNCTUATOR_START.contains(c) && !(c == '.' && isDigit(peek())))
				return nextPunctuator();
			
			// additional tokens provided by child class
			Token tok = nextToken(c);
			
			successful = tok != null;
			
			return tok;
		} finally {
			if(successful)
				source.unmark();
			else source.reset();
		}
	}

	// checks if the given codepoint is the start of an identifier
	private static boolean isIdentifierStart(int c) {
		return (c >= 'a' && c <= 'z')
			|| (c >= 'A' && c <= 'Z')
			|| c == '_';
	}

	// checks if the given codepoint is part of an identifier
	protected static boolean isIdentifierPart(int c) {
		return isIdentifierStart(c)
			|| (c >= '0' && c <= '9');
	}
	
	// checks if the given codepoint is a decimal digit
	protected static boolean isDigit(int c) {
		return c >= '0' && c <= '9';
	}

	// checks if the given codepoint is an octal digit
	protected static boolean isOctDigit(int c) {
		return c >= '0' && c <= '7';
	}
	
	// checks if the given codepoint is a hexadecimal digit
	protected static boolean isHexDigit(int c) {
		return (c >= 'a' && c <= 'f')
			|| (c >= 'A' && c <= 'F')
			|| (c >= '0' && c <= '9');
	}
	
	// convert hexadecimal digits (0-9, a-f, A-F) into their numeric counterpart (0-15)
	protected static int hexToDec(int c) {
		return (c >= '0' && c <= '9')
			? c - '0'
			: (c >= 'A' && c <= 'F')
				? c - 'A' + 0xA
				: (c >= 'a' && c <= 'f')
					? c - 'a' + 0xA
					: 0;
	}

	/* according to § 6.1 - Semantics
	 * new-line is intentionally left out (handled separately)
	 */
	private static boolean isWhitespace(int c) {
		return c == ' ' // space
			|| c == '\t' // horizontal tab
			|| c == '\f' // form feed
			|| c == 0x0B; // vertical tab
	}
	
}
