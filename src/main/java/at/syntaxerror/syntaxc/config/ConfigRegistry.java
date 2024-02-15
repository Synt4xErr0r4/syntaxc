/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import at.syntaxerror.syntaxc.config.flag.Flag;
import at.syntaxerror.syntaxc.config.flag.IFlag;
import at.syntaxerror.syntaxc.config.machineopt.IMachineOption;
import at.syntaxerror.syntaxc.config.warning.IWarning;
import at.syntaxerror.syntaxc.config.warning.Warning;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 *
 * @author Thomas Kasper
 */
@RequiredArgsConstructor
public class ConfigRegistry<T extends IConfigurable> {

	public static final ConfigRegistry<IFlag> FLAGS = new ConfigRegistry<>("flag");
	public static final ConfigRegistry<IWarning> WARNINGS = new ConfigRegistry<>("warning");
	public static final ConfigRegistry<IMachineOption> MACHINE_OPTIONS = new ConfigRegistry<>("machine-dependent option");
	
	static {
		FLAGS.addAll(Flag.class);
		WARNINGS.addAll(Warning.class);
	}
	
	private final Map<String, T> entries = new LinkedHashMap<>();
	
	@Getter
	private final String name;
	
	public void addAll(Class<? extends T> enumClass) {
		if(!enumClass.isEnum())
			throw new IllegalArgumentException("Expected enum class");
		
		addAll(enumClass.getEnumConstants());
	}
	
	@SuppressWarnings("unchecked")
	public void addAll(T...entries) {
		for(T entry : entries)
			this.entries.put(entry.getConfigValue().getName(), entry);
	}
	
	public Map<String, T> getEntries() {
		return Collections.unmodifiableMap(entries);
	}
	
	public void setValue(String name, String value) {
		boolean unset = name.startsWith("no-");
		
		T configurable = entries.get(name);
		
		if(configurable == null)
			throw new IllegalArgumentException("Unknown " + this.name + " »" + name + "«");
		
		ConfigValue cfg = configurable.getConfigValue();

		if(value == null)
			cfg.setState(!unset);
		else cfg.setValue(value);
	}
	
}
