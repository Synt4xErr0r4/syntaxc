/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.config.warning;

import at.syntaxerror.syntaxc.config.ConfigValue;
import at.syntaxerror.syntaxc.config.IConfigurable;

/**
 * 
 *
 * @author Thomas Kasper
 */
public interface IWarning extends IConfigurable {
	
	WarningLevel getLevel();
	void setLevel(WarningLevel level);
	
	default String getLogName() {
		ConfigValue cfg = getConfigValue();
		
		if(cfg.isHasValue())
			return cfg.getName() + "=" + cfg.getValue();
		
		return cfg.getName();
	}
	
}
