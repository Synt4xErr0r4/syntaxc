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

import java.util.HashSet;
import java.util.Set;

import at.syntaxerror.syntaxc.generator.asm.target.RegisterTarget;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Thomas Kasper
 * 
 */
@Setter
@Getter
public class LiveInterval {

	private long from, to;
	
	private RegisterTarget assignedRegister;
	
	private final Set<Long> interference = new HashSet<>();
	
	public LiveInterval(long from, long to) {
		this.from = from;
		this.to = to;
	}
	
	public boolean isAssigned() {
		return assignedRegister != null;
	}
	
	public boolean isUnassigned() {
		return assignedRegister == null;
	}
	
	public boolean interferesWith(LiveInterval other) {
		if(other instanceof LiveInterval live)
			return (from >= live.from && from <= live.to)
				|| (to >= live.from && to <= live.to);
		
		return other.interferesWith(this);
	}

	@Override
	public String toString() {
		return "[" + from + ";" + to + "]"
			+ (interference.isEmpty()
				? ""
				: ":" + interference)
			+ (assignedRegister == null
				? ""
				: "@" + assignedRegister);
	}
	
}
