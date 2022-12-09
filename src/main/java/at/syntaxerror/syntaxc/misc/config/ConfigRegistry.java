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
package at.syntaxerror.syntaxc.misc.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.config.Configurable.Toggleable;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
public class ConfigRegistry {

	private static final Registry MACHINES = new Registry("machine-specific option");
	private static final Registry OPTIMIZATIONS = new Registry("optimization");
	private static final Registry WARNINGS = new Registry("warning");
	private static final Registry FLAGS = new Registry("flag");
	
	public static List<Configurable> getMachineSpecifics() {
		return MACHINES.getEntries();
	}

	public static List<Configurable> getOptimizations() {
		return OPTIMIZATIONS.getEntries();
	}
	
	public static List<Configurable> getWarnings() {
		return WARNINGS.getEntries();
	}
	
	public static List<Configurable> getFlags() {
		return FLAGS.getEntries();
	}

	public static void registerMachineSpecific(String name, Configurable option) {
		MACHINES.put(name, option);
	}
	
	public static void registerOptimization(String name, Toggleable option) {
		OPTIMIZATIONS.put(name, option);
	}

	public static void registerWarning(String name, Toggleable option) {
		WARNINGS.put(name, option);
	}

	public static void registerFlag(String name, Toggleable option) {
		FLAGS.put(name, option);
	}
	
	public static void enableMachineSpecific(String name) {
		MACHINES.set(name);
	}
	
	public static void enableOptimization(String name) {
		OPTIMIZATIONS.set(name);
	}

	public static void enableWarning(String name) {
		WARNINGS.set(name);
	}
	
	public static void enableFlag(String name) {
		FLAGS.set(name);
	}
	
	@RequiredArgsConstructor
	private static class Registry {
		
		private final String genericName;
		
		private Map<String, Configurable> entries = new LinkedHashMap<>();
		
		public List<Configurable> getEntries() {
			return new ArrayList<>(entries.values());
		}
		
		public void put(String name, Configurable entry) {
			if(name.contains("["))
				name = name.substring(0, name.indexOf('['));

			if(name.contains("="))
				name = name.substring(0, name.indexOf('='));
			
			entries.put(name, entry);
		}
		
		public void set(String name) {
			boolean state = true;
			String value = null;
			
			if(name.startsWith("no-")) {
				name = name.substring(3);
				state = false;
			}
			
			if(name.contains("=")) {
				int idx = name.indexOf('=');
				
				value = name.substring(idx + 1);
				name = name.substring(0, idx);
			}
			
			Configurable entry = entries.get(name);
			
			if(entry == null) {
				Logger.warn("Unrecognized %s »%s«", genericName, name);
				return;
			}
			
			if(entry instanceof Toggleable toggle)
				toggle.setEnabled(state);
			
			if(value != null) {
				if(!entry.acceptsValue())
					Logger.warn("The %s »%s« does not accept values", genericName, name);
				
				entry.setValue(value);
			}
		}
		
	}
	
}
