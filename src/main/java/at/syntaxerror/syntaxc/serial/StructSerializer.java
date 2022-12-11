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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.config.Warnings;
import at.syntaxerror.syntaxc.parser.ExpressionParser;
import at.syntaxerror.syntaxc.parser.node.declaration.Initializer;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.NumberLiteralExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.serial.proc.Accumulator;
import at.syntaxerror.syntaxc.serial.proc.PaddingProcessor;
import at.syntaxerror.syntaxc.serial.proc.StructElementProcessor;
import at.syntaxerror.syntaxc.serial.proc.StructIntegerProcessor;
import at.syntaxerror.syntaxc.symtab.global.GlobalVariableInitializer;
import at.syntaxerror.syntaxc.symtab.global.IntegerInitializer;
import at.syntaxerror.syntaxc.symtab.global.ListInitializer;
import at.syntaxerror.syntaxc.symtab.global.ZeroInitializer;
import at.syntaxerror.syntaxc.tracking.Position;
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
public class StructSerializer {
	
	private static final Map<Integer, Type> LENGTH_TO_TYPE = new HashMap<>();
	private static int maxIntBytes;
	
	private static void initializeTypeTable() {
		if(!LENGTH_TO_TYPE.isEmpty())
			return;
		
		for(NumericValueType type : NumericValueType.values())
			if(!type.isSigned() && !type.isFloating())
				LENGTH_TO_TYPE.put(type.getSize(), type.asType());
		
		maxIntBytes = getLengthsStream().max().getAsInt();
	}

	private static IntStream getLengthsStream() {
		return LENGTH_TO_TYPE.values()
			.stream()
			.mapToInt(Type::sizeof);
	}
	
	protected static GlobalVariableInitializer serialize(Type type, Initializer initializer) {
		return processStruct(
			type,
			initializer,
			new ListInitializer(new ArrayList<>()),
			!ArchitectureRegistry.isUnrestrictedBitfieldSerialization(),
			(offset, init, memType) -> List.of(InitializerSerializer.serialize(memType, init)),
			(pos, value, offset, length) -> new IntegerInitializer(value, length),
			(pos, offset, length) -> new ZeroInitializer(length),
			(list, entry) -> list.initializers().add(entry)
		);
	}
	
	protected static List<ExpressionNode> toAssignment(ExpressionParser parser, VariableExpressionNode target,
			int targetOffset, Type type, Initializer initializer) {
		
		return processStruct(
			type,
			initializer,
			new ArrayList<>(),
			true,
			(offset, init, memType) -> InitializerSerializer.toRawAssignment(
				parser,
				target,
				targetOffset + offset,
				memType,
				init
			),
			(pos, offset, value, length) -> new NumberLiteralExpressionNode(
				pos,
				value,
				LENGTH_TO_TYPE.get(length)
			),
			InitializerSerializer.padding(target),
			List::add
		);
	}
	
