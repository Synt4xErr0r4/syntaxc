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
package at.syntaxerror.syntaxc.intermediate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxc.builtin.BuiltinFunction.ExpressionArgument;
import at.syntaxerror.syntaxc.intermediate.representation.AddIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.AddressOfIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.ArrayIndexIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.AssignIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BitwiseAndIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BitwiseNotIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BitwiseOrIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BitwiseXorIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BuiltinIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.CallIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.CastIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.DivideIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.EqualIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.GreaterThanIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.GreaterThanOrEqualIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.IndirectionIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.ConstantOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.FreeIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.GlobalOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.IndexOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.LocalOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.Operand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.ReturnValueOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.TemporaryOperand;
import at.syntaxerror.syntaxc.intermediate.representation.JumpIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.LabelIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.LessThanIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.LessThanOrEqualIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.LogicalAndIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.LogicalOrIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.MemberIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.MemcpyIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.MemsetIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.MinusIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.ModuloIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.MultiplyIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.NotEqualIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.ShiftLeftIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.ShiftRightIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.SubtractIntermediate;
import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.parser.node.expression.ArrayIndexExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.BinaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.BuiltinExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CallExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CastExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ConditionalExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.MemberAccessExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.MemcpyExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.MemsetExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.NumberLiteralExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.UnaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.VariableExpressionNode;
import at.syntaxerror.syntaxc.parser.node.statement.CompoundStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.ExpressionStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.GotoStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.JumpStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.LabeledStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.StatementNode;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.StructType;
import at.syntaxerror.syntaxc.type.Type;
import at.syntaxerror.syntaxc.type.TypeUtils;

/**
 * @author Thomas Kasper
 * 
 */
@SuppressWarnings("unchecked")
public class IntermediateGenerator {

	private static final Map<Punctuator, BinaryConstructor> BINARY_OPERATIONS;
	private static final Map<Punctuator, UnaryConstructor> UNARY_OPERATIONS;
	
	private static void binary(Punctuator punct, BinaryConstructor constructor) {
		BINARY_OPERATIONS.put(punct, constructor);
	}

	private static void unary(Punctuator punct, UnaryConstructor constructor) {
		UNARY_OPERATIONS.put(punct, constructor);
	}
	
	static {
		BINARY_OPERATIONS = new HashMap<>();
		UNARY_OPERATIONS = new HashMap<>();
		
		// INDIRECTION and MULTIPLY have the same symbol (*)
		binary(Punctuator.INDIRECTION,	MultiplyIntermediate::new);
		binary(Punctuator.MULTIPLY,		MultiplyIntermediate::new);
		binary(Punctuator.DIVIDE,		DivideIntermediate::new);
		binary(Punctuator.MODULO,		ModuloIntermediate::new);
		// PLUS and AND have the same symbol (+)
		binary(Punctuator.ADD,			AddIntermediate::new);
		binary(Punctuator.PLUS,			AddIntermediate::new);
		// MINUS and SUBTRACT have the same symbol (-)
		binary(Punctuator.SUBTRACT,		SubtractIntermediate::new);
		binary(Punctuator.MINUS,		SubtractIntermediate::new);
		binary(Punctuator.LSHIFT,		ShiftLeftIntermediate::new);
		binary(Punctuator.RSHIFT,		ShiftRightIntermediate::new);
		binary(Punctuator.LESS,			LessThanIntermediate::new);
		binary(Punctuator.LESS_EQUAL,	LessThanOrEqualIntermediate::new);
		binary(Punctuator.GREATER,		GreaterThanIntermediate::new);
		binary(Punctuator.GREATER_EQUAL, GreaterThanOrEqualIntermediate::new);
		binary(Punctuator.EQUAL,		EqualIntermediate::new);
		binary(Punctuator.NOT_EQUAL,	NotEqualIntermediate::new);
		// ADDRESS_OF and BITWISE_AND have the same symbol (&)
		binary(Punctuator.BITWISE_AND,	BitwiseAndIntermediate::new);
		binary(Punctuator.ADDRESS_OF,	BitwiseAndIntermediate::new);
		binary(Punctuator.BITWISE_OR,	BitwiseOrIntermediate::new);
		binary(Punctuator.BITWISE_XOR,	BitwiseXorIntermediate::new);
		binary(Punctuator.LOGICAL_AND,	LogicalAndIntermediate::new);
		binary(Punctuator.LOGICAL_OR,	LogicalOrIntermediate::new);

		
		unary(Punctuator.BITWISE_NOT,	BitwiseNotIntermediate::new);
		// MINUS and SUBTRACT have the same symbol (-)
		unary(Punctuator.MINUS,			MinusIntermediate::new);
		unary(Punctuator.SUBTRACT,		MinusIntermediate::new);
		// INDIRECTION and MULTIPLY have the same symbol (*)
		unary(Punctuator.INDIRECTION,	IndirectionIntermediate::new);
		unary(Punctuator.MULTIPLY,		IndirectionIntermediate::new);
		// ADDRESS_OF and BITWISE_AND have the same symbol (&)
		unary(Punctuator.ADDRESS_OF,	AddressOfIntermediate::new);
		unary(Punctuator.BITWISE_AND,	AddressOfIntermediate::new);
	}
	
