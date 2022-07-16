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
package at.syntaxerror.syntaxc.parser.node;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import at.syntaxerror.syntaxc.lexer.Keyword;
import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.misc.StringUtils;
import at.syntaxerror.syntaxc.parser.tree.TreeNode;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public abstract class Node implements Positioned, TreeNode {
	
	@Override
	public String getLeafName() {
		return TreeNode.super.getLeafName().replaceFirst("Node$", "");
	}
	
	public static Pair<String, TreeNode> child(String name, TreeNode node) {
		return Pair.of(name, node);
	}

	public static Pair<String, TreeNode> child(String name, List<? extends TreeNode> list) {
		AtomicInteger index = new AtomicInteger();
		
		return child(
			name,
			new TreeListNode(
				list.stream()
					.map(node -> child("#" + index.getAndIncrement(), node))
					.toList()
			)
		);
	}

	public static Pair<String, TreeNode> child(String name, Type type) {
		return child(name, type.toString());
	}
	
	public static Pair<String, TreeNode> child(String name, String string) {
		return child(name, new TreeStringNode(string));
	}

	public static Pair<String, TreeNode> child(String name, boolean bool) {
		return child(name, new TreeStringNode(Boolean.toString(bool)));
	}

	public static Pair<String, TreeNode> child(String name, Number num, NumericValueType type) {
		return child(name, new TreeStringNode(type + " " + num));
	}

	public static Pair<String, TreeNode> child(String name, Token token) {
		String strval;
		
		switch(token.getType()) {
		case IDENTIFIER:
			strval = '»' + token.getString() + '«';
			break;
			
		case STRING:
			strval = '"' + StringUtils.quote(token.getString()) + '"';
			break;
			
		case PUNCTUATOR:
			return child(name, token.getPunctuator());
			
		case KEYWORD:
			return child(name, token.getKeyword());
		
		case CONSTANT:
			return child(
				name,
				token.getNumericType().isFloating()
					? token.getDecimal()
					: token.getInteger(),
				token.getNumericType()
			);
		
		default:
			strval = "?";
			break;
		}
		
		return child(name, strval);
	}

	public static Pair<String, TreeNode> child(String name, Punctuator punct) {
		return child(name, '»' + punct.getName() + '«');
	}

	public static Pair<String, TreeNode> child(String name, Keyword keyword) {
		return child(name, keyword.getName());
	}
	
	private static record TreeListNode(List<Pair<String, TreeNode>> list) implements TreeNode {
		
		@Override
		public String getLeafName() {
			return "";
		}
		
		@Override
		public List<Pair<String, TreeNode>> getChildren() {
			return list;
		}
		
	}
	
	private static record TreeStringNode(String name) implements TreeNode {
		
		@Override
		public String getLeafName() {
			return name;
		}
		
	}
	
}
