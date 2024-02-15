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
public abstract class Type {

	private Type aliasBase;
	private String alias;
	
	public Type alias(String alias) {
		Type clone = clone();
		clone.aliasBase = this;
		clone.alias = alias;
		
		return clone;
	}

	public Type unalias() {
		if(alias == null)
			return this;
		
		return aliasBase;
	}
	
	public Type unaliasFully() {
		Type type = this;
		
		while(type.alias != null)
			type = type.aliasBase;
		
		return type;
	}
	
	public abstract int sizeof();

	protected abstract Type clone();
	
	protected abstract String toStringPrefix();
	protected abstract String toStringSuffix();
	
	@Override
	public String toString() {
		if(alias != null)
			return alias;
		
		return toStringPrefix() + toStringSuffix();
	}
	
}
