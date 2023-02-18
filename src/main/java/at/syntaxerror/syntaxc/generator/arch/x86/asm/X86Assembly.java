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
package at.syntaxerror.syntaxc.generator.arch.x86.asm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.SystemUtils.OperatingSystem;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.generator.arch.x86.call.X86CallingConvention;
import at.syntaxerror.syntaxc.generator.arch.x86.call.X86Cdecl;
import at.syntaxerror.syntaxc.generator.arch.x86.call.X86Microsoftx64Call;
import at.syntaxerror.syntaxc.generator.arch.x86.call.X86SystemVCall;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86Register;
import at.syntaxerror.syntaxc.generator.arch.x86.register.X86RegisterProvider;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.misc.config.Flags;
import at.syntaxerror.syntaxc.type.FunctionType;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
public class X86Assembly {
	
	public static X86Assembly INSTANCE;
	
	public final List<X86Register> registers;
	
	public final X86Register RBP;
	public final X86Register RSP;
	public final X86Register RIP;

	public final X86Register RAX;
	public final X86Register RBX;
	public final X86Register RCX;
	public final X86Register RDX;

	public final X86Register RSI;
	public final X86Register RDI;
	
	public final boolean intelSyntax;
	public final boolean bit32;
	
	public final int threshold;
	
	@Getter
	private final X86RegisterProvider registerProvider;
	
	@SuppressWarnings("unchecked")
	public X86Assembly(boolean intelSyntax, BitSize bits) {
		INSTANCE = this;
		
		this.intelSyntax = intelSyntax;
		
		if(!Flags.LONG_DOUBLE.isEnabled())
			X86Register.disable(X86Register.GROUP_ST);
		
		if(bits != BitSize.B64) {
			bit32 = true;
			threshold = 16;
			
			X86Register.disable(
				X86Register.RAX,
				X86Register.RBX,
				X86Register.RCX,
				X86Register.RDX,
				X86Register.RSI,
				X86Register.RDI,
				X86Register.RBP,
				X86Register.RSP,
				X86Register.RIP,
				X86Register.SIL,
				X86Register.DIL
			);
			
			X86Register.disable(
				X86Register.GROUP_R8,
				X86Register.GROUP_R9,
				X86Register.GROUP_R10,
				X86Register.GROUP_R11,
				X86Register.GROUP_R12,
				X86Register.GROUP_R13,
				X86Register.GROUP_R14,
				X86Register.GROUP_R15,
				X86Register.GROUP_XMM
			);

			RBP = X86Register.EBP;
			RSP = X86Register.ESP;
			RIP = X86Register.EIP;

			RAX = X86Register.EAX;
			RBX = X86Register.EBX;
			RCX = X86Register.ECX;
			RDX = X86Register.EDX;

			RSI = X86Register.ESI;
			RDI = X86Register.EDI;
			
		}
		else {
			bit32 = false;
			threshold = 32;

			RBP = X86Register.RBP;
			RSP = X86Register.RSP;
			RIP = X86Register.RIP;

			RAX = X86Register.RAX;
			RBX = X86Register.RBX;
			RCX = X86Register.RCX;
			RDX = X86Register.RDX;

			RSI = X86Register.RSI;
			RDI = X86Register.RDI;
			
		}
		
		registers = Stream.of(
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
			X86Register.GROUP_A,
			X86Register.GROUP_XMM,
			X86Register.GROUP_ST
		).collect(
			ArrayList::new,
			ArrayList::addAll,
			ArrayList::addAll
		);
		
		registerProvider = new X86RegisterProvider();
	}
	
	public X86CallingConvention<?> getCallingConvention(FunctionType type, Instructions asm, X86AssemblyGenerator generator) {
		
		if(bit32) // cdecl (32-bit)
			return new X86Cdecl(type, asm, generator);
		
		// Microsoft x64 calling convention (64-bit)
		if(ArchitectureRegistry.getOperatingSystem() == OperatingSystem.WINDOWS)
			return new X86Microsoftx64Call(type, asm, generator);
		
		// System V AMD64 ABI (64-bit)
		return new X86SystemVCall(type, asm, generator);
	}
	
}