	private static int conditionLabelId = 0;
	
	private final Map<SymbolObject, Operand> operands = new HashMap<>();
	
	private Operand variable(SymbolObject object) {
		return operands.computeIfAbsent(
			object,
			x -> {
				String name = object.getFullName();
				Type type = object.getType();
				
				if(object.isReturnValue())
					return new ReturnValueOperand(type);

				if(object.isTemporaryVariable())
					return temporary(type);
				
				if(object.isLocalVariable())
					return local(name, object.getOffset(), type);
				
				return global(
					name,
					object.isFunction()
						&& object.isPrototype()
						&& !object.isInitialized(),
					type
				);
			}
		);
	}
	
	private Operand global(String name, boolean extern, Type type) {
		return new GlobalOperand(name, extern, type);
	}
	
	private Operand local(String name, int offset, Type type) {
		return new LocalOperand(name, offset, type);
	}
	
	private Operand constant(Number number, Type type) {
		return new ConstantOperand(number, type);
	}
	
	private Operand temporary(Type type) {
		return new TemporaryOperand(type);
	}
	
	private Operand result(Type type, IRContext context) {
		return context.needResult() && !type.isVoid()
			? temporary(type)
			: null;
	}

	/**
	 * Indicates that the operand's register can now be reallocated
	 */
	private void free(List<Intermediate> ir, Operand operand) {
		if(operand != null)
			ir.addAll(operand.free());
	}

	/**
	 * Indicates that the operands' registers can now be reallocated
	 */
	private void free(List<Intermediate> ir, Pair<List<Intermediate>, Operand>...operands) {
		for(var operand : operands)
			if(operand != null)
				operand.ifRightPresent(op -> free(ir, op));
	}

	/**
	 * Makes sure that the operands are not free'd yet
	 */
	private void unfree(Pair<List<Intermediate>, Operand>...operands) {
		for(var operand : operands)
			if(operand != null)
				operand.ifRightPresent(Operand::unfree);
	}

	/**
	 * Accumulates the intermediates from the results into the master IR list
	 */
	private void accumulate(List<Intermediate> ir, Pair<List<Intermediate>, Operand>...results) {
		for(var result : results)
			if(result != null)
				result.ifLeftPresent(ir::addAll);
		
		unfree(results);
	}
	
	public List<Intermediate> toIntermediateRepresentation(List<StatementNode> statements) {
		return processStatements(statements);
	}
	
	private List<Intermediate> processStatements(List<StatementNode> statements) {
		List<Intermediate> ir = new ArrayList<>();
		
		for(StatementNode statement : statements)
			if(statement instanceof CompoundStatementNode compound)
				ir.addAll(processStatements(compound.getStatements()));
		
			else if(statement instanceof ExpressionStatementNode stmt) {
				ExpressionNode child = stmt.getExpression();
				
				var result = processExpression(
					child,
					IRContext.defaults(child)
						.withoutResult()
				);
				
				ir.addAll(result.getLeft());

				free(ir, result);
			}
		
			else if(statement instanceof GotoStatementNode gxto)
				ir.add(new JumpIntermediate(
					gxto.getPosition(),
					gxto.getLabel()
				));

			else if(statement instanceof JumpStatementNode jump) {
				var condition = processExpression(jump.getCondition());
				
				accumulate(ir, condition);
				
				ir.add(new JumpIntermediate(
					jump.getPosition(),
					condition.getRight(),
					jump.getJumpLabel()
				));
				
				free(ir, condition);
			}
		
			else if(statement instanceof LabeledStatementNode labeled) {
				ir.add(new LabelIntermediate(
					labeled.getPosition(),
					labeled.getLabel()
				));
				
				ir.addAll(processStatements(
					List.of(labeled.getStatement())
				));
			}
		
		return ir
			.stream()
			// filter out discarded frees
			.filter(imm -> !(imm instanceof FreeIntermediate free) || !free.isDiscarded())
			.collect(
				ArrayList::new,
				ArrayList::add,
				ArrayList::addAll
			);
	}

