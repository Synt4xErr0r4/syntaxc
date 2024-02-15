/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.lexer;

/**
 * 
 *
 * @author Thomas Kasper
 */
public enum TokenType {

	KEYWORD,
	IDENTIFIER,
	CONSTANT,
	STRING,
	PUNCTUATOR,
	
	HEADER,
	PPNUMBER,
	CHARACTER,
	UNKNOWN,
	
	WHITESPACE,
	EOL,
	EOF
	
}
