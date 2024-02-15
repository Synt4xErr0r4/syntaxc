/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c;

import java.util.Map;

import at.syntaxerror.syntaxc.argp.Category;
import at.syntaxerror.syntaxc.argp.Description;
import at.syntaxerror.syntaxc.argp.Parameter;
import at.syntaxerror.syntaxc.argp.Value;
import lombok.ToString;

/**
 * 
 *
 * @author Thomas Kasper
 */
@ToString
@Category("Preprocessor configuration")
public class CPreprocArgs {
	
	@Parameter(name = "define", mnemonic = 'D')
	@Value("defn[=value]")
	@Description("Adds a macro definition with an optional value to all input files")
	public Map<String, String> definitions;
	
	@Parameter(name = "precompile-headers", mnemonic = 'P')
	@Description("Precompiles all specified ».h« files, generating a corresponding ».h.sych« file each")
	public boolean precompileHeaders;
	
}