	private Pair<List<Intermediate>, Operand> processExpression(ExpressionNode expression) {
		return processExpression(expression, null);
	}

	@SuppressWarnings("preview")
	private Pair<List<Intermediate>, Operand> processExpression(ExpressionNode expression, IRContext ctx) {
		if(ctx == null)
			ctx = IRContext.defaults(expression);
		
		ExpressionProcessorHandle<?> proc = switch(expression) {
		case ArrayIndexExpressionNode idx ->	handle(idx, this::processArrayIndexExpression);
		case BinaryExpressionNode bin ->		handle(bin, this::processBinaryExpression);
		case BuiltinExpressionNode blt ->		handle(blt, this::processBuiltinExpression);
		case CallExpressionNode cll ->			handle(cll, this::processCallExpression);
		case CastExpressionNode cst ->			handle(cst, this::processCastExpression);
		case ConditionalExpressionNode cnd ->	handle(cnd, this::processConditionalExpression);
		case MemberAccessExpressionNode mem ->	handle(mem, this::processMemberAccessExpression);
		case MemcpyExpressionNode cpy ->		handle(cpy, this::processMemcpyExpression);
		case MemsetExpressionNode set ->		handle(set, this::processMemsetExpression);
		case NumberLiteralExpressionNode lit ->	handle(lit, this::processNumberLiteralExpression);
		case UnaryExpressionNode uny ->			handle(uny, this::processUnaryExpression);
		case VariableExpressionNode var ->		handle(var, this::processVariableExpression);
		default -> null;
		};
		
		if(proc == null)
			Logger.error(expression, "Unrecognized expression type");
		
		var result = proc.process(ctx);
		
		unfree(result);
		
		return result;
	}

	private Pair<List<Intermediate>, Operand> processArrayIndexExpression(ArrayIndexExpressionNode idx, IRContext context) {
		List<Intermediate> ir = new ArrayList<>();
		
		Operand result = result(idx.getType(), context);
		
		boolean lvalue = context.lvalue();
		context = context.reset();
		
		var target = processExpression(idx.getTarget(), context);
		var index = processExpression(idx.getIndex(), context);
		
		if(idx.isSwapped()) // change order of evaluation if format is index[pointer]
			accumulate(ir, index, target);
		else accumulate(ir, target, index);
		
		if(lvalue) {
			Operand indexOperand = index.getRight();
			
			if(indexOperand instanceof ConstantOperand constant && !constant.isFloating())
				return Pair.of(
					ir,
					new IndexOperand(
						target.getRight(),
						constant.getValue().longValue(),
						idx.getType()
					)
				);
			
			Operand address = temporary(NumericValueType.POINTER.asType());
			
			ir.add(new AddIntermediate(
				context.position(),
				address,
				target.getRight(),
				indexOperand
			));

			free(ir, index, target);
			
			return Pair.of(
				ir,
				new IndexOperand(
					address,
					idx.getType()
				)
			);
		}
		
		ir.add(new ArrayIndexIntermediate(
			context.position(),
			result,
			target.getRight(),
			index.getRight()
		));

		free(ir, index, target);
		
		return Pair.of(ir, result);
	}

