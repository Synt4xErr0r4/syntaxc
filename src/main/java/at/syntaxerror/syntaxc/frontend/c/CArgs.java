/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c;

import at.syntaxerror.syntaxc.argp.Category;
import at.syntaxerror.syntaxc.argp.Description;
import at.syntaxerror.syntaxc.argp.Parameter;
import lombok.ToString;

/**
 * 
 *
 * @author Thomas Kasper
 */
@ToString
@Category("C/C++ options")
public class CArgs {

	@Parameter(name = "ansi")
	@Description("Alias for »-std=c90« (C) or »-std=c++98« (C++)")
	public boolean ansi;

	@Parameter(name = "pedantic")
	@Description("Enforces ISO C and ISO C++ standards. Disables all compiler extensions")
	public boolean pedantic;
	
}
