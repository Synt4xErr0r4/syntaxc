/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c;

import at.syntaxerror.syntaxc.config.std.IStandard;
import lombok.Getter;

/**
 * 
 *
 * @author Thomas Kasper
 */
@Getter
public enum CStandard implements IStandard {

	C90("ISO C90", "c89", "c90", "iso9899:1990"),
	C95("ISO C90, Amendment 1", "c95", "iso9899:199409"),
	C99("ISO C99", "c99", "c9x", "iso9899:1999", "iso9899:199x"),
	C11("ISO C11", "c11", "c1x", "iso9899:2011"),
	C17("ISO C17", "c17", "c18", "iso9899:2017", "iso9899:2018"),
	C23("ISO C23", "c23", "c2x", "iso9899:2024"),
	
	GNU90("ISO C90, GNU dialect", "gnu89", "gnu90"),
	GNU99("ISO C99, GNU dialect", "gnu99", "gnu9x"),
	GNU11("ISO C11, GNU dialect", "gnu11", "gnu1x"),
	GNU17("ISO C17, GNU dialect", "gnu17", "gnu18"),
	GNU23("ISO C23, GNU dialect", "gnu23", "gnu2x"),
	
	SYN90("ISO C90, SyntaxC dialect", "syn89", "syn90"),
	SYN99("ISO C99, SyntaxC dialect", "syn99", "syn9x"),
	SYN11("ISO C11, SyntaxC dialect", "syn11", "syn1x"),
	SYN17("ISO C17, SyntaxC dialect", "syn17", "syn18"),
	SYN23("ISO C23, SyntaxC dialect", "syn23", "syn2x"),
	
	;
	
	private static final int ID_C90 = 0x01;
	private static final int ID_C95 = 0x02;
	private static final int ID_C99 = 0x03;
	private static final int ID_C11 = 0x04;
	private static final int ID_C17 = 0x05;
	private static final int ID_C23 = 0x06;
	private static final int ID_GNU = 0x10;
	private static final int ID_SYN = 0x20;
	
	private static final int ID_STD_MASK = 0x07;
	
	static {
		C90.id = ID_C90;
		C95.id = ID_C95;
		C99.id = ID_C99;
		C11.id = ID_C11;
		C17.id = ID_C17;
		C23.id = ID_C23;
		
		GNU90.id = ID_C95 | ID_GNU;
		GNU99.id = ID_C99 | ID_GNU;
		GNU11.id = ID_C11 | ID_GNU;
		GNU17.id = ID_C17 | ID_GNU;
		GNU23.id = ID_C23 | ID_GNU;
		
		SYN90.id = ID_C95 | ID_GNU | ID_SYN;
		SYN99.id = ID_C99 | ID_GNU | ID_SYN;
		SYN11.id = ID_C11 | ID_GNU | ID_SYN;
		SYN17.id = ID_C17 | ID_GNU | ID_SYN;
		SYN23.id = ID_C23 | ID_GNU | ID_SYN;
	}
	
	public static CStandard standard = C23;
	
	private int id;
	private final String[] aliases;
	private final String name;
	
	private CStandard(String name, String...aliases) {
		this.name = name;
		this.aliases = aliases;
	}
	
	@Override
	public void enable() {
		standard = this;
	}
	
	public static boolean atLeast(CStandard std) {
		return (standard.id & ID_STD_MASK) >= (std.id & ID_STD_MASK);
	}
	
	public static boolean atMost(CStandard std) {
		return (standard.id & ID_STD_MASK) <= (std.id & ID_STD_MASK);
	}
	
	public static boolean isGNU() {
		return (standard.id & ID_GNU) != 0;
	}
	
	public static boolean isSynExt() {
		return (standard.id & ID_SYN) != 0;
	}
	
	// <%, %>, <:, :>, %:, %:%:
	public static boolean supportsDigraphs() {
		return atLeast(C95);
	}

	// ??<, ??>, ??(, ??), ??=, ??/, ??', ??!, ??-
	public static boolean supportsTrigraphs() {
		return atMost(C17);
	}

