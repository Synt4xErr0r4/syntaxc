/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.config.std;

/**
 * 
 *
 * @author Thomas Kasper
 */
public interface IStandard {

	String getName();
	String[] getAliases();
	
	void enable();
	
}
