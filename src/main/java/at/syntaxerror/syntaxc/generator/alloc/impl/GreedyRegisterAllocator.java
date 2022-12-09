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
package at.syntaxerror.syntaxc.generator.alloc.impl;

import at.syntaxerror.syntaxc.generator.alloc.RegisterAllocator;

/**
 * The default register allocation implementation. This is functionally based on LLVM's RegAllocGreedy, as described
 * in <a href="https://youtu.be/IK8TMJf3G6U">Matthias Braun's talk at the 2018 LLVM Developer's meeting</a>
 * 
 * @author Thomas Kasper
 * 
 */
public class GreedyRegisterAllocator extends RegisterAllocator {

	@Override
	public void allocate() {
		
	}
	
	/**
	 * try to assign a physical register to the virtual register
	 */
	private boolean assign() {
		
		return false;
	}

	/**
	 * try to revert previous allocations
	 */
	private boolean evict() {
		
		return false;
	}

	/**
	 * try live range splitting
	 */
	private boolean split() {
		
		return false;
	}

	/**
	 * try to spill and reload
	 */
	private boolean spill() {
		
		return false;
	}
	
	/**
	 * fail if register could not be allocated
	 */
	private void error() {
		
	}
	
}
