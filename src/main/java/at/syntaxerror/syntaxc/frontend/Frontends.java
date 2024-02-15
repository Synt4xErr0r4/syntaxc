/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxc.argp.IStringTransformer;
import at.syntaxerror.syntaxc.frontend.c.CFrontend;

/**
 * 
 *
 * @author Thomas Kasper
 */
public class Frontends {

	private static final List<IFrontend> FRONTENDS = new ArrayList<>();
	private static final Map<String, IFrontend> BY_LANG = new LinkedHashMap<>();
	private static final Map<String, String> BY_EXTENSION = new LinkedHashMap<>();
	
	public static String language = null;
	
	static {
		FRONTENDS.add(CFrontend.INSTANCE);
		
		FRONTENDS.forEach(frontend -> {
			frontend.getLanguages().forEach(lang -> BY_LANG.put(lang, frontend));
			BY_EXTENSION.putAll(frontend.getFileExtensions());
		});
	}
	
	public static void init() {}
	
	public static Map<String, IFrontend> getLanguages() {
		return Collections.unmodifiableMap(BY_LANG);
	}
	
	public static List<IFrontend> getFrontends() {
		return Collections.unmodifiableList(FRONTENDS);
	}
	
	public static String determineByExtension(String filename) {
		String language = null;
		
		int dot1 = filename.lastIndexOf('.');
		int dot2 = filename.lastIndexOf('.', dot1);
		
		if(dot2 > -1) {
			String ext = filename.substring(dot2 + 1);
			language = BY_EXTENSION.get(ext);
		}
		
		if(dot1 > -1 && language == null) {
			String ext = filename.substring(dot1 + 1);
			language = BY_EXTENSION.get(ext);
		}
		
		if(language == null)
			throw new IllegalArgumentException("Could not derive language from file name »" + filename + "«");
		
		return language;
	}
	
	public static class Transformer implements IStringTransformer<Void> {

		@Override
		public Void apply(String lang) {
			if(lang != null && !BY_LANG.containsKey(lang))
				throw new IllegalArgumentException("No such language »" + lang + "«");
			
			language = lang;
			return null;
		}
		
	}
	
}
