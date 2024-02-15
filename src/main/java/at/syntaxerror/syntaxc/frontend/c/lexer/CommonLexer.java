/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.lexer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import at.syntaxerror.syntaxc.frontend.c.CStandard;
import at.syntaxerror.syntaxc.frontend.c.CWarning;
import at.syntaxerror.syntaxc.frontend.c.math.CNumber;
import at.syntaxerror.syntaxc.frontend.c.string.CString;
import at.syntaxerror.syntaxc.frontend.c.string.CUnicode;
import at.syntaxerror.syntaxc.frontend.c.string.EncodingPrefix;
import at.syntaxerror.syntaxc.io.CharSource;
import at.syntaxerror.syntaxc.io.CharSource.Position;
import at.syntaxerror.syntaxc.io.CharSource.Range;
import at.syntaxerror.syntaxc.log.LogableException;
import at.syntaxerror.syntaxc.log.Logger;
import at.syntaxerror.syntaxc.misc.ByteArrays;
import at.syntaxerror.syntaxc.misc.Unicode;
import lombok.RequiredArgsConstructor;

/**
 * 
 *
 * @author Thomas Kasper
 */
@RequiredArgsConstructor
public class CommonLexer {

	protected static final long NO_ENCODE = 0x8000000000000000L;
	
	protected final CharSource source;
	private List<Long> trigraphs = new ArrayList<>();
	
	protected Position start;
	protected Position last;
	protected Position current;
	
	protected String raw;
	
	public LogableException error(String message, Object...args) {
		return error(start, message, args);
	}

	public LogableException error(Position range, String message, Object...args) {
		return new LogableException(range(range), message, args);
	}

	public LogableException error(Range range, String message, Object...args) {
		return new LogableException(range, message, args);
	}
	
	public Position mark() {
		return source.getPosition();
	}
	
	public Range range(Position position) {
		return position.ranged(source.getPosition());
	}
	
	public Position startToken() {
		return start = mark();
	}
	
	public Range endToken() {
		var mark = last;
		long end = mark.absolute();
		
		StringBuilder raw = new StringBuilder();
		
		start.seek();
		
		while(mark().absolute() <= end)	{
			int c = next();
			
			if(c < 0 || c > 0x10FFFF)
				break;
			
			raw.append(Character.toString(c));
		}
		
		this.raw = raw.toString();
		return start.ranged(mark);
	}
	
	public Range endTokenNoRaw() {
		return start.ranged(last);
	}
	
	private int nextTrigraph() {
		last = current;
		var start = current = mark();
		
		int c = source.next();
		
		if(!CStandard.supportsTrigraphs() || c != '?')
			return c;
		
		var mark = mark();
		
		c = source.next();
		
		if(c != '?') {
			mark.seek();
			return '?';
		}
		
		int n;
		
		switch(c = source.next()) {
		case '<': n = '{'; break;
		case '>': n = '}'; break;
		case '(': n = '['; break;
		case ')': n = ']'; break;
		case '=': n = '#'; break;
		case '/': n = '\\'; break;
		case '\'': n = '^'; break;
		case '!': n = '|'; break;
		case '-': n = '~'; break;
		
		default:
			mark.seek();
			return '?';
		};
		
		if(!trigraphs.contains(mark.absolute())) {
			trigraphs.add(mark.absolute());
			Logger.warn(CWarning.TRIGRAPHS, range(start), "replaced trigraph »&+&l??{c}&-« with »&+&l{c}&-«", c, n);
		}
		
		return n;
	}
	
	public int next() {
		int c = nextTrigraph();
		
		if(c == '\\') {
			var mark = mark();
			
			if(nextTrigraph() == '\n')
				return next();
			
			mark.seek();
		}
		
		return c;
	}

