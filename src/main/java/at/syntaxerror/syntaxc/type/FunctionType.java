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
package at.syntaxerror.syntaxc.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import at.syntaxerror.syntaxc.lexer.Keyword;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import lombok.Getter;

/**
 * This class represents a function type, e.g. {@code void my_func(int, long) }
 * 
 * @author Thomas Kasper
 * 
 */
@Getter
public class FunctionType extends Type {

	/**
	 * Constant for an implicitly defined function, e.g. {@code int my_func()}
	 */
	public static final FunctionType IMPLICIT = new FunctionType(INT) {
		
		@Override
		public void addParameter(Parameter param) { }
		
		@Override
		public void setVariadic() { }
		
		@Override
		public void setKAndR() { }
		
	};
	
	static {
		IMPLICIT.kAndR = true;
	}
	
	private Type returnType;
	
	private List<Parameter> parameters;
	private boolean variadic;  // functions with ellipsis in parameter list, e.g. void my_func(int i, ...)
	private boolean kAndR;	   // functions declared using K&R syntax
	
	/**
	 * Constructs a new function type with type given return type
	 * 
	 * @param returnType the return type
	 */
	public FunctionType(Type returnType) {
		super(TypeKind.FUNCTION);
		this.returnType = returnType;
		
		parameters = new ArrayList<>();
	}
	
	/**
	 * Returns an unmodifiable view of the function parameters
	 * 
	 * @return an unmodifiable view of the function parameters
	 */
	public List<Parameter> getParameters() {
		return Collections.unmodifiableList(parameters);
	}
	
	/**
	 * Adds an parameter to the function
	 * 
	 * @param param the paramter to be added
	 */
	public void addParameter(Parameter param) {
		parameters.add(param);
	}
	
	/**
	 * Marks this function as variadic, e.g. {@code void my_func(int, ...)}
	 */
	public void setVariadic() {
		variadic = true;
	}

	/**
	 * Marks this function as declared using old K&R syntax, e.g.:
	 * <code><pre>
	 * void my_func(a, b, c)
	 * int a;
	 * long b;
	 * char *c;
	 * {
	 *   /&ast; ... &ast;/
	 * }
	 * </pre></code>
	 */
	public void setKAndR() {
		kAndR = true;
	}
	
	@Override
	protected Type clone() {
		FunctionType cloned = new FunctionType(returnType);
		
		cloned.parameters = parameters;
		cloned.variadic = variadic;
		
		return inheritProperties(cloned);
	}
	
	@Override
	public String toStringPrefix() {
		return returnType.toString() + " (";
	}
	
	@Override
	protected String toStringSuffix() {
		return ")("
			+ (!kAndR && parameters.isEmpty() // non-K&R function without parameters: e.g. int func(void)
				? "void"
				: String.join(
					", ",
					parameters.stream()
						.map(Parameter::toParameterString)
						.toList()
				)
			) + (variadic ? ", ..." : "") + ")";
	}
	
	@Override
	public String toString() {
		/* remove '()' in the middle of the string ('(' at the end of the prefix, ')' at the begin of the suffix);
		 * this is where pointers would go if this was a function pointer:
		 * 
		 * float (*)(int a, int b, int c); <-- function pointer
		 * float (int a, int b, int c); <-- function
		 */
		
		String prefix = toStringPrefix();
		String suffix = toStringSuffix();
		
		return prefix.substring(0, prefix.length() - 2)
			+ suffix.substring(1);
	}
	
	/**
	 * This class represents the parameter of {@code FunctionType}
	 * 
	 * @author Thomas Kasper
	 *
	 */
	public static record Parameter(Positioned pos, String name, Type type, Optional<Keyword> storageSpecs) implements Positioned {
		
		@Override
		public Position getPosition() {
			return pos.getPosition();
		}
		
		public String toParameterString() {
			return name == null && type == null
				? "<error_type>"
				: name != null && type != null
					? type + " " + name
					: name != null
						? name
						: type.toString();
		}
		
	}

}
