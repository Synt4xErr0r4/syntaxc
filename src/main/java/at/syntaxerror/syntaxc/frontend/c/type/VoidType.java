/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.type;

/**
 * 
 *
 * @author Thomas Kasper
 */
public class VoidType extends Type {

	protected VoidType() { }

	@Override
	public int sizeof() {
		return 0;
	}

	@Override
	protected Type clone() {
		return new VoidType();
	}

	@Override
	protected String toStringPrefix() {
		return "void";
	}

	@Override
	protected String toStringSuffix() {
		return null;
	}

}
