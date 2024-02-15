/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.lexer;

import java.util.List;
import java.util.function.Function;

import at.syntaxerror.syntaxc.frontend.c.CStandard;
import at.syntaxerror.syntaxc.frontend.c.string.CUnicode;
import at.syntaxerror.syntaxc.io.CharSource;
import at.syntaxerror.syntaxc.log.LogableException;

/**
 * 
 *
 * @author Thomas Kasper
 */
public class PreLexer extends CommonLexer {
	
	private static final List<Function<PreLexer, Token>> GENERATORS = List.of(
		PreLexer::nextWhitespace,
		PreLexer::nextEOL,
		PreLexer::nextCharacter,
		PreLexer::nextString,
		PreLexer::nextIdentifier,
		PreLexer::nextNumber,
		PreLexer::nextPunctuator,
		PreLexer::nextEOF
	);
	
	public PreLexer(CharSource source) {
		super(source);
	}
	
	public Token nextToken() {
		var mark = mark();
		
		for(var gen : GENERATORS)
			try {
				return gen.apply(this);
			}
			catch (LogableException e) {
				if(!e.isRecoverable())
					throw e;
				
				mark.seek();
			}
		
		return nextUnparseable();
	}
	
	public Token.PPNumber nextNumber() {
		var last = startToken();
		
		int c = next();
		
		if(c == '.')
			c = next();
		
		if(!CUnicode.isDecimalDigit(c))
			throw error("expected preprocessing number").recoverable();
		
		while(true) {
			var mark = mark();
			c = next();
			
			if((CStandard.supportsHexFloat() && (c == 'p' || c == 'P')) || (c == 'e' || c == 'E')) {
				var reset = mark();
				int n = next();
				
				if(n == '+' || n == '-') {
					last = reset;
					continue;
				}
				
				reset.seek();
			}
			
			if(CStandard.supportsDigitSeparator() && c == '\'') {
				last = mark;
				continue;
			}
			
			if(CUnicode.isIdentifierChar(c, false) || c == '.') {
				last = mark;
				continue;
			}

			mark.seek();
			break;
		}
		
		this.last = last;
		
		return new Token.PPNumber(endToken(), raw);
	}

}
