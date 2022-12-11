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
package at.syntaxerror.syntaxc.generator.asm.target;

import java.util.List;

import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.type.Type;
import at.syntaxerror.syntaxc.type.TypeUtils;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public class VirtualRegisterTarget implements AssemblyTarget {

	private final Type type;
	private final long id;
	
	private final List<? extends RegisterTarget> registerHints;

	private VirtualRegisterTarget(Type type, long id, List<? extends RegisterTarget> registerHints) {
		this.type = type;
		this.id = id;
		this.registerHints = registerHints;
	}
	
	public VirtualRegisterTarget(Type type, long id) {
		this(type, id, List.of());
	}
	
	public VirtualRegisterTarget(Type type) {
		this(type, SymbolObject.getNextTemporaryId(), List.of());
	}
	
	public VirtualRegisterTarget(Type type, RegisterTarget...registerHints) {
		this(type, SymbolObject.getNextTemporaryId(), List.of(registerHints));
	}
	
	public VirtualRegisterTarget(Type type, List<? extends RegisterTarget> registerHints) {
		this(type, SymbolObject.getNextTemporaryId(), registerHints);
	}
	
	public boolean hasRegisterHint() {
		return !registerHints.isEmpty();
	}

	@Override
	public VirtualRegisterTarget resized(Type type) {
		return new VirtualRegisterTarget(type, id, registerHints);
	}
	
	@Override
	public boolean isRegister() {
		return true;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj != null
			&& obj instanceof VirtualRegisterTarget vrt
			&& id == vrt.id
			&& TypeUtils.isEqual(type, vrt.type);
	}
	
	@Override
	public String toString() {
		return "VReg(" + id + ":" + type + ")";
	}
	
}
