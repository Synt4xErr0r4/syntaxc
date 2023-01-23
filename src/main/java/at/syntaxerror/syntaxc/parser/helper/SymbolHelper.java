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
package at.syntaxerror.syntaxc.parser.helper;

import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.config.Warnings;
import at.syntaxerror.syntaxc.parser.Parser;
import at.syntaxerror.syntaxc.parser.node.declaration.Initializer;
import at.syntaxerror.syntaxc.serial.InitializerSerializer;
import at.syntaxerror.syntaxc.symtab.Linkage;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.SymbolTable;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.Type;
import at.syntaxerror.syntaxc.type.TypeUtils;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
public class SymbolHelper implements Logable {

	@Delegate(types = Logable.class)
	private final Parser parser;
	
	private final List<SymbolObject> parameters = new ArrayList<>();
	
	public SymbolTable getSymbolTable() {
		return parser.getSymbolTable();
	}
	
	public void warnUseless(Type type) {
		if(!type.isEnum() && (!type.isStructLike() || type.toStructLike().isAnonymous()))
			warn(Warnings.USELESS, "Useless declaration does not declare anything");
	}
	
	public void resetParameters() {
		parameters.clear();
	}
	
	public List<SymbolObject> getParameters() {
		return new ArrayList<>(parameters);
	}
	
	public SymbolObject registerParameter(Positioned pos, Type type, String name) {
		SymbolObject obj = SymbolObject.local(pos, name, type);

		SymbolTable symtab = getSymbolTable();
		
		if(!symtab.addObject(obj))
			softError(obj, "Redeclaration of local variable »%s«", name);

		obj.setUnused(false);
		obj.setInitialized(true);
		obj.setParameter(true);
		
		return obj;
	}
	
	public SymbolObject registerVariable(Positioned pos, Type type, String name, Initializer initializer, DeclarationState state) {
		boolean hasInit = initializer != null;
		
		if(hasInit && !state.acceptsInitializer())
			softError(
				"»%s« does not accept an initializer",
				state.typedef()
					? "typedef"
					: "extern"
			);
		
		SymbolTable symtab = getSymbolTable();
		
		// the name has already been declared within this scope before
		if(symtab.hasObjectInScope(name)) {
			SymbolObject obj = symtab.findObjectInScope(name);
			
			/* e.g.:
			 * 
			 *   long *some_name;
			 *   typedef int some_name;
			 * 
			 * first, some_name is declared as a pointer, then as a typedef name
			 */
			if(obj.isTypedef() != state.typedef()) {
				softError(pos, "Redefinition of »%s« as another kind", name);
				note(obj, "Previously declared here");
			}
			
			/**
			 * e.g.:
			 * 
			 *   long *some_name;
			 *   struct s some_name;
			 * 
			 * first, some_name is declared as a pointer, then as a structure
			 */
			else if(!TypeUtils.isEqual(type, obj.getType())) {
				softError(pos, "Incompatible types for »%s«", name);
				note(obj, "Previously declared here");
			}
			
			/**
			 * e.g.:
			 * 
			 *   void fun() { ... }
			 *   void fun();
			 * 
			 * (valid) prototypes after the actual function implementation are discarded
			 */
			else if(obj.isPrototype()) // discard subsequent prototypes
				return null;
			
			/**
			 * e.g.:
			 * 
			 *   int some_name = 1;
			 *   int some_name = 2;
			 * 
			 * here, some_name is defined twice (although declaring it twice would be fine)
			 */
			else if(obj.getVariableData() != null && obj.getVariableData().initializer() != null && hasInit) {
				softError(pos, "Redefinition of »%s«", name);
				note(obj, "Previously declared here");
			}
			
			/**
			 * e.g.:
			 * 
			 *   int some_name;
			 *   int some_name;
			 * 
			 * here, the second declaration of some_name is discarded, since both declarations are equivalent
			 */
			else if(state.linkage() == Linkage.EXTERNAL)
				return null;
			
			/**
			 * e.g.:
			 * 
			 *   static int some_name;
			 *   int some_name;
			 * 
			 * first, some_name is declared as a static variable (which is not visible from without the current file),
			 * and then declared as an external variable (which is visible)
			 */
			else if(state.internal() && obj.getLinkage() == Linkage.EXTERNAL) {
				softError(pos, "Cannot redeclare »%s« as »static« after previous non-static declaration", name);
				note(obj, "Previously declared here");
			}
			
			// remove the old association with the name
			symtab.removeObject(obj);
		}
		
		SymbolObject obj;
		
		// e.g. typedef unsigned int uint32_t;
		if(state.typedef())
			obj = SymbolObject.typedef(pos, name, type);
		
		// e.g. int printf(const char *, ...);
		else if(type.isFunction())
			obj = SymbolObject.prototype(pos, name, type, state.linkage());
		
		// e.g. extern int errno;
		else if(state.external())
			obj = SymbolObject.extern(pos, name, type);
		
		else if(state.local()) {
			obj = SymbolObject.local(
				pos,
				name,
				type
			);
			
			if(hasInit)
				obj.setInitialized(true);
		}
		
		// e.g. static int array[256];
		else {
			obj = SymbolObject.global(
				pos,
				name,
				type,
				state.linkage(),
				hasInit
					? InitializerSerializer.serialize(type, initializer)
					: null
			);
			
			if(hasInit)
				obj.setInitialized(true);
		}
		
		symtab.addObject(obj);
		
		return obj;
	}
	
	public void registerFunction() {
		
	}

	public static record DeclarationState(Linkage linkage, boolean typedef, boolean external, boolean internal, boolean local) {
		
		public boolean acceptsInitializer() {
			return !typedef && !external;
		}
		
	}
	
}
