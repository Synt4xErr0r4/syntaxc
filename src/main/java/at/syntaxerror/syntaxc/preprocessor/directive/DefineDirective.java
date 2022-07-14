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
package at.syntaxerror.syntaxc.preprocessor.directive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import at.syntaxerror.syntaxc.lexer.Punctuator;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.lexer.TokenType;
import at.syntaxerror.syntaxc.preprocessor.Preprocessor;
import at.syntaxerror.syntaxc.preprocessor.macro.FunctionMacro;
import at.syntaxerror.syntaxc.preprocessor.macro.Macro;
import at.syntaxerror.syntaxc.preprocessor.macro.StandardMacro;

/**
 * @author Thomas Kasper
 * 
 */
public class DefineDirective extends Directive {

	private boolean undef;
	
	public DefineDirective(Preprocessor preprocessor, Token self, boolean undef) {
		super(preprocessor, self);
		this.undef = undef;
	}
	
	private Token getMacroName() {
		Token tok = nextToken();
		
		if(tok == null)
			error("Missing macro name for »%s« preprocessing directive", getName());
		
		if(tok.is(TokenType.NEWLINE)) {
			error(getPrevious(), "Expected identifier for »%s« directive", getName());
			return null;
		}
		
		if(!tok.is(TokenType.IDENTIFIER)) {
			error(tok, "Expected identifier for »%s« directive", getName());
			return null;
		}
		
		return tok;
	}

	@Override
	public void processSimple() {
		Token name = getMacroName();
		
		if(undef) {
			undefineMacro(name);
			skipTrailing(true);
			return;
		}

		
		Token tok = nextTokenRaw();
		
		// empty replacement list
		if(tok == null || tok.is(TokenType.NEWLINE)) {
			defineMacro(
				new StandardMacro(
					name,
					List.of()
				)
			);
			return;
		}
		
		LinkedHashMap<String, Token> args = null;
		List<Token> body = new ArrayList<>();
		
		boolean function = tok.is(Punctuator.LPAREN);
		
		if(function) { // function-like macro
			args = new LinkedHashMap<>();
			
			boolean first = true;
			
			while(true) {
				tok = nextToken();

				// missing ')'
				if(tok == null || tok.is(TokenType.NEWLINE))
					error(getPrevious(), "Unclosed argument list for »define« preprocessing directive");
				
				// empty argument list
				if(first && tok.is(Punctuator.RPAREN))
					break;
				
				first = false;
				
				// parameter name needs to be an identifier
				if(!tok.is(TokenType.IDENTIFIER))
					error(tok, "Expected identifier in argument list for »define« preprocessing directive");
				
				String argName = tok.getString();
				
				// check if parameter name was already declared
				if(args.containsKey(argName))
					error(tok, "Duplicate parameter for function-like macro");
				
				args.put(argName, tok);
				
				tok = nextToken();
				
				// missing ')'
				if(tok == null || tok.is(TokenType.NEWLINE))
					error(getPrevious(), "Unclosed argument list for »define« preprocessing directive");
				
				// ')' terminates the parameter list
				if(tok.is(Punctuator.RPAREN))
					break;
				
				// ')' or ',' is required after parameter name
				if(!tok.is(Punctuator.COMMA))
					error(getPrevious(), "Expected »)« after argument list for »define« preprocessing directive");
			}
		}
		
		else if(!tok.is(TokenType.WHITESPACE))
			body.add(tok);
		
		boolean stringify = false;
		
		while(true) { // macro replacement list
			tok = nextTokenRaw();

			// stop at new-line or end-of-file
			if(tok == null || tok.is(TokenType.NEWLINE))
				break;
			
			// skip whitespace after '#' and at the start of the replacement list
			if(tok.is(TokenType.WHITESPACE) && (body.isEmpty() || stringify))
				continue;
			
			// '#' can only precede parameter names
			if(stringify && (!tok.is(TokenType.IDENTIFIER) || !args.containsKey(tok.getString())))
				error(tok, "»#« is not followed by macro parameter name");
			
			stringify = false;
			
			if(tok.is(Punctuator.HASH)) {
				if(!function)
					error(tok, "»#« cannot appear outside of function-like macros");
				
				stringify = true;
			}
			
			body.add(tok);
		}
		
		// check for trailing '#'
		if(stringify)
			error(tok, "»#« cannot appear at end of replacement list");

		int idx = body.size() - 1;
		
		// remove trailing whitespace
		while(!body.isEmpty() && body.get(idx).is(TokenType.WHITESPACE))
			body.remove(idx--);
		
		// check for leading/trailing '##'
		if(!body.isEmpty()) {
			tok = body.get(0);
			
			if(tok.is(Punctuator.DOUBLE_HASH))
				error(tok, "»##« cannot appear at begin of replacement list");
			
			tok = body.get(body.size() - 1);

			if(tok.is(Punctuator.DOUBLE_HASH))
				error(tok, "»##« cannot appear at end of replacement list");
		}
		
		Macro macro;
		
		if(function)
			macro = new FunctionMacro(name, args, body);
		else macro = new StandardMacro(name, body);
		
		defineMacro(macro);
	}

}
