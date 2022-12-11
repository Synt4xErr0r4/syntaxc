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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.IEEE754Utils;
import at.syntaxerror.syntaxc.misc.config.Warnings;
import at.syntaxerror.syntaxc.optimizer.ExpressionOptimizer;
import at.syntaxerror.syntaxc.parser.ConstantExpressionEvaluator;
import at.syntaxerror.syntaxc.parser.ExpressionParser;
import at.syntaxerror.syntaxc.parser.helper.ExpressionHelper;
import at.syntaxerror.syntaxc.parser.helper.PointerHelper;
import at.syntaxerror.syntaxc.parser.node.declaration.Initializer;
import at.syntaxerror.syntaxc.parser.node.expression.ArrayIndexExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.MemsetExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.parser.node.statement.ExpressionStatementNode;
import at.syntaxerror.syntaxc.serial.proc.PaddingProcessor;
import at.syntaxerror.syntaxc.symtab.global.GlobalVariableInitializer;
import at.syntaxerror.syntaxc.symtab.global.IntegerInitializer;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class InitializerSerializer {
	
	protected static PaddingProcessor<ExpressionNode> padding(VariableExpressionNode target) {
		return (pos, offset, length) -> MemsetExpressionNode.memzero(
			pos,
			target.getVariable(),
			offset,
			length
		);
	}

	public static GlobalVariableInitializer serialize(Type type, Initializer initializer) {
		return serialize(type, initializer, Integer.MAX_VALUE);
	}
	
	protected static GlobalVariableInitializer serialize(Type type, Initializer initializer, int integerBitWidth) {
		if(type.isArray())
			return ArraySerializer.serialize(type, initializer);
		
		if(type.isIncomplete())
			Logger.error(initializer, Warnings.SEM_NONE, "Cannot initialize incomplete type");
		
		if(type.isStructLike())
			return StructSerializer.serialize(type, initializer);
		
		if(type.isEnum())
			type = type.toEnum().asNumberType();

		while(initializer.isList()) {
			Logger.warn(initializer, Warnings.SCALAR_BRACES, "Braces around scalar initializer");
			initializer = initializer.getList().get(0);
		}
		
		if(type.isPointer())
			return ConstantExpressionEvaluator.evalAddress(initializer.getExpression());
		
		if(type.isInteger())
			return serializeInteger(type, initializer, integerBitWidth);
		
		if(type.isFloating())
			return serializeFloating(type, initializer);
		
		Logger.error(initializer, Warnings.SEM_NONE, "Illegal type for initializer");
		return null;
	}

	private static GlobalVariableInitializer serializeInteger(Type type, Initializer initializer, int integerBitWidth) {
		NumericValueType numericType = type.toNumber().getNumericType();
		
		BigInteger bigint = ConstantExpressionEvaluator.evalInteger(initializer.getExpression());
		
		if(!numericType.inRange(bigint) || bigint.bitCount() > integerBitWidth)
			Logger.warn(initializer, Warnings.INITIALIZER_OVERFLOW, "Integer is too large for initialization");
		
		return new IntegerInitializer(
			numericType.mask(bigint),
			numericType.getSize()
		);
	}
	
	private static GlobalVariableInitializer serializeFloating(Type type, Initializer initializer) {
		NumericValueType numericType = type.toNumber().getNumericType();
		
		Number result = ConstantExpressionEvaluator.evalArithmetic(initializer.getExpression());
		
		return new IntegerInitializer(
			IEEE754Utils.decimalToFloat(
				result instanceof BigInteger bigint
					? new BigDecimal(bigint)
					: (BigDecimal) result,
				numericType.getFloatingSpec()
			),
			numericType.getSize()
		);
	}
	
	protected static List<ExpressionNode> toRawAssignment(ExpressionParser parser, VariableExpressionNode target,
			int targetOffset, Type type, Initializer initializer) {
		Position pos = target.getPosition();
		
		List<ExpressionNode> assignments = new ArrayList<>();
		
		
		if(type.isArray())
			assignments.addAll(ArraySerializer.toAssignment(parser, target, targetOffset, type, initializer));
		
		else if(type.isIncomplete())
			Logger.error(initializer, Warnings.SEM_NONE, "Cannot initialize incomplete type");
		
		else if(type.isStructLike()) {
			if(initializer.isSimple())
				assignments.add(
					parser.getChecker()
						.checkAssignment(
							pos,
							target,
							initializer.getExpression(),
							false
						)
				);
			
			else assignments.addAll(StructSerializer.toAssignment(parser, target, targetOffset, type, initializer));
		}
		
		else {
			while(initializer.isList()) {
				Logger.warn(initializer, Warnings.SCALAR_BRACES, "Braces around scalar initializer");
				initializer = initializer.getList().get(0);
			}
			
			ExpressionNode dest;
			
			if(target.getType().isArray())
				dest = new ArrayIndexExpressionNode( // target[offset / sizeof *target]
					pos,
					target,
					ExpressionHelper.newNumber( // offset / sizeof *target
						pos,
						BigInteger.valueOf(targetOffset / target.getType().dereference().sizeof()),
						Type.ULONG
					),
					false,
					false,
					null
				);
				
			else if(target.getType().isStructLike())
				dest = new ArrayIndexExpressionNode( // ((unsigned char *) &target)[offset]
					pos,
					ExpressionHelper.newCast( // (unsigned char *) &target
						pos,
						PointerHelper.addressOf( // &target
							pos,
							target
						),
						Type.UCHAR.addressOf()
					),
					ExpressionHelper.newNumber(
						pos,
						BigInteger.valueOf(targetOffset),
						Type.ULONG
					),
					false,
					false,
					null
				);
			
			else dest = target;
			
			assignments.add(
				parser.getChecker().checkAssignment(
					pos,
					dest,
					initializer.getExpression(),
					false
				)
			);
		}
		
		return assignments;
	}

	public static List<ExpressionStatementNode> toAssignment(ExpressionParser parser, VariableExpressionNode target,
			Type type, Initializer initializer) {

		return toRawAssignment(parser, target, 0, type, initializer)
			.stream()
			.map(ExpressionOptimizer::optimize)
			.map(ExpressionStatementNode::new)
			.toList();
	}
	
}
