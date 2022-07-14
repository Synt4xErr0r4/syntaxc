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
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
@Getter
public enum Flag implements NamedToggle {
	ELIFDEF			("elifdef", 		"Specifies whether #elifdef and #elifndef preprocessing directives are allowed"),
	FUNC			("func",			"Specifies whether __FUNCTION__ and __func__ return the current function name"),
	BINARY_LITERALS	("binary-literals",	"Specifies whether binary literals are allowed")
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

	@Setter
	private boolean enabled = true;
	
}
