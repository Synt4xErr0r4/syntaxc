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
package at.syntaxerror.syntaxc.preprocessor.directive;

import java.util.HashMap;
import java.util.Map;

import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.preprocessor.Preprocessor;
import at.syntaxerror.syntaxc.preprocessor.directive.Directive.DirectiveConstructor;
import at.syntaxerror.syntaxc.preprocessor.directive.Directive.ElseDirectiveConstructor;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class Directives {

	private static final Map<String, DirectiveConstructor> DIRECTIVES = new HashMap<>();
	private static final Map<String, ElseDirectiveConstructor> ELSE_DIRECTIVES = new HashMap<>();
	
	private static void register(String name, DirectiveConstructor constructor) {
		DIRECTIVES.put(name, constructor);
	}

	private static void register(String name, ElseDirectiveConstructor constructor) {
		register(name, (pp, tok) -> constructor.construct(pp, tok, false));
	}

	private static void registerElse(String name, ElseDirectiveConstructor constructor) {
		ELSE_DIRECTIVES.put(name, constructor);
	}
	
	static {
		/* #elifdef and #elifndef are extension (standardized in C23),
		 * that behave the same as #ifdef and #ifndef, except they are
		 * used in the same context as the #elif and #else directives.
		 * 
		 * § 6.8 Preprocessing directives
		 */
		
		register("define",			(pp, tok) -> new DefineDirective(pp, tok, false));
		register("undef",			(pp, tok) -> new DefineDirective(pp, tok, true));

		register("include",			IncludeDirective::new);
		register("line",			LineDirective::new);
		register("error",			ErrorDirective::new);
		register("pragma",			PragmaDirective::new);
		
		register("if",				IfDirective::new);
		register("ifdef",			(pp, tok) -> new IfdefDirective(pp, tok, true, false));
		register("ifndef",			(pp, tok) -> new IfdefDirective(pp, tok, false, false));

		/* else directives */

		registerElse("endif",		EmptyDirective::new);
		registerElse("else",		ElseDirective::new);
		registerElse("elif",		IfDirective::new);
		
		/* extensions */
		registerElse("elifdef",		(pp, tok, f) -> new IfdefDirective(pp, tok, true, f));
		registerElse("elifndef",	(pp, tok, f) -> new IfdefDirective(pp, tok, false, f));
	}
	
	public static Directive find(Preprocessor preprocessor, Token token) {
		String name = token.getString();
		
		if(!DIRECTIVES.containsKey(name))
			return null;
		
		return DIRECTIVES
			.get(name)
			.construct(preprocessor, token)
			.setName(name);
	}
	
	public static Directive findElse(Preprocessor preprocessor, Token token, boolean alreadySucceeded) {
		String name = token.getString();
		
		if(!ELSE_DIRECTIVES.containsKey(name))
			return null;
		
		return ELSE_DIRECTIVES
			.get(name)
			.construct(preprocessor, token, alreadySucceeded)
			.setName(name);
	}
	
}
