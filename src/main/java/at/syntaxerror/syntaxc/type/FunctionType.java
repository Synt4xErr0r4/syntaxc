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

import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public class FunctionType extends Type {

	public static final FunctionType IMPLICIT = new FunctionType(INT.addressOf().addressOf().addressOf().addressOf().addressOf()) {
		
		@Override
		public void addParameter(Token name, Type type) { }
		
		@Override
		public void setPrototype() { }
		
		@Override
		public void setVariadic() { }
		
	};
	
	private Type returnType;
	
	private List<Parameter> parameters;
	private boolean variadic;  // functions with ellipsis in parameter list, e.g. void my_func(int i, ...) 
	private boolean prototype; // false for functions without parameter list, e.g. void my_func()
	
	public FunctionType(Type returnType) {
		super(TypeKind.FUNCTION);
		this.returnType = returnType;
		
		parameters = new ArrayList<>();
	}
	
	public List<Parameter> getParameters() {
		return Collections.unmodifiableList(parameters);
	}
	
	public void addParameter(Token name, Type type) {
		parameters.add(new Parameter(name, type));
	}
	
	public void setVariadic() {
		variadic = true;
	}
	
	public void setPrototype() {
		prototype = true;
	}
	
	@Override
	public String toStringPrefix() {
		return returnType.toString() + " (";
	}
	
	@Override
	protected String toStringSuffix() {
		if(!prototype) return ")()";

		return ")(" + String.join(
			", ",
			parameters.stream()
				.map(param -> param.type() + " " + param.name().getString())
				.toList()
		) + (variadic ? ", ..." : "") + ")";
	}
	
	public static record Parameter(Token name, Type type) implements Positioned {
		
		@Override
		public Position getPosition() {
			return name.getPosition();
		}
		
		public String getName() {
			return name.getString();
		}
		
	}

}
