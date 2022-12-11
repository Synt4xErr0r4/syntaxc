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
package at.syntaxerror.syntaxc.serial;

import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.config.Warnings;
import at.syntaxerror.syntaxc.parser.ExpressionParser;
import at.syntaxerror.syntaxc.parser.node.declaration.Initializer;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.serial.proc.Accumulator;
import at.syntaxerror.syntaxc.serial.proc.ArrayElementProcessor;
import at.syntaxerror.syntaxc.serial.proc.ArrayStringProcessor;
import at.syntaxerror.syntaxc.serial.proc.PaddingProcessor;
import at.syntaxerror.syntaxc.symtab.global.GlobalVariableInitializer;
import at.syntaxerror.syntaxc.symtab.global.ListInitializer;
import at.syntaxerror.syntaxc.symtab.global.ZeroInitializer;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.ArrayType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class ArraySerializer {

	protected static GlobalVariableInitializer serialize(Type type, Initializer initializer) {
		return process(
			type,
			initializer,
			new ListInitializer(new ArrayList<>()),
			StringSerializer::serialize,
			(idx, offset, init, base) -> List.of(InitializerSerializer.serialize(base, init)),
			(pos, offset, length) -> new ZeroInitializer(length),
			(list, entry) -> list.initializers().add(entry)
		);
	}
	
	protected static List<ExpressionNode> toAssignment(ExpressionParser parser, VariableExpressionNode target,
			int targetOffset, Type type, Initializer initializer) {
		
		return process(
			type,
			initializer,
			new ArrayList<>(),
			(init, array, base, len) -> StringSerializer.toAssignment(
				parser,
				target,
				targetOffset,
				initializer,
				array,
				base,
				len
			),
			(idx, offset, init, base) -> InitializerSerializer.toRawAssignment(
				parser,
				target,
				targetOffset + offset * idx,
				base,
				init
			),
			InitializerSerializer.padding(target),
			List::add
		);
	}
	
	private static <R, S extends R, A extends R, T> R process(Type type, Initializer initializer, A result,
			ArrayStringProcessor<S> string, ArrayElementProcessor<T> element,
			PaddingProcessor<T> padding, Accumulator<A, T> accumulator) {
		
		ArrayType array = type.toArray();
		Type base = array.getBase();

		int len = array.getLength();
		
		List<Initializer> inits;
		
		if(initializer.isSimple()) {
			if(initializer.getExpression().getType().isString())
				return string.process(initializer, array, base, len);
			
			Logger.warn(initializer, Warnings.MISSING_BRACES, "Missing braces around initializer");
			
			inits = List.of(initializer);
		}
		else inits = initializer.getList();
		
		Position pos = initializer.getPosition();
		
		if(len == ArrayType.SIZE_UNKNOWN)
			array.setLength(len = inits.size());
		
		else if(len < inits.size())
			Logger.warn(initializer, Warnings.INITIALIZER_OVERFLOW, "Too many initializers for array");

		int nEntries = Math.min(inits.size(), len);
		int offset = base.sizeof();
		
		for(int i = 0; i < nEntries; ++i)
			accumulator.accumulateAll(
				result,
				element.process(
					i,
					offset,
					inits.get(i),
					base
				)
			);
		
		if(nEntries < len)
			accumulator.accumulate(
				result,
				padding.process(
					pos,
					nEntries * offset,
					offset * (len - nEntries)
				)
			);
		
		return result;
	}
	
}
