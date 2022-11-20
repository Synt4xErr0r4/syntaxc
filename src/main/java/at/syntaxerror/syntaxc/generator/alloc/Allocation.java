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
package at.syntaxerror.syntaxc.generator.alloc;

import at.syntaxerror.syntaxc.generator.asm.target.AssemblyRegister;

/**
 * @author Thomas Kasper
 * 
 */
public record Allocation(Kind kind, AssemblyRegister register, long stackOffset) {
	
	public static Allocation register(AssemblyRegister register) {
		return new Allocation(Kind.REGISTER, register, -1);
	}
	
	public static Allocation stack(long offset) {
		return new Allocation(Kind.STACK, null, offset);
	}
	
	public boolean isRegister() {
		return kind == Kind.REGISTER;
	}
	
	public boolean isStack() {
		return kind == Kind.STACK;
	}
	
	public static enum Kind {
		REGISTER,
		STACK
	}
	
}
