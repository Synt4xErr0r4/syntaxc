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
package at.syntaxerror.syntaxc.lexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum Punctuator {

	LBRACKET		("["),
	RBRACKET		("]"),
	LPAREN			("("),
	RPAREN			(")"),
	LBRACE			("{"),
	RBRACE			("}"),
	MEMBER_DIRECT	("."),
	MEMBER_INDIRECT	("->"),
	INCREMENT		("++"),
	DECREMENT		("--"),
	BITWISE_AND		("&"), // ambiguous
	ADDRESS_OF		("&"), // ambiguous
	MULTIPLY		("*"), // ambiguous
	INDIRECTION		("*"), // ambiguous
	POINTER			("*"), // ambiguous
	ADD				("+"), // ambiguous
	PLUS			("+"), // ambiguous
	SUBTRACT		("-"), // ambiguous
	MINUS			("-"), // ambiguous
	BITWISE_NOT		("~"),
	LOGICAL_NOT		("!"),
	DIVIDE			("/"),
	MODULO			("%"),
	LSHIFT			("<<"),
	RSHIFT			(">>"),
	LESS			("<"),
	GREATER			(">"),
	LESS_EQUAL		("<="),
	GREATER_EQUAL	(">="),
	EQUAL			("=="),
	NOT_EQUAL		("!="),
	BITWISE_XOR		("^"),
	BITWISE_OR		("|"),
	LOGICAL_AND		("&&"),
	LOGICAL_OR		("||"),
	TERNARY_THEN	("?"),
	TERNARY_ELSE	(":"),
	ASSIGN			("="),
	ASSIGN_MULTIPLY	("*="),
	ASSIGN_DIVIDE	("/="),
	ASSIGN_MODULO	("%="),
	ASSIGN_ADD		("+="),
	ASSIGN_SUBTRACT	("-="),
	ASSIGN_LSHIFT	("<<="),
	ASSIGN_RSHIFT	(">>="),
	ASSIGN_AND		("&="),
	ASSIGN_XOR		("^="),
	ASSIGN_OR		("|="),
	COMMA			(","),
	SEMICOLON		(";"),
	ELLIPSIS		("..."),
	HASH			("#"),
	DOUBLE_HASH		("##");
	
	private static final Map<String, Punctuator> MAPPED;
	private static final List<String> NAMES;
	
	static {
		MAPPED = new HashMap<>();
		
		Stream.of(values()).forEach(kw -> MAPPED.put(kw.name, kw));
		
		List<String> names = new ArrayList<>(MAPPED.keySet());
		
		// sort by length, in reverse order (longest to smallest)
		Collections.sort(names, (a, b) -> Integer.compare(b.length(), a.length()));
		
		NAMES = Collections.unmodifiableList(names);
	}
	
	public static Punctuator of(String name) {
		return MAPPED.get(name);
	}
	
	public static List<String> getNames() {
		return NAMES;
	}
	
	private final String name;
	
}
