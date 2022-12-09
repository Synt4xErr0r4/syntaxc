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

import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.misc.config.Configurable.Toggleable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
@Getter
public enum Warnings implements Toggleable {
	NONE,
	
	/**
	 * ===== PREPROCESSING =====
	 */
	$0(CompilationStage.PREPROCESSING),
	PREPROC_NONE,
	
	PRAGMA					("pragma",					"Warns when an unknown §d#pragma §fdirective was encountered"),
	LINE_ZERO				("line-zero",				"Warns when a §d#line §fdirective sets the current line to zero"),
	UNDEF					("undef",					"Warns when a §d#undef §fdirective undefines a non-existent macro"),
	REDEF					("redef",					"Warns when a §d#define §fdirective redefines an existent macro"),
	BUILTIN_UNDEF			("undef-builtin",			"Warns when a §d#undef §fdirective undefines a builtin macro"),
	BUILTIN_REDEF			("redef-builtin",			"Warns when a §d#define §fdirective redefines a builtin macro"),
	TRAILING				("trailing",				"Warns when there is trailing data after a preprocessing directive"),
	EMPTY_DIRECTIVE			("empty-directive",			"Warns when an empty preprocessing directive was encountered"),

	/**
	 * ===== LEXICAL ANALYSIS =====
	 */
	$1(CompilationStage.LEXICAL),
	LEX_NONE,
	CHAR_HEX_OVERFLOW		("char-hex-overflow",		"Warns when a hexadecimal escape sequence (§c\\xABC...§f) overflows the maximum character width"),
	CHAR_OCT_OVERFLOW		("char-oct-overflow",		"Warns when an octal escape sequence (§c\\012...§f) overflows the maximum character width"),
	CHAR_OVERFLOW			("char-overflow",			"Warns when a character literal overflows the maximum width"),
	CODEPOINT				("codepoint",				"Warns when a character codepoint is out of range"),
	MULTICHAR				("multichar",				"Warns when a character literal contains multiple characters"),
	TRIGRAPHS				("trigraphs",				"Warns when encountering and processing a trigraph (e.g. §c??=§f)"),
	INT_OVERFLOW			("int-overflow",			"Warns when an integer overflows"),
	FLOAT_OVERFLOW			("float-overflow",			"Warns when a floating number overflows"),

	/**
	 * ===== SYNTACTIC ANALYSIS =====
	 */
	$2(CompilationStage.SYNTACTIC),
	SYN_NONE,
	DUPLICATE_QUALIFIER		("duplicate-qualifier",		"Warns when a type qualifier occurs multiple times"),
	K_AND_R					("k&r",						"Warns when a function is declared using K&R syntax"),
	REGISTER				("register",				"Warns when encountering the unsupported »§cregister§f« storage-class specifier"),

	/**
	 * ===== SEMANTIC ANALYSIS =====
	 */
	$3(CompilationStage.SEMANTIC),
	SEM_NONE,
	OVERFLOW				("implicit-overflow",		"Warns when implicitly casting a constant expression alters its value"),
	IMPLICIT_FUNCTION		("implicit-function",		"Warns when a function is not explicitly declared"),
	IMPLICIT_INT			("implicit-int",			"Warns when a type is implicitly declard as »§cint§f«"),
	DEREF_VOID				("deref-void-pointer",		"Warns when dereferencing a pointer to »§cvoid§f«"),
	INCOMPATIBLE_POINTERS	("incompatible-pointers",	"Warns when comparing two pointers of different types"),
	NEGATIVE_SHIFT			("negative-shift",			"Warns when the second operand for a shift operation (§c<<§f or §c>>§f) is negative"),
	USELESS					("useless-declaration",		"Warns when a declaration does not have a declarator or struct/union name"),
	MISSING_BRACES			("missing-braces",			"Warns when braces are missing around an initializer"),
	SCALAR_BRACES			("scalar-braces",			"Warns when braces are present around an scalar initializer"),
	STRING_INITIALIZER		("string-initializer",		"Warns when a string initializer overflows the array"),
	INITIALIZER				("initializer",				"Warns when an initializer contains too many values"),
	INITIALIZER_OVERFLOW	("initializer-overflow",	"Warns when an initializer value (integer or floating number) overflows"),
	RETURN_VOID				("return-void",				"Warns when a §creturn §fstatement returns nothing inside a non-void function"),
	RETURN_VALUE			("return-value",			"Warns when a §creturn §fstatement returns a value inside a void function"),
	RETURN_TYPE				("return-type",				"Warns when a §creturn §fstatement returns an incompatible type"),
	DEAD_CODE				("dead-code",				"Warns when a section of code is unreachable"),
	UNUSED_LABEL			("unused-label",			"Warns when a label is not used"),
	UNUSED_VALUE			("unused-value",			"Warns when the value of an expression is not used"),
	UNUSED					("unused",					"Warns when a variable is declared, but not used"),
	UNINITIALIZED			("uninitialized",			"Warns when a variable might not have been initialized yet"),
	VARARGS					("varargs",					"Warns when »§c__builtin_va_start§f« is not used with the last function parameter"),
	EMPTY_DECLARATION		("empty-declaration",		"Warns when a struct member declares an unnamed »§cenum§f«"),
	DIVISION_BY_ZERO		("div-by-zero",				"Warns when trying to divide floating point numbers by zero"),
	
	/**
	 * ===== OPTIMIZATION =====
	 */
	$4(CompilationStage.OPTIMIZATION),
	OPT_NONE,
	

	/**
	 * ===== GENERATION =====
	 */
	$5(CompilationStage.GENERATION),
	GEN_NONE,
	
	
	
	;
	
	private static void group(String name, String description, Warnings...warnings) {
		ConfigRegistry.registerWarning(name, new WarningGroup(name, description, List.of(warnings)));
	}
	
	static {
		CompilationStage previous = null;
		
		List<Warnings> warnings = new ArrayList<>();
		
		for(Warnings warning : values())
			if(warning.stage != null)
				previous = warning.stage;
			else {
				warning.stage = previous;
				
				if(warning.name != null) {
					warnings.add(warning);
					
					ConfigRegistry.registerWarning(warning.name, warning);
				}
			}
		
		group("all", "Enables all warnings", warnings.toArray(Warnings[]::new));
		group("overflow", "Enables all *-overflow warnings", CHAR_HEX_OVERFLOW, CHAR_OCT_OVERFLOW, CHAR_OVERFLOW, INT_OVERFLOW, FLOAT_OVERFLOW);
		group("kandr", "Alias for -Wk&r", K_AND_R);
		
		// default values
		
		EMPTY_DIRECTIVE.setEnabled(false);
	}
	
	public static void init() { }
	
	private CompilationStage stage;
	private final String name;
	private final String description;

	@Setter
	private boolean enabled = true;
	
	private Warnings(CompilationStage stage) {
		this();
		this.stage = stage;
	}
	
	private Warnings() {
		this(null, null);
	}
	
	@Override
	public String getDescription() {
		return description + (enabled ? " §8(§aenabled§8)" : " §8(§9disabled§8)");
	}
	
	public static record WarningGroup(String name, String description, List<Warnings> warnings) implements Toggleable {
		
		@Override
		public String getName() {
			return name;
		}
		
		@Override
		public String getDescription() {
			return description;
		}
		
		@Override
		public boolean isEnabled() {
			return false;
		}
		
		@Override
		public void setEnabled(boolean state) {
			warnings.forEach(w -> w.setEnabled(state));
		}
		
	}
	
}
