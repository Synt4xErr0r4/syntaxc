/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c;

import java.util.function.Function;

import at.syntaxerror.syntaxc.config.ConfigValue;
import at.syntaxerror.syntaxc.config.warning.IWarning;
import at.syntaxerror.syntaxc.config.warning.WarningLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 *
 * @author Thomas Kasper
 */
@Getter
public enum CWarning implements IWarning, ConfigValue.ChangeListener {

	TRIGRAPHS("Warns when encountering trigraphs"),
	MULTICHAR("Warns when a character literal contains multiple characters")
	;
	
	static {
		TRIGRAPHS.enable();
		MULTICHAR.enable();
	}
	
	@Setter
	private WarningLevel level;
	
	private final ConfigValue cfg;
	private final String[] options;
	
	private CWarning(String description, String[] options, Function<ConfigValue.Builder, ConfigValue.Builder> builderConsumer) {
		this.options = options;
		
		var builder = ConfigValue.builder(CFrontend.INSTANCE, "warning")
			.description(description)
			.name(name().toLowerCase().replace('_', '-'));
		
		if(options != null)
			builder.requiredValue(String.join("|", options));
		
		if(builderConsumer != null)
			builder = builderConsumer.apply(builder);
			
		cfg = builder.get();
	}
	
	private CWarning(String description, String... options) {
		this(description, options, null);
	}
	
	private CWarning(String description, Function<ConfigValue.Builder, ConfigValue.Builder> builderConsumer) {
		this(description, null, builderConsumer);
	}
	
	private CWarning(String description) {
		this(description, null, null);
	}

	public void enable() {
		getConfigValue().setState(true);
	}

	public void disable() {
		getConfigValue().setState(false);
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
		
		throw new IllegalArgumentException("Illegal value »" + value + "« for warning »" + cfg.getName() + "«");
	}

}
