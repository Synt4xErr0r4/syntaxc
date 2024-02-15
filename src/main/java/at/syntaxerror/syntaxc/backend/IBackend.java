/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.backend;

import java.util.List;

import at.syntaxerror.syntaxc.config.IConfigContext;

/**
 * 
 *
 * @author Thomas Kasper
 */
public interface IBackend extends IConfigContext {

	List<String> getNames();
	
	default List<Object> getCommandLineOptions() {
		return List.of();
	}
	
	void init();
	
}
