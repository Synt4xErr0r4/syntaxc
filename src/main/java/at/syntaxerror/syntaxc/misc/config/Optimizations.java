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
package at.syntaxerror.syntaxc.misc.config;

import at.syntaxerror.syntaxc.misc.config.Configurable.Toggleable;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public enum Optimizations implements Toggleable {
	
	CONST_FOLDING	("const-folding",		"Automatically inlines global arithmetic variables declared as »§cconst§f«"),
	GOTO			("goto",				"Automatically removes »§cgoto§f« statements followed by the label they jump to"),
	JUMP_TO_JUMP	("jump-to-jump",		"Automatically chooses the shortest path instead of jumping several times in a row"),
	;
	
	static {
		for(Optimizations opt : values())
			ConfigRegistry.registerOptimization(opt.name, opt);
	}
	
	public static void init() { }
	
	private final String name;
	private final String description;

	private final boolean acceptsValue;
	
	@Setter
	private boolean enabled = true;
	
	@Setter
	private String value;
	
	private Optimizations(String name, String description) {
		this(name, description, true, null);
	}

	private Optimizations(String name, String description, String value) {
		this(name, description, true, value);
	}

	private Optimizations(String name, String description, boolean enabled) {
		this(name, description, enabled, null);
	}

	private Optimizations(String name, String description, boolean enabled, String value) {
		this.name = name;
		this.description = description;
		
		this.enabled = enabled;
		this.value = value;
		
		acceptsValue = value != null;
	}

	@Override
	public String getDescription() {
		return description + (enabled ? " §8(§aenabled§8)" : " §8(§9disabled§8)");
	}
	
	@Override
	public boolean acceptsValue() {
		return acceptsValue;
	}

}
