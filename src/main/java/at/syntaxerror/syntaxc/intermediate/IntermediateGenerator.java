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

import at.syntaxerror.syntaxc.intermediate.representation.AddIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.AddressOfIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.AssignIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BitwiseAndIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BitwiseNotIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BitwiseOrIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.BitwiseXorIntermediate;
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
import at.syntaxerror.syntaxc.intermediate.representation.MinusIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.ModuloIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.MultiplyIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.NotEqualIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.ShiftLeftIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.ShiftRightIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.SubtractIntermediate;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.parser.node.expression.BinaryExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CallExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.CastExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ConditionalExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.node.expression.MemberAccessExpressionNode;
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
				var result = processExpression(expr.getExpression());
				
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
	
	private Pair<List<Intermediate>, Operand> processExpression(ExpressionNode expression) {

		Position pos = expression.getPosition();
		
		if(expression instanceof BinaryExpressionNode binop) {
			List<Intermediate> ir = new ArrayList<>();
			
			Operand result = temporary(binop.getType());
			
			var exprLeft = binop.getLeft();
			var exprRight = binop.getRight();
			
			if(exprLeft.getType().isPointerLike() || exprRight.getType().isPointerLike()) {
				
				
				
			}
			
			var left = processExpression(binop.getLeft());
			left.ifLeftPresent(ir::addAll);

			var right = processExpression(binop.getRight());
			right.ifLeftPresent(ir::addAll);
			
			var operandLeft = left.getRight();
			var operandRight = right.getRight();
			
			switch(binop.getOperation()) {
			case INDIRECTION: // INDIRECTION and MULTIPLY have the same symbol (*)
			case MULTIPLY:
				ir.add(new MultiplyIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case DIVIDE:
				ir.add(new DivideIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case MODULO:
				ir.add(new ModuloIntermediate(pos, result, operandLeft, operandRight));
				break;

			case PLUS: // PLUS and AND have the same symbol (+)
			case ADD:
				ir.add(new AddIntermediate(pos, result, operandLeft, operandRight));
				break;

			case MINUS: // MINUS and SUBTRACT have the same symbol (-)
			case SUBTRACT:
				ir.add(new SubtractIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case LSHIFT:
				ir.add(new ShiftLeftIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case RSHIFT:
				ir.add(new ShiftRightIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case LESS:
				ir.add(new LessThanIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case LESS_EQUAL:
				ir.add(new LessThanOrEqualIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case GREATER:
				ir.add(new GreaterThanIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case GREATER_EQUAL:
				ir.add(new GreaterThanOrEqualIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case EQUAL:
				ir.add(new EqualIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case NOT_EQUAL:
				ir.add(new NotEqualIntermediate(pos, result, operandLeft, operandRight));
				break;

			case ADDRESS_OF: // ADDRESS_OF and BITWISE_AND have the same symbol (&)
			case BITWISE_AND:
				ir.add(new BitwiseAndIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case BITWISE_OR:
				ir.add(new BitwiseOrIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case BITWISE_XOR:
				ir.add(new BitwiseXorIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case LOGICAL_AND:
				ir.add(new LogicalAndIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case LOGICAL_OR:
				ir.add(new LogicalOrIntermediate(pos, result, operandLeft, operandRight));
				break;
				
			case ASSIGN:
				ir.add(new AssignIntermediate(pos, result, operandRight));
				ir.add(new AssignIntermediate(pos, operandLeft, result));
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
					var arg = processExpression(expr);

					arg.ifLeftPresent(ir::addAll);
					
					return arg.getRight();
				})
				.toList();
			
			var function = processExpression(call.getTarget());
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
			
			var target = processExpression(cast.getTarget());
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
			
			var condition = processExpression(cond.getCondition());
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
			
			var whenTrue = processExpression(cond.getWhenTrue());
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

			var whenFalse = processExpression(cond.getWhenTrue());
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
			List<Intermediate> ir = new ArrayList<>();
			
			Operand result = temporary(mem.getType());
			
			var target = processExpression(mem.getTarget());
			target.ifLeftPresent(ir::addAll);

			String name = mem.getMember();
			
			StructType.Member member = mem
				.getTarget()
				.getType()
				.toStructLike()
				.getMember(name);
			
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
		
		else if(expression instanceof NumberLiteralExpressionNode lit)
			return Pair.ofRight(constant(
				lit.getLiteral(),
				lit.getType()
			));
		
		else if(expression instanceof UnaryExpressionNode unop) {
			List<Intermediate> ir = new ArrayList<>();
			
			Operand result = temporary(unop.getType());

			var target = processExpression(unop.getTarget());
			target.ifLeftPresent(ir::addAll);
			
			switch(unop.getOperation()) {
			case BITWISE_NOT:
				ir.add(new BitwiseNotIntermediate(pos, result, target.getRight()));
				break;

			case MINUS: // MINUS and SUBTRACT have the same symbol (-)
			case SUBTRACT:
				ir.add(new MinusIntermediate(pos, result, target.getRight()));
				break;

			case INDIRECTION: // INDIRECTION and MULTIPLY have the same symbol (*)
			case MULTIPLY:
				ir.add(new IndirectionIntermediate(pos, result, target.getRight()));
				break;

			case ADDRESS_OF: // ADDRESS_OF and BITWISE_AND have the same symbol (&)
			case BITWISE_AND:
				ir.add(new AddressOfIntermediate(pos, result, target.getRight()));
				break;
				
			default:
				Logger.error(expression, "Unrecognized unary expression type");
			}

			free(ir, target);

			return Pair.of(ir, result);
		}
		
		else if(expression instanceof VariableExpressionNode var)
			return Pair.ofRight(variable(var.getVariable()));
		
		Logger.error(expression, "Unrecognized expression type");
		return null;
	}
	
}