	/*
	 * Special cases for binary operations on pointers:
	 * 
	 * - 'pointer - pointer':
	 *   
	 *   Returns the number of elements between those two pointers, e.g.:
	 *   
	 *     int *array = ...;
	 *     int n = &array[7] - &array[4];
	 *   
	 *   'n' now has the value '3', since there are three integer elements
	 *   between '&array[4]' (inclusive) and '&array[7]' (exclusive). This
	 *   is, however, NOT equal to the number of bytes, which would be 12
	 *   (assuming an 'int' takes 4 bytes). Therefore, the result of the
	 *   subtraction is internally divided by the size of the pointer's
	 *   base type. Given two pointers 'x' and 'y' of the base type 'type_t',
	 *   the subtraction 'x - y' is converted into the following:
	 *   
	 *     ((char *) x - (char *) y) / sizeof(type_t)
	 *   
	 * - 'pointer - integer', 'pointer + integer', and 'integer + pointer':
	 *   
	 *   Moves the pointer a given number of elements backwards/forwards:
	 *   
	 *     int *array = ...;
	 *     int *elem = &array[7] - 4;
	 *   
	 *   'elem' is now a pointer to the 4th element (index 3) of the array.
	 *   The internal calculation is performed similarly to the pointer-
	 *   pointer subtraction: the offset is multiplied by the size of the
	 *   array's base type. Given a pointer 'x' of the base type 'type_t'
	 *   and an offset 'i', the additions 'i + x' and 'x + i', and the
	 *   subtraction 'x - i' are converted into the following:
	 *   
	 *     i * sizeof(type_t) + ((char *) x
	 *     ((char *) x + i * sizeof(type_t)
	 *     ((char *) x - i * sizeof(type_t)
	 *   
	 */
	private Pair<List<Intermediate>, Operand> processBinaryPointerExpression(BinaryExpressionNode binop, IRContext context) {
		List<Intermediate> ir = new ArrayList<>();
		
		Operand result = result(binop.getType(), context);

		context = context.reset();
		
		Punctuator op = binop.getOperation();

		var exprLeft = binop.getLeft();
		var exprRight = binop.getRight();
		
		Type typeLeft = exprLeft.getType();
		Type typeRight = exprRight.getType();
		
		Position pos = context.position();
		
		boolean swap = false;
		ExpressionNode exprPtr = exprLeft;
		ExpressionNode exprOff = exprRight;
		
		switch(op) {
		// PLUS and AND have the same symbol (+)
		case ADD:
		case PLUS:
			if(swap = typeRight.isPointerLike()) {
				exprPtr = exprRight;
				exprOff = exprLeft;
			}
			break;
			
		// SUBTRACT and MINUS have the same symbol (-)
		case SUBTRACT:
		case MINUS:
			if(typeLeft.isPointerLike() && typeRight.isPointerLike()) {
				var left = processExpression(exprLeft, context);
				var right = processExpression(exprRight, context);
				
				accumulate(ir, left, right);
				
				Operand diff = temporary(NumericValueType.PTRDIFF.asType());
				
				// _1 = x - y
				ir.add(new SubtractIntermediate(
					pos,
					diff,
					left.getRight(),
					right.getRight()
				));
				
				free(ir, right, left);
				
				// _2 = _1 / sizeof(type_t)
				ir.add(new DivideIntermediate(
					pos,
					result,
					diff,
					constant(
						BigInteger.valueOf(typeLeft.sizeof()),
						diff.getType()
					)
				));
				
				return Pair.of(ir, result);
			}
			break;
			
		default:
			Logger.error(binop, "Illegal binary expression on pointer type");
		}
		
		if(!exprOff.getType().isInteger())
			Logger.error(exprOff, "Illegal type for pointer offset");

		Pair<List<Intermediate>, Operand> ptr = processExpression(exprPtr, context);
		Pair<List<Intermediate>, Operand> off = null;

		Operand pointer = ptr.getRight();
		Operand offset;
		
		// pointer + offset: evaluate pointer first
		if(!swap)
			accumulate(ir, ptr);
		
		int sizeof = exprPtr.getType().dereference().sizeof();

		if(exprOff instanceof NumberLiteralExpressionNode lit)
			// evaluate constant expression
			offset = constant(
				((BigInteger) lit.getLiteral())
					.multiply(BigInteger.valueOf(sizeof)),
				lit.getType()
			);
		
		else {
			off = processExpression(exprOff);
			accumulate(ir, off);

			offset = temporary(NumericValueType.PTRDIFF.asType());
			
			// offset * sizeof(type_t)
			ir.add(new MultiplyIntermediate(
				pos,
				offset,
				off.getRight(),
				constant(BigInteger.valueOf(sizeof), typeRight)
			));
		}
		
		// offset + pointer: evaluate offset first
		if(swap)
			accumulate(ir, ptr);
		
		unfree(off, ptr);
		
		// pointer + offset * sizeof(type_t)
		ir.add(
			op == Punctuator.ADD || op == Punctuator.PLUS
				? new AddIntermediate(pos, result, pointer, offset)
				: new SubtractIntermediate(pos, result, pointer, offset)
		);

		free(ir, pointer);
		free(ir, offset);
		
		return Pair.of(ir, result);
	}
	
