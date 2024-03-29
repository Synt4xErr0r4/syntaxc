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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import at.syntaxerror.syntaxc.misc.StringUtils;
import at.syntaxerror.syntaxc.preprocessor.macro.Macro;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.NumericValueType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * This class represents a token generated by the lexer
 * 
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Setter(AccessLevel.PRIVATE)
@Accessors(chain = true)
public class Token implements Positioned {

	/* COMMON */

	public static Token ofIdentifier(Position position, String string) {
		return new Token(position, TokenType.IDENTIFIER).setString(string).setRaw(string);
	}

	public static Token ofString(Position position, String string, boolean wide) {
		return new Token(position, TokenType.STRING).setString(string).setWide(wide).setRaw('"' + StringUtils.quote(string) + '"');
	}

	public static Token ofCharacter(Position position, BigInteger integer, boolean wide) {
		return new Token(position, TokenType.CHARACTER).setInteger(integer).setWide(wide);
	}

	public static Token ofPunctuator(Position position, Punctuator punctuator) {
		return new Token(position, TokenType.PUNCTUATOR).setPunctuator(punctuator).setRaw(punctuator.getName());
	}

	/* C */

	public static Token ofKeyword(Position position, Keyword keyword) {
		return new Token(position, TokenType.KEYWORD).setKeyword(keyword);
	}

	public static Token ofConstant(Position position, BigInteger integer, NumericValueType type) {
		return new Token(position, TokenType.CONSTANT).setInteger(integer).setNumericType(type);
	}

	public static Token ofConstant(Position position, BigDecimal decimal, NumericValueType type) {
		return new Token(position, TokenType.CONSTANT).setDecimal(decimal).setNumericType(type);
	}

	/* PREPROCESSOR */

	public static Token ofHeader(Position position, String string) {
		return new Token(position, TokenType.HEADER).setString(string);
	}
	
	public static Token ofNumber(Position position, String string) {
		return new Token(position, TokenType.NUMBER).setString(string).setRaw(string);
	}

	public static Token ofWhitespace(Position position) {
		return new Token(position, TokenType.WHITESPACE).setRaw(" ");
	}

	public static Token ofNewline(Position position) {
		return new Token(position, TokenType.NEWLINE).setRaw(" ");
	}
	
	public static Token ofUnparseable(Position position, int c) {
		return new Token(position, TokenType.UNPARSEABLE).setRaw(String.valueOf(Character.toChars(c)));
	}
	
	private final Position position;
	private final TokenType type;
	
	private Keyword keyword;
	private Punctuator punctuator;
	private String string;
	private BigInteger integer;
	private BigDecimal decimal;
	
	private NumericValueType numericType;
	private boolean wide;
	
	@Setter(AccessLevel.PUBLIC)
	private String raw;
	
	private final Set<Macro> macros = new LinkedHashSet<>();

	/**
	 * Returns whether this token was created as part of any macro expansion
	 * 
	 * @return whether this token was created as part of any macro expansion
	 */
	public boolean hasBeenExpanded() {
		return !macros.isEmpty();
	}
	
	/**
	 * Marks this token to be created as part of a macro expansion
	 * 
	 * @param macro the macro of which this token originates
	 */
	public void inExpansionOf(Macro macro) {
		macros.add(macro);
	}
	
	/**
	 * Returns whether this token was created as part of a macro expansion of the given macro
	 * 
	 * @param macro the macro to be checked
	 * @return whether this token was created as part of a macro expansion
	 */
	public boolean isInExpansionOf(Macro macro) {
		return macros.contains(macro);
	}
	
	/**
	 * Returns a clone of this token with its position changed.
	 * The token's macro expansions are copied to the position
	 * 
	 * @param positioned the new position
	 * @return the repositioned token
	 */
	public Token atPosition(Positioned positioned) {
		Position pos = positioned.getPosition();
		
		Token clone = new Token(
			new Position(
				pos.bytenum(),
				pos.position(),
				pos.column(),
				pos.line(),
				pos.length(),
				pos.file(),
				new LinkedHashSet<>(macros),
				position
			),
			type
		);
		
		clone.keyword = keyword;
		clone.punctuator = punctuator;
		clone.string = string;
		clone.integer = integer;
		clone.decimal = decimal;
		clone.numericType = numericType;
		clone.wide = wide;
		clone.raw = raw;
		clone.macros.addAll(macros);
		
		return clone;
	}
	
	/**
	 * Checks if the token satisfies any of the given conditions.
	 * Each condition may be one of the following:
	 * <ul>
	 * <li>{@link TokenType}</li>
	 * <li>{@link Keyword}</li>
	 * <li>{@link Punctuator}</li>
	 * <li>{@link String}</li>
	 * </ul>
	 * 
	 * @param args the conditions
	 * @return {@code true} if any of the conditions is satisfied
	 */
	public boolean is(Object...args) {
		for(Object arg : args) {
			if(arg == type || arg == keyword || arg == punctuator)
				return true;
			
			if(arg instanceof String str) {
				if(keyword != null && keyword.getName().equals(str))
					return true;

				if(punctuator != null && punctuator.getName().equals(str))
					return true;
				
				if(type == TokenType.IDENTIFIER && string.equals(str))
					return true;
			}
			
			if(arg instanceof Punctuator punct && punctuator != null && punct.getName().equals(punctuator.getName()))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Escapes potential quotes of this token (in string or character constants)
	 * 
	 * @return the escaped token string
	 */
	public String quoted() {
		return is(TokenType.STRING, TokenType.CHARACTER)
			? StringUtils.quote(raw)
			: raw;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof Token tok
			&& tok.type == type
			&& Objects.equals(tok.raw, raw);
	}
	
	@Override
	public String toString() {
		switch(type) {
		case IDENTIFIER:	return "Token(ident=" + string + ")";
		case STRING:		return "Token(string=" + StringUtils.quote(string) + ", wide=" + wide + ")";
		case PUNCTUATOR:	return "Token(" + punctuator.getName() + ")";
		
		case KEYWORD:		return "Token(" + keyword.getName() + ")";
		case CONSTANT:		return "Token(" + (integer == null ? decimal : integer) + ", type=" + numericType + ")";
		
		case HEADER:		return "Token(" + StringUtils.quote(string) + ", pp)";
		case NUMBER:		return "Token(" + string + ", pp)";
		case CHARACTER:		return "Token(" + integer + ", pp)";
		case WHITESPACE:	return "Token(whitespace, pp)";
		case NEWLINE:		return "Token(new-line, pp)";
		case UNPARSEABLE:	return "Token(unparseable, pp)";
		}
		
		return "Token(unknown)";
	}
	
}
