/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.config.warning;

import java.util.function.Function;

import at.syntaxerror.syntaxc.config.ConfigValue;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 *
 * @author Thomas Kasper
 */
@Getter
public enum Warning implements IWarning, ConfigValue.ChangeListener {

	;

	@Setter
	private WarningLevel level;
	
	private final ConfigValue cfg;
	private final String[] options;
	
	private Warning(String description, String[] options, Function<ConfigValue.Builder, ConfigValue.Builder> builderConsumer) {
		this.options = options;
		
		var builder = ConfigValue.builder(null, "warning")
			.description(description)
			.name(name().toLowerCase().replace('_', '-'));
		
		if(options != null)
			builder.requiredValue(String.join("|", options));
		
		if(builderConsumer != null)
			builder = builderConsumer.apply(builder);
			
		cfg = builder.get();
	}
	
	private Warning(String description, String... options) {
		this(description, options, null);
	}
	
	private Warning(String description, Function<ConfigValue.Builder, ConfigValue.Builder> builderConsumer) {
		this(description, null, builderConsumer);
	}
	
	private Warning(String description) {
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
		
		throw new IllegalArgumentException("Illegal value »" + value + "« for warning »" + cfg.getName() + "«");
	}

}