	public Token.Identifier nextIdentifier() {
		startToken();
		
		List<Integer> codepoints = new ArrayList<>();
		
		while(true) {
			var ucnStart = mark();
			int c = next();
			
			if(c == '\\') {
				var mark = mark();
				int n = next();
				
				boolean wide = n == 'U';
				
				if(wide || n == 'u') {
					c = nextUCN(ucnStart, wide, ucs -> CUnicode.isUCNIdentifierChar(ucs, codepoints.isEmpty()));
					codepoints.add(c);
					continue;
				}
				else mark.seek();
			}
			
			if(!CUnicode.isIdentifierChar(c, codepoints.isEmpty())) {
				ucnStart.seek();
				break;
			}

			codepoints.add(c);
		}
		
		if(codepoints.isEmpty())
			throw error("expected identifier").recoverable();
		
		return new Token.Identifier(
			endToken(),
			CString.of(codepoints),
			raw
		);
	}
	
	private EncodingPrefix nextPrefix(int start) {
		EncodingPrefix prefix = null;
		
		int c = next();
		
		if(c == start)
			return EncodingPrefix.NONE;

		switch(c) {
		case 'L':
			prefix = EncodingPrefix.WIDE;
			break;
			
		case 'u':
			var mark = mark();
			c = next();
			
			if(c == '8') {
				if((start == '"' && !CStandard.supportsUTF8Strings()) ||
					(start == '\'' && !CStandard.supportsUTF8Chars()))
					return null;
				
				prefix = EncodingPrefix.UTF8;
			}
			else {
				if(!CStandard.supportsUTF16Strings())
					return null;
				
				mark.seek();
				prefix = EncodingPrefix.UTF16;
			}
			break;
			
		case 'U':
			if(!CStandard.supportsUTF32Strings())
				return null;
			
			prefix = EncodingPrefix.UTF32;
			break;
			
		default:
			break;
		}
		
		return prefix;
	}
	
	public Token.Constant nextCharacter() {
		startToken();
		
		EncodingPrefix prefix = nextPrefix('\'');
		
		if(prefix == null)
			throw error("expected character literal").recoverable();
		
		if(prefix != EncodingPrefix.NONE && next() != '\'')
			throw error("expected character literal").recoverable();
		
		BigInteger value = BigInteger.ZERO;
		int nChars = 0;
		int nBytes = 0;
		int maxBytes = prefix.getType().sizeof();
		
		if(prefix == EncodingPrefix.NONE)
			maxBytes = 1;
		
		while(true) {
			var mark = mark();
			int c = next();
			
			if(c == '\'') {
				next();
				break;
			}
			
			long v = nextChar(mark, c, maxBytes, "character literal");
			
			byte[] bytes;
			
			if((v & NO_ENCODE) != 0)
				bytes = ByteArrays.pack(v & ~NO_ENCODE, maxBytes);
			else bytes = prefix.charset.encode((int) v);
			
			nBytes += bytes.length;
			++nChars;
			
			for(byte b : bytes)
				value = value.shiftLeft(8).or(BigInteger.valueOf(b & 0xFF));
		}

		Range pos = endToken();
		
		if(nChars == 0)
			Logger.error(pos, "empty character literal");
		else if(prefix != EncodingPrefix.NONE && nChars > 1) {
			Logger.error(pos, "multi-character literal cannot have an encoding prefix");
			value = BigInteger.ZERO;
		}
		else if(prefix != EncodingPrefix.NONE && nBytes > maxBytes) {
			Logger.error(pos, "character not encodable in a single code unit");
			value = BigInteger.ZERO;
		}
		else if(!prefix.inRange(value))
			Logger.warn(pos, "multi-character literal with {} characters exceeds »{l}« size of {} bytes", nBytes, prefix.getType(), prefix.getType().sizeof());
		else if(nChars > 1)
			Logger.warn(CWarning.MULTICHAR, pos, "multi-character literal");

		value = prefix.truncate(value);
		
		return new Token.Constant(
			pos,
			CNumber.of(prefix.getType(), value),
			raw
		);
	}
	
