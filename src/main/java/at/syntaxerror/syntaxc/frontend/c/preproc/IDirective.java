/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.preproc;

import java.util.List;

import at.syntaxerror.syntaxc.frontend.c.lexer.Token;

/**
 * 
 *
 * @author Thomas Kasper
 */
public interface IDirective {

	void process(Token start, List<Token> tokens);
	
	default boolean isSupported() {
		return true;
	}
	
}
