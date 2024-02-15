/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c;

import java.util.function.Function;

import at.syntaxerror.syntaxc.config.ConfigValue;
import at.syntaxerror.syntaxc.config.flag.IFlag;
import at.syntaxerror.syntaxc.frontend.c.type.Types;
import at.syntaxerror.syntaxc.frontend.c.type.Types.CharSign;
import lombok.Getter;

/**
 * 
 *
 * @author Thomas Kasper
 */
@Getter
public enum CFlag implements IFlag, ConfigValue.ChangeListener {

	UNSIGNED_CHAR(
		"when enabled, »char« is unsigned (like »unsigned char«).\n »-fno-unsigned-char« is an alias for »-fsigned-char«",
		builder -> builder) {
		
		@Override
		public void onStateChange(boolean newState) {
			SIGNED_CHAR.cfg.setState(!newState);
			
			if(newState)
				Types.charSign = CharSign.UNSIGNED;
			else Types.charSign = CharSign.SIGNED;
		}
		
	},
	SIGNED_CHAR(
		"when enabled, »char« is signed (like »signed char«).\n »-fno-signed-char« is an alias for »-funsigned-char«",
		builder -> builder) {
		
		@Override
		public void onStateChange(boolean newState) {
			UNSIGNED_CHAR.cfg.setState(!newState);
			
			if(newState)
				Types.charSign = CharSign.SIGNED;
			else Types.charSign = CharSign.UNSIGNED;
		}
		
	},
	FLOATEVAL(
		"""
		specifies the floating-point evaluation method (cf. »FLT_EVAL_METHOD« in <float.h>):
		 - »runtime«  - evaluate all operations and constants just to the range and precision of the type. This is true for 
		                 both compile-time and runtime evaluation (FLT_EVAL_METHOD = 0)
		 - »double«   - evaluate operations and constants of type »float« and »double« to the range and precision of the
		                 »double« type, evaluate »long double« operations and constants to the range and precision of the
		                 »long double« type. This is true for both compile-time and runtime evaluation (FLT_EVAL_METHOD = 1)
		 - »ldouble«  - evaluate all operations and constants to the range and precision of the »long double« type. This is
		                 true for both compile-time and runtime evaluation (FLT_EVAL_METHOD = 2)
		 - »infinite« - evaluate all constant operations with infinite precision. This only affects compile-time operations
		                 (i.e., evaluation of constant expression). The runtime behavior (and thus »FLT_EVAL_METHOD«) can
		                 additional be changed one of the flags above
		
		By default, »runtime« mode is used for both runtime and compile-time operations.
		""",
		opts("runtime", "double", "ldouble", "infinite"),
		builder -> builder) {
		
		@Override
		public void onValueChange(String value) {
			
		}
		
	}
	;

	private static String[] opts(String...options) {
		return options;
	}
	
	private final ConfigValue cfg;
	private final String[] options;
	
	private CFlag(String description, String[] options, Function<ConfigValue.Builder, ConfigValue.Builder> builderConsumer) {
		this.options = options;
		
		var builder = ConfigValue.builder(CFrontend.INSTANCE, "flag")
			.description(description)
			.name(name().toLowerCase().replace('_', '-'));
		
		if(options != null)
			builder.requiredValue(String.join("|", options));
		
		if(builderConsumer != null)
			builder = builderConsumer.apply(builder);
			
		cfg = builder.get();
	}
	
	private CFlag(String description, String... options) {
		this(description, options, null);
	}
	
	private CFlag(String description, Function<ConfigValue.Builder, ConfigValue.Builder> builderConsumer) {
		this(description, null, builderConsumer);
	}
	
	private CFlag(String description) {
		this(description, null, null);
	}

	@Override
	public ConfigValue getConfigValue() {
		return cfg;
	}

	@Override
	public void onValueChange(String value) {
		if(options == null)
			return;
		
		for(String option : options)
			if(value.equalsIgnoreCase(option)) {
				onValueChange(option);
				return;
			}
		
		throw new IllegalArgumentException("Illegal value »" + value + "« for flag »" + cfg.getName() + "«");
	}
	
}
