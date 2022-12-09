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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxc.builtin.BuiltinFunction.ExpressionArgument;
import at.syntaxerror.syntaxc.intermediate.operand.ConditionOperand;
import at.syntaxerror.syntaxc.intermediate.operand.ConstantOperand;
import at.syntaxerror.syntaxc.intermediate.operand.GlobalOperand;
import at.syntaxerror.syntaxc.intermediate.operand.IndexOperand;
import at.syntaxerror.syntaxc.intermediate.operand.LocalOperand;
import at.syntaxerror.syntaxc.intermediate.operand.MemberOperand;
import at.syntaxerror.syntaxc.intermediate.operand.Operand;
import at.syntaxerror.syntaxc.intermediate.operand.TemporaryOperand;
import at.syntaxerror.syntaxc.intermediate.representation.AssignIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BinaryIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BinaryIntermediate.BinaryOperation;
import at.syntaxerror.syntaxc.intermediate.representation.BuiltinIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.CallIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.CastIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate;
import at.syntaxerror.syntaxc.intermediate.representation.JumpIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.LabelIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.MemcpyIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.MemsetIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.UnaryIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.UnaryIntermediate.UnaryOperation;
import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.logger.Logger;
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
import at.syntaxerror.syntaxc.parser.node.statement.NullStatementNode;
import at.syntaxerror.syntaxc.parser.node.statement.StatementNode;
import at.syntaxerror.syntaxc.symtab.SymbolObject;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.StructType;
import at.syntaxerror.syntaxc.type.Type;
import at.syntaxerror.syntaxc.type.TypeUtils;

/**
 * @author Thomas Kasper
 * 
 */
public class IntermediateGenerator {

	private static final Map<Punctuator, BinaryOperation> BINARY_OPERATIONS;
	private static final Map<Punctuator, UnaryOperation> UNARY_OPERATIONS;
	
	private static void binary(Punctuator punct, BinaryOperation op) {
		BINARY_OPERATIONS.put(punct, op);
	}

	private static void unary(Punctuator punct, UnaryOperation op) {
		UNARY_OPERATIONS.put(punct, op);
	}
	
	static {
		BINARY_OPERATIONS = new HashMap<>();
		UNARY_OPERATIONS = new HashMap<>();
		
		// INDIRECTION and MULTIPLY have the same symbol (*)
		binary(Punctuator.INDIRECTION,	BinaryOperation.MULTIPLY);
		binary(Punctuator.MULTIPLY,		BinaryOperation.MULTIPLY);
		binary(Punctuator.DIVIDE,		BinaryOperation.DIVIDE);
		binary(Punctuator.MODULO,		BinaryOperation.MODULO);
		// PLUS and AND have the same symbol (+)
		binary(Punctuator.ADD,			BinaryOperation.ADD);
		binary(Punctuator.PLUS,			BinaryOperation.ADD);
		// MINUS and SUBTRACT have the same symbol (-)
		binary(Punctuator.SUBTRACT,		BinaryOperation.SUBTRACT);
		binary(Punctuator.MINUS,		BinaryOperation.SUBTRACT);
		binary(Punctuator.LSHIFT,		BinaryOperation.SHIFT_LEFT);
		binary(Punctuator.RSHIFT,		BinaryOperation.SHIFT_RIGHT);
		binary(Punctuator.LESS,			BinaryOperation.LESS);
		binary(Punctuator.LESS_EQUAL,	BinaryOperation.LESS_EQUAL);
		binary(Punctuator.GREATER,		BinaryOperation.GREATER);
		binary(Punctuator.GREATER_EQUAL, BinaryOperation.GREATER_EQUAL);
		binary(Punctuator.EQUAL,		BinaryOperation.EQUAL);
		binary(Punctuator.NOT_EQUAL,	BinaryOperation.NOT_EQUAL);
		// ADDRESS_OF and BITWISE_AND have the same symbol (&)
		binary(Punctuator.BITWISE_AND,	BinaryOperation.BITWISE_AND);
		binary(Punctuator.ADDRESS_OF,	BinaryOperation.BITWISE_AND);
		binary(Punctuator.BITWISE_OR,	BinaryOperation.BITWISE_OR);
		binary(Punctuator.BITWISE_XOR,	BinaryOperation.BITWISE_XOR);
		binary(Punctuator.LOGICAL_AND,	BinaryOperation.LOGICAL_AND);
		binary(Punctuator.LOGICAL_OR,	BinaryOperation.LOGICAL_OR);

		
		unary(Punctuator.BITWISE_NOT,	UnaryOperation.BITWISE_NOT);
		unary(Punctuator.LOGICAL_NOT,	UnaryOperation.LOGICAL_NOT);
		// MINUS and SUBTRACT have the same symbol (-)
		unary(Punctuator.MINUS,			UnaryOperation.MINUS);
		unary(Punctuator.SUBTRACT,		UnaryOperation.MINUS);
		// INDIRECTION and MULTIPLY have the same symbol (*)
		unary(Punctuator.INDIRECTION,	UnaryOperation.INDIRECTION);
		unary(Punctuator.MULTIPLY,		UnaryOperation.INDIRECTION);
		// ADDRESS_OF and BITWISE_AND have the same symbol (&)
		unary(Punctuator.ADDRESS_OF,	UnaryOperation.ADDRESS_OF);
		unary(Punctuator.BITWISE_AND,	UnaryOperation.ADDRESS_OF);
	}
	
