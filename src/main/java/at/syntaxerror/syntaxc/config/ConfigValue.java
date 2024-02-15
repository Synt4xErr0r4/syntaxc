/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.config;

import org.apache.commons.lang3.StringUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 
 *
 * @author Thomas Kasper
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class ConfigValue {
	
	public static Builder builder(IConfigContext context, String kindName) {
		return new Builder(context, kindName);
	}

	private final IConfigContext context;
	private final String kindName;
	private final String name;
	private final String description;
	
	private final boolean hasValue;
	private final boolean required;
	private final boolean prefixable;
	private final String valueName;
	private final String defaultValue;
	
	private final ChangeListener listener;
	
	private String value;
	private boolean enabled;
	
	public void setValueAndState(boolean newState, String newValue) {
		if(!prefixable)
			throw new IllegalArgumentException("Cannot use prefix »no-« together with value for " + kindName + " »" + name + "«");

		value = newValue;
		enabled = newState;
		
		if(listener != null) {
			listener.onValueAndStateChange(value, enabled);
			listener.onValueChange(value);
			listener.onStateChange(true);
		}
	}
	
	public void setValue(String newValue) {
		if(!hasValue)
			throw new IllegalArgumentException(StringUtils.capitalize(kindName) + " »" + name + "« does not accept values");
		
		value = newValue;
		enabled = true;
		
		if(listener != null) {
			listener.onValueChange(value);
			listener.onStateChange(true);
		}
	}
	
	public void setState(boolean newState) {
		if(required)
			throw new IllegalArgumentException(StringUtils.capitalize(kindName) + " »" + name + "« requires a value");
		
		value = defaultValue;
		enabled = newState;
		
		if(listener != null) {
			if(hasValue)
				listener.onValueChange(value);
			
			listener.onStateChange(enabled);
		}
	}
	
	public String toHelpString() {
		StringBuilder help = new StringBuilder();

		help.append(name);
		
		if(hasValue) {
			help.append('=');
			
			if(required)
				help.append('<');
			else help.append('[');
			
			help.append(valueName);

			if(required)
				help.append('>');
			else help.append(']');
		}
		
		help.append(" (default: ");
		
		if(hasValue)
			help.append(value);
		else help.append(enabled);
		
		return help.append(')').toString();
	}
	
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	@Accessors(fluent = true, chain = true)
	public static class Builder {

		private final IConfigContext context;
		private final String kindName;
		
		@Setter
		private String name;

		@Setter
		private String description;
		
		private boolean hasValue;
		private String valueName;
		private boolean required;
		
		@Setter
		private boolean prefixable;

		@Setter
		private String defaultValue;

		@Setter
		private ChangeListener listener;
		
		private String value;
		private boolean state;
		
		public Builder optionalValue(String valueName) {
			this.hasValue = true;
			this.valueName = valueName;
			this.required = false;
			return this;
		}
		
		public Builder requiredValue(String valueName) {
			this.hasValue = true;
			this.valueName = valueName;
			this.required = true;
			return this;
		}
		
		public Builder defaults(String value) {
			this.defaultValue = this.value = value;
			return this;
		}

		public Builder defaults(boolean state) {
			this.state = state;
			return this;
		}
		
		public ConfigValue get() {
			return new ConfigValue(context, kindName, name, description, hasValue, required, prefixable, valueName, defaultValue, listener, value, state);
		}
		
	}
	
	public static interface ChangeListener {
		
		default void onValueChange(String value) {}
		default void onStateChange(boolean state) {}
		default void onValueAndStateChange(String value, boolean state) {}
		
	}
	
}
