/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.lexer;

import org.apache.commons.lang3.StringUtils;

import at.syntaxerror.syntaxc.frontend.c.CStandard;

/**
 * 
 *
 * @author Thomas Kasper
 */
public enum KeywordType {

	AUTO,
	BREAK,
	CASE,
	CHAR,
	CONST,
	CONTINUE,
	DEFAULT,
	DO,
	DOUBLE,
	ELSE,
	ENUM,
	EXTERN,
	FLOAT,
	FOR,
	GOTO,
	IF,
	INLINE,
	INT,
	LONG,
	REGISTER,
	RETURN,
	SHORT,
	SIGNED,
	SIZEOF,
	STATIC,
	STRUCT,
	SWITCH,
	TYPEDEF,
	UNION,
	UNSIGNED,
	VOID,
	VOLATILE,
	WHILE,

	// since C99
	RESTRICT(CStandard.C99),
	_BOOL("bool", CStandard.C99, CStandard.C23),
	_COMPLEX(CStandard.C99),
	_IMAGINARY(CStandard.C99),

	// since C11
	_ALIGNAS("alignas", CStandard.C11, CStandard.C23),
	_ALIGNOF("alignof", CStandard.C11, CStandard.C23),
	_GENERIC(CStandard.C11),
	_NORETURN(CStandard.C11),
	_STATIC_ASSERT("static_assert", CStandard.C11, CStandard.C23),
	_THREAD_LOCAL("thread_local", CStandard.C11, CStandard.C23),

	// since C23
	CONSTEXPR(CStandard.C23),
	FALSE(CStandard.C23),
	NULLPTR(CStandard.C23),
	TRUE(CStandard.C23),
	TYPEOF(CStandard.C23),
	TYPEOF_UNQUAL(CStandard.C23),
	_ATOMIC(CStandard.C11),
	_BITINT(CStandard.C23),
	_DECIMAL128(CStandard.C23),
	_DECIMAL32(CStandard.C23),
	_DECIMAL64(CStandard.C23),
	;
	
	private final String name;
	private final String alias;
	private final CStandard standard;
	private final CStandard aliasStandard;
	
	private KeywordType(String alias, CStandard standard, CStandard aliasStandard) {
		String name = name().toLowerCase();
		
		if(name.equals("_BITINT"))
			name = "_BitInt";
		else if(name.charAt(0) == '_')
			name = "_" + StringUtils.capitalize(name.substring(1));
		
		this.name = name;
		this.alias = alias;
		this.standard = standard;
		this.aliasStandard = aliasStandard;
	}
	
	private KeywordType(String alias, CStandard standard) {
		this(alias, standard, null);
	}

	private KeywordType(CStandard standard) {
		this(null, standard, null);
	}
	
	private KeywordType(String alias) {
		this(alias, null, null);
	}
	
	private KeywordType() {
		this(null, null, null);
	}
	
	public boolean isEqual(Object other) {
		if(other == this)
			return true;
		
		if(standard != null && !CStandard.atLeast(standard))
			return false;
		
		if(other instanceof String str) {
			if(str.equals(name))
				return true;

			if(aliasStandard != null && !CStandard.atLeast(aliasStandard))
				return false;
			
			return str.equals(alias);
		}
		
		return false;
	}
	
}
