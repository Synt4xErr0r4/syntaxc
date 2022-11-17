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
package at.syntaxerror.syntaxc.intermediate.representation;

import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.generator.asm.AssemblyGenerator;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.Type;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Subclasses of this class represent the intermediate representation
 * of a machine code instruction
 * 
 * @author Thomas Kasper
 * 
 */
public abstract class Intermediate implements Positioned {

	public abstract void generate(AssemblyGenerator assemblyGenerator);
	
	/**
	 * Converts this intermediate representation into C-like code
	 * 
	 * @return the C-like code
	 */
	@Override
	public final String toString() {
		String strval = toStringInternal();
		
		// remove discard operator from string representation
		if(strval.startsWith("<discard> = "))
			return strval.substring(12);
		
		return strval;
	}
	
	protected abstract String toStringInternal();
	
	/**
	 * not actually a intermediate representation, but signales to the code generator that
	 * the memory used by a temporary operand is no longer in use and can be reallocated again  
	 * 
	 * @author Thomas Kasper
	 */
	@RequiredArgsConstructor
	@Getter
	public static class FreeIntermediate extends Intermediate {
		
		private final TemporaryOperand operand;
		private boolean discarded;
		
		@Override
		public Position getPosition() {
			return null;
		}
		
		@Override
		public void generate(AssemblyGenerator assemblyGenerator) {
			assemblyGenerator.free(operand.getId());
		}
		
		@Override
		public String toStringInternal() {
			return discarded
				? "/* */"
				: "/*synthetic: free _%d*/".formatted(operand.id);
		}
		
	}
	
	/**
	 * operand for the intermediate representation of a machine code instruction
	 * 
	 * @author Thomas Kasper
	 */
	public static interface Operand {

		Type getType();
		
		default int getSize() {
			return getType().sizeof();
		}
		
		default boolean isFloating() {
			return getType().isFloating();
		}
		
		void unfree();
		
		default List<Intermediate> free() {
			return List.of();
		}
		
	}
	
	/**
	 * operand telling the code generator that the result of an operation should be discarded  
	 * 
	 * @author Thomas Kasper
	 */
	@Getter
	public static class DiscardOperand implements Operand {
		
		@Override
		public Type getType() {
			return Type.VOID;
		}
		
		@Override
		public void unfree() { }
		
		@Override
		public String toString() {
			return "<discard>";
		}
		
	}
	
	/**
	 * operand representing an index into an array
	 * 
	 * @author Thomas Kasper
	 */
	@RequiredArgsConstructor
	@Getter
	public static class IndexOperand implements Operand {
		
		private final Operand target;
		private final Operand index;
		private final Type type;
		
		@Override
		public List<Intermediate> free() {
			List<Intermediate> freed = new ArrayList<>();
			
			freed.addAll(target.free());
			freed.addAll(index.free());
			
			return freed;
		}
		
		@Override
		public void unfree() {
			target.unfree();
			index.unfree();
		}
		
		@Override
		public String toString() {
			return "%s[%s]".formatted(target, index);
		}
		
	}
	
	/**
	 * operand representing dereferencing of a pointer
	 * 
	 * @author Thomas Kasper
	 */
	@RequiredArgsConstructor
	@Getter
	public static class IndirectionOperand implements Operand {
		
		private final Operand target;
		private final Type type;
		
		@Override
		public List<Intermediate> free() {
			return target.free();
		}
		
		@Override
		public void unfree() {
			target.unfree();
		}
		
		@Override
		public String toString() {
			return "*%s".formatted(target);
		}
		
	}
	
	/**
	 * global variable or function operand, typically resides in memory relative
	 * to the instruction pointer or is resolved via the Procedure Linkage Table (PLT)
	 * 
	 * @author Thomas Kasper
	 */
	@RequiredArgsConstructor
	@Getter
	public static class GlobalOperand implements Operand {
		
		private final String name;
		private final boolean extern;
		private final Type type;
		
		@Override
		public void unfree() { }
		
		@Override
		public String toString() {
			return name;
		}
		
	}

	/**
	 * local variable operand, typically resides in memory relative to the base pointer
	 * 
	 * @author Thomas Kasper
	 */
	@RequiredArgsConstructor
	@Getter
	public static class LocalOperand implements Operand {
		
		private final String name;
		private final int offset;
		private final Type type;
		
		@Override
		public void unfree() { }
		
		@Override
		public String toString() {
			return name;
		}
		
	}

	/**
	 * temporarily allocated memory, typically using a register
	 * 
	 * @author Thomas Kasper
	 */
	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
	public static class TemporaryOperand implements Operand {
		
		private static int PREVIOUS_ID = 0;

		private final int id;
		private final Type type;
		private boolean freed;
		private FreeIntermediate freeIntermediate;
		
		public TemporaryOperand(Type type) {
			this(++PREVIOUS_ID, type);
		}
		
		@Override
		public List<Intermediate> free() {
			if(freed)
				return List.of();
			
			freed = true;
			
			return List.of(
				freeIntermediate = new FreeIntermediate(this)
			);
		}
		
		@Override
		public void unfree() {
			if(freed) {
				freeIntermediate.discarded = true;
				freed = false;
			}
		}
		
		@Override
		public String toString() {
			return "_" + id;
		}
		
	}

	/**
	 * a constant number literal operand
	 * 
	 * @author Thomas Kasper
	 */
	@RequiredArgsConstructor
	@Getter
	public static class ConstantOperand implements Operand {
		
		private final Number value;
		private final Type type;
		
		@Override
		public void unfree() { }
		
		@Override
		public String toString() {
			return value.toString();
		}
		
	}

	/**
	 * operand designating the return value
	 * 
	 * @author Thomas Kasper
	 */
	@Getter
	public static class ReturnValueOperand extends TemporaryOperand {
		
		public static final int ID = -1;
		
		public ReturnValueOperand(Type type) {
			super(ID, type);
			super.freed = true;
		}
		
		@Override
		public List<Intermediate> free() {
			return List.of();
		}
		
		@Override
		public void unfree() { }
		
		@Override
		public String toString() {
			return SymbolObject.RETURN_VALUE_NAME;
		}
		
	}
	
}
