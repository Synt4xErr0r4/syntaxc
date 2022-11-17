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

import java.math.BigInteger;

import at.syntaxerror.syntaxc.symtab.global.GlobalVariableInitializer;
import at.syntaxerror.syntaxc.symtab.global.StringInitializer;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.FunctionType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString(of = { "name", "type", "kind" })
public class SymbolObject implements Symbol, Positioned {

	public static final int OFFSET_NONE = Integer.MIN_VALUE;
	
	public static final String RETURN_VALUE_NAME = ".RV";
	
	private static long temporaryId = 0;
	private static long localStaticId = 0;

	/*
	 * Helper method for possibly uninitialized symbols
	 */
	private static SymbolObject uninitialized(Positioned pos, Type type, String name, SymbolKind kind) {
		return new SymbolObject(
			pos.getPosition(),
			type,
			name,
			kind,
			null
		);
	}
	
	/**
	 * Helper method for possibly initialized symbols.
	 * Sets the {@link #isInitialized()} flag accordingly.
	 */
	private static SymbolObject initialized(Positioned pos, Type type, String name, SymbolKind kind, SymbolData data) {
		SymbolObject obj = new SymbolObject(
			pos.getPosition(),
			type,
			name,
			kind,
			data
		);
		
		if(data != null) {
			@SuppressWarnings("preview")
			boolean init = switch(data) {
			case SymbolFunctionData fun -> !fun.isImplicit() && kind != SymbolKind.PROTOTYPE;
			case SymbolVariableData var -> var.initializer() != null;
			case SymbolEnumeratorData enm -> true;
			default -> false;
			};
			
			obj.setInitialized(init);
		}
		
		return obj;
	}
	
	/**
	 * Returns the (synthetic) return variable for the function.
	 * Its internal name will always be equal to {@link #RETURN_VALUE_NAME}
	 */
	public static SymbolObject returns(Positioned pos, Type type) {
		return uninitialized(
			pos,
			type,
			RETURN_VALUE_NAME,
			SymbolKind.VARIABLE_RETURN
		);
	}
	
	/*
	 * Creates an implicit function declaration. When a function is invoked with a
	 * (yet) existing definition or prototype of it, it its implicitly declared as:
	 * 
	 *   int function_name();
	 *   
	 * It accepts any number of arguments and returns an integer. However, since this
	 * might not actually be the function's signature, any use of implicit functions
	 * will produce a diagnostic warning (but can be disabled via the command line
	 * option '-Wno-implicit-function')
	 */
	public static SymbolObject implicit(Positioned pos, String name) {
		return initialized(
			pos.getPosition(),
			FunctionType.IMPLICIT,
			name,
			SymbolKind.FUNCTION,
			new SymbolFunctionData(
				Linkage.INTERNAL,
				true,
				null
			)
		);
	}
	
	/*
	 * Creates a temporary variable used by some expressions or statements
	 */
	public static SymbolObject temporary(Positioned pos, Type type) {
		return uninitialized(
			pos,
			type,
			Long.toString(temporaryId++),
			SymbolKind.VARIABLE_TEMPORARY
		);
	}
	
	/*
	 * Creates a local variable, e.g.:
	 * 
	 *   {
	 *       int a;
	 *       ...
	 *   }
	 * 
	 * 'a' is now accessible from within this scope (e.g. a function).
	 * Function parameters are also treated as local variables
	 */
	public static SymbolObject local(Positioned pos, String name, Type type) {
		return uninitialized(
			pos,
			type,
			name,
			SymbolKind.VARIABLE_LOCAL
		);
	}
	
	/*
	 * Creates a string, e.g.:
	 * 
	 *   const char *name1 = "John Doe";
	 *   const char *name2 = "John Doe";
	 * 
	 * Both 'name1' and 'name2' now point to the SAME string "John Doe" at the SAME address.
	 * The string is immutable, trying to change its contents would likely result in a
	 * memory access error.
	 */
	public static SymbolObject string(Positioned pos, StringInitializer initializer) {
		return initialized(
			pos,
			Type.getStringType( // const char[], const wchar_t[]
				initializer.value(),
				initializer.wide()
			).asConst(),
			".STR" + initializer.id(),
			SymbolKind.STRING,
			new SymbolVariableData(
				Linkage.INTERNAL,
				initializer
			)
		);
	}
	
	/*
	 * Creates a function, e.g.:
	 * 
	 *   int main() { ... }
	 *   static void do_sth() { ... }
	 * 
	 * 'main' and 'do_sth' can now be called from within any function in the current file.
	 * While 'main' is also accessible from other files, 'do_sth' is not (due to the 'static' keyword).
	 */
	public static SymbolObject function(Positioned pos, String name, Type type, Linkage linkage, String returnLabel) {
		return initialized(
			pos.getPosition(),
			type,
			name,
			SymbolKind.FUNCTION,
			new SymbolFunctionData(
				linkage,
				false,
				returnLabel
			)
		);
	}
	
	/*
	 * Creates a function prototype, e.g.:
	 * 
	 *   int printf(const char *, ...);
	 * 
	 * 'printf' can now be used without being defined (yet).
	 */
	public static SymbolObject prototype(Positioned pos, String name, Type type, Linkage linkage) {
		return initialized(
			pos,
			type,
			name,
			SymbolKind.PROTOTYPE,
			new SymbolFunctionData(
				linkage,
				false,
				null
			)
		);
	}
	
