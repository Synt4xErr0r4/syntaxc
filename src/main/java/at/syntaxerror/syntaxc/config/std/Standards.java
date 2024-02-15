/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.config.std;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import at.syntaxerror.syntaxc.argp.IStringTransformer;
import lombok.experimental.UtilityClass;

/**
 * 
 *
 * @author Thomas Kasper
 */
@UtilityClass
public class Standards {

	private static final Map<String, IStandard> STANDARDS = new LinkedHashMap<>();

	public static <E extends Enum<E> & IStandard> void addAll(Class<E> enumClass) {
		addAll(enumClass.getEnumConstants());
	}
	
	public static void addAll(IStandard...standards) {
		for(IStandard std : standards)
			for(String alias : std.getAliases())
				STANDARDS.put(alias, std);
	}
	
	public static Map<String, IStandard> getStandards() {
		return Collections.unmodifiableMap(STANDARDS);
	}
	
	public static void enable(String standard) {
		IStandard std = STANDARDS.get(standard);
		
		if(std == null)
			throw new IllegalArgumentException("No such standard »" + standard + "«");
		
		std.enable();
	}
	
	public static class Transformer implements IStringTransformer<Void> {

		@Override
		public Void apply(String std) {
			enable(std);
			return null;
		}
		
	}
	
}