	public Token.StringLiteral nextString() {
		startToken();
		
		EncodingPrefix prefix = nextPrefix('"');
		
		if(prefix == null)
			throw error("expected string literal").recoverable();
		
		if(prefix != EncodingPrefix.NONE && next() != '"')
			throw error("expected string literal").recoverable();
		
		List<Integer> codepoints = new ArrayList<>();

		int maxBytes = prefix.getType().sizeof();
		
		if(prefix == EncodingPrefix.NONE)
			maxBytes = 1;
		
		while(true) {
			var mark = mark();
			int c = next();
			
			if(c == '"') {
				next();
				break;
			}

			long v = nextChar(mark, c, maxBytes, "string literal");

			if((v & NO_ENCODE) != 0) {
				byte[] bytes = ByteArrays.pack(v & ~NO_ENCODE, maxBytes);
				
				for(byte b : bytes)
					codepoints.add(CString.RAW_BYTE | (b & 0xFF));
			}
			else codepoints.add((int) v);
		}
		
		return new Token.StringLiteral(endToken(), CString.of(codepoints), raw);
	}
	
	public long nextChar(Position start, int c, int maxBytes, String context) {
		if(c == -1)
			throw error("unclosed {}", context);
		
		if(c == '\n')
			throw error("unexpected end of line in {}", context);
		
		if(c != '\\')
			return c;
		
		c = next();
		
		if(c == -1)
			throw error("unclosed escape sequence in {}", context);
		
		if(c == 'u' || c == 'U')
			return nextUCN(start, c == 'U');
		
		if(Unicode.isOctalDigit(c)) {
			int val = Unicode.parseOctalDigit(c);
			Position last = this.last;
			
			for(int i = 1; i < 3; ++i) {
				var mark = mark();
				c = Unicode.parseOctalDigit(next());
				
				if(c == -1) {
					mark.seek();
					break;
				}
				
				val = (val << (3 * i)) | c;
				last = mark;
			}
			
			if(maxBytes == 1 && val > 0xFF) {
				Logger.warn(last.ranged(start), "octal escape sequence out of range");
				val &= 0xFF;
			}
			
			return val | NO_ENCODE;
		}
		
		if(c == 'x') {
			BigInteger val = BigInteger.ZERO;
			boolean hasChars = false;
			Position last = null;
			
			while(true) {
				var mark = mark();
				
				c = next();
				int v = Unicode.parseHexDigit(c);
				
				if(v == -1) {
					mark.seek();

					if(!hasChars) {
						Logger.error(start, "expected hexadecimal digit for hexadecimal escape sequence, got {U} instead", c);
						return NO_ENCODE;
					}
					
					break;
				}
				
				hasChars = true;
				val = val.shiftLeft(4).or(BigInteger.valueOf(v));
				last = mark;
			}
			
			if(Math.ceilDiv(val.bitLength(), 8) > maxBytes) {
				Logger.warn(last.ranged(start), "hex escape sequence out of range");
				val = val.and(BigInteger.ONE.shiftLeft(maxBytes * 8).subtract(BigInteger.ONE));
			}
			
			return val.intValue() | NO_ENCODE;
		}
		
		switch(c) {
		case '\'':
		case '"':
		case '?':
		case '\\':
			return c;
			
		case 'a': return 0x07;
		case 'b': return 0x08;
		case 't': return 0x09;
		case 'n': return 0x0A;
		case 'v': return 0x0B;
		case 'f': return 0x0C;
		case 'r': return 0x0D;
		
		default:
			Logger.error(start.ranged(last), "illegal escape sequence »&+&l\\{c}&-« ({u})", c, c);
			return 0;
		}
	}

	public int nextUCN(Position start, boolean wide) {
		return nextUCN(start, wide, null);
	}

