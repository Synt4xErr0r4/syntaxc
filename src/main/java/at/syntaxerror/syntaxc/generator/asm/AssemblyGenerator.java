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

import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.Operand;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public interface AssemblyGenerator {

	AssemblyTarget allocate(int id, Type type);
	AssemblyTarget free(int id);
	
	AssemblyTarget target(Operand operand);
	
	void begin();
	void end();
	
	void metadata(SymbolObject object);
	
	void prologue(FunctionMetadata metadata);
	void epilogue(FunctionMetadata metadata);
	
	void nulString(String value);
	void rawString(String value);
	void pointerOffset(String label, BigInteger offset);	
	void constant(BigInteger value, int size);
	void zero(int size);
	
	void add			(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void subtract		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void multiply		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void divide			(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void modulo			(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void bitwiseAnd		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void bitwiseOr		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void bitwiseXor		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void shiftLeft		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void shiftRight		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void logicalAnd		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void logicalOr		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void equal			(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void notEqual		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void greater		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void greaterEqual	(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void less			(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	void lessEqual		(AssemblyTarget result, AssemblyTarget left, AssemblyTarget right);
	
	void assign			(AssemblyTarget result, AssemblyTarget value);
	void bitwiseNot		(AssemblyTarget result, AssemblyTarget value);
	void minus			(AssemblyTarget result, AssemblyTarget value);
	void addressOf		(AssemblyTarget result, AssemblyTarget value);
	void indirection	(AssemblyTarget result, AssemblyTarget value);

	void call			(AssemblyTarget result, AssemblyTarget target, List<AssemblyTarget> args);
	
	void cast			(AssemblyTarget result, AssemblyTarget value, boolean resultFloat, boolean valueFloat);
	
	void member			(AssemblyTarget result, AssemblyTarget value, int offset, int bitOffset, int bitWidth);
	
	void arrayIndex		(AssemblyTarget result, AssemblyTarget target, AssemblyTarget index);
	
	void jump			(AssemblyTarget condition, String name);
	void jump			(String name);
	
	void label			(String name);
	
}
