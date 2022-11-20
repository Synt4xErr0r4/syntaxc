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
package at.syntaxerror.syntaxc.parser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.Flag;
import at.syntaxerror.syntaxc.misc.IEEE754Utils;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.optimizer.ExpressionOptimizer;
import at.syntaxerror.syntaxc.parser.node.declaration.Initializer;
import at.syntaxerror.syntaxc.parser.node.expression.ArrayIndexExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.MemcpyExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.MemsetExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.NumberLiteralExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.parser.node.statement.ExpressionStatementNode;
import at.syntaxerror.syntaxc.symtab.global.GlobalVariableInitializer;
import at.syntaxerror.syntaxc.symtab.global.IntegerInitializer;
import at.syntaxerror.syntaxc.symtab.global.ListInitializer;
import at.syntaxerror.syntaxc.symtab.global.StringInitializer;
import at.syntaxerror.syntaxc.symtab.global.ZeroInitializer;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.ArrayType;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.StructType;
import at.syntaxerror.syntaxc.type.StructType.Member;
import at.syntaxerror.syntaxc.type.Type;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class InitializerSerializer {

	public static GlobalVariableInitializer serialize(Type type, Initializer initializer) {
		return serialize(type, initializer, Integer.MAX_VALUE);
	}
	
	private static GlobalVariableInitializer serialize(Type type, Initializer initializer, int integerBitWidth) {
		if(type.isArray())
			return serializeArray(type, initializer);
		
		if(type.isIncomplete())
			Logger.error(initializer, Warning.SEM_NONE, "Cannot initialize incomplete type");
		
		if(type.isStructLike())
			return serializeStruct(type, initializer);
		
		if(type.isEnum())
			type = type.toEnum().asNumberType();

		while(initializer.isList()) {
			Logger.warn(initializer, Warning.SCALAR_BRACES, "Braces around scalar initializer");
			initializer = initializer.getList().get(0);
		}
		
		if(type.isPointer())
			return ConstantExpressionEvaluator.evalAddress(initializer.getExpression());
		
		if(type.isInteger())
			return serializeInteger(type, initializer, integerBitWidth);
		
		if(type.isFloating())
			return serializeFloating(type, initializer);
		
		Logger.error(initializer, Warning.SEM_NONE, "Illegal type for initializer");
		return null;
	}

	private static GlobalVariableInitializer serializeString(Initializer initializer, ArrayType array, Type base, int len) {
		return processString(
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

	private static GlobalVariableInitializer serializeArray(Type type, Initializer initializer) {
		return processArray(
			type,
			initializer,
			new ListInitializer(new ArrayList<>()),
			InitializerSerializer::serializeString,
			(idx, offset, init, base) -> List.of(serialize(base, init)),
			(pos, offset, length) -> new ZeroInitializer(length),
			(list, entry) -> list.initializers().add(entry)
		);
	}

	private static GlobalVariableInitializer serializeStruct(Type type, Initializer initializer) {
		return processStruct(
			type,
			initializer,
			new ListInitializer(new ArrayList<>()),
			Type.ULONG.sizeof(),
			(offset, init, memType) -> List.of(serialize(memType, init)),
			(pos, value, offset, length) -> new IntegerInitializer(value, length),
			(pos, offset, length) -> new ZeroInitializer(length),
			(list, entry) -> list.initializers().add(entry)
		);
	}

	private static GlobalVariableInitializer serializeInteger(Type type, Initializer initializer, int integerBitWidth) {
		NumericValueType numericType = type.toNumber().getNumericType();
		
		BigInteger bigint = ConstantExpressionEvaluator.evalInteger(initializer.getExpression());
		
		if(!numericType.inRange(bigint) || bigint.bitCount() > integerBitWidth)
			Logger.warn(initializer, Warning.INITIALIZER_OVERFLOW, "Integer is too large for initialization");
		
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

	private static List<ExpressionNode> toStringAssignment(ExpressionParser parser, VariableExpressionNode target,
			int targetOffset, Initializer initializer, ArrayType array, Type base, int len) {
		
		return processString(
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
	
	private static PaddingProcessor<ExpressionNode> padding(VariableExpressionNode target) {
		return (pos, offset, length) -> MemsetExpressionNode.memzero(
			pos,
			target.getVariable(),
			offset,
			length
		);
	}
	
	private static List<ExpressionNode> toArrayAssignment(ExpressionParser parser, VariableExpressionNode target,
			int targetOffset, Type type, Initializer initializer) {
		
		return processArray(
			type,
			initializer,
			new ArrayList<>(),
			(init, array, base, len) -> toStringAssignment(
				parser,
				target,
				targetOffset,
				initializer,
				array,
				base,
				len
			),
			(idx, offset, init, base) -> toRawAssignment(
				parser,
				target,
				targetOffset + offset * idx,
				base,
				init
			),
			padding(target),
			List::add
		);
	}
	
	private static List<ExpressionNode> toStructAssignment(ExpressionParser parser, VariableExpressionNode target,
			int targetOffset, Type type, Initializer initializer) {
		
		return processStruct(
			type,
			initializer,
			new ArrayList<>(),
			Type.ULONG.sizeof(),
			(offset, init, memType) -> toRawAssignment(
				parser,
				target,
				targetOffset + offset,
				memType,
				init
			),
			(pos, offset, value, length) -> new NumberLiteralExpressionNode(
				pos,
				value,
				Type.ULONG
			),
			padding(target),
			List::add
		);
	}
	
	private static List<ExpressionNode> toRawAssignment(ExpressionParser parser, VariableExpressionNode target,
			int targetOffset, Type type, Initializer initializer) {
		Position pos = target.getPosition();
		
		List<ExpressionNode> assignments = new ArrayList<>();
		
		if(type.isArray())
			assignments.addAll(toArrayAssignment(parser, target, targetOffset, type, initializer));
		
		else if(type.isIncomplete())
			Logger.error(initializer, Warning.SEM_NONE, "Cannot initialize incomplete type");
		
		else if(type.isStructLike())
			assignments.addAll(toStructAssignment(parser, target, targetOffset, type, initializer));
		
		else {
			while(initializer.isList()) {
				Logger.warn(initializer, Warning.SCALAR_BRACES, "Braces around scalar initializer");
				initializer = initializer.getList().get(0);
			}
			
			ExpressionNode dest;
			
			if(target.getType().isArray())
				dest = new ArrayIndexExpressionNode( // target[offset / sizeof *target]
					pos,
					target,
					ExpressionParser.newNumber( // offset / sizeof *target
						pos,
						BigInteger.valueOf(targetOffset / target.getType().dereference().sizeof()),
						Type.ULONG
					),
					false
				);
				
			else if(target.getType().isStructLike())
				dest = new ArrayIndexExpressionNode( // ((unsigned char *) &target)[offset]
					pos,
					ExpressionParser.newCast( // (unsigned char *) &target
						pos,
						PointerHelper.addressOf( // &target
							pos,
							target
						),
						Type.UCHAR.addressOf()
					),
					ExpressionParser.newNumber(
						pos,
						BigInteger.valueOf(targetOffset),
						Type.ULONG
					),
					false
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
	
	private static <R> R processString(Initializer initializer, ArrayType array, Type base, int len,
			StringPaddedProcessor<R> padded, StringProcessor<R> string) {

		Position pos = initializer.getPosition();
		
		ExpressionNode expr = ExpressionOptimizer.optimize(initializer.getExpression());
		
		if(!(expr instanceof VariableExpressionNode var) || !var.getVariable().isString())
			Logger.error(pos, Warning.SEM_NONE, "Expected string literal for initializer");
		
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
					Logger.warn(initializer, Warning.STRING_INITIALIZER, "Initializer string for array is too long");
	
				truncated = true;
			}
		}
		
		return string.process(pos, init, var, len, truncated);
	}
	
	private static <R, S extends R, A extends R, T> R processArray(Type type, Initializer initializer, A result,
			ArrayStringProcessor<S> string, ArrayElementProcessor<T> element,
			PaddingProcessor<T> padding, Accumulator<A, T> accumulator) {
		
		ArrayType array = type.toArray();
		Type base = array.getBase();

		int len = array.getLength();
		
		List<Initializer> inits;
		
		if(initializer.isSimple()) {
			if(initializer.getExpression().getType().isString())
				return string.process(initializer, array, base, len);
			
			Logger.warn(initializer, Warning.MISSING_BRACES, "Missing braces around initializer");
			
			inits = List.of(initializer);
		}
		else inits = initializer.getList();
		
		Position pos = initializer.getPosition();
		
		if(len == ArrayType.SIZE_UNKNOWN)
			array.setLength(len = inits.size());
		
		else if(len < inits.size())
			Logger.warn(initializer, Warning.INITIALIZER_OVERFLOW, "Too many initializers for array");

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
	
	private static <R, T> R processStruct(Type type, Initializer initializer, R result,
			int maxIntWidth, StructElementProcessor<T> element, StructIntegerProcessor<T> integer,
			PaddingProcessor<T> padding, Accumulator<R, T> accumulator) {
		
		Position pos = initializer.getPosition();
		
		StructType struct = type.toStructLike();
		
		List<Member> members = new ArrayList<>(struct.getMembers());
		
		if(!struct.getMembers().isEmpty() && struct.isUnion())
			members = members.subList(0, 1);

		List<Initializer> inits;
		
		if(initializer.isSimple()) {
			Logger.warn(initializer, Warning.MISSING_BRACES, "Missing braces around initializer");
			
			inits = List.of(initializer);
		}
		else inits = initializer.getList();

		int memlen = members.size();
		int initlen = inits.size();
		
		if(initlen > memlen)
			Logger.warn(
				initializer,
				Warning.INITIALIZER_OVERFLOW,
				"Too many initializers for %s",
				struct.isUnion()
					? "union"
					: "struct"
			);

		BigInteger bits = BigInteger.ZERO;
		int bitWidth = 0;
		int bitOffset = -1;
		int offset = 0;
		
		for(int i = 0; i < initlen; ++i) {
			Member member = members.get(i);
			
			type = member.getType();
			offset = member.getOffset();
			
			Initializer init = inits.get(i);

			int pad = member.getPadding();
			
			if(pad != 0)
				accumulator.accumulate(
					result,
					padding.process(pos, offset, pad)
				);
			
			if(!type.isBitfield())
				accumulator.accumulateAll(
					result,
					element.process(offset, init, type)
				);
			
			else if(type.isBitfield()) {
				var bitValue = ((IntegerInitializer) serialize(type, init, member.getBitWidth())).value();

				if(Flag.PACKED.isEnabled()) {
					if(bitOffset == -1)
						bitOffset = offset;

					bits = bits
						.or( // merge values
							bitValue
								.and( // create bitmask and apply to value
									BigInteger.ONE
										.shiftLeft(member.getBitWidth())
										.subtract(BigInteger.ONE)
								)
								.shiftLeft( // shift the value to its desired place
									member.getBitOffset()
									 + 8 * (offset - bitOffset)
								)
						);
					
					bitWidth += member.getBitWidth();
					
					// compact bitfields by skipping/postponing accumulation
					if(i != initlen - 1 && members.get(i + 1).isBitfield())
						continue;
				}
				else accumulator.accumulate(
					result,
					integer.process(
						pos,
						bitValue,
						offset,
						/* convert number of bits into number of bytes */
						Math.ceilDiv(member.getBitWidth(), 8)
					)
				);
			}
			
			if(bitOffset != -1) {
				/* convert number of bits into number of bytes
				 * 
				 * e.g.:
				 * - 5 bits = 1 byte (3 bits padding)
				 * - 9 bits = 2 bytes (7 bits padding)
				 * - 8 bits = 1 byte (no padding)
				 */

				bitWidth = Math.ceilDiv(bitWidth, 8);
				
				if(maxIntWidth == -1)
					accumulator.accumulate(
						result,
						integer.process(
							pos,
							bits,
							bitOffset,
							bitWidth
						)
					);
				
				else {
					// break up into chunks of 'maxIntWidth' bytes
					
					int chunks = bitWidth / maxIntWidth;
					int last = bitWidth % maxIntWidth;
					
					BigInteger mask = BigInteger.ONE
						.shiftLeft(maxIntWidth << 3)
						.subtract(BigInteger.ONE);
					
					for(int j = 0; j < chunks; ++j) {
						accumulator.accumulate(
							result,
							integer.process(
								pos,
								bits.and(mask),
								bitOffset + maxIntWidth * j,
								maxIntWidth
							)
						);
						
						bits = bits.shiftLeft(maxIntWidth << 3);
					}
					
					if(last != 0)
						accumulator.accumulate(
							result,
							integer.process(
								pos,
								bits.and(
									BigInteger.ONE
										.shiftLeft(last << 3)
										.subtract(BigInteger.ONE)
								),
								bitOffset + maxIntWidth * chunks,
								last
							)
						);
				}
				
				// reset state
				bits = BigInteger.ZERO;
				bitOffset = -1;
				bitWidth = 0;
			}
		}
		
		int padStart = 0;
		
		if(initlen != 0) {
			Member last = members.get(initlen - 1);
			
			padStart = last.getOffset() + last.sizeof();
		}
		
		int pad = struct.sizeof() - padStart;
		
		if(pad > 0)
			accumulator.accumulate(
				result,
				padding.process(pos, padStart, pad)
			);
		
		return result;
	}
	
	private static interface Accumulator<R, T> {
		
		void accumulate(R result, T element);
		
		default void accumulateAll(R result, List<T> element) {
			element.forEach(e -> accumulate(result, e));
		}
		
	}
	
	private static interface PaddingProcessor<T> {
		
		T process(Position pos, int offset, int length);
		
	}
	
	private static interface StringPaddedProcessor<R> {
		
		R process(Position pos, StringInitializer initializer, VariableExpressionNode var, int offset, int length);
		
	}
	
	private static interface StringProcessor<R> {
		
		R process(Position pos, StringInitializer initializer, VariableExpressionNode var, int length, boolean truncated);
		
	}
	
	private static interface ArrayStringProcessor<R> {
		
		R process(Initializer initializer, ArrayType array, Type base, int length);
		
	}
	
	private static interface ArrayElementProcessor<T> {
		
		List<T> process(int index, int offset, Initializer initializer, Type type);
		
	}
	
	private static interface StructElementProcessor<T> {
		
		List<T> process(int offset, Initializer initializer, Type type);
		
	}
	
	private static interface StructIntegerProcessor<T> {
		
		T process(Position pos, BigInteger value, int offset, int length);
		
	}
	
}
