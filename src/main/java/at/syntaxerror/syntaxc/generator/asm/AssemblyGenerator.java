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
package at.syntaxerror.syntaxc.generator.asm;

import java.math.BigInteger;
import java.util.List;

import at.syntaxerror.syntaxc.generator.alloc.Allocation;
import at.syntaxerror.syntaxc.generator.alloc.RegisterAllocator;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.generator.asm.target.MemoryTarget;
import at.syntaxerror.syntaxc.generator.asm.target.StackTarget;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.ConstantOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.DiscardOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.GlobalOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.IndexOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.LocalOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.Operand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.ReturnValueOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.TemporaryOperand;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public abstract class AssemblyGenerator {

	protected RegisterAllocator allocator;
	
	public void enter(RegisterAllocator allocator, FunctionMetadata metadata) {
		this.allocator = allocator;
		prologue(metadata);
	}
	
	public void leave(FunctionMetadata metadata) {
		epilogue(metadata);
		allocator = null;
	}
	
	@SuppressWarnings("preview")
	public AssemblyTarget target(Operand operand) {
		switch(operand) {
		
		case DiscardOperand discard:
			return null;
			
		case ReturnValueOperand retval:
			return getReturnValueTarget(retval.getType());
			
		case IndexOperand index: {
			AssemblyTarget target = target(index.getTarget());
			
			if(target instanceof StackTarget stack)
				return new StackTarget(
					index.getType(),
					stack.getOffset() + index.getIndex()
				);
			
			return new MemoryTarget(
				index.getType(),
				target,
				index.getIndex()
			);
		}
			
		case GlobalOperand global:
			return null;
			
		case LocalOperand local:
			return null;
		
		case TemporaryOperand temporary: {
			Allocation allocation = allocator.getAllocation(temporary.getId());
			
			if(allocation.isStack())
				return new StackTarget(temporary.getType(), allocation.stackOffset());
			
			return allocation.register();
		}
		
		case ConstantOperand constant:
			return null;
		
		default:
			Logger.error(
				"Unknown operand type: %s",
				operand == null
					? "null"
					: operand.getClass().getName()
			);
			return null;
		}
	}
	
	public abstract AssemblyTarget getReturnValueTarget(Type type);
	
	public abstract void begin();
	public abstract void end();
	
	public abstract void metadata(SymbolObject object);
	
	public abstract void prologue(FunctionMetadata metadata);
	public abstract void epilogue(FunctionMetadata metadata);
	
	public abstract void nulString(String value);
	public abstract void rawString(String value);
	public abstract void pointerOffset(String label, BigInteger offset);	
	public abstract void constant(BigInteger value, int size);
	public abstract void zero(int size);
	
	public abstract void memcpy			(AssemblyTarget dst, AssemblyTarget src, int dstOffset, int srcOffset, int length);
	public abstract void memset			(AssemblyTarget dst, int offset, int length, int value);
	
	public abstract void add			(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void subtract		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void multiply		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void divide			(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void modulo			(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void bitwiseAnd		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void bitwiseOr		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void bitwiseXor		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void shiftLeft		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void shiftRight		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void logicalAnd		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void logicalOr		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void equal			(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void notEqual		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void greater		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void greaterEqual	(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void less			(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	public abstract void lessEqual		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	
	public abstract void assign			(AssemblyTarget result, AssemblyTarget value);
	public abstract void bitwiseNot		(AssemblyTarget result, AssemblyTarget value);
	public abstract void minus			(AssemblyTarget result, AssemblyTarget value);
	public abstract void addressOf		(AssemblyTarget result, AssemblyTarget value);
	public abstract void indirection	(AssemblyTarget result, AssemblyTarget value);

	public abstract void call			(AssemblyTarget result, AssemblyTarget target, List<AssemblyTarget> args);
	
	public abstract void cast			(AssemblyTarget result, AssemblyTarget value, boolean resultFloat, boolean valueFloat);
	
	public abstract void member			(AssemblyTarget result, AssemblyTarget value, int offset, int bitOffset, int bitWidth);
	
	public abstract void arrayIndex		(AssemblyTarget result, AssemblyTarget target, AssemblyTarget index);
	
	public abstract void jump			(AssemblyTarget condition, String name);
	public abstract void jump			(String name);
	
	public abstract void label			(String name);
	
}
