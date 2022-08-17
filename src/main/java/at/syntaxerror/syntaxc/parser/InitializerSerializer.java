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

import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.IEEE754Utils;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.optimizer.ExpressionOptimizer;
import at.syntaxerror.syntaxc.parser.node.declaration.Initializer;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.parser.node.statement.ExpressionStatementNode;
import at.syntaxerror.syntaxc.symtab.global.GlobalVariableInitializer;
import at.syntaxerror.syntaxc.symtab.global.IntegerInitializer;
import at.syntaxerror.syntaxc.symtab.global.ListInitializer;
import at.syntaxerror.syntaxc.symtab.global.ListInitializer.ListInitializerEntry;
import at.syntaxerror.syntaxc.symtab.global.StringInitializer;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.symtab.global.ZeroInitializer;
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
		if(type.isArray()) {
			ArrayType array = type.toArray();
			Type base = array.getBase();

			int len = array.getLength();
			
			List<Initializer> inits;
			
			if(initializer.isSimple()) {
				if(initializer.getExpression().getType().isString()) {
					
					ExpressionNode expr = ExpressionOptimizer.optimize(initializer.getExpression());
					
					if(!(expr instanceof VariableExpressionNode var) || !var.getVariable().isString())
						Logger.error("Expected string literal for initializer");
					
					StringInitializer init = (StringInitializer) ((VariableExpressionNode) expr).getVariable()
						.getVariableData().initializer();
					
					if(len == ArrayType.SIZE_UNKNOWN)
						array.setLength(init.value().getBytes(StandardCharsets.UTF_8).length + 1);
					
					return init;
				}
				
				Logger.warn(initializer, Warning.MISSING_BRACES, "Missing braces around initializer");
				
				inits = List.of(initializer);
			}
			else inits = initializer.getList();
			
			if(len == ArrayType.SIZE_UNKNOWN)
				array.setLength(len = inits.size());
			
			else if(len < inits.size())
				Logger.warn(initializer, Warning.INITIALIZER_OVERFLOW, "Too many initializers for array");

			List<ListInitializerEntry> entries = new ArrayList<>();
			
			int nEntries = Math.min(inits.size(), len);
			
			int offset = base.sizeof();
			
			for(int i = 0; i < nEntries; ++i)
				entries.add(new ListInitializerEntry(
					i * offset,
					serialize(base, inits.get(i))
				));
			
			if(nEntries < len) {
				final ZeroInitializer zero = new ZeroInitializer(offset);
				
				for(int i = nEntries; i < len; ++i)
					entries.add(new ListInitializerEntry(i * offset, zero));
			}
			
			return new ListInitializer(entries);
		}
		
		if(type.isIncomplete())
			Logger.error(initializer, Warning.SEM_NONE, "Cannot initialize incomplete type");
		
		if(type.isStructLike()) {
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

			int len = members.size();
			
			if(len < inits.size())
				Logger.warn(
					initializer,
					Warning.INITIALIZER_OVERFLOW,
					"Too many initializers for %s",
					struct.isUnion()
						? "union"
						: "struct"
				);

			List<ListInitializerEntry> entries = new ArrayList<>();
			
			int nEntries = inits.size();
			
			BigInteger bits = BigInteger.ZERO;
			int bitWidth = 0;
			int bitOffset = -1;
			
			for(int i = 0; i < len; ++i) {
				Member member = members.get(i);
				type = member.getType();
				
				GlobalVariableInitializer init = null;
				
				if(i >= nEntries)
					init = type.isBitfield()
						? new IntegerInitializer(BigInteger.ZERO, 0)
						: new ZeroInitializer(type.sizeof());
				
				if(!type.isBitfield())
					init = serialize(type, inits.get(i));
				
				else if(type.isBitfield()) {
					if(init == null)
						init = serialize(type, inits.get(i), member.getBitWidth());
					
					if(bitOffset == -1)
						bitOffset = member.getOffset();
					
					var bitValue = (IntegerInitializer) init;

					bits = bits
						.or( // merge values
							bitValue.value()
								.and( // create bitmask and apply to value
									BigInteger.ONE
										.shiftLeft(member.getBitWidth())
										.subtract(BigInteger.ONE)
								)
								.shiftLeft( // shift the value to its desired place
									member.getBitOffset()
									 + 8 * (member.getOffset() - bitOffset)
								)
						);
					
					bitWidth += bitValue.size();
					
					// make sure to add bitfields if they are at the end of the struct/union
					if(i != len - 1)
						continue;
				}
				
				if(bitOffset != -1) {
					/* convert number of bits into number of bytes
					 * 
					 * e.g.:
					 * - 5 bits = 1 byte (3 bits padding)
					 * - 9 bits = 2 bytes (7 bits padding)
					 * - 8 bits = 1 byte (no padding)
					 */
					
					bitWidth = (bitWidth >> 3) + ((bitWidth & 7) == 0 ? 0 : 1);
					
					entries.add(new ListInitializerEntry(bitOffset, new IntegerInitializer(bits, bitWidth)));

					// reset state
					bits = BigInteger.ZERO;
					bitOffset = -1;
					bitWidth = 0;
					
					// this condition can only be true if the last entry has been reached
					if(type.isBitfield())
						break;
				}
				
				entries.add(new ListInitializerEntry(member.getOffset(), init));
			}
			
			return new ListInitializer(entries);
		}
		
		if(type.isEnum())
			type = type.toEnum().asNumberType();

		while(initializer.isList()) {
			Logger.warn(initializer, Warning.SCALAR_BRACES, "Braces around scalar initializer");
			initializer = initializer.getList().get(0);
		}
		
		if(type.isPointer())
			return ConstantExpressionEvaluator.evalAddress(initializer.getExpression());
		
		if(type.isInteger()) {
			NumericValueType numericType = type.toNumber().getNumericType();
			
			BigInteger bigint = ConstantExpressionEvaluator.evalInteger(initializer.getExpression());
			
			if(!numericType.inRange(bigint) || bigint.bitCount() > integerBitWidth)
				Logger.warn(initializer, Warning.INITIALIZER_OVERFLOW, "Integer is too large for initialization");
			
			return new IntegerInitializer(
				numericType.mask(bigint),
				numericType.getSize()
			);
		}
		
		if(type.isFloating()) {
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
		
		Logger.error(initializer, Warning.SEM_NONE, "Illegal type for initializer");
		return null;
	}
	
	public static List<ExpressionStatementNode> toAssignment(ExpressionParser parser, VariableExpressionNode target,
			Type type, Initializer initializer) {
		Position pos = target.getPosition();
		
		List<ExpressionNode> assignments = new ArrayList<>();
		
		if(type.isArray()) {
			// TODO
		}
		
		else if(type.isIncomplete())
			Logger.error(initializer, Warning.SEM_NONE, "Cannot initialize incomplete type");
		
		else if(type.isStructLike()) {
			// TODO
		}
		
		else {
			while(initializer.isList()) {
				Logger.warn(initializer, Warning.SCALAR_BRACES, "Braces around scalar initializer");
				initializer = initializer.getList().get(0);
			}
			
			assignments.add(
				parser.checkAssignment(
					Token.ofPunctuator(pos, Punctuator.ASSIGN),
					target,
					initializer.getExpression(),
					false
				)
			);
		}
		
		return assignments
			.stream()
			.map(ExpressionOptimizer::optimize)
			.map(ExpressionStatementNode::new)
			.toList();
	}
	
}
