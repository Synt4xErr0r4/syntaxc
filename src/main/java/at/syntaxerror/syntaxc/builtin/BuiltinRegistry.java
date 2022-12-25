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
package at.syntaxerror.syntaxc.builtin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.preprocessor.macro.BuiltinMacro;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class BuiltinRegistry {
	
	/*
	 * Built-in functions:
	 * 
	 * - size_t __builtin_offsetof(<type>, <member-designator>)
	 * - void __builtin_va_start(va_list ap, <parmN>);
	 * - <type> __builtin_va_arg(va_list ap, <type>);
	 * - void __builtin_va_end(va_list ap);
	 */

	private static final Map<String, Supplier<BuiltinFunction>> BUILTIN = new HashMap<>();

	static {
		// #define __builtin_va_list void *
		BuiltinMacro.defineList(
			"__builtin_va_list",
			p -> List.of(
				Token.ofIdentifier(p.getPosition(), "void"),
				Token.ofPunctuator(p.getPosition(), Punctuator.POINTER)
			)
		);
		
		BuiltinKind.init();
	}
	
	public static void init() { }
	
	public static boolean isBuiltin(String name) {
		if(!name.startsWith("__builtin_"))
			return false;
		
		return BUILTIN.containsKey(name.substring(10));
	}
	
	public static BuiltinFunction newFunction(String name) {
		if(!name.startsWith("__builtin_"))
			return null;
		
		return BUILTIN.get(name.substring(10)).get();
	}
	
	public static void register(String name, Supplier<BuiltinFunction> function) {
		BUILTIN.put(name, function);
	}
	
}
