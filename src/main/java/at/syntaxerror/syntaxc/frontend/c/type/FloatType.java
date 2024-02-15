/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.type;

import at.syntaxerror.ieee754.Floating;
import at.syntaxerror.ieee754.FloatingCodec;
import at.syntaxerror.ieee754.FloatingFactory;

/**
 * 
 *
 * @author Thomas Kasper
 */
public abstract sealed class FloatType<T extends Floating<T>> extends NumberType permits BinaryFloatType, DecimalFloatType {

	public abstract FloatingCodec<T> getCodec();
	public abstract FloatingFactory<T> getFactory();
	
}
