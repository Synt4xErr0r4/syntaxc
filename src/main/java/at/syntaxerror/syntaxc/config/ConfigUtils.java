/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxc.backend.IBackend;
import at.syntaxerror.syntaxc.frontend.IFrontend;
import lombok.experimental.UtilityClass;

/**
 * 
 *
 * @author Thomas Kasper
 */
@UtilityClass
public class ConfigUtils {

	private static void printAll(List<ConfigValue> options) {
		for(ConfigValue option : options) {
			System.out.println("  " + option.toHelpString());
			
			String desc = option.getDescription();
			
			if(desc != null) {
				String indent = " ".repeat(8);
				
				for(String line : desc.split("\\R")) {
					System.out.print(indent);
					System.out.println(line);
				}
			}
		}
	}
	
	public static <T extends IConfigurable> void printAll(Map<String, T> options) {
		List<ConfigValue> globals = new ArrayList<>();
		Map<IConfigContext, List<ConfigValue>> byFrontend = new LinkedHashMap<>();
		Map<IConfigContext, List<ConfigValue>> byBackend = new LinkedHashMap<>();
		Map<IConfigContext, List<ConfigValue>> byMisc = new LinkedHashMap<>();
		
		for(var entry : options.entrySet()) {
			T ent = entry.getValue();
			
			if(ent == null)
				continue;
			
			ConfigValue cfg = ent.getConfigValue();
			
			IConfigContext ctx = cfg.getContext();
			
			if(ctx == null)
				globals.add(cfg);
			else {
				var map = byMisc;
				
				if(ctx instanceof IFrontend)
					map = byFrontend;
				else if(ctx instanceof IBackend)
					map = byBackend;
				
				map.computeIfAbsent(ctx, c -> new ArrayList<>()).add(cfg);
			}
		}
		
		printAll(globals);
		
		for(var entry : byFrontend.entrySet()) {
			System.out.println();
			System.out.printf("For the %s frontend:%n", entry.getKey().getName());
			printAll(entry.getValue());
		}
		
		for(var entry : byBackend.entrySet()) {
			System.out.println();
			System.out.printf("For the %s backend:%n", entry.getKey().getName());
			printAll(entry.getValue());
		}
		
		for(var entry : byMisc.entrySet()) {
			System.out.println();
			System.out.printf("For %s:%n", entry.getKey().getName());
			printAll(entry.getValue());
		}
	}
	
	public static <T extends IConfigurable> void setValue(Map<String, T> map, String kindName, String name, String value) {
	}
	
}