	private static final Map<SymbolObject, Operand> OPERANDS = new HashMap<>();

	private static int conditionLabelId = 0;
	
	private static GlobalOperand global(String name, boolean extern, Type type) {
		return new GlobalOperand(name, extern, type);
	}
	
	private static LocalOperand local(String name, int offset, Type type) {
		return new LocalOperand(name, offset, type);
	}
	
	private static ConstantOperand constant(Number number, Type type) {
		return new ConstantOperand(number, type);
	}
	
	private static TemporaryOperand temporary(Type type) {
		return new TemporaryOperand(type);
	}
	
	private static IndexOperand index(Operand base, Operand index, int scale, Type type) {
		return new IndexOperand(base, index, scale, type);
	}

	private static IndexOperand index(Operand base, Operand index, Type type) {
		return new IndexOperand(base, index, type);
	}
	
	private static IndexOperand index(Operand base, Type type) {
		return new IndexOperand(base, type);
	}
	
	private static Operand result(Type type, IRContext context, boolean noLvalue) {
		return context.needResult() && !type.isVoid() && (!noLvalue || !context.lvalue())
			? context.hasResult()
				? context.result()
				: temporary(type)
			: null;
	}

	private static Operand result(ExpressionNode expr, IRContext context, boolean noLvalue) {
		return result(expr.getType(), context, noLvalue);
	}
	
