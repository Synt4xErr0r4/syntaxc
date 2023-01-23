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
package at.syntaxerror.syntaxc.preprocessor.macro;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import at.syntaxerror.syntaxc.SyntaxC;
import at.syntaxerror.syntaxc.lexer.Token;
import at.syntaxerror.syntaxc.preprocessor.Preprocessor;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.type.NumericValueType;

/**
 * This class handles built-in macros and allows creation of such.
 * 
 * @author Thomas Kasper
 * 
 */
public record BuiltinMacro(String name, Function<Token, List<Token>> function, boolean constant) implements Macro {
	
	private static final Token DUMMY = Token.ofUnparseable(Position.dummy(), 0);
	
	private static final Map<String, BuiltinMacro> BUILTIN_MACROS = new HashMap<>();
	
	// the values for the __DATE__ and __TIME__ macros, initialized in the static block below
	private static final String DATE, TIME;
	
	static {
		Calendar cal = Calendar.getInstance();
		
		int month = cal.get(Calendar.MONTH);
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int year = cal.get(Calendar.YEAR);
		
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
		
		String[] monthNames = {
			"Jan", "Feb", "Mar",
			"Apr", "May", "Jun",
			"Jul", "Aug", "Sep",
			"Oct", "Nov", "Dec"
		};
		
		DATE = "%s %s%d %04d".formatted(monthNames[month], day < 10 ? " " : "", day, year);
		TIME = "%02d:%02d:%02d".formatted(hour, minute, second);
	}
	
	/**
	 * Returns an unmodifiable view of all defined built-in macros.
	 * The returned map also reflects changes made afterwards.
	 * 
	 * @return all defined built-in macros
	 */
	public static Map<String, BuiltinMacro> getBuiltinMacros() {
		return Collections.unmodifiableMap(BUILTIN_MACROS);
	}

	/**
	 * Defines a macro expanding to multiple tokens.<br>
	 * Effectively equal to {@code #define name tokens}, where {@code tokens} is the result of invoking {@code function}.
	 * 
	 * @param name the name of the macro
	 * @param function a function accepting the token representing the macro, returning a list of substituted tokens
	 * @param constant whether the result of the function is constant
	 */
	public static void defineList(String name, Function<Token, List<Token>> function, boolean constant) {
		BUILTIN_MACROS.put(name, new BuiltinMacro(name, function, constant));
	}

	/**
	 * Defines a macro expanding to multiple tokens.<br>
	 * Effectively equal to {@code #define name tokens}, where {@code tokens} is the result of invoking {@code function}.
	 * 
	 * @param name the name of the macro
	 * @param function a function accepting the token representing the macro, returning a list of tokens to be substituted
	 */
	public static void defineList(String name, Function<Token, List<Token>> function) {
		defineList(name, function, false);
	}

	/**
	 * Defines a macro expanding to a single token.<br>
	 * Effectively equal to {@code #define name token}, where {@code token} is the result of invoking {@code function}.
	 * 
	 * @param name the name of the macro
	 * @param function a function accepting the token representing the macro, returning a single token to be substituted
	 * @param constant whether the result of the function is constant
	 */
	public static void defineToken(String name, Function<Token, Token> function, boolean constant) {
		defineList(name, self -> List.of(function.apply(self)), constant);
	}

	/**
	 * Defines a macro expanding to a single token.<br>
	 * Effectively equal to {@code #define name token}, where {@code token} is the result of invoking {@code function}.
	 * 
	 * @param name the name of the macro
	 * @param function a function accepting the token representing the macro, returning a single token to be substituted
	 */
	public static void defineToken(String name, Function<Token, Token> function) {
		defineList(name, self -> List.of(function.apply(self)), false);
	}

	/**
	 * Defines a macro expanding to a string.<br>
	 * Effectively equal to {@code #define name "value"}, where {@code "value"} is the result of invoking {@code function}.
	 * 
	 * @param name the name of the macro
	 * @param function a function accepting the token representing the macro, returning the string to be substituted
	 */
	public static void defineString(String name, Function<Token, String> function) {
		defineToken(
			name,
			self -> {
				String str = function.apply(self);
				
				return Token.ofString(
					self.getPosition(),
					str,
					false
				);
			},
			false
		);
	}

	/**
	 * Defines a macro expanding to a string.<br>
	 * Effectively equal to {@code #define name "value"}.
	 * 
	 * @param name the name of the macro
	 * @param value the string to be substituted
	 */
	public static void defineString(String name, String value) {
		defineToken(
			name,
			self -> Token.ofString(
				self.getPosition(),
				value,
				false
			),
			true
		);
	}

	/**
	 * Defines a macro expanding to a signed integer.<br>
	 * Effectively equal to {@code #define name value}, where {@code value} is the result of invoking {@code function}.
	 * 
	 * @param name the name of the macro
	 * @param function a function accepting the token representing the macro, returning the number to be substituted
	 */
	public static void defineNumber(String name, Function<Token, Number> function) {
		defineNumber(name, function, NumericValueType.SIGNED_INT);
	}

