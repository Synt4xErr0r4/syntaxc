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
import lombok.ToString;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString(exclude = "position")
public class SymbolObject implements Symbol, Positioned {

	private static long temporaryId = 0;
	private static long stringId = 0;
	
	public static SymbolObject returns(Positioned pos, Type type) {
		return new SymbolObject(
			pos.getPosition(),
			".RV",
			SymbolKind.VARIABLE_RETURN,
			type,
			null
		);
	}
	
	public static SymbolObject implicit(Positioned pos, String name) {
		return new SymbolObject(
			pos.getPosition(),
			name,
			SymbolKind.FUNCTION,
			FunctionType.IMPLICIT,
			new SymbolFunctionData(Linkage.INTERNAL, true)
		);
	}
	
	public static SymbolObject temporary(Positioned pos, Type type) {
		return new SymbolObject(
			pos.getPosition(),
			Long.toString(temporaryId++),
			SymbolKind.VARIABLE_TEMPORARY,
			type,
			null
		);
	}
	
	public static SymbolObject local(Positioned pos, String name, Type type) {
		return new SymbolObject(
			pos.getPosition(),
			name,
			SymbolKind.VARIABLE_LOCAL,
			type,
			null
		);
	}
	
	public static SymbolObject string(Positioned pos, StringInitializer initializer) {
		return new SymbolObject(
			pos.getPosition(),
			Long.toString(stringId++),
			SymbolKind.STRING,
			Type.getStringType(initializer.value(), initializer.wide()),
			new SymbolVariableData(
				Linkage.INTERNAL,
				initializer
			)
		);
	}
	
	public static SymbolObject function(Positioned pos, String name, Type type, Linkage linkage) {
		return new SymbolObject(
			pos.getPosition(),
			name,
			SymbolKind.FUNCTION,
			type,
			new SymbolFunctionData(
				linkage,
				false
			)
		);
	}
	
	public static SymbolObject prototype(Positioned pos, String name, Type type, Linkage linkage) {
		return new SymbolObject(
			pos.getPosition(),
			name,
			SymbolKind.PROTOTYPE,
			type,
			new SymbolFunctionData(
				linkage,
				false
			)
		);
	}
	
	public static SymbolObject global(Positioned pos, String name, Type type, Linkage linkage, GlobalVariableInitializer initializer) {
		return new SymbolObject(
			pos.getPosition(),
			name,
			SymbolKind.VARIABLE_GLOBAL,
			type,
			new SymbolVariableData(
				linkage,
				initializer
			)
		);
	}
	
	public static SymbolObject extern(Positioned pos, String name, Type type) {
		return global(pos, name, type, Linkage.EXTERNAL, null);
	}
	
	public static SymbolObject typedef(Positioned pos, String name, Type type) {
		return new SymbolObject(
			pos.getPosition(),
			name,
			SymbolKind.TYPEDEF,
			type,
			null
		);
	}
	
	public static SymbolObject enumerator(Positioned pos, String name, Type type, BigInteger value) {
		return new SymbolObject(
			pos.getPosition(),
			name,
			SymbolKind.ENUMERATOR,
			type,
			new SymbolEnumeratorData(value)
		);
	}
	
	private final Position position;
	private final String name;
	private final SymbolKind kind;
	private final Type type;

	private final SymbolData data;
	
	private boolean unused = true;
	
	public void setUnused(boolean unused) {
		this.unused = unused;
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
	
	public boolean isVariable() {
		return kind == SymbolKind.VARIABLE_LOCAL
			|| kind == SymbolKind.VARIABLE_GLOBAL
			|| kind == SymbolKind.VARIABLE_TEMPORARY
			|| kind == SymbolKind.STRING
			|| kind == SymbolKind.FUNCTION;
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
		
	}
	
	// additional data for functions
	public static record SymbolFunctionData(Linkage linkage, boolean isImplicit) implements SymbolData {
		
	}

	// additional data for global variables
	public static record SymbolVariableData(Linkage linkage, GlobalVariableInitializer initializer) implements SymbolData {
		
	}
	
	// additional data for enumerators
	public static record SymbolEnumeratorData(BigInteger value) implements SymbolData {
		
	}
	
}
