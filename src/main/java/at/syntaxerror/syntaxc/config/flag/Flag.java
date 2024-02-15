/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.config.flag;

import java.nio.ByteOrder;
import java.util.function.Function;

import at.syntaxerror.syntaxc.config.ConfigValue;
import at.syntaxerror.syntaxc.io.CharSource;
import at.syntaxerror.syntaxc.io.Charsets;
import at.syntaxerror.syntaxc.log.ErrorStack;
import lombok.Getter;

/**
 * 
 *
 * @author Thomas Kasper
 */
@Getter
public enum Flag implements IFlag, ConfigValue.ChangeListener {

	CHARSET(
		"Specifies the charset of the input files",
		opts("utf-8", "utf-16", "utf-16le", "utf-16be", "utf-32", "utf-32le", "utf-32be"),
		b -> b.defaults("utf-8")) {

		@Override
		public void onValueChange(String value) {
			CharSource.defaultCharset = switch(value) {
			case "utf-16" -> ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? Charsets.UTF16LE : Charsets.UTF16BE;
			case "utf-16le" -> Charsets.UTF16LE;
			case "utf-16be" -> Charsets.UTF16BE;
			case "utf-32" -> ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? Charsets.UTF32LE : Charsets.UTF32BE;
			case "utf-32le" -> Charsets.UTF32LE;
			case "utf-32be" -> Charsets.UTF32BE;
			default -> Charsets.UTF8;
			};
		}
		
	},
	
	MAX_ERRORS(
		"Limits the number of emitted errors to n, stopping compilation when more errors occur. A value of 0 disables the limit",
		b -> b.defaults("0").requiredValue("n")) {
		
		@Override
		public void onValueChange(String value) {
			try {
				ErrorStack.maxErrors = Integer.parseUnsignedInt(value);
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Expected unsigned integer for flag »max-errors«");
			}
		}
		
	}
	
	;

	private static String[] opts(String...options) {
		return options;
	}
	
	private final ConfigValue cfg;
	private final String[] options;
	
	private Flag(String description, String[] options, Function<ConfigValue.Builder, ConfigValue.Builder> builderConsumer) {
		this.options = options;
		
		var builder = ConfigValue.builder(null, "flag")
			.description(description)
			.name(name().toLowerCase().replace('_', '-'));
		
		if(options != null)
			builder.requiredValue(String.join("|", options));
		
		if(builderConsumer != null)
			builder = builderConsumer.apply(builder);
			
		cfg = builder.get();
	}
	
	private Flag(String description, String... options) {
		this(description, options, null);
	}
	
	private Flag(String description, Function<ConfigValue.Builder, ConfigValue.Builder> builderConsumer) {
		this(description, null, builderConsumer);
	}
	
	private Flag(String description) {
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