	/**
	 * Defines a macro expanding to a number of a given type.<br>
	 * Effectively equal to {@code #define name ((type) value)}, where {@code value} is the result of invoking {@code function}.
	 * 
	 * @param name the name of the macro
	 * @param function a function accepting the token representing the macro, returning the number to be substituted
	 * @param type the type of the number
	 */
	public static void defineNumber(String name, Function<Token, Number> function, NumericValueType type) {
		defineToken(
			name,
			self -> makeNumberToken(self, function.apply(self), type),
			false
		);
	}

	/**
	 * Defines a macro expanding to a signed integer.<br>
	 * Effectively equal to {@code #define name value}.
	 * 
	 * @param name the name of the macro
	 * @param value the number to be substituted
	 */
	public static void defineNumber(String name, Number value) {
		defineNumber(name, value, NumericValueType.SIGNED_INT);
	}

	/**
	 * Defines a macro expanding to a number of a given type.<br>
	 * Effectively equal to {@code #define name ((type) value)}.
	 * 
	 * @param name the name of the macro
	 * @param value the number to be substituted
	 * @param type the type of the number
	 */
	public static void defineNumber(String name, Number value, NumericValueType type) {
		final Number num;
		
		if(value instanceof BigInteger || value instanceof BigDecimal)
			num = value;
		
		else if(type.isFloating())
			num = BigDecimal.valueOf(value.doubleValue());
		
		else num = BigInteger.valueOf(value.longValue());
		
		defineToken(
			name,
			self -> makeNumberToken(self, num, type),
			true
		);
	}
	
	/**
	 * Creates a numeric token of the given {@code type}, with the given
	 * {@code value} at the position of the given {@code token},
	 * effectively aiming to replace {@code token} with the new token.
	 * 
	 * @param token the token to be replaced
	 * @param value the value of the new token
	 * @param type the type of the new token
	 * @return the new token
	 */
	private static Token makeNumberToken(Token token, Number value, NumericValueType type) {
		if(value instanceof BigInteger bigint)
			return Token.ofConstant(
				token.getPosition(),
				type.mask(bigint),
				type
			).setRaw(bigint.toString());
		
		if(value instanceof Integer || value instanceof Long)
			return makeNumberToken(token, BigInteger.valueOf(value.longValue()), type);
		
		BigDecimal bigdec = (BigDecimal) value;
		
		return Token.ofConstant(
			token.getPosition(),
			type.inRange(bigdec)
				? bigdec
				: bigdec.compareTo((BigDecimal) type.getMax()) > 0
					? (BigDecimal) type.getMax()
					: (BigDecimal) type.getMin(),
			type
		).setRaw(bigdec.toEngineeringString());
	}
	
	/**
	 * Defines a macro expanding to the signed integer of value {@code 1}.<br>
	 * Effectively equal to {@code #define name 1}.
	 * 
	 * @param name
	 */
	public static void define(String name) {
		defineNumber(name, 1L);
	}
	
	static {
		defineString("__FILE__", self -> self.getPosition().file().getOverriddenName());
		defineString("__DATE__", DATE);
		defineString("__TIME__", TIME);
		
		defineNumber("__LINE__", self -> self.getPosition().file().getOverridenLineOffset() + self.getPosition().line());
		defineNumber("__STDC__", 1L);
		
		// extension
		defineToken("__FUNCTION__", self -> Token.ofIdentifier(self.getPosition(), "__func__"), true);
		
		// https://sourceforge.net/p/predef/wiki/VersionNormalization/
		long versionFull =
			(SyntaxC.Version.MAJOR << 24)
			| (SyntaxC.Version.MINOR << 16)
			| SyntaxC.Version.PATCH;
		
		defineNumber("__SYNTAXC__", versionFull);
		
		defineNumber("__SYNTAXC_MAJOR__", SyntaxC.Version.MAJOR);
		defineNumber("__SYNTAXC_MINOR__", SyntaxC.Version.MINOR);
		defineNumber("__SYNTAXC_PATCH__", SyntaxC.Version.PATCH);
		defineString("__SYNTAXC_VERSION__", SyntaxC.Version.VERSION);
		
		defineNumber("__ORDER_BIG_ENDIAN__", 4321);
		defineNumber("__ORDER_LITTLE_ENDIAN__", 1234);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isFunction() {
		return false;
	}

	@Override
	public int getArgCount() {
		return 0;
	}

	@Override
	public LinkedHashMap<String, Token> getArgs() {
		return null;
	}
	
	@Override
	public List<Token> getBody() {
		return List.of();
	}

	@Override
	public List<Token> substitute(Preprocessor preprocessor, Token self, List<List<Token>> args) {
		return new ArrayList<>(function.apply(self));
	}
	
	@Override
	public Position getPosition() {
		return null;
	}
	
	@Override
	public String toString() {
		return constant
			? function.apply(DUMMY)
				.stream()
				.map(Token::getRaw)
				.map(s -> s + ' ')
				.reduce(String::concat)
				.map(String::strip)
				.orElse("§8<empty>§r")
			: "§8<dynamic>§r";
	}

}
