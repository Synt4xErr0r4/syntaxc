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
package at.syntaxerror.syntaxc.generator.arch.x86;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import at.syntaxerror.syntaxc.generator.alloc.RegisterProvider;
import at.syntaxerror.syntaxc.generator.alloc.RegisterSupplier;
import at.syntaxerror.syntaxc.generator.alloc.RegisterSupplier.Builder;
import at.syntaxerror.syntaxc.type.Type;
import lombok.Setter;

/**
 * @author Thomas Kasper
 * 
 */
public class X86RegisterProvider implements RegisterProvider {

	@Setter
	private X86Assembly x86;

	@Override
	public List<RegisterSupplier> getSuppliers() {
		Builder i8 = RegisterSupplier.builder().size(1).typeCheck(Type::isScalar);
		Builder i16 = RegisterSupplier.builder().size(2).typeCheck(Type::isScalar);
		Builder i32 = RegisterSupplier.builder().size(4).typeCheck(Type::isScalar);
		Builder i64 = RegisterSupplier.builder().size(8).typeCheck(Type::isScalar);
		
		Stream.of(
			X86Register.GROUP_R15,
			X86Register.GROUP_R14,
			X86Register.GROUP_R13,
			X86Register.GROUP_R12,
			X86Register.GROUP_R11,
			X86Register.GROUP_R10,
			X86Register.GROUP_B,
			X86Register.GROUP_R9,
			X86Register.GROUP_R8,
			X86Register.GROUP_C,
			X86Register.GROUP_D,
			X86Register.GROUP_SI,
			X86Register.GROUP_DI,
			X86Register.GROUP_A
		).forEach(
			list -> list.stream()
				.filter(X86Register::isUsable)
				.forEach(
					register -> {
						switch(register.getSize()) {
						case 1: i8 .register(register); break;
						case 2: i16.register(register); break;
						case 4: i32.register(register); break;
						case 8: i64.register(register); break;
						}
					}
				)
		);
		
		return Arrays.asList(
			i8.build(),
			i16.build(),
			i32.build(),
			i64.build(),
			RegisterSupplier.builder()
				.size(4, 8)
				.typeCheck(Type::isFloating)
				.register(X86Register.GROUP_XMM)
				.build(),
			RegisterSupplier.builder()
				.size(Type.LDOUBLE.sizeof())
				.typeCheck(type -> type == Type.LDOUBLE)
				.register(X86Register.GROUP_ST)
				.build()
		);
	}
	
}