	public int nextUCN(Position start, boolean wide, Predicate<Integer> validator) {
		int nibbles = wide ? 8 : 4;
		int value = 0;
		
		for(int i = 0; i < nibbles; ++i) {
			int c = next();
			int v = Unicode.parseHexDigit(c);
			
			if(v == -1)
				throw error(mark(), "expected hexadecimal digit for universal character name, got {U} instead", c);
			
			value = (value << 4) | v;
		}
		
		boolean valid = true;
		
		if(value < 0xA0 || (value >= 0xD800 && value <= 0xDFFF) || value > 0x10FFFF) {
			valid = value == 0x24 // $
				|| value == 0x40 // @
				|| value == 0x60; // `
		}
		else if(validator != null)
			valid = validator.test(value);
		
		if(!valid)
			throw error(start.ranged(last), "universal character name {u} does not denote a legal codepoint", value);
		
		return value;
	}
	
	public Token.Punctuator nextPunctuator() {
		var start = startToken();
		
		int c = next();
		
		Position mark = mark();
		
		PunctType punct = switch(c) {
		case '[': yield PunctType.LBRACKET;
		case ']': yield PunctType.RBRACKET;
		case '(': yield PunctType.LPAREN;
		case ')': yield PunctType.RPAREN;
		case '{': yield PunctType.LBRACE;
		case '}': yield PunctType.RBRACE;
		case '?': yield PunctType.TERNARY_IF;
		case ';': yield PunctType.SEMICOLON;
		case ',': yield PunctType.COMMA;
		case '~': yield PunctType.BIT_NOT;
		
		case '.':
			if(next() == '.' && next() == '.')
				yield PunctType.ELLIPSIS;
			
			mark.seek();
			last = start;
			yield PunctType.PERIOD;
			
		case ':':
			c = next();
			
			if(CStandard.supportsDigraphs() && c == '>')
				yield PunctType.RBRACKET;
				
			if(c == ':')
				yield PunctType.ATTRIBUTE;
			
			mark.seek();
			last = start;
			yield PunctType.TERNARY_ELSE;
			
		case '-':
			switch(next()) {
			case '>': yield PunctType.ARROW;
			case '-': yield PunctType.DECREMENT;
			case '=': yield PunctType.ASSIGN_SUBTRACT;
			default:
				mark.seek();
				last = start;
				yield PunctType.SUBTRACT;
			}

		case '+':
			switch(next()) {
			case '+': yield PunctType.INCREMENT;
			case '=': yield PunctType.ASSIGN_ADD;
			default:
				mark.seek();
				last = start;
				yield PunctType.ADD;
			}
			
		case '/':
			switch(next()) {
			case '=': yield PunctType.ASSIGN_DIVIDE;
			default:
				mark.seek();
				last = start;
				yield PunctType.DIVIDE;
			}
			
		case '%':
			c = next();
			
			if(CStandard.supportsDigraphs()) {
				switch(c) {
				case '>': yield PunctType.RBRACE;
				case ':':
					var reset = mark();
					
					if(next() == '%' && next() == ':')
						yield PunctType.CONCAT;
					
					reset.seek();
					last = mark;
					yield PunctType.DIRECTIVE;
				default:
					break;
				}
			}
			
			switch(c) {
			case '=': yield PunctType.ASSIGN_MODULO;
			default:
				mark.seek();
				last = start;
				yield PunctType.MODULO;
			}
			
		case '*':
			switch(next()) {
			case '=': yield PunctType.ASSIGN_MULTIPLY;
			default:
				mark.seek();
				last = start;
				yield PunctType.MULTIPLY;
			}
			
		case '<':
			c = next();
			
			if(CStandard.supportsDigraphs()) {
				switch(c) {
				case '%': yield PunctType.LBRACE;
				case ':': yield PunctType.LBRACKET;
				default:
					break;
				}
			}
			
			switch(c) {
			case '=': yield PunctType.LESS_EQUAL;
			case '<':
				var reset = mark();
				
				if(next() == '=')
					yield PunctType.ASSIGN_LSHIFT;
				
				reset.seek();
				last = mark;
				yield PunctType.LSHIFT;
			default:
				mark.seek();
				last = start;
				yield PunctType.LESS;
			}
			
		case '>':
			switch(next()) {
			case '=': yield PunctType.GREATER_EQUAL;
			case '>':
				var reset = mark();
				
				if(next() == '=')
					yield PunctType.ASSIGN_RSHIFT;
				
				reset.seek();
				last = mark;
				yield PunctType.RSHIFT;
			default:
				mark.seek();
				last = start;
				yield PunctType.GREATER;
			}
			
		case '=':
			if(next() == '=')
				yield PunctType.EQUAL;
			
			mark.seek();
			last = start;
			yield PunctType.ASSIGN;
			
		case '!':
			if(next() == '=')
				yield PunctType.NOT_EQUAL;
			
			mark.seek();
			last = start;
			yield PunctType.LOGICAL_NOT;
			
		case '^':
			if(next() == '=')
				yield PunctType.ASSIGN_XOR;
			
			mark.seek();
			last = start;
			yield PunctType.BIT_XOR;
			
		case '|':
			switch(next()) {
			case '|': yield PunctType.LOGICAL_OR;
			case '=': yield PunctType.ASSIGN_OR;
			default:
				mark.seek();
				last = start;
				yield PunctType.BIT_OR;
			}
			
		case '&':
			switch(next()) {
			case '&': yield PunctType.LOGICAL_AND;
			case '=': yield PunctType.ASSIGN_AND;
			default:
				mark.seek();
				last = start;
				yield PunctType.BIT_AND;
			}
			
		case '#':
			if(next() == '#')
				yield PunctType.CONCAT;
			
			mark.seek();
			last = start;
			yield PunctType.DIRECTIVE;
		
		default:
			yield null;
		};
		
		if(punct == null)
			throw error("expected punctuator").recoverable();
		
		return new Token.Punctuator(endToken(), punct, raw);
	}
	