	private static <R, T> R processStruct(Type type, Initializer initializer, R result,
			boolean restrictSize, StructElementProcessor<T> element, StructIntegerProcessor<T> integer,
			PaddingProcessor<T> padding, Accumulator<R, T> accumulator) {
		
		Position pos = initializer.getPosition();
		
		StructType struct = type.toStructLike();
		
		List<Member> members = new ArrayList<>(struct.getMembers());
		
		if(!struct.getMembers().isEmpty() && struct.isUnion())
			members = members.subList(0, 1);
		
		List<Initializer> inits;
		
		if(initializer.isSimple()) {
			Logger.warn(initializer, Warnings.MISSING_BRACES, "Missing braces around initializer");
			
			inits = List.of(initializer);
		}
		else inits = initializer.getList();

		int memlen = members.size();
		int initlen = inits.size();
		
		if(initlen > memlen)
			Logger.warn(
				initializer,
				Warnings.INITIALIZER_OVERFLOW,
				"Too many initializers for %s",
				struct.isUnion()
					? "union"
					: "struct"
			);

		BigInteger bits = BigInteger.ZERO;
		
		int bitStart = -1;
		
		boolean bitfield;
		
		int offset = 0;
		
		int initIndex = 0;
		
		for(int i = 0; i < memlen; ++i) {
			Member member = members.get(i);
			
			type = member.getType();
			offset = member.getOffset();
			bitfield = member.isBitfield();
			
			Initializer init = null;
			
			if(member.getName() != null) /* no initializer for anonymous member */
				init = inits.get(initIndex++);
			
			if(!bitfield) /* flush bit fields when encountering non-bitfield member */
				flushBits(pos, bits, bitStart, offset, result, restrictSize, integer, accumulator);
			
			int pad = member.getPadding();
			
			if(pad > 0 && bitStart == -1)
				accumulator.accumulate(
					result,
					padding.process(
						pos,
						offset - pad,
						pad
					)
				);
			
			if(!bitfield) {
				bitStart = -1;
				bits = BigInteger.ZERO;
				
				accumulator.accumulateAll(
					result,
					element.process(offset, init, type)
				);
			}
			
			else {
				/* accumulate bitfield value */
				
				if(bitStart == -1)
					bitStart = offset;
				
				BigInteger bitValue;
				
				if(init == null) /* set anonymous bitfields to zero */
					bitValue = BigInteger.ZERO;
					
				else bitValue = ((IntegerInitializer) InitializerSerializer.serialize(
					type,
					init,
					member.getBitWidth()
				)).value();

				int bitOffset = (offset - bitStart) * 8 + member.getBitOffset();
				
				bits = bits.or(bitValue.shiftLeft(bitOffset));
			}
		}

		/* flush trailing bit fields */
		
		flushBits(pos, bits, bitStart, offset, result, restrictSize, integer, accumulator);
		
		/* append padding to match struct size */
		
		int padStart = 0;
		
		if(memlen != 0) {
			Member last = members.get(memlen - 1);
			
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
	
	private static <R, T> void flushBits(Position pos, BigInteger bits, int start, int offset, R result,
			boolean restrictSize, StructIntegerProcessor<T> integer, Accumulator<R, T> accumulator) {
		
		if(start == -1)
			return;

		initializeTypeTable();

		int byteCount = offset - start;
		
		if(!restrictSize || LENGTH_TO_TYPE.containsKey(byteCount)) {
			accumulator.accumulate(
				result,
				integer.process(
					pos,
					bits,
					start,
					byteCount
				)
			);
			return;
		}
		
		int maxIntBits = maxIntBytes << 3;
		
		int chunks = byteCount / maxIntBytes;
		int last = byteCount % maxIntBytes;
		
		BigInteger mask = bitMask(maxIntBits);
		
		for(int chunk = 0; chunk < chunks; ++chunk) {
			accumulator.accumulate(
				result,
				integer.process(
					pos,
					bits.and(mask),
					start + maxIntBytes * chunk,
					maxIntBytes
				)
			);
			
			bits = bits.shiftRight(maxIntBits);
		}
		
		int baseOffset = start + maxIntBytes * chunks;
		
		while(last != 0) {
			int chunk = last;
			
			if(!LENGTH_TO_TYPE.containsKey(chunk)) {
				final int currentChunk = chunk;
				
				chunk = getLengthsStream()
					.filter(sz -> sz < currentChunk)
					.max()
					.orElse(-1);
			}
			
			last -= chunk;
			
			int chunkBits = chunk << 3;
			
			accumulator.accumulate(
				result,
				integer.process(
					pos,
					bits.and(bitMask(chunkBits)),
					baseOffset,
					chunk
				)
			);

			bits = bits.shiftRight(chunkBits);
			
			baseOffset += chunk;
		}
	}
	
	private static BigInteger bitMask(int n) {
		return BigInteger.ONE
			.shiftLeft(n)
			.subtract(BigInteger.ONE);
	}
	
}
