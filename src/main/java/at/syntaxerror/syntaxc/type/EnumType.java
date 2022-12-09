/* MIT License
 * 
 * Copyright (c) 2022 Thomas Kasper
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package at.syntaxerror.syntaxc.type;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import lombok.Getter;

/**
 * This class represents an enumerator type, e.g. <code>enum { A, B, C }</code>
 * 
 * @author Thomas Kasper
 * 
 */
public class EnumType extends Type {

	/**
	 * Creates a new enum type without a name
	 * 
	 * @return the new enum type
	 */
	public static EnumType forAnonymousEnum() {
		return forEnum(getAnonymousName());
	}

	/**
	 * Creates a new enum type with a name
	 * 
	 * @param name the name of the enum
	 * @return the new enum type
	 */
	public static EnumType forEnum(String name) {
		return new EnumType(name);
	}

	/**
	 * the list of types which an enumeration constant may have.
	 * Smaller types are preferred over larger types.
	 */
	private static final NumericValueType[] ENUM_TYPES = {
		NumericValueType.SIGNED_INT,
		NumericValueType.UNSIGNED_INT,
		NumericValueType.SIGNED_LONG,
		NumericValueType.UNSIGNED_LONG
	};

	@Getter
	private final String name;
	
	// index into the ENUM_TYPES array
	private int numericType = 0;
	
	private Enumerator previous;
	
	private LinkedHashMap<String, Enumerator> enumerators = new LinkedHashMap<>();

	@Getter
	private boolean incomplete = true;
	
	private EnumType(String name) {
		super(TypeKind.ENUM);
		this.name = name;
	}
	
	/**
	 * Marks the enum as complete
	 */
	public void setComplete() {
		incomplete = false;
	}
	
	/**
	 * Returns the type of an enumeration constant
	 * 
	 * @return the type of an enumeration constant
	 */
	public NumericValueType getNumericType() {
		return ENUM_TYPES[numericType];
	}
	
	/**
	 * Returns an unmodifiable view of all enumeration constants
	 * 
	 * @return an unmodifiable view of all enumeration constants
	 */
	public Map<String, Enumerator> getEnumerators() {
		return Collections.unmodifiableMap(enumerators);
	}
	
	/**
	 * Returns the type of an enumeration constant, including potential type qualifiers
	 * 
	 * @return the type of an enumeration constant
	 */
	public NumberType asNumberType() {
		return (NumberType) getNumericType().asType().inheritQualifiers(this);
	}
	
	/**
	 * Adds a new enumeration constant to the enum, with its value
	 * set to previous constant's value increased by one
	 * 
	 * @param name the name of the constant
	 */
	public void addEnumerator(Token name) {
		if(previous == null)
			addEnumerator(name, BigInteger.ZERO, true);
		
		else addEnumerator(
			name,
			previous.value().add(BigInteger.ONE),
			false
		);
	}
	
	/**
	 * Adds a new enumeration constant with a given value
	 * 
	 * @param name the name of the constant
	 * @param value the value of the constant
	 */
	public void addEnumerator(Token name, BigInteger value) {
		addEnumerator(name, value, true);
	}

	/**
	 * Adds a new enumeration constant with a given value
	 * 
	 * @param name the name of the constant
	 * @param value the value of the constant
	 * @param resize whether the type should be resized in case of an overflow
	 */
	private void addEnumerator(Token name, BigInteger value, boolean resize) {
		setComplete();
		
		if(!getNumericType().inRange(value)) {
			if(!resize)
				Logger.error(name, "Overflow in enumeration");
			
			while(++numericType < ENUM_TYPES.length)
				if(getNumericType().inRange(value))
					break;
		}
		
		previous = new Enumerator(name, value);
		
		enumerators.put(previous.getName(), previous);
	}
	
	/**
	 * Returns the value of an enumeration constant
	 * 
	 * @param name the name of the enumeration constant
	 * @return the value of an enumeration constant
	 */
	public BigInteger getValue(String name) {
		return enumerators.containsKey(name)
			? enumerators.get(name).value()
			: null;
	}
	
	@Override
	protected Type clone() {
		EnumType cloned = new EnumType(name);
		
		cloned.numericType = numericType;
		cloned.previous = previous;
		cloned.enumerators = enumerators;
		cloned.incomplete = incomplete;
		
		return inheritProperties(cloned);
	}
	
	@Override
	public String toStringPrefix() {
		return toStringQualifiers() + "enum " + name;
	}
	
	/**
	 * This class represents an enumerator of an {@link EnumType}
	 * 
	 * @author Thomas Kasper
	 *
	 */
	public static record Enumerator(Token name, BigInteger value) implements Positioned {
		
		@Override
		public Position getPosition() {
			return name.getPosition();
		}
		
		public String getName() {
			return name.getString();
		}
		
	}

}