	private static Operand variable(SymbolObject object) {
		return OPERANDS.computeIfAbsent(
			object,
			x -> {
				String name = object.getFullName();
				Type type = object.getType();
				
				if(object.isReturnValue())
					return TemporaryOperand.forReturnValue(type);

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
	
	private List<Intermediate> ir;
	
	public List<Intermediate> toIntermediateRepresentation(List<StatementNode> statements) {
		ir = new ArrayList<>();
		
		processStatements(statements);
		
		return ir
			.stream()
			.collect(
				ArrayList::new,
				ArrayList::add,
				ArrayList::addAll
			);
	}
	
	private void processStatements(List<StatementNode> statements) {
		for(StatementNode statement : statements)
			if(statement instanceof CompoundStatementNode compound)
				processStatements(compound.getStatements());
		
			else if(statement instanceof ExpressionStatementNode stmt) {
				ExpressionNode child = stmt.getExpression();
				
				processExpression(
					child,
					IRContext.DEFAULT.withoutResult()
				);
			}
		
			else if(statement instanceof GotoStatementNode gxto)
				ir.add(new JumpIntermediate(
					gxto.getPosition(),
					gxto.getLabel()
				));

			else if(statement instanceof JumpStatementNode jump) {
				ExpressionNode child = jump.getCondition();
				
				Operand condition = processExpression(
					child,
					IRContext.DEFAULT.asCondition()
				);
				
				ir.add(new JumpIntermediate(
					jump.getPosition(),
					condition,
					jump.getJumpLabel()
				));
			}
		
			else if(statement instanceof LabeledStatementNode labeled) {
				ir.add(new LabelIntermediate(
					labeled.getPosition(),
					labeled.getLabel()
				));
				
				processStatements(
					List.of(labeled.getStatement())
				);
			}
	}

	@SuppressWarnings("preview")
	private Operand processExpression(ExpressionNode expression, IRContext context) {
		
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
		
		return proc.process(context);
	}
	
	private Operand[] processSwappable(boolean swapped, ExpressionNode exprA, ExpressionNode exprB, IRContext ctx) {
		Operand[] operands = new Operand[2];
		
		if(swapped) {
			operands[1] = processExpression(exprB, ctx);
			operands[0] = processExpression(exprA, ctx);
		}
		else {
			operands[0] = processExpression(exprA, ctx);
			operands[1] = processExpression(exprB, ctx);
		}
		
		return operands;
	}
	
	private Operand processArrayIndexExpression(ArrayIndexExpressionNode expr, IRContext ctx) {
		
		ctx = ctx.reset();
		
		Operand[] operands = processSwappable(
			expr.isSwapped(),
			expr.getTarget(),
			expr.getIndex(),
			ctx
		);
		
		Operand target = operands[0];
		Operand index = operands[1];

		return index(target, index, expr.getType());
	}
	
	private Operand processAssignmentExpression(BinaryExpressionNode expr, IRContext ctx) {
		
		Position pos = expr.getPosition();
		
		boolean needResult = ctx.needResult();
		ctx = ctx.reset();
		
		Type typeLeft = expr.getLeft().getType();
		Type typeRight = expr.getRight().getType();
		
		boolean compatible = TypeUtils.isCompatible(typeLeft, typeRight);

		Operand left = processExpression(expr.getLeft(), ctx.asLvalue());
		Operand right = processExpression(
			expr.getRight(),
			compatible
				? ctx.withResult(left)
				: ctx
		);

		if(left == right)
			return needResult
				? left
				: null;
		
		// implicit type cast (e.g. assigning a char to an int)
		if(!compatible) {
			if(typeLeft.isScalar() && typeRight.isScalar() && right instanceof ConstantOperand constant) {
				
				boolean floatLeft = typeLeft.isFloating();
				boolean floatRight = typeRight.isFloating();
				
				if(floatLeft == floatRight)
					right = constant(constant.getValue(), typeLeft);
				
				else if(floatLeft)
					right = constant(
						new BigDecimal((BigInteger) constant.getValue()),
						typeLeft
					);
				
				else right = constant(
					((BigDecimal) constant.getValue()).toBigInteger(),
					typeLeft
				);
				
				ir.add(new AssignIntermediate(pos, left, right));
			}
			
			else ir.add(new CastIntermediate(
				pos,
				left,
				right
			));
		}
		
		else ir.add(new AssignIntermediate(pos, left, right));
		
		Operand result = null;
		
		if(needResult) {
			// prefer register over memory access (assuming that a register is is allocated to the TemporaryOperand)
			if(left instanceof TemporaryOperand)
				result = left;
			
			else result = right; // don't care otherwise
		}

		return result;
	}

	private Operand processCommaExpression(BinaryExpressionNode expr, IRContext ctx) {

		boolean needResult = ctx.needResult();
		ctx = ctx.reset();
		
		processExpression(expr.getLeft(), ctx.withoutResult());
		
		return processExpression(
			expr.getLeft(),
			needResult
				? ctx
				: ctx.withoutResult()
		);
	}
	
	private Operand processPointerExpression(BinaryExpressionNode expr, IRContext ctx, BinaryOperation op) {

		Position pos = expr.getPosition();
		
		Operand result = result(expr, ctx, false);
		
		ctx = ctx.reset();
		
		Type typeLeft = expr.getLeft().getType();
		Type typeRight = expr.getRight().getType();

		boolean swapped = false;
		
		int sz;
		
		if(typeRight.isPointerLike()) {
			sz = typeRight.dereference().sizeof();
			
			if(op == BinaryOperation.SUBTRACT) {
				/* -- Pointer-pointer subtraction --
				 * 
				 * Calculates the number of elements between
				 * the first and the second pointer operand
				 */

				boolean compact = sz == 1;
				
				Type typeDiff = NumericValueType.PTRDIFF.asType();
				
				Operand left = processExpression(expr.getLeft(), ctx);
				Operand right = processExpression(expr.getRight(), ctx);
				
				Operand diff = compact ? result : temporary(typeDiff);
				
				// _1 = ptrStart - ptrEnd
				ir.add(new BinaryIntermediate(
					pos,
					diff,
					left,
					right,
					BinaryOperation.SUBTRACT
				));
				
				if(!compact) // result = _1 / sizeof *ptrStart [skipped if size is 1]
					ir.add(new BinaryIntermediate(
						pos,
						result,
						diff,
						constant(
							BigInteger.valueOf(sz),
							typeDiff
						),
						op
					));
				
				return result;
			}
			
			swapped = true;
		}
		else sz = typeLeft.dereference().sizeof();
		
		/* -- Pointer-offset addition/subtraction --
		 * 
		 * Calculates the address of the nth element of an
		 * array, with the pointer marking the 0th element.
		 */
		
		Operand[] operands = processSwappable(
			swapped,
			expr.getLeft(),
			expr.getRight(),
			ctx
		);
		
		return index(
			operands[0],
			operands[1],
			sz,
			expr.getType()
		);
	}

	private Operand processBinaryExpression(BinaryExpressionNode expr, IRContext ctx) {
		
		switch(expr.getOperation()) {
		case ASSIGN:
			return processAssignmentExpression(expr, ctx);
			
		case COMMA:
			return processCommaExpression(expr, ctx);
		
		default:
			break;
		}

		BinaryOperation op = BINARY_OPERATIONS.get(expr.getOperation());
		
		if((op == BinaryOperation.ADD || op == BinaryOperation.SUBTRACT) &&
			(expr.getLeft().getType().isPointerLike() ||
			expr.getRight().getType().isPointerLike()))
			return processPointerExpression(expr, ctx, op);
		
		Position pos = expr.getPosition();
		
		Operand result = null;

		if(op.isConditional() && ctx.condition())
			result = new ConditionOperand(op, expr.getType());
		
		if(result == null)
			result = result(expr, ctx, false);
		
		ctx = ctx.reset();
		
		Operand left = processExpression(expr.getLeft(), ctx.asLvalue());
		Operand right = processExpression(expr.getRight(), ctx);

		ir.add(new BinaryIntermediate(pos, result, left, right, op));

		return result;
	}

	private Operand processBuiltinExpression(BuiltinExpressionNode expr, IRContext ctx) {

		Operand result = result(expr, ctx, false);
		
		expr.getFunction()
			.getArgs()
			.stream()
			.filter(arg -> !(arg instanceof ExpressionArgument))
			.map(ExpressionArgument.class::cast)
			.map(arg -> {
				Operand op = processExpression(arg.getExpression(), IRContext.DEFAULT); 
				
				arg.setOperand(op);
				
				return op;
			})
			.toList();
		
		ir.add(new BuiltinIntermediate(
			expr.getPosition(),
			result,
			expr.getFunction()
		));

		return result;
	}

	private Operand processCallExpression(CallExpressionNode expr, IRContext ctx) {
		
		Operand result = result(expr, ctx, false);

		List<Operand> args = expr.getParameters()
			.stream()
			.map(arg -> processExpression(arg, IRContext.DEFAULT))
			.toList();

		Operand function = processExpression(
			expr.getTarget(),
			ctx.reset()
		);
		
		ir.add(new CallIntermediate(
			expr.getPosition(),
			result,
			function,
			args
		));
		
		return result;
	}

	private Operand processCastExpression(CastExpressionNode expr, IRContext ctx) {

		Type targetType = expr.getTarget().getType();
		Type castType = expr.getType();
		
		if(targetType.sizeof() == castType.sizeof() && targetType.isFloating() == castType.isFloating())
			return processExpression(
				expr.getTarget(),
				ctx.reset()
			);
		
		Operand result = result(expr, ctx, false);

		Operand target = processExpression(
			expr.getTarget(),
			ctx.reset()
		);
		
		ir.add(new CastIntermediate(
			expr.getPosition(),
			result,
			target
		));
		
		return result;
	}

	private Operand processConditionalExpression(ConditionalExpressionNode expr, IRContext ctx) {

		/*
		 * Convert 'r = c ? x : y' into
		 * 
		 *     if(c)
		 *         goto .true;
		 * .false:
		 *     r = y;
		 *     goto .end;
		 * .true:
		 *     r = x;
		 * .end:
		 *     ;
		 */
		
		Position pos = expr.getPosition();
		
		Type type = expr.getType();
		
		SymbolObject result = SymbolObject.temporary(pos, type);
		
		VariableExpressionNode var = new VariableExpressionNode(pos, result);
		
		String labelTrue = ".IF" + conditionLabelId++;
		String labelFalse = ".IF" + conditionLabelId++;
		String labelEnd = ".IF" + conditionLabelId++;
		
		processStatements(List.of(
			new JumpStatementNode(  // if(c) goto .true;
				pos,
				expr.getCondition(),
				labelTrue
			),
			new LabeledStatementNode( // .false:
				pos,
				labelFalse,
				new CompoundStatementNode(
					pos,
					List.of(
						new ExpressionStatementNode( // r = y;
							new BinaryExpressionNode(
								pos,
								var,
								expr.getWhenFalse(),
								Punctuator.ASSIGN,
								type
							)
						),
						new GotoStatementNode( // goto .end;
							pos,
							labelEnd
						)
					)
				)
			),
			new LabeledStatementNode( // .true:
				pos,
				labelTrue,
				new ExpressionStatementNode( // r = x;
					new BinaryExpressionNode(
						pos,
						var,
						expr.getWhenTrue(),
						Punctuator.ASSIGN,
						type
					)
				)
			),
			new LabeledStatementNode( // .end: ;
				pos,
				labelEnd,
				new NullStatementNode(pos)
			)
		));
		
		return variable(result);
	}

	private Operand processMemberAccessExpression(MemberAccessExpressionNode expr, IRContext ctx) {

		String name = expr.getMember();
		
		StructType.Member member = expr
			.getTarget()
			.getType()
			.dereference()
			.toStructLike()
			.getMember(name);
		
		Operand target = processExpression(
			expr.getTarget(),
			ctx.reset()
		);
		
		return new MemberOperand(
			target,
			member,
			expr.getType()
		);
	}

	private Operand processMemcpyExpression(MemcpyExpressionNode expr, IRContext ctx) {

		Operand source = variable(expr.getSource());
		Operand destination = variable(expr.getDestination());

		ir.add(new MemcpyIntermediate(
			expr.getPosition(),
			source,
			destination,
			expr.getSourceOffset(),
			expr.getDestinationOffset(),
			expr.getLength()
		));

		return null;
	}

	private Operand processMemsetExpression(MemsetExpressionNode expr, IRContext ctx) {
		
		Operand target = variable(expr.getTarget());

		ir.add(new MemsetIntermediate(
			expr.getPosition(),
			target,
			expr.getOffset(),
			expr.getLength(),
			expr.getValue()
		));
		
		return null;
	}

	private Operand processNumberLiteralExpression(NumberLiteralExpressionNode expr, IRContext ctx) {
		return constant(
			expr.getLiteral(),
			expr.getType()
		);
	}

	private Operand processUnaryExpression(UnaryExpressionNode expr, IRContext ctx) {
		
		UnaryOperation op = UNARY_OPERATIONS.get(expr.getOperation());

		Operand target = processExpression(
			expr.getTarget(),
			ctx.reset()
		);
		
//		if(op == UnaryOperation.ADDRESS_OF)
//			return index(target, expr.getType()); // TODO

		Operand result = ctx.condition() && op == UnaryOperation.LOGICAL_NOT
			? new ConditionOperand(BinaryOperation.NOT_EQUAL, expr.getType())
			: result(expr, ctx, true);
		
		if(ctx.lvalue() && expr.isLvalue() && op == UnaryOperation.INDIRECTION)
			return index(
				target,
				expr.getType()
			);

		ir.add(new UnaryIntermediate(
			expr.getPosition(),
			result,
			target,
			op
		));
		
		return result;
	}

	private Operand processVariableExpression(VariableExpressionNode expr, IRContext ctx) {
		return variable(expr.getVariable());
	}
	
	private static <E extends ExpressionNode> ExpressionProcessorHandle<E> handle(E expr, ExpressionProcessor<E> processor) {
		return ctx -> processor.process(expr, ctx);
	}
	
	private static interface ExpressionProcessorHandle<E extends ExpressionNode> {
		
		Operand process(IRContext context);
		
	}
	
	private static interface ExpressionProcessor<E extends ExpressionNode> {
		
		Operand process(E expression, IRContext context);
		
	}
	
	private static record IRContext(Operand result, boolean needResult, boolean lvalue, boolean condition) {
		
		public static final IRContext DEFAULT = new IRContext(null, true, false, false);

		public IRContext withoutResult() {
			return new IRContext(null, false, lvalue, condition);
		}

		public IRContext withResult(Operand operand) {
			return new IRContext(operand, true, lvalue, condition);
		}
		
		public IRContext asLvalue() {
			return new IRContext(result, needResult, true, condition);
		}

		public IRContext asCondition() {
			return new IRContext(result, needResult, lvalue, true);
		}
		
		public IRContext reset() {
			return DEFAULT;
		}
		
		public boolean hasResult() {
			return result != null;
		}
		
	}
	
}