	// void fun(a, b) int a; char b; { ... }
	public static boolean supportsOldStyleFunctions() {
		return atMost(C17);
	}
	
	// "A" L"B"
	public static boolean supportsMixedConcatenation() {
		return atMost(C17);
	}

	// _Decimal32, _Decimal64, _Decimal128
	public static boolean supportsDecimalFloat() {
		return atLeast(C23);
	}
	
	// _BitInt(n)
	public static boolean supportsBitInt() {
		return atLeast(C23);
	}

	// 0b10101
	public static boolean supportsBinaryLiterals() {
		return atLeast(C23) || isGNU();
	}
	
	// u8'a'
	public static boolean supportsU8Chars() {
		return atLeast(C23);
	}

	// 123'456'789
	public static boolean supportsDigitSeparator() {
		return atLeast(C23);
	}

	// 0x123.456p+789
	public static boolean supportsHexFloat() {
		return atLeast(C23) || isSynExt();
	}
	
	// ... = {};
	public static boolean supportsEmptyInit() {
		return atLeast(C23);
	}
	
	// [[attribute]]
	public static boolean supportsAttributes() {
		return atLeast(C23);
	}
	
	// int func(int, char) { ... }
	public static boolean supportsUnnamedParams() {
		return atLeast(C23);
	}
	
	// #elifdef, #elifndef 
	public static boolean supportsElifdef() {
		return atLeast(C23) || isSynExt();
	}
	
	// #warning
	public static boolean supportsWarning() {
		return atLeast(C23) || isSynExt();
	}
	
	// #embed
	public static boolean supportsEmbed() {
		return atLeast(C23) || isSynExt();
	}
	
	// nullptr, nullptr_t
	public static boolean supportsNullptr() {
		return atLeast(C23);
	}
	
	// bool, true, false (without <stdbool.h>)
	public static boolean supportsTrueFalse() {
		return atLeast(C23);
	}
	
	// label: } or label: declaration
	public static boolean supportsStandaloneLabels() {
		return atLeast(C23);
	}
	
	// _Static_assert(..., ...)
	public static boolean supportsStaticAssert() {
		return atLeast(C11);
	}

	// _Thread_local
	public static boolean supportsThreadLocal() {
		return atLeast(C11);
	}

	// static_assert(..., ...)
	public static boolean supportsNewStaticAssert() {
		return atLeast(C23) || (atLeast(C11) && isSynExt());
	}

	// thread_local
	public static boolean supportsNewThreadLocal() {
		return atLeast(C23) || (atLeast(C11) && isSynExt());
	}

	// static_assert(...)
	public static boolean supportsSingleArgumentStaticAssert() {
		return atLeast(C23) || (atLeast(C11) && isSynExt());
	}

	// universal character name: \\uXXXX, \\UXXXXXXXX
	public static boolean supportsUCN() {
		return atLeast(C99) || isSynExt();
	}
	
	// according to Annex D
	public static boolean supportsC99Identifiers() {
		return atLeast(C99) || isSynExt();
	}

	// according to Annex D (D.1, D.2)
	public static boolean supportsC11Identifiers() {
		return atLeast(C11) || isSynExt();
	}
	
	// Unicode properties XID_Start, XID_Continue
	public static boolean supportsC23Identifiers() {
		return atLeast(C23) || isSynExt();
	}

	// more than one codepoint in L'...', u8'...', u'...', U'...'
	public static boolean supportsMultichars() {
		return atMost(C17);
	}
	
	// u8'...'
	public static boolean supportsUTF8Chars() {
		return atLeast(C23);
	}
	
	// u8"..."
	public static boolean supportsUTF8Strings() {
		return atLeast(C11);
	}

	// u8'...' and u8"..."
	public static boolean supportsUTF16Strings() {
		return atLeast(C11);
	}

	// u8'...' and u8"..."
	public static boolean supportsUTF32Strings() {
		return atLeast(C11);
	}

	// »// comment«
	public static boolean supportsSingleLineComments() {
		return atLeast(C99);
	}
	
}
