/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.config.warning;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import at.syntaxerror.syntaxc.config.ConfigRegistry;
import at.syntaxerror.syntaxc.config.ConfigValue;
import at.syntaxerror.syntaxc.config.ConfigValue.ChangeListener;
import lombok.experimental.UtilityClass;

/**
 * 
 *
 * @author Thomas Kasper
 */
@UtilityClass
public class WarningGroups {

	public static final List<IWarning> EXTRA = new ArrayList<>();
	
	static {
		create(
			builder -> builder.name("all").description("Enables almost all warnings"),
			state -> {
				if(!state)
					throw new IllegalArgumentException("Unknown warning »no-all«");
				
				ConfigRegistry.WARNINGS.getEntries().values()
					.stream()
					.filter(Predicate.not(EXTRA::contains))
					.forEach(warning -> warning.getConfigValue().setState(true));
			}
		);
		
		create(
			builder -> builder.name("none").description("Disables all warnings"),
			state -> {
				if(!state)
					throw new IllegalArgumentException("Unknown warning »no-none«");
				
				ConfigRegistry.WARNINGS.getEntries().values()
					.stream()
					.forEach(warning -> warning.getConfigValue().setState(false));
			}
		);
		
		create(
			builder -> builder.name("extra"),
			state -> {
				EXTRA.forEach(warning -> warning.getConfigValue().setState(state));
			}
		);
		
		create(
			builder -> builder.name("fatal-errors"),
			state -> {
				if(!state)
					throw new IllegalArgumentException("Unknown warning »no-fatal-errors«");
				
				var level = state ? WarningLevel.FATAL_ERROR : WarningLevel.NORMAL;
				
				ConfigRegistry.WARNINGS.getEntries().values()
					.stream()
					.forEach(warning -> warning.setLevel(level));
			}
		);
		
		create(
			builder -> builder.name("error").optionalValue("error"),
			state -> {
				var level = state ? WarningLevel.ERROR : WarningLevel.NORMAL;
				
				ConfigRegistry.WARNINGS.getEntries().values()
					.stream()
					.forEach(warning -> warning.setLevel(level));
			},
			(value, state) -> {
				var level = state ? WarningLevel.ERROR : WarningLevel.NORMAL;
				
				IWarning warning = ConfigRegistry.WARNINGS.getEntries().get(value);
				
				if(warning == null || warning instanceof Group)
					throw new IllegalArgumentException("Unknown warning »" + value + "«");
				
				warning.setLevel(level);
				
				if(state)
					warning.getConfigValue().setState(true);
			}
		);
	}
	
	private static IWarning create(Function<ConfigValue.Builder, ConfigValue.Builder> cfg, Consumer<Boolean> action) {
		return create(cfg, action);
	}

	private static IWarning create(Function<ConfigValue.Builder, ConfigValue.Builder> cfg, Consumer<Boolean> stateAction, BiConsumer<String, Boolean> valueStateAction) {
		ConfigValue val = cfg.apply(ConfigValue.builder(null, "warning group"))
			.listener(new ChangeListener() {
				
				@Override
				public void onValueChange(String value) {}
				
				@Override
				public void onStateChange(boolean state) {
					if(stateAction != null)
						stateAction.accept(state);
				}
				
				@Override
				public void onValueAndStateChange(String value, boolean state) {
					if(valueStateAction != null)
						valueStateAction.accept(value, state);
				}
				
			}).get();
		
		Group group = new Group(val);
		
		ConfigRegistry.WARNINGS.addAll(group);
		
		return group;
	}
	
	private static record Group(ConfigValue cfg) implements IWarning {

		@Override
		public WarningLevel getLevel() {
			return null;
		}

		@Override
		public void setLevel(WarningLevel level) {}

		@Override
		public ConfigValue getConfigValue() {
			return cfg;
		}
		
	}
	
}
