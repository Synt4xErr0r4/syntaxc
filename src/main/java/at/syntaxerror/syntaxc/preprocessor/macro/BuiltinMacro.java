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
import at.syntaxerror.syntaxc.type.NumericType;

/**
 * @author Thomas Kasper
 * 
 */
public record BuiltinMacro(String name, Function<Token, List<Token>> function, boolean constant) implements Macro {
	
	private static final Token DUMMY = Token.ofUnparseable(new Position(0, 0, 0, 0, 0, null), 0);
	
	private static final Map<String, BuiltinMacro> BUILTIN_MACROS = new HashMap<>();
	
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
	
	public static Map<String, BuiltinMacro> getBuiltinMacros() {
		return Collections.unmodifiableMap(BUILTIN_MACROS);
	}

	public static void defineList(String name, Function<Token, List<Token>> function, boolean constant) {
		BUILTIN_MACROS.put(name, new BuiltinMacro(name, function, constant));
	}
	
	public static void defineList(String name, Function<Token, List<Token>> function) {
		defineList(name, function, false);
	}
	
	public static void defineToken(String name, Function<Token, Token> function, boolean constant) {
		defineList(name, self -> List.of(function.apply(self)), constant);
	}
	
	public static void defineToken(String name, Function<Token, Token> function) {
		defineList(name, self -> List.of(function.apply(self)), false);
	}
	
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
	
	public static void defineString(String name, String constant) {
		defineToken(
			name,
			self -> Token.ofString(
				self.getPosition(),
				constant,
				false
			),
			true
		);
	}
	
	public static void defineLong(String name, Function<Token, Long> function) {
		defineToken(
			name,
			self -> {
				BigInteger value = BigInteger.valueOf(function.apply(self));
				
				return Token.ofConstant(
					self.getPosition(),
					value,
					NumericType.SIGNED_INT
				).setRaw(value.toString());
			},
			false
		);
	}
	
	public static void defineLong(String name, long constant) {
		final BigInteger value = BigInteger.valueOf(constant);
		
		defineToken(
			name,
			self -> Token.ofConstant(
				self.getPosition(),
				value,
				NumericType.SIGNED_INT
			).setRaw(value.toString()),
			true
		);
	}
	
	public static void define(String name) {
		defineLong(name, 1L);
	}
	
	static {
		defineString("__FILE__", self -> self.getPosition().file().getOverriddenName());
		defineString("__DATE__", DATE);
		defineString("__TIME__", TIME);
		
		defineLong("__LINE__", self -> self.getPosition().file().getOverridenLineOffset() + self.getPosition().line());
		defineLong("__STDC__", 1L);
		
		// extension
		defineToken("__FUNCTION__", self -> Token.ofIdentifier(self.getPosition(), "__func__"), true);
		
		// https://sourceforge.net/p/predef/wiki/VersionNormalization/
		long versionFull =
			(SyntaxC.Version.MAJOR << 24)
			| (SyntaxC.Version.MINOR << 16)
			| SyntaxC.Version.PATCH;
		
		defineLong("__SYNTAXC__", versionFull);
		
		defineLong("__SYNTAXC_MAJOR__", SyntaxC.Version.MAJOR);
		defineLong("__SYNTAXC_MINOR__", SyntaxC.Version.MINOR);
		defineLong("__SYNTAXC_PATCH__", SyntaxC.Version.PATCH);
		defineString("__SYNTAXC_VERSION__", SyntaxC.Version.VERSION);
		
		defineLong("__ORDER_BIG_ENDIAN__", 4321);
		defineLong("__ORDER_LITTLE_ENDIAN__", 1234);
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
				.reduce(String::concat)
				.orElse("§8<empty>§r")
			: "§8<dynamic>§r";
	}

}
