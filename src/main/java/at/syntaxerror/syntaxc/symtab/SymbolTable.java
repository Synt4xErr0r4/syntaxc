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
package at.syntaxerror.syntaxc.symtab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
public class SymbolTable {

	private Map<String, SymbolObject> objects; // structs, unions, enums
	private Map<String, SymbolVariable> variables; // local/global variables, typedefs, functions
	
	@Getter
	private SymbolTable parent;
	
	private List<SymbolTable> children;
	
	public SymbolTable() {
		this(null);
	}

	private SymbolTable(SymbolTable parent) {
		this.parent = parent;

		objects = new LinkedHashMap<>();
		variables = new LinkedHashMap<>();
		
		children = new ArrayList<>();
	}
	
	public List<SymbolTable> getChildren() {
		return Collections.unmodifiableList(children);
	}
	
	public SymbolTable newChild() {
		SymbolTable child = new SymbolTable(this);
		
		children.add(child);
		
		return child;
	}
	
	public void addObject(SymbolObject object) {
		objects.put(object.getName(), object);
	}
	
	public void addVariable(SymbolVariable symbol) {
		variables.put(symbol.getName(), symbol);
	}
	
	public SymbolObject findObject(String name) {
		return objects.containsKey(name)
			? objects.get(name)
			: parent != null
				? parent.findObject(name)
				: null;
	}
	
	public SymbolVariable findVariable(String name) {
		return variables.containsKey(name)
			? variables.get(name)
			: parent != null
				? parent.findVariable(name)
				: null;
	}
	
}
