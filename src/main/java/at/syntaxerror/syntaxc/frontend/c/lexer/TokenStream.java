/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.lexer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * 
 *
 * @author Thomas Kasper
 */
public class TokenStream {

	private static final Token.EOF EOF = new Token.EOF(null);
	
	private final List<Token> tokenCache;
	private final Supplier<Token> nextToken;
	
	private int tokenIndex;
	
	public TokenStream(Supplier<Token> nextToken) {
		this.nextToken = nextToken;
		this.tokenCache = new ArrayList<>();
	}

	public TokenStream(Collection<Token> token) {
		this.nextToken = null;
		this.tokenCache = new ArrayList<>(token);
	}
	
	public void back() {
		if(tokenIndex > 0)
			--tokenIndex;
	}
	
	public void append(Token... toks) {
		tokenCache.addAll(tokenIndex, List.of(toks));
	}

	public void append(List<Token> toks) {
		tokenCache.addAll(tokenIndex, toks);
	}
	
	public Token nextToken() {
		if(tokenIndex >= 0 && tokenIndex < tokenCache.size())
			return tokenCache.get(tokenIndex++);
		
		if(nextToken != null) {
			var tok = nextToken.get();
			
			tokenCache.add(tok);
			++tokenIndex;
			
			return tok;
		}
		
		return EOF;
	}
	
	public Token nextTokenSkipWS() {
		Token tok;
		
		while(true) {
			tok = nextToken();
			
			if(tok.is(TokenType.WHITESPACE, TokenType.EOL))
				continue;
			
			return tok;
		}
	}
	
	public int markToken() {
		return tokenIndex;
	}
	
	public void seekToken(int mark) {
		tokenIndex = mark;
	}
	
}