	private Pair<List<Intermediate>, Operand> processBinaryExpression(BinaryExpressionNode binop, IRContext context) {
		Position pos = context.position();
		
		var exprLeft = binop.getLeft();
		var exprRight = binop.getRight();

		Punctuator op = binop.getOperation();
		
		switch(op) {
		case ADD:
		case PLUS:
		case SUBTRACT:
		case MINUS:
			if(exprLeft.getType().isPointerLike() || exprRight.getType().isPointerLike())
				return processBinaryPointerExpression(binop, context.asRvalue());
			
		default:
			break;
		}
		
		List<Intermediate> ir = new ArrayList<>();
		
		Operand result = op == Punctuator.COMMA
			? null
			: result(binop.getType(), context);

		boolean needResult = context.needResult();
		context = context.reset();
		
		var left = processExpression(
			binop.getLeft(),
			op == Punctuator.ASSIGN
				? context.asLvalue()
				: context
		);
		
		var right = processExpression(binop.getRight(), context);
		
		accumulate(ir, left, right);
		
		var operandLeft = left.getRight();
		var operandRight = right.getRight();
		
		BinaryConstructor constructor = BINARY_OPERATIONS.get(op);
		
		if(constructor != null)
			ir.add(constructor.construct(pos, result, operandLeft, operandRight));
		
		else switch(op) {
		case ASSIGN:
			Type typeLeft = exprLeft.getType();
			Type typeRight = exprRight.getType();
			
			// implicit type cast (e.g. assigning a char to an int)
			if(!TypeUtils.isCompatible(typeLeft, typeRight))
				ir.add(new CastIntermediate(
					pos,
					operandLeft,
					operandRight,
					typeLeft.isFloating(),
					typeRight.isFloating()
				));
			
			else ir.add(new AssignIntermediate(pos, operandLeft, operandRight));
			
			if(needResult) {
				// prefer register over memory access (assuming that a register is is allocated to the TemporaryOperand)
				if(operandLeft instanceof TemporaryOperand)
					result = operandLeft;
				
				else result = operandRight; // don't care otherwise
			}
			break;
		
		case COMMA:
			if(needResult)
				result = operandRight;
			break;
			
		default:
			Logger.error(binop, "Unrecognized binary expression type");
		}

		free(ir, right, left);

		return Pair.of(ir, result);
	}

	private Pair<List<Intermediate>, Operand> processBuiltinExpression(BuiltinExpressionNode builtin, IRContext context) {
		Operand result = result(builtin.getType(), context);
		
		List<Intermediate> ir = new ArrayList<>();
		
		List<Operand> operands = new ArrayList<>();
		
		builtin.getFunction().getArgs()
			.forEach(arg -> {
				if(arg instanceof ExpressionArgument expr) {
					var processed = processExpression(expr.getExpression());
					
					accumulate(ir, processed);
					
					processed.ifRightPresent(operands::add);
					processed.ifRightPresent(expr::setOperand);
				}
			});
		
		ir.add(new BuiltinIntermediate(
			context.position(),
			builtin.getFunction()
		));
		
		operands.forEach(arg -> free(ir, arg));

		return Pair.of(ir, result);
	}

	private Pair<List<Intermediate>, Operand> processCallExpression(CallExpressionNode call, IRContext context) {
		List<Intermediate> ir = new ArrayList<>();
		
		Operand result = result(call.getType(), context);
		
		var args = call.getParameters()
			.stream()
			.map(expr -> {
				var arg = processExpression(expr);

				accumulate(ir, arg);
				
				return arg.getRight();
			})
			.toList();
		
		var function = processExpression(call.getTarget(), context.reset());
		accumulate(ir, function);
		
		args.forEach(Operand::unfree);
		
		ir.add(new CallIntermediate(
			context.position(),
			result,
			function.getRight(),
			args
		));
		
		free(ir, function);
		
		// free arguments in reverse order to avoid any stack push/pop-order problems
		args.forEach(arg -> free(ir, arg));
		
		return Pair.of(ir, result);
	}

