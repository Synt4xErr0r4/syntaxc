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
package at.syntaxerror.syntaxc.generator;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.SyntaxCException;
import at.syntaxerror.syntaxc.generator.asm.AssemblyInstruction;
import at.syntaxerror.syntaxc.misc.StringUtils;
import at.syntaxerror.syntaxc.parser.node.FunctionNode;
import at.syntaxerror.syntaxc.parser.node.GlobalVariableNode;
import at.syntaxerror.syntaxc.parser.node.Node;
import at.syntaxerror.syntaxc.parser.node.statement.StatementNode;
import at.syntaxerror.syntaxc.symtab.Linkage;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.symtab.SymbolObject.SymbolVariableData;
import at.syntaxerror.syntaxc.symtab.global.AddressInitializer;
import at.syntaxerror.syntaxc.symtab.global.GlobalVariableInitializer;
import at.syntaxerror.syntaxc.symtab.global.IntegerInitializer;
import at.syntaxerror.syntaxc.symtab.global.ListInitializer;
import at.syntaxerror.syntaxc.symtab.global.StringInitializer;
import at.syntaxerror.syntaxc.symtab.global.ZeroInitializer;
import at.syntaxerror.syntaxc.type.ArrayType;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings("preview")
public abstract class CodeGenerator {

	private final List<AssemblyInstruction> instructions = new ArrayList<>();

	public final List<AssemblyInstruction> generate(List<Node> nodes) {
		instructions.clear();
		
		add(generateBegin());
		
		nodes.forEach(node -> {
			
			if(node instanceof GlobalVariableNode glob)
				generateObj(glob.getObject());
			
			else if(node instanceof FunctionNode fun) {
				fun.getDeclarations().forEach(this::generateObj);
				
				generateObj(fun.getObject());
				
				add(asmFunctionPrologue());
				
				generateAll(fun.getBody().getStatements());

				add(asmFunctionEpilogue());
			}
			
		});
		
		add(generateEnd());
		
		return instructions;
	}
	
	private final void add(AssemblyInstruction...instructions) {
		add(List.of(instructions));
	}
	
	private final void add(List<AssemblyInstruction> instructions) {
		this.instructions.addAll(instructions);
	}
	
	private final void generateObj(SymbolObject globalVariable) {
		
		String name = globalVariable.getName();

		Type type = globalVariable.getType();
		
		GlobalVariableInitializer init = null;
		Linkage linkage;
		
		if(globalVariable.isFunction())
			linkage = globalVariable.getFunctionData().linkage();
		
		else {
			SymbolVariableData data = globalVariable.getVariableData();
			
			linkage = data.linkage();
			init = data.initializer();
		}
		
		add(asmVariable(
			name,
			type.sizeof(),
			linkage == Linkage.EXTERNAL,
			globalVariable.isFunction(),
			init != null,
			type.isConst() || type instanceof ArrayType
		));
		
		generateInit(init);
	}
	
	private final int generateInit(GlobalVariableInitializer init) {
		int size;
		
		switch(init) {
		case StringInitializer strInit:
			
			add(
				StringUtils.toASCII(strInit.value(), strInit.wide())
					.stream()
					.map(
						strInit.withNul()
							? this::asmNulString
							: this::asmString
					)
					.toList()
			);
			
			size = strInit.value()
				.getBytes(StandardCharsets.UTF_8)
				.length;
			
			if(strInit.withNul())
				size += (strInit.wide() ? NumericValueType.WCHAR : NumericValueType.CHAR).getSize();
			
			return size;
		
		case AddressInitializer addrInit:
			
			add(asmPointer(
				addrInit.object().getName(),
				addrInit.offset()
			));
			
			return NumericValueType.POINTER.getSize() / 8;
		
		case IntegerInitializer intInit:
			
			size = intInit.size();
			
			add(asmConstant(
				intInit.value(),
				size
			));
			
			return size;
			
		case ListInitializer listInit:
		
			size = 0;
			
			for(var entry : listInit.initializers()) {
				
				int diff = entry.offset() - size;
				
				if(diff != 0) {
					if(diff < 0)
						throw new SyntaxCException("Illegal offset for list initializer entry resides inside previous sibling's data");
					
					add(asmZero(diff));
				}
				
				size += generateInit(entry.initializer());
			}
			
			return size;
			
		case ZeroInitializer zeroInit:
			
			size = zeroInit.size();
			
			add(asmZero(size));
			
			return size;
			
		case null:
		default:
			return 0;
		}
	}

	private final void generateAll(List<StatementNode> statements) {
		statements.forEach(this::generateStmt);
	}

	private final void generateStmt(StatementNode statement) {
		
	}
	
	public List<AssemblyInstruction> generateBegin() {
		return List.of();
	}

	public List<AssemblyInstruction> generateEnd() {
		return List.of();
	}
	
	public abstract List<AssemblyInstruction> asmVariable(String name, int size,
			boolean external, boolean function, boolean initialized, boolean readOnly);
	
	public abstract AssemblyInstruction asmLabel(String label);
	
	public abstract AssemblyInstruction asmZero(int n);
	public abstract AssemblyInstruction asmNulString(String string);
	public abstract AssemblyInstruction asmString(String string);
	public abstract AssemblyInstruction asmPointer(String label, BigInteger offset);
	
	public abstract List<AssemblyInstruction> asmConstant(BigInteger value, int size);
	
	public abstract List<AssemblyInstruction> asmFunctionPrologue();
	public abstract List<AssemblyInstruction> asmFunctionEpilogue();
	
}
