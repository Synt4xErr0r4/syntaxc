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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import at.syntaxerror.syntaxc.generator.asm.target.RegisterTarget;
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

	@Getter
	private final List<RegisterTarget> registers;
	
	private final Predicate<Type> typeCheck;
	
	public RegisterSupplier(List<RegisterTarget> registers, Predicate<Type> typeCheck) {
		this.registers = registers;
		this.typeCheck = typeCheck;
	}
	
	public int getRegisterCount() {
		return registers.size();
	}
	
	public boolean isSuitableFor(Type type) {
		return typeCheck.test(type);
	}
	
	public RegisterTarget getRegister(int i) {
		return registers.get(i);
	}
	
	public static class Builder {
		
		private List<RegisterTarget> registers;
		private Predicate<Type> typeCheck;
		
		public Builder register(RegisterTarget register, RegisterTarget...more) {
			if(registers == null)
				registers = new ArrayList<>();
			
			registers.add(register);
			
			for(RegisterTarget reg : more)
				registers.add(reg);
			
			return this;
		}

		public Builder register(List<? extends RegisterTarget> regs) {
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
			if(registers == null)
				return null;
			
			var filtered = registers.stream()
				.filter(r -> r != null && r.isUsable())
				.toList();
			
			if(filtered.isEmpty())
				return null;
			
			return new RegisterSupplier(
				filtered,
				Objects.requireNonNullElseGet(
					typeCheck,
					() -> t -> true
				)
			);
		}
		
	}
	
}
