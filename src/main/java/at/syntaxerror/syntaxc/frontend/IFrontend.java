/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend;

import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxc.config.IConfigContext;
import at.syntaxerror.syntaxc.config.std.IStandard;
import at.syntaxerror.syntaxc.io.IFileStream;

/**
 * 
 *
 * @author Thomas Kasper
 */
public interface IFrontend extends IConfigContext {

	List<String> getLanguages();
	Map<String, String> getFileExtensions();
	
	IStandard getStandard();
	
	default List<Object> getCommandLineOptions() {
		return List.of();
	}
	
	Object process(IFileStream file);
	
}
