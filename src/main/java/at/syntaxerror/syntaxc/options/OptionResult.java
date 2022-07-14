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
package at.syntaxerror.syntaxc.options;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
public class OptionResult {

	private final Map<String, List<String>> valuesName;
	private final Map<Character, List<String>> valuesMnemonic;
	private final List<String> fallbacks;
	
	private final @Getter OptionParser parser;
	
	protected OptionResult(OptionParser parser) {
		this.parser = parser;
		
		valuesName = new LinkedHashMap<>();
		valuesMnemonic = new LinkedHashMap<>();
		fallbacks = new ArrayList<>();
	}
	
	protected void add(Option option, String value) {
		String name = option.name();
		char mnemonic = option.mnemonic();
		
		if(name == null && mnemonic == 0)
			fallbacks.add(value);
		
		else {
			if(name != null) valuesName.computeIfAbsent(name, x -> new ArrayList<>()).add(value);
			if(mnemonic != 0) valuesMnemonic.computeIfAbsent(mnemonic, x -> new ArrayList<>()).add(value);
		}
	}
	
	protected boolean has(Option option) {
		String name = option.name();
		char mnemonic = option.mnemonic();
		
		if(name != null)
			return valuesName.containsKey(name);
		
		if(mnemonic != 0)
			return valuesMnemonic.containsKey(mnemonic);

		return !fallbacks.isEmpty();
	}
	
	public boolean hasUnnamed() {
		return !fallbacks.isEmpty();
	}
	
	public boolean has(String name) {
		return valuesName.containsKey(name);
	}
	
	public boolean has(char mnemonic) {
		return valuesMnemonic.containsKey(mnemonic);
	}
	
	public int getUnnamedCount() {
		return fallbacks.size();
	}
	
	public int getCount(String name) {
		return get(name).size();
	}
	
	public int getCount(char mnemonic) {
		return get(mnemonic).size();
	}
	
	public List<String> getUnnamed() {
		return fallbacks;
	}
	
	public List<String> get(String name) {
		return valuesName.getOrDefault(name, List.of());
	}
	
	public List<String> get(char mnemonic) {
		return valuesMnemonic.getOrDefault(mnemonic, List.of());
	}
	
}
