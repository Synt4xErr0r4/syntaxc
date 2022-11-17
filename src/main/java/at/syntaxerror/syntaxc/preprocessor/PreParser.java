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
package at.syntaxerror.syntaxc.preprocessor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxc.SyntaxC;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.logger.Logable;
import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.parser.ExpressionParser;
import at.syntaxerror.syntaxc.parser.node.expression.ExpressionNode;
import at.syntaxerror.syntaxc.symtab.SymbolTable;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.NumericValueType;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public class PreParser extends ExpressionParser implements Logable {

	private Preprocessor preprocessor;
	private boolean eol, failure;
	private List<Token> inserted;
	private int insertionIndex, mark;
	
	public PreParser(Preprocessor preprocessor) {
		super(null, null);
		this.preprocessor = preprocessor;
		inserted = new ArrayList<>();
	}
	
	@Override
	public boolean onBeforeSoftError() {
		if(failure || eol)
			return false;
		
		return failure = true;
	}
	
	@Override
	public void onAfterSoftError() {
		throw new AbortException();
	}
	
	@Override
	public Warning getDefaultWarning() {
		return Warning.SYN_NONE;
	}

	@Override
	public Position getPosition() {
		return preprocessor.getPosition();
	}
	
	@Override
	public SymbolTable getSymbolTable() {
		return null;
	}
	
	@Override
	public void reread() {
		throw new UnsupportedOperationException("reread is not supported by PreParser");
	}
	
	@Override
	public void markTokenState() {
		preprocessor.getInput().mark();
		mark = insertionIndex;
	}
	
	@Override
	public void unmarkTokenState() {
		preprocessor.getInput().unmark();
	}
	
	@Override
	public void resetTokenState() {
		preprocessor.getInput().reset();
		insertionIndex = mark;
	}

	@Override
	protected void sync() { }
	
	@Override
	public Token readNextToken() {
		if(eol || failure) return null;
		
		Token tok;
		
		if(inserted.size() > insertionIndex)
			tok = inserted.get(insertionIndex++);
		
		else tok = preprocessor.nextToken();
		
		eol = tok == null || tok.is(TokenType.NEWLINE);
		
		if(eol) return null;
		
		if(tok.is(TokenType.WHITESPACE))
			return readNextToken();
		
		if(tok.is(TokenType.IDENTIFIER, TokenType.NUMBER))
			return SyntaxC.postprocess(tok);
		
		if(tok.is(TokenType.CHARACTER))
			return Token.ofConstant(
				tok.getPosition(),
				tok.getInteger(),
				NumericValueType.SIGNED_INT
			);
		
		return tok;
	}
	
	private ExpressionNode nextDefined() {
		Positioned pos = current;
		
		boolean parenthesized = optional("(");
		
		Token macro = require(TokenType.IDENTIFIER);
		
		if(parenthesized)
			require(")");
		
		next();
		
		return ExpressionParser.newNumber(
			pos,
			preprocessor.resolveMacro(macro.getString()) == null
				? BigInteger.ZERO
				: BigInteger.ONE,
			Type.INT
		);
	}
	
	@Override
	public ExpressionNode nextPrimary() {
		if(equal(TokenType.IDENTIFIER)) {
			if(current.getString().equals("defined"))
				return nextDefined();
			
			/* If the identifier has been created as part of a macro expansion,
			 * it is guaranteed that does not denote another macro. Therefore it
			 * is treated as if it had a value of 0
			 */
			if(current.hasBeenExpanded()) {
				next();
				return ExpressionParser.newNumber(
					previous,
					BigInteger.ZERO,
					Type.INT
				);
			}

			inserted.addAll(preprocessor.substitute(current, true));
			
			next();

			return nextExpression();
		}
		
		if(equal(TokenType.STRING))
			softError(current, "Strings are not allowed inside preprocessor expressions");
		
		return super.nextPrimary();
	}
	
	@Override
	public ExpressionNode nextPostfix() {
		ExpressionNode expr = nextPrimary();
		
		if(optional("[", "(", ".", "->", "++", "--"))
			softError(current, "»%s« is not allowed inside preprocessor expressions", current.getPunctuator());
		
		return expr;
	}
	
	@Override
	public ExpressionNode nextUnary() {
		if(equal("*", "&"))
			softError(current, "Missing left operand for »%s«", current.getPunctuator());
		
		if(equal("++", "--"))
			softError(current, "»%s« is not allowed inside preprocessor expressions", current.getPunctuator());
		
		return super.nextUnary();
	}
	
	@Override
	public ExpressionNode nextCast() {
		return nextUnary(); // casts are illegal here
	}
	
	@Override
	public ExpressionNode nextAssignment() {
		if(equal("=", "*=", "/=", "%=", "+=", "-=", "<<=", ">>=", "&=", "^=", "|="))
			softError(current, "»%s« is not allowed inside preprocessor expressions", current.getPunctuator());
		
		return nextConditional();
	}
	
	public boolean evaluate() {
		eol = failure = false;
		
		next();
		
		try {
			return nextIntegerConstantExpression()
				.compareTo(BigInteger.ZERO) != 0;
		} catch (AbortException e) {
			return false;
		}
	}

	/**
	 * This exception is thrown whenever a soft error occurs within this class.
	 * Its sole purpose is to abort the evaluation of the expression (and treat it as {@code false})
	 * 
	 * @author Thomas Kasper
	 */
	@SuppressWarnings("serial")
	private static class AbortException extends RuntimeException {
		
	}
	
}
