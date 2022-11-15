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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import at.syntaxerror.syntaxc.builtin.BuiltinFunction.ExpressionArgument;
import at.syntaxerror.syntaxc.intermediate.representation.AddIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.AddressOfIntermediate;
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
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.GlobalOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.IndexOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.IndirectionOperand;
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
import at.syntaxerror.syntaxc.type.StructType;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
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
				String name = object.getName();
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
	
	private void free(List<Intermediate> ir, Operand operand) {
		if(operand != null)
			ir.addAll(operand.free());
	}

	private void free(List<Intermediate> ir, Pair<List<Intermediate>, Operand> operand) {
		free(ir, operand.getRight());
	}
	
	public List<Intermediate> toIntermediateRepresentation(List<StatementNode> statements) {
		return processStatements(statements);
	}
	
	private List<Intermediate> processStatements(List<StatementNode> statements) {
		List<Intermediate> ir = new ArrayList<>();
		
		for(StatementNode statement : statements)
			if(statement instanceof CompoundStatementNode compound)
				ir.addAll(processStatements(compound.getStatements()));
		
			else if(statement instanceof ExpressionStatementNode expr) {
				var result = processExpression(expr.getExpression(), false, false);
				
				ir.addAll(result.getLeft());

				free(ir, result);
			}
		
			else if(statement instanceof GotoStatementNode gxto)
				ir.add(new JumpIntermediate(
					gxto.getPosition(),
					gxto.getLabel()
				));

			else if(statement instanceof JumpStatementNode jump) {
				var condition = processExpression(jump.getCondition(), false);
				
				condition.ifLeftPresent(ir::addAll);
				
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
		
		return ir;
	}
	
	private Pair<List<Intermediate>, Operand> processExpression(ExpressionNode expression, boolean lvalue) {
		return processExpression(expression, true, false);
	}
	
	private Pair<List<Intermediate>, Operand> processExpression(ExpressionNode expression, boolean needResult, boolean lvalue) {

		Position pos = expression.getPosition();
		
		if(expression instanceof BinaryExpressionNode binop) {
			List<Intermediate> ir = new ArrayList<>();
			
			Operand result = needResult
				? temporary(binop.getType())
				: null;
			
			var exprLeft = binop.getLeft();
			var exprRight = binop.getRight();
			
			if(exprLeft.getType().isPointerLike() || exprRight.getType().isPointerLike()) {
				
				// TODO
				
			}
			
			Punctuator op = binop.getOperation();
			
			var left = processExpression(binop.getLeft(), lvalue || op == Punctuator.ASSIGN);
			left.ifLeftPresent(ir::addAll);

			var right = processExpression(binop.getRight(), lvalue);
			right.ifLeftPresent(ir::addAll);
			
			var operandLeft = left.getRight();
			var operandRight = right.getRight();
			
			BinaryConstructor constructor = BINARY_OPERATIONS.get(op);
			
			if(constructor != null)
				ir.add(constructor.construct(pos, result, operandLeft, operandRight));
			
			else switch(op) {
			case ASSIGN:
				if(needResult) {
					ir.add(new AssignIntermediate(pos, result, operandRight));
					ir.add(new AssignIntermediate(pos, operandLeft, result));
				}
				else ir.add(new AssignIntermediate(pos, operandLeft, operandRight));
				break;
			
			case COMMA:
				ir.add(new AssignIntermediate(pos, result, operandRight));
				break;
				
			default:
				Logger.error(expression, "Unrecognized binary expression type");
			}

			free(ir, right);
			free(ir, left);

			return Pair.of(ir, result);
		}
		
		else if(expression instanceof CallExpressionNode call) {
			List<Intermediate> ir = new ArrayList<>();
			
			Operand result = call.getType().isVoid()
				? null
				: temporary(call.getType());
			
			var args = call.getParameters()
				.stream()
				.map(expr -> {
					var arg = processExpression(expr, false);

					arg.ifLeftPresent(ir::addAll);
					
					return arg.getRight();
				})
				.toList();
			
			var function = processExpression(call.getTarget(), lvalue);
			function.ifLeftPresent(ir::addAll);
			
			ir.add(new CallIntermediate(
				pos,
				result,
				function.getRight(),
				args
			));
			
			free(ir, function);
			
			// free arguments in reverse order to avoid any stack push/pop-order problems
			args.stream()
				.collect(Collectors.toCollection(LinkedList::new))
				.descendingIterator()
				.forEachRemaining(arg -> free(ir, arg));
			
			return Pair.of(ir, result);
		}
		
		else if(expression instanceof CastExpressionNode cast) {
			List<Intermediate> ir = new ArrayList<>();
			
			Operand result = temporary(cast.getType());
			
			var target = processExpression(cast.getTarget(), lvalue);
			target.ifLeftPresent(ir::addAll);

			ir.add(new CastIntermediate(
				pos,
				result,
				target.getRight(),
				cast.getType().isFloating(),
				cast.getTarget().getType().isFloating()
			));
			
			free(ir, target);
			
			return Pair.of(ir, result);
		}
		
		else if(expression instanceof ConditionalExpressionNode cond) {
			
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
			
			List<Intermediate> ir = new ArrayList<>();
			
			Operand result = temporary(cond.getType());
			
			var condition = processExpression(cond.getCondition(), lvalue);
			condition.ifLeftPresent(ir::addAll);
			
			String labelFalse = ".IF" + conditionLabelId++;
			String labelEnd = ".IF" + conditionLabelId++;

			// if(c) goto .false;
			ir.add(new JumpIntermediate(
				pos,
				condition.getRight(),
				labelFalse
			));

			free(ir, condition);
			
			var whenTrue = processExpression(cond.getWhenTrue(), lvalue);
			whenTrue.ifLeftPresent(ir::addAll);

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

			var whenFalse = processExpression(cond.getWhenTrue(), lvalue);
			whenFalse.ifLeftPresent(ir::addAll);

			free(ir, whenFalse);

			// r = y;
			ir.add(new AssignIntermediate(
				pos,
				result,
				whenFalse.getRight()
			));
			
			// .end: ;
			ir.add(new LabelIntermediate(
				pos,labelEnd
			));
			
			return Pair.of(ir, result);
		}
		
		else if(expression instanceof MemberAccessExpressionNode mem) {
			
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
				.toStructLike()
				.getMember(name);
			
			if(lvalue && !member.isBitfield()) {
				var target = processExpression(mem.getTarget(), lvalue);
				target.ifLeftPresent(ir::addAll);

				int offset = member.getOffset();
				
				return Pair.of(
					ir,
					null // TODO
				);
			}
			
			Operand result = temporary(mem.getType());
			
			var target = processExpression(mem.getTarget(), lvalue);
			target.ifLeftPresent(ir::addAll);
			
			ir.add(new MemberIntermediate(
				pos,
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
		
		else if(expression instanceof ArrayIndexExpressionNode idx) {
			List<Intermediate> ir = new ArrayList<>();
			
			var target = processExpression(idx.getTarget(), lvalue);
			target.ifLeftPresent(ir::addAll);

			var index = processExpression(idx.getIndex(), lvalue);
			index.ifLeftPresent(ir::addAll);
			
			return Pair.of(
				ir,
				new IndexOperand(
					target.getRight(),
					index.getRight(),
					idx.getType()
				)
			);
		}
		
		else if(expression instanceof NumberLiteralExpressionNode lit)
			return Pair.ofRight(constant(
				lit.getLiteral(),
				lit.getType()
			));
		
		else if(expression instanceof UnaryExpressionNode unop) {
			List<Intermediate> ir = new ArrayList<>();

			Punctuator op = unop.getOperation();
			
			// lvalue indirection
			// INDIRECTION and MULTIPLY have the same symbol (*)
			if(lvalue && unop.isLvalue() && (op == Punctuator.INDIRECTION || op == Punctuator.MULTIPLY)) {
				var target = processExpression(unop.getTarget(), lvalue);
				target.ifLeftPresent(ir::addAll);
				
				return Pair.of(
					ir,
					new IndirectionOperand(
						target.getRight(),
						unop.getType()
					)
				);
			}
			
			Operand result = temporary(unop.getType());

			var target = processExpression(unop.getTarget(), lvalue);
			target.ifLeftPresent(ir::addAll);

			UnaryConstructor constructor = UNARY_OPERATIONS.get(op);
			
			if(constructor != null)
				ir.add(constructor.construct(pos, result, target.getRight()));
			
			else Logger.error(expression, "Unrecognized unary expression type");

			free(ir, target);

			return Pair.of(ir, result);
		}
		
		else if(expression instanceof VariableExpressionNode var)
			return Pair.ofRight(variable(var.getVariable()));
		
		else if(expression instanceof MemsetExpressionNode memset) {
			Operand target = variable(memset.getTarget());

			List<Intermediate> ir = new ArrayList<>();
			
			ir.add(new MemsetIntermediate(
				pos,
				target,
				memset.getOffset(),
				memset.getLength(),
				memset.getValue()
			));
			
			free(ir, target);
			
			return Pair.ofLeft(ir);
		}
		
		else if(expression instanceof MemcpyExpressionNode memcpy) {
			Operand source = variable(memcpy.getSource());
			Operand destination = variable(memcpy.getDestination());

			List<Intermediate> ir = new ArrayList<>();
			
			ir.add(new MemcpyIntermediate(
				pos,
				source,
				destination,
				memcpy.getSourceOffset(),
				memcpy.getDestinationOffset(),
				memcpy.getLength()
			));
			
			free(ir, source);
			free(ir, destination);
			
			return Pair.ofLeft(ir);
		}
		
		else if(expression instanceof BuiltinExpressionNode builtin) {
			Operand result = needResult && !builtin.getType().isVoid()
				? temporary(builtin.getType())
				: null;
			
			List<Intermediate> ir = new ArrayList<>();
			
			List<Operand> operands = new ArrayList<>();
			
			builtin.getFunction().getArgs()
				.forEach(arg -> {
					if(arg instanceof ExpressionArgument expr) {
						var processed = processExpression(expr.getExpression(), false);
						
						processed.ifLeftPresent(ir::addAll);
						processed.ifRightPresent(operands::add);
						processed.ifRightPresent(expr::setOperand);
					}
				});
			
			ir.add(new BuiltinIntermediate(pos, builtin.getFunction()));
			
			for(int i = operands.size() - 1; i >= 0; --i)
				free(ir, operands.get(i));

			return Pair.of(ir, result);
		}
		
		Logger.error(expression, "Unrecognized expression type");
		return null;
	}
	
}
