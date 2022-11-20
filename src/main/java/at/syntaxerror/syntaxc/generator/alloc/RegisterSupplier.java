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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.function.Predicate;

import at.syntaxerror.syntaxc.generator.asm.target.AssemblyRegister;
import at.syntaxerror.syntaxc.type.Type;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
public class RegisterSupplier {
	
	public static Builder builder() {
		return new Builder();
	}

	private final PriorityQueue<AssemblyRegister> queue;
	private final List<AssemblyRegister> order;
	
	@Getter
	private final List<Integer> sizes;
	
	private final Predicate<Type> typeCheck;
	
	private RegisterSupplier(List<Integer> sizes, List<AssemblyRegister> registers, Predicate<Type> typeCheck) {
		this.queue = new PriorityQueue<>(this::sort);
		this.sizes = sizes;
		this.order = registers;
		this.typeCheck = typeCheck;
		
		queue.addAll(order);
	}
	
	private int sort(AssemblyRegister a, AssemblyRegister b) {
		return Integer.compare(
			order.indexOf(a),
			order.indexOf(b)
		);
	}
	
	public boolean isSuitableFor(Type type) {
		return typeCheck.test(type);
	}
	
	public boolean hasNext() {
		return !queue.isEmpty();
	}
	
	public AssemblyRegister next() {
		return queue.poll();
	}
	
	public void expire(AssemblyRegister register) {
		queue.offer(register);
	}
	
	public void reset() {
		queue.clear();
		queue.addAll(order);
	}
	
	@Override
	public String toString() {
		return "RegisterSupplier"
			+ order.stream()
				.map(r -> r + "=" + queue.contains(r))
				.toList();
	}
	
	public static class Builder {
		
		private List<Integer> sizes;
		private List<AssemblyRegister> registers;
		private Predicate<Type> typeCheck;
		
		public Builder size(int size, int...more) {
			if(sizes == null)
				sizes = new ArrayList<>();
			
			sizes.add(size);
			
			for(int sz : more)
				sizes.add(sz);
			
			return this;
		}

		public Builder register(AssemblyRegister register, AssemblyRegister...more) {
			if(registers == null)
				registers = new ArrayList<>();
			
			registers.add(register);
			
			for(AssemblyRegister reg : more)
				registers.add(reg);
			
			return this;
		}

		public Builder register(List<? extends AssemblyRegister> regs) {
			if(!regs.isEmpty()) {
				if(registers == null)
					registers = new ArrayList<>();
				
				registers.addAll(regs);
			}
			
			return this;
		}
		
		public Builder typeCheck(Predicate<Type> predicate){
			if(typeCheck != null)
				typeCheck = typeCheck.or(predicate);
			
			else typeCheck = predicate;
			
			return this;
		}
		
		public RegisterSupplier build() {
			Objects.requireNonNull(sizes, "Missing sizes for register supplier");
			
			if(registers == null)
				return null;
			
			var filtered = registers.stream()
				.filter(r -> r != null && r.isUsable())
				.toList();
			
			if(filtered.isEmpty())
				return null;
			
			return new RegisterSupplier(
				Collections.unmodifiableList(sizes),
				filtered,
				Objects.requireNonNullElseGet(
					typeCheck,
					() -> t -> true
				)
			);
		}
		
	}
	
}
