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
package at.syntaxerror.syntaxc.generator.arch.x86.register;

import java.util.Arrays;
import java.util.List;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.generator.alloc.impl.RegisterSupplier;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public class X86RegisterProvider {

	public List<RegisterSupplier> getSuppliers() {
		return Arrays.asList(
			RegisterSupplier.builder()
				.typeCheck(X86RegisterProvider::isInteger)
				.register(X86Register.EAX)
				.register(X86Register.EBX)
				.register(X86Register.ECX)
				.register(X86Register.EDX)
				.register(X86Register.ESI)
				.register(X86Register.EDI)
				.register(X86Register.R8)
				.register(X86Register.R9)
				.register(X86Register.R10)
				.register(X86Register.R11)
				.register(X86Register.R12)
				.register(X86Register.R13)
				.register(X86Register.R14)
				.register(X86Register.R15)
				.build(),
				
			RegisterSupplier.builder()
				.typeCheck(X86RegisterProvider::isXMMFloat)
				.register(X86Register.GROUP_XMM)
				.build()
		);
	}
	
	private static boolean isInteger(Type type) {
		return type.isInteger()
			|| type.isPointerLike();
	}
	
	private static boolean isXMMFloat(Type type) {
		return ArchitectureRegistry.getBitSize() == BitSize.B32
			? false
			: type != Type.LDOUBLE && type.isFloating();
	}
	
}
