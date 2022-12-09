/* MIT License
 * 
 * Copyright (c) 2020, 2021 Thomas Kasper
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
package at.syntaxerror.syntaxc.misc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public enum Flag implements NamedToggle {
	NO_STDLIB			("stdlib",				"Adds default library paths to the include path"),
	ELIFDEF				("elifdef", 			"Specifies whether §c#elifdef §fand §c#elifndef §fpreprocessing directives are allowed"),
	FUNC				("func",				"Specifies whether §c__FUNCTION__ §fand §c__func__ §freturn the current function name"),
	BINARY_LITERALS		("binary-literals",		"Specifies whether binary literals are allowed"),
	LONG_DOUBLE			("long-double",			"When disabled, §clong double §fis an alias for §cdouble"),
	UNSIGNED_CHAR		("unsigned-char",		"When enabled, §cchar§fs will be unsigned", false),
	SYNTAX_TREE			("syntax-tree",			"Generates the syntax tree in DOT, PNG, or SVG format", false, "dot"),
	CONTROL_FLOW_GRAPH	("control-flow-graph",	"Generates the control flow graph in DOT, PNG, or SVG format", false, "dot"),
	ALIGN				("align",				"Specifies the alignment for global variables, must be a multiple of 4", false, "4"),
	VERBOSE				("verbose",				"Enables more verbose diagnostic messages"),
	VERY_VERBOSE		("very-verbose",		"Enables very verbose diagnostic messages", false)
	;
	
	private static final Map<String, Flag> FLAGS;
	
	static {
		FLAGS = new HashMap<>();
		
		for(Flag flag : values())
			FLAGS.put(flag.name, flag);
	}
	
	public static Collection<Flag> getFlags() {
		return Collections.unmodifiableCollection(FLAGS.values());
	}
	
	public static Flag of(String name) {
		return FLAGS.get(name);
	}
	
	private final String name;
	private final String description;

	private final boolean acceptsValue;
	
	@Setter
	private boolean enabled = true;
	
	@Setter
	private String value;
	
	private Flag(String name, String description) {
		this(name, description, true, null);
	}

	private Flag(String name, String description, String value) {
		this(name, description, true, value);
	}

	private Flag(String name, String description, boolean enabled) {
		this(name, description, enabled, null);
	}

	private Flag(String name, String description, boolean enabled, String value) {
		this.name = name;
		this.description = description;
		
		this.enabled = enabled;
		this.value = value;
		
		acceptsValue = value != null;
	}
	
}
