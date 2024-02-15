/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.lexer;

import at.syntaxerror.syntaxc.frontend.c.math.CNumber;
import at.syntaxerror.syntaxc.frontend.c.string.CString;
import at.syntaxerror.syntaxc.io.CharSource.Positioned;
import at.syntaxerror.syntaxc.io.CharSource.Range;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 
 *
 * @author Thomas Kasper
 */
@Accessors(fluent = true)
public abstract class Token implements Positioned {

	public abstract String raw();
	public abstract TokenType type();
	
	public boolean is(Object...any) {
		TokenType type = type();
		
		for(Object obj : any)
			if(this == obj || type == obj || equals(obj))
				return true;
		
		return false;
	}
	
	@RequiredArgsConstructor
	@Accessors(fluent = true)
	@Getter
	@ToString(exclude = { "position", "raw" })
	public static class Identifier extends Token {
		
		private final Range position;
		private final CString value;
		private final String raw;
		
		@Override
		public TokenType type() {
			return TokenType.IDENTIFIER;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof Identifier ident)
				return value.equals(ident.value);
			
			return value.equals(obj);
		}
		
	}

	@RequiredArgsConstructor
	@Accessors(fluent = true)
	@Getter
	@ToString(exclude = { "position" })
	public static class PPNumber extends Token {

		private final Range position;
		private final String raw;
		
		@Override
		public TokenType type() {
			return TokenType.PPNUMBER;
		}
		
	}
	
	@RequiredArgsConstructor
	@Accessors(fluent = true)
	@Getter
	@ToString(exclude = { "position", "raw" })
	public static class Keyword extends Token {
		
		private final Range position;
		private final KeywordType keyword;
		private final String raw;
		
		@Override
		public TokenType type() {
			return TokenType.KEYWORD;
		}
		
		@Override
		public boolean equals(Object obj) {
			return keyword.isEqual(obj);
		}
		
	}
	
	@RequiredArgsConstructor
	@Accessors(fluent = true)
	@Getter
	@ToString(exclude = { "position", "raw" })
	public static class Constant extends Token {
		
		private final Range position;
		private final CNumber constant;
		private final String raw;
		
		@Override
		public TokenType type() {
			return TokenType.CONSTANT;
		}
		
	}
	
	@RequiredArgsConstructor
	@Accessors(fluent = true)
	@Getter
	@ToString(exclude = { "position", "raw" })
	public static class StringLiteral extends Token {
		
		private final Range position;
		private final CString string;
		private final String raw;
		
		@Override
		public TokenType type() {
			return TokenType.STRING;
		}
		
	}
	
	@RequiredArgsConstructor
	@Accessors(fluent = true)
	@Getter
	@ToString(exclude = { "position", "raw" })
	public static class Punctuator extends Token {
		
		private final Range position;
		private final PunctType punctuator;
		private final String raw;
		
		@Override
		public TokenType type() {
			return TokenType.PUNCTUATOR;
		}
		
		@Override
		public boolean equals(Object obj) {
			return punctuator.isEqual(obj);
		}
		
	}
	
	@RequiredArgsConstructor
	@Accessors(fluent = true)
	@Getter
	@ToString(exclude = { "position" })
	public static class Whitespace extends Token {

		private final Range position;
	
		@Override
		public TokenType type() {
			return TokenType.WHITESPACE;
		}

		@Override
		public String raw() {
			return " ";
		}
		
	}

	@RequiredArgsConstructor
	@Accessors(fluent = true)
	@Getter
	@ToString(exclude = { "position" })
	public static class EOL extends Token {

		private final Range position;
	
		@Override
		public TokenType type() {
			return TokenType.EOL;
		}

		@Override
		public String raw() {
			return " ";
		}
		
	}

	@RequiredArgsConstructor
	@Accessors(fluent = true)
	@Getter
	@ToString(exclude = { "position" })
	public static class EOF extends Token {

		private final Range position;
	
		@Override
		public TokenType type() {
			return TokenType.EOF;
		}

		@Override
		public String raw() {
			return "";
		}
		
	}

	@RequiredArgsConstructor
	@Accessors(fluent = true)
	@Getter
	@ToString(exclude = { "position", "raw" })
	public static class Unparseable extends Token {

		private final Range position;
		private final int codepoint;
		private final String raw;
	
		@Override
		public TokenType type() {
			return TokenType.UNKNOWN;
		}
		
	}
		
}
