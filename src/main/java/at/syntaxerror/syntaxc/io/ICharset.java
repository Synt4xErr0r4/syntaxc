/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.io;

import java.util.function.Supplier;

/**
 * 
 *
 * @author Thomas Kasper
 */
public interface ICharset {
	
	int decode(Supplier<Integer> next);
	byte[] encode(int codepoint);
	
}