	/*
	 * Creates a global variable, e.g.:
	 * 
	 *   float PI = 3.1415926f;
	 *   static int LOOKUP[256] = { ... };
	 *   
	 * 'PI' and 'LOOKUP' can now be used from within any function in the current file.
	 * While 'PI' is also accessible from other files, 'LOOKUP' is not (due to the 'static' keyword).
	 * 
	 * These rules only apply when the variables are declared outside of a function body.
	 */
	public static SymbolObject global(Positioned pos, String name, Type type,
			Linkage linkage, GlobalVariableInitializer initializer) {
		return initialized(
			pos,
			type,
			name,
			SymbolKind.VARIABLE_GLOBAL,
			new SymbolVariableData(
				linkage,
				initializer
			)
		);
	}
	
	/*
	 * Creates an external declaration, e.g.:
	 * 
	 *   extern int puts(const char *);
	 *   
	 * 'puts' can now be used without its definition within this file.
	 */
	public static SymbolObject extern(Positioned pos, String name, Type type) {
		SymbolObject obj = global(pos, name, type, Linkage.EXTERNAL, null);
		
		obj.extern = true;
		
		return obj;
	}
	
	/*
	 * Creates a type alias, e.g.:
	 * 
	 *   typedef unsigned int uint32_t;
	 *   
	 * 'uint32_t' is now an alias for 'unsigned int'.
	 */
	public static SymbolObject typedef(Positioned pos, String name, Type type) {
		return uninitialized(
			pos,
			type,
			name,
			SymbolKind.TYPEDEF
		);
	}
	
	/*
	 * Creates an enumerator for an enum, e.g.:
	 * 
	 *   enum { A, B = 5, C }
	 *   
	 * 'A', 'B', and 'C' are enumerators here, with the values 1, 5, and 6, respectively.
	 */
	public static SymbolObject enumerator(Positioned pos, String name, Type type, BigInteger value) {
		return initialized(
			pos,
			type,
			name,
			SymbolKind.ENUMERATOR,
			new SymbolEnumeratorData(value)
		);
	}
	
	private final Position position;
	private final Type type;
	private final String name;
	
	private final SymbolKind kind;

	private final SymbolData data;
	
	// the ID appended to the object's name (if > -1). Used for static local variables
	private long fullNameId = -1;

	// whether the object was declared with the 'extern' keyword
	private boolean extern = false;
	
	// whether this object has been referenced
	private @Setter boolean unused = true;
	
	// whether this object has been initialized
	private @Setter boolean initialized = false;
	
	// the stack offset of a local variable
	private @Setter int offset = OFFSET_NONE;

	// whether the syntax tree generator should ignore this object
	private @Setter boolean syntaxTreeIgnore = false;
	
	/**
	 * Marks this local variable as 'static'
	 */
	public void setLocalStatic() {
		fullNameId = localStaticId++;
	}
	
	/**
	 * @return whether this object is a static local variable
	 */
	public boolean isLocalStatic() {
		return fullNameId > -1;
	}
	
	public String getFullName() {
		return name + (isLocalStatic() ? "." + fullNameId : "");
	}
	
	public String getDebugName() {
		return getFullName() + (isTemporaryVariable() ? ".temp" : "");
	}
	
	public SymbolFunctionData getFunctionData() {
		return (SymbolFunctionData) data;
	}
	
	public SymbolVariableData getVariableData() {
		return (SymbolVariableData) data;
	}
	
	public SymbolEnumeratorData getEnumeratorData() {
		return (SymbolEnumeratorData) data;
	}
	
	public Linkage getLinkage() {
		return data.linkage();
	}
	
	public boolean isUserDefined() {
		return kind == SymbolKind.VARIABLE_LOCAL
			|| kind == SymbolKind.VARIABLE_GLOBAL
			|| kind == SymbolKind.FUNCTION;
	}
	
	public boolean isVariable() {
		return kind == SymbolKind.VARIABLE_LOCAL
			|| kind == SymbolKind.VARIABLE_GLOBAL
			|| kind == SymbolKind.VARIABLE_TEMPORARY
			|| kind == SymbolKind.STRING
			|| kind == SymbolKind.FUNCTION;
	}
	
	public boolean isReturnValue() {
		return kind == SymbolKind.VARIABLE_RETURN;
	}

	public boolean isLocalVariable() {
		return kind == SymbolKind.VARIABLE_LOCAL;
	}

	public boolean isTemporaryVariable() {
		return kind == SymbolKind.VARIABLE_TEMPORARY;
	}
	
	public boolean isGlobalVariable() {
		return kind == SymbolKind.VARIABLE_GLOBAL
			|| kind == SymbolKind.STRING;
	}
	
	public boolean isString() {
		return kind == SymbolKind.STRING;
	}
	
	public boolean isFunction() {
		return kind == SymbolKind.FUNCTION
			|| kind == SymbolKind.PROTOTYPE;
	}
	
	public boolean isPrototype() {
		return kind == SymbolKind.PROTOTYPE;
	}
	
	public boolean isTypedef() {
		return kind == SymbolKind.TYPEDEF;
	}
	
	public boolean isEnumerator() {
		return kind == SymbolKind.ENUMERATOR;
	}
	
	public static interface SymbolData {
		
		default Linkage linkage() {
			return Linkage.INTERNAL;
		}
		
	}

	// additional data for functions
	public static record SymbolFunctionData(Linkage linkage, boolean isImplicit, String returnLabel) implements SymbolData {
		
	}

	// additional data for global variables
	public static record SymbolVariableData(Linkage linkage, GlobalVariableInitializer initializer) implements SymbolData {
		
	}
	
	// additional data for enumerators
	public static record SymbolEnumeratorData(BigInteger value) implements SymbolData {
		
	}
	
}
