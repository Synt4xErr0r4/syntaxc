/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.lexer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 *
 * @author Thomas Kasper
 */
@RequiredArgsConstructor
@Getter
public enum PunctType {

	LBRACKET		("[", "<:"),
	RBRACKET		("]", ">:"),
	LPAREN			("("),
	RPAREN			(")"),
	LBRACE			("{", "<%"),
	RBRACE			("}", "%>"),
	PERIOD			("."),
	ARROW			("->"),
	INCREMENT		("++"),
	DECREMENT		("--"),
	BIT_AND			("&"),
	ADDRESS_OF		("&"),
	MULTIPLY		("*"),
	DEREFERENCE		("*"),
	PLUS			("+"),
	ADD				("+"),
	MINUS			("-"),
	SUBTRACT		("-"),
	BIT_NOT			("~"),
	LOGICAL_NOT		("!"),
	DIVIDE			("/"),
	MODULO			("%"),
	LSHIFT			("<<"),
	RSHIFT			(">>"),
	LESS			("<"),
	GREATER			(">"),
	LESS_EQUAL		("<="),
	GREATER_EQUAL	(">="),
	EQUAL			("=="),
	NOT_EQUAL		("!="),
	BIT_XOR			("^"),
	BIT_OR			("|"),
	LOGICAL_AND		("&&"),
	LOGICAL_OR		("||"),
	TERNARY_IF		("?"),
	TERNARY_ELSE	(":"),
	ATTRIBUTE		("::"), // C23
	SEMICOLON		(";"),
	ELLIPSIS		("..."),
	ASSIGN			("="),
	ASSIGN_MULTIPLY	("*="),
	ASSIGN_DIVIDE	("/="),
	ASSIGN_MODULO	("%="),
	ASSIGN_ADD		("+="),
	ASSIGN_SUBTRACT	("-="),
	ASSIGN_LSHIFT	("<<="),
	ASSIGN_RSHIFT	(">>="),
	ASSIGN_AND		("&="),
	ASSIGN_XOR		("^="),
	ASSIGN_OR		("|="),
	COMMA			(","),
	DIRECTIVE		("#", "%:"),
	STRINGIFY		("#", "%:"),
	CONCAT			("##", "%:%:");
	
	private final String symbol;
	private final String digraph;
	
	private PunctType(String symbol) {
		this(symbol, null);
	}
	
	public boolean isEqual(Object other) {
		if(other == this)
			return true;
		
		if(other instanceof String str)
			return str.equals(symbol)
				|| str.equals(digraph);
		
		return false;
	}
	
}