	private Pair<List<Intermediate>, Operand> processCastExpression(CastExpressionNode cast, IRContext context) {
		List<Intermediate> ir = new ArrayList<>();
		
		Operand result = result(cast.getType(), context);
		
		var target = processExpression(cast.getTarget(), context.reset());
		accumulate(ir, target);

		ir.add(new CastIntermediate(
			context.position(),
			result,
			target.getRight(),
			cast.getType().isFloating(),
			cast.getTarget().getType().isFloating()
		));
		
		free(ir, target);
		
		return Pair.of(ir, result);
	}

	private Pair<List<Intermediate>, Operand> processConditionalExpression(ConditionalExpressionNode cond, IRContext context) {
		
		/*
		 * Convert 'r = c ? x : y' into
		 * 
		 *     if(c)
		 *         goto .false;
		 *     r = x;
		 *     goto .end;
		 * .false: 
		 *     r = y;
		 * .end:
		 *     ;
		 */
		
		Position pos = context.position();
		
		List<Intermediate> ir = new ArrayList<>();
		
		Operand result = result(cond.getType(), context);
		context = context.reset();
		
		var condition = processExpression(cond.getCondition(), context);
		accumulate(ir, condition);
		
		String labelFalse = ".IF" + conditionLabelId++;
		String labelEnd = ".IF" + conditionLabelId++;

		// if(c) goto .false;
		ir.add(new JumpIntermediate(
			pos,
			condition.getRight(),
			labelFalse
		));

		free(ir, condition);
		
		var whenTrue = processExpression(cond.getWhenTrue(), context);
		accumulate(ir, whenTrue);

		// r = x;
		ir.add(new AssignIntermediate(
			pos,
			result,
			whenTrue.getRight()
		));

		free(ir, whenTrue);

		// goto .end;
		ir.add(new JumpIntermediate(
			pos,
			labelEnd
		));

		// .false:
		ir.add(new LabelIntermediate(
			pos,
			labelFalse
		));

		var whenFalse = processExpression(cond.getWhenTrue(), context);
		accumulate(ir, whenFalse);

		// r = y;
		ir.add(new AssignIntermediate(
			pos,
			result,
			whenFalse.getRight()
		));

		free(ir, whenFalse);
		
		// .end: ;
		ir.add(new LabelIntermediate(
			pos,
			labelEnd
		));
		
		return Pair.of(ir, result);
	}

	private Pair<List<Intermediate>, Operand> processMemberAccessExpression(MemberAccessExpressionNode mem, IRContext context) {
		
		/* Intermediate representation for 's.m'
		 * 
		 * x86 implementation example:
		 * 
		 * lvalue: ('s.m = v')
		 * 	 global:
		 *   	mov ?WORD PTR s[rip+offsetof(struct s, m)], v 
		 * 	local:
		 * 		mov ?WORD PTR [rbp+local_offsetof(s)+offsetof(struct s, m)], v
		 * 	derived:
		 * 		mov rax, do_stuff(s)
		 * 		mov ?WORD PTR [rax+offsetof(struct s, m)], v
		 * 
		 * rvalue: ('v = s.m')
		 * 	 global:
		 * 		mov ?, ?WORD PTR s[rip+offsetof(struct s, m)]
		 * 	local:
		 * 		mov ?, ?WORD PTR [rbp+local_offsetof(s)+offsetof(struct s, m)]
		 * 	derived:
		 * 		mov rax, do_stuff(s)
		 * 		mov ?, ?WORD PTR [rax+offsetof(struct s, m)]
		 * 
		 * LocalOperand = [rbp+OFFSET]
		 * GlobalOperand = NAME[rip]
		 * TemporaryOperand = REGISTER
		 */
		
		List<Intermediate> ir = new ArrayList<>();

		String name = mem.getMember();
		
		StructType.Member member = mem
			.getTarget()
			.getType()
			.dereference()
			.toStructLike()
			.getMember(name);
		
		if(!member.isBitfield()) {
			var target = processExpression(mem.getTarget(), context.reset());
			accumulate(ir, target);
			
			Operand result = new IndexOperand(
				target.getRight(),
				member.getOffset(),
				mem.getType()
			);
			
			free(ir, target);
			
			return Pair.of(ir, result);
		}
		
		Operand result = result(mem.getType(), context);
		
		var target = processExpression(mem.getTarget(), context.reset());
		accumulate(ir, target);
		
		ir.add(new MemberIntermediate(
			context.position(),
			result,
			target.getRight(),
			local(
				name,
				member.getOffset(),
				member.getType()
			),
			member.getBitOffset(),
			member.getBitWidth()
		));

		free(ir, target);
		
		return Pair.of(ir, result);
	}

