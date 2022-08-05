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
public enum Optimization implements NamedToggle {
	INLINE_CONST	("inline-const",		"Automatically inlines global arithmetic variables declared as »§cconst§f«"),
	EVAL_EXPR		("eval-expressions",	"Automatically evaluates expression with a constant value (such as »§c1 + 1§f«)")
	;
	
	private static final Map<String, Optimization> OPTIMIZATIONS;
	
	static {
		OPTIMIZATIONS = new HashMap<>();
		
		for(Optimization flag : values())
			OPTIMIZATIONS.put(flag.name, flag);
	}
	
	public static Collection<Optimization> getOptimizations() {
		return Collections.unmodifiableCollection(OPTIMIZATIONS.values());
	}
	
	public static Optimization of(String name) {
		return OPTIMIZATIONS.get(name);
	}
	
	private final String name;
	private final String description;

	private final boolean acceptsValue;
	
	@Setter
	private boolean enabled = true;
	
	@Setter
	private String value;
	
	private Optimization(String name, String description) {
		this(name, description, true, null);
	}

	private Optimization(String name, String description, String value) {
		this(name, description, true, value);
	}

	private Optimization(String name, String description, boolean enabled) {
		this(name, description, enabled, null);
	}

	private Optimization(String name, String description, boolean enabled, String value) {
		this.name = name;
		this.description = description;
		
		this.enabled = enabled;
		this.value = value;
		
		acceptsValue = value != null;
	}
	
}
