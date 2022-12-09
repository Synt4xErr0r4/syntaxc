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
package at.syntaxerror.syntaxc.builtin;

import static at.syntaxerror.syntaxc.parser.tree.SyntaxTreeNode.child;

import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.intermediate.operand.Operand;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.parser.tree.SyntaxTreeNode;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.Type;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor
@Getter
public abstract class BuiltinFunction {

	private final String name;
	
	protected final List<BuiltinArgument> args = new ArrayList<>();
	
	protected Type returnType = Type.VOID;
	protected Number returnValue = null;
	protected boolean inline = false;
	
	public abstract void populate(BuiltinContext context);
	
	public static interface BuiltinArgument extends Positioned, SyntaxTreeNode {
		
	}
	
	@RequiredArgsConstructor
	@Getter
	public static class ExpressionArgument implements BuiltinArgument {
		
		private final ExpressionNode expression;
		
		@Setter
		private Operand operand;
		
		public Type getType() {
			return expression.getType();
		}
		
		@Override
		public Position getPosition() {
			return expression.getPosition();
		}
		
		@Override
		public String toString() {
			return operand == null
				? "<null>"
				: operand.toString();
		}
		
		@Override
		public List<Pair<String, SyntaxTreeNode>> getChildren() {
			return List.of(
				child("expression", expression)
			);
		}
		
	}

	public static record TypeArgument(Position position, Type type) implements BuiltinArgument {
		
		@Override
		public Position getPosition() {
			return position;
		}
		
		@Override
		public String toString() {
			return type.toString();
		}
		
		@Override
		public List<Pair<String, SyntaxTreeNode>> getChildren() {
			return List.of(
				child("type", type)
			);
		}
		
	}

	public static record IdentifierArgument(Token identifier) implements BuiltinArgument {
		
		@Override
		public Position getPosition() {
			return identifier.getPosition();
		}
		
		@Override
		public String toString() {
			return identifier.getString();
		}
		
		@Override
		public List<Pair<String, SyntaxTreeNode>> getChildren() {
			return List.of(
				child("identifier", identifier.getString())
			);
		}
		
	}
	
}