	public Token.Whitespace nextWhitespace() {
		startToken();
		boolean hasWhitespace = false;
		
		while(true) {
			var mark = mark();
			int c = next();
			
			if(c == '/') {
				var reset = mark();
				c = next();
				
				if(CStandard.supportsSingleLineComments() && c == '/') {
					hasWhitespace = true;
					
					while(true) {
						reset = mark();
						c = next();
						
						if(c == '\n' || c == -1) {
							reset.seek();
							last = reset;
							break;
						}
					}
					
					break;
				}
				
				if(c == '*') {
					hasWhitespace = true;
					
					while(true) {
						c = next();
						
						if(c == -1) {
							Logger.error(mark, "unclosed multi-line comment");
							break;
						}
						
						if(c == '*') {
							reset = mark();
							
							if(next() == '/')
								break;
							
							reset.seek();
						}
					}
					
					continue;
				}
				
				mark.seek();
				last = reset;
				break;
			}
			
			if(c == '\n' || !Character.isWhitespace(c)) {
				mark.seek();
				break;
			}
			
			hasWhitespace = true;
		}
		
		if(!hasWhitespace)
			throw error("expected whitespace").recoverable();
		
		return new Token.Whitespace(endTokenNoRaw());
	}

	public Token.EOL nextEOL() {
		startToken();
		int c = next();
		
		if(c != '\n')
			throw error("expected end-of-line").recoverable();
		
		return new Token.EOL(endTokenNoRaw());
	}

	public Token.EOF nextEOF() {
		startToken();
		int c = next();
		
		if(c != -1)
			throw error("expected end-of-file").recoverable();
		
		return new Token.EOF(endTokenNoRaw());
	}
	
	public Token nextUnparseable() {
		var start = startToken();
		int c = next();
		
		if(c == '\\') {
			var reset = mark();
			c = next();
			
			if(c == 'u' || c == 'U')
				c = nextUCN(start, c == 'U');
			else {
				reset.seek();
				c = '\\';
			}
		}
		
		Logger.error(endToken(), "ERROR: {U} ({})", c, raw);
		
		if(this instanceof Object)
			throw null;
		
		return new Token.Unparseable(endToken(), c, raw);
	}
	
}
