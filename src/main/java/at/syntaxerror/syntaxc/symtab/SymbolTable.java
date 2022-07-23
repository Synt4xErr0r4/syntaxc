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
import java.util.List;

import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
public class SymbolTable {
	
	private Scope<SymbolTag> tags; // structs, unions, enums
	private Scope<SymbolObject> objects; // local/global variables, typedefs, functions
	
	@Getter
	private SymbolTable parent;
	
	@Getter
	private StringTable stringTable;
	
	private List<SymbolTable> children;
	
	public SymbolTable() {
		this(null);
	}

	private SymbolTable(SymbolTable parent) {
		this.parent = parent;

		if(parent == null) {
			tags = new Scope<>(null);
			objects = new Scope<>(null);
			
			stringTable = new StringTable();
		}
		else {
			tags = new Scope<>(parent.tags);
			objects = new Scope<>(parent.objects);
			
			stringTable = parent.stringTable;
		}
		
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
	
	public boolean addObject(SymbolObject object) {
		return objects.add(object);
	}
	
	public boolean addTag(SymbolTag tag) {
		return tags.add(tag);
	}
	
	public SymbolObject findObject(String name) {
		return objects.find(name);
	}
	
	public SymbolTag findTag(String name) {
		return tags.find(name);
	}
	
	public boolean hasObject(String name) {
		return objects.has(name);
	}
	
	public boolean hasTag(String name) {
		return tags.has(name);
	}
	
}