	private Pair<List<Intermediate>, Operand> processMemcpyExpression(MemcpyExpressionNode memcpy, IRContext context) {
		Operand source = variable(memcpy.getSource());
		Operand destination = variable(memcpy.getDestination());

		List<Intermediate> ir = new ArrayList<>();
		
		ir.add(new MemcpyIntermediate(
			context.position(),
			source,
			destination,
			memcpy.getSourceOffset(),
			memcpy.getDestinationOffset(),
			memcpy.getLength()
		));

		free(ir, destination);
		free(ir, source);
		
		return Pair.ofLeft(ir);
	}

	private Pair<List<Intermediate>, Operand> processMemsetExpression(MemsetExpressionNode memset, IRContext context) {
		Operand target = variable(memset.getTarget());

		List<Intermediate> ir = new ArrayList<>();
		
		ir.add(new MemsetIntermediate(
			context.position(),
			target,
			memset.getOffset(),
			memset.getLength(),
			memset.getValue()
		));
		
		free(ir, target);
		
		return Pair.ofLeft(ir);
	}

	private Pair<List<Intermediate>, Operand> processNumberLiteralExpression(NumberLiteralExpressionNode lit, IRContext context) {
		return Pair.ofRight(constant(
			lit.getLiteral(),
			lit.getType()
		));
	}

	private Pair<List<Intermediate>, Operand> processUnaryExpression(UnaryExpressionNode unop, IRContext context) {
		List<Intermediate> ir = new ArrayList<>();

		Punctuator op = unop.getOperation();
		
		// lvalue indirection
		// INDIRECTION and MULTIPLY have the same symbol (*)
		if(context.lvalue() && unop.isLvalue() && (op == Punctuator.INDIRECTION || op == Punctuator.MULTIPLY)) {
			var target = processExpression(unop.getTarget(), context.reset());
			accumulate(ir, target);
			
			return Pair.of(
				ir,
				new IndexOperand(
					target.getRight(),
					unop.getType()
				)
			);
		}
		
		Operand result = result(unop.getType(), context);

		var target = processExpression(unop.getTarget(), context.reset());
		accumulate(ir, target);

		// ADDRESS_OF and BITWISE_AND have the same symbol (&)
		if(target.getRight() instanceof IndexOperand index && (op == Punctuator.ADDRESS_OF || op == Punctuator.BITWISE_AND))
			ir.add(new AddIntermediate(
				context.position(),
				result,
				index.getTarget(),
				new ConstantOperand(
					BigInteger.valueOf(index.getIndex()),
					NumericValueType.POINTER.asType()
				)
			));
		
		else {
			UnaryConstructor constructor = UNARY_OPERATIONS.get(op);
		
			if(constructor != null)
				ir.add(constructor.construct(
					context.position(),
					result,
					target.getRight()
				));
			
			else Logger.error(unop, "Unrecognized unary expression type");
		}

		free(ir, target);

		return Pair.of(ir, result);
	}

	private Pair<List<Intermediate>, Operand> processVariableExpression(VariableExpressionNode var, IRContext context) {
		return Pair.ofRight(variable(var.getVariable()));
	}
	
	private static <E extends ExpressionNode> ExpressionProcessorHandle<E> handle(E expr, ExpressionProcessor<E> processor) {
		return ctx -> processor.process(expr, ctx);
	}
	
	private static interface ExpressionProcessorHandle<E extends ExpressionNode> {
		
		Pair<List<Intermediate>, Operand> process(IRContext context);
		
	}
	
	private static interface ExpressionProcessor<E extends ExpressionNode> {
		
		Pair<List<Intermediate>, Operand> process(E expression, IRContext context);
		
	}
	
	private static record IRContext(Position position, boolean needResult, boolean lvalue) {
		
		public static IRContext defaults(Positioned pos) {
			return new IRContext(pos.getPosition(), true, false);
		}

		public IRContext withoutResult() {
			return new IRContext(position, false, lvalue);
		}

		public IRContext asLvalue() {
			return new IRContext(position, needResult, true);
		}

		public IRContext asRvalue() {
			return new IRContext(position, needResult, false);
		}
		
		public IRContext reset() {
			return new IRContext(position, true, false);
		}
		
	}
	
}
