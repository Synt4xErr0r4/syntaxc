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
@Category("Output configuration")
public class COutputArgs {

	@Parameter(mnemonic = 'E')
	@Description("Stops compilation after preprocessing (outputs to stdout by default)")
	public boolean outputPreprocessed;
	
}
