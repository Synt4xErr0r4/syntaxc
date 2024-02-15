/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.preproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxc.frontend.c.lexer.PreLexer;
import at.syntaxerror.syntaxc.frontend.c.lexer.Token;
import at.syntaxerror.syntaxc.frontend.c.lexer.TokenStream;
import at.syntaxerror.syntaxc.frontend.c.lexer.TokenType;
import at.syntaxerror.syntaxc.log.Logger;
import lombok.experimental.Delegate;

/**
 * 
 *
 * @author Thomas Kasper
 */
public class Preprocessor {
	
	private static final Map<String, IDirective> DIRECTIVES = new HashMap<>();

	static {
		DIRECTIVES.put("if", null);
		DIRECTIVES.put("ifdef", null);
		DIRECTIVES.put("ifndef", null);

		DIRECTIVES.put("elif", null);
		DIRECTIVES.put("elifdef", null);
		DIRECTIVES.put("elifndef", null);

		DIRECTIVES.put("else", null);
		DIRECTIVES.put("endif", null);

		DIRECTIVES.put("include", null);
		DIRECTIVES.put("embed", null);
		
		DIRECTIVES.put("define", null);
		DIRECTIVES.put("undef", null);
		
		DIRECTIVES.put("line", null);
		DIRECTIVES.put("error", null);
		DIRECTIVES.put("warning", null);
		DIRECTIVES.put("pragma", null);
	}
	
	private final PreLexer lexer;
	
	@Delegate(types = TokenStream.class)
	private final TokenStream tokenStream;
	
	public Preprocessor(PreLexer lexer) {
		this.lexer = lexer;
		tokenStream = new TokenStream(lexer::nextToken);
	}
	
	private boolean maybeExpand(Token tok) {
		if(tok instanceof Token.Identifier ident) {
			// TODO
			return false;
		}
		
		return false;
	}
	
	public Token nextExpandedToken() {
		Token tok = nextToken();
		
		if(maybeExpand(tok))
			return nextToken();
		
		return tok;
	}
	
	public Token nextExpandedTokenSkipWS() {
		Token tok = nextTokenSkipWS();

		if(maybeExpand(tok))
			return nextTokenSkipWS();
		
		return tok;
	}
	
	public List<Token> preprocess() {
		List<Token> tokens = new ArrayList<>();
		
		boolean newline = true;
		
		while(true) {
			Token tok = nextToken();
			
			if(tok.is(TokenType.EOF))
				break;

			if(tok.is(TokenType.WHITESPACE))
				continue;
			
			if(tok.is(TokenType.EOL)) {
				newline = true;
				continue;
			}
			
			if(newline && tok.is("#")) {
				// directive
				List<Token> directiveTokens = new ArrayList<>();
				
				while(true) {
					tok = nextToken();
					
					if(tok.is(TokenType.WHITESPACE) && directiveTokens.isEmpty())
						continue;
					
					if(tok.is(TokenType.EOL))
						break;
					
					if(tok.is(TokenType.EOF)) {
						Logger.warn(null, "missing new-line at end of file");
						break;
					}
					
					directiveTokens.add(tok);
				}
				
				if(directiveTokens.isEmpty())
					continue;
				
				tok = directiveTokens.removeFirst();
				
				if(tok instanceof Token.Identifier ident) {
					String value = ident.value().toString();
					
					IDirective directive = DIRECTIVES.get(value);
					
					if(directive != null && directive.isSupported()) {
						directive.process(ident, directiveTokens);
						continue;
					}
				}
				
				Logger.warn(tok, "unknown preprocessing directive");
				continue;
			}
			
			newline = false;
			
			if(tok instanceof Token.Identifier ident) {
				String value = ident.value().toString();
				
				if(value.equals("_Pragma")) {
					parsePragma(tok);
					continue;
				}

				if(maybeExpand(tok))
					continue;
			}
			
			tokens.add(tok);
		}
		
		return tokens;
	}
	
	private void parsePragma(Token tok) {
		int mark = markToken();
		Token lparen = nextExpandedTokenSkipWS();
		
		if(!lparen.is("(")) {
			Logger.error(tok, "expected »{l}« for {l} expression", "(", "_Pragma");
			seekToken(mark);
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		boolean hasString = false;
		
		while(true) {
			Token pragma = nextExpandedTokenSkipWS();

			if(pragma instanceof Token.StringLiteral string) {
				hasString = true;
				sb.append(string.string().toString());
				continue;
			}
			
			if(!pragma.is(")")) {
				Logger.error(pragma, "expected »{l}« for {l} expression", ")", "_Pragma");
				seekToken(mark);
				return;
			}
			
			break;
		}
		
		if(!hasString) {
			Logger.error(tok, "expected string literal for {l} expression", "_Pragma");
			seekToken(mark);
		}
		
		/*
		 * standard pragmas:
		 *  - "STDC FP_CONTRACT <ON|OFF>"
		 *  - "STDC FENV_ACCESS <ON|OFF>"
		 *  - "STDC FENV_DEC_ROUND <FE_DEC_DOWNWARD|FE_DEC_TONEAREST|FE_DEC_TONEARESTFROMZERO|FE_DEC_TOWARDZERO|FE_DEC_UPWARD|FE_DEC_DYNAMIC>"
		 *  - "STDC FENV_ROUND <FE_DOWNWARD|FE_TONEAREST|FE_TONEARESTFROMZERO|FE_TOWARDZERO|FE_UPWARD|FE_DYNAMIC>"
		 *  - "STDC CX_LIMITED_RANGE <ON|OFF>"
		 * 
		 * SyntaxC extensions:
		 *  - "once"
		 */
		
		// TODO
		System.out.println("_Pragma ( " + sb + " )");
	}
	
}
