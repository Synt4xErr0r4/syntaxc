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

import java.nio.charset.StandardCharsets;
import java.util.List;

import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.config.Warnings;
import at.syntaxerror.syntaxc.optimizer.ExpressionOptimizer;
import at.syntaxerror.syntaxc.parser.ExpressionParser;
import at.syntaxerror.syntaxc.parser.node.declaration.Initializer;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.MemcpyExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.MemsetExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.serial.proc.StringPaddedProcessor;
import at.syntaxerror.syntaxc.serial.proc.StringProcessor;
import at.syntaxerror.syntaxc.symtab.global.GlobalVariableInitializer;
import at.syntaxerror.syntaxc.symtab.global.ListInitializer;
import at.syntaxerror.syntaxc.symtab.global.StringInitializer;
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
public class StringSerializer {

	protected static GlobalVariableInitializer serialize(Initializer initializer, ArrayType array, Type base, int len) {
		return process(
			initializer,
			array,
			base,
			len,
			(pos, init, var, offset, length) -> new ListInitializer(List.of(
				init,
				new ZeroInitializer(length)
			)),
			(pos, init, var, length, truncated) ->
				truncated
					? new StringInitializer(
						init.id(),
						init.value()
							.substring(0, len),
						init.wide(),
						false
					)
					: init
		);
	}

	protected static List<ExpressionNode> toAssignment(ExpressionParser parser, VariableExpressionNode target,
			int targetOffset, Initializer initializer, ArrayType array, Type base, int len) {
		
		return process(
			initializer,
			array,
			base,
			len,
			(pos, init, var, offset, length) -> List.of(
				new MemcpyExpressionNode( // copy 'offset' bytes from (var + 0) into (target + targetOffset)
					pos,
					var.getVariable(),
					target.getVariable(),
					0,
					targetOffset,
					offset
				),
				MemsetExpressionNode.memzero( // memzero remaining bytes
					pos,
					target.getVariable(),
					offset + targetOffset,
					length
				)
			),
			(pos, init, var, length, truncated) -> List.of(
				new MemcpyExpressionNode( // copy 'length' bytes from (var + 0) into (target + targetOffset)
					pos,
					var.getVariable(),
					target.getVariable(),
					0,
					targetOffset,
					length
				)
			)
		);
	}
	
	private static <R> R process(Initializer initializer, ArrayType array, Type base, int len,
			StringPaddedProcessor<R> padded, StringProcessor<R> string) {

		Position pos = initializer.getPosition();
		
		ExpressionNode expr = ExpressionOptimizer.optimize(initializer.getExpression());
		
		if(!(expr instanceof VariableExpressionNode var) || !var.getVariable().isString())
			Logger.error(pos, Warnings.SEM_NONE, "Expected string literal for initializer");
		
		VariableExpressionNode var = ((VariableExpressionNode) expr);
		
		StringInitializer init = (StringInitializer) var.getVariable()
			.getVariableData().initializer();
		
		int strlen = init.value().getBytes(StandardCharsets.UTF_8).length + 1;
		boolean truncated = false;
		
		if(len == ArrayType.SIZE_UNKNOWN)
			array.setLength(len = strlen);
		
		else {
			int width = base.sizeof();
			
			if(len > strlen)
				return padded.process(
					pos,
					init,
					var,
					strlen * width,
					(len - strlen) * width
				);
			
			if(len < strlen) {
				if(len + 1 != strlen)
					Logger.warn(initializer, Warnings.STRING_INITIALIZER, "Initializer string for array is too long");
	
				truncated = true;
			}
		}
		
		return string.process(pos, init, var, len, truncated);
	}
	
}
