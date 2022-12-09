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

import at.syntaxerror.syntaxc.generator.arch.x86.insn.X86Size;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.type.Type;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
public class VirtualRegisterTarget implements AssemblyTarget {

	private final Type type;
	private final long id;
	
	private final X86Size size;
	
	private final List<? extends RegisterTarget> registerHints;

	private VirtualRegisterTarget(Type type, long id, X86Size size, List<? extends RegisterTarget> registerHints) {
		this.type = type;
		this.id = id;
		this.size = size;
		this.registerHints = registerHints;
	}
	
	private VirtualRegisterTarget(Type type, long id, List<? extends RegisterTarget> registerHints) {
		this(type, id, X86Size.of(type), List.of());
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

	public VirtualRegisterTarget resized(X86Size size) {
		return new VirtualRegisterTarget(type, id, size, registerHints);
	}
	
	@Override
	public boolean isRegister() {
		return true;
	}
	
	@Override
	public String toString() {
		return "VReg(" + id + ":" + size.getPointerName() + ")";
	}
	
}
