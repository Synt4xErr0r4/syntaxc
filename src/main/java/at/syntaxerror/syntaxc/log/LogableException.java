/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.log;

import at.syntaxerror.syntaxc.io.CharSource.Range;
import at.syntaxerror.syntaxc.log.Logger.Level;
import lombok.Getter;
import lombok.NonNull;

/**
 * 
 *
 * @author Thomas Kasper
 */
@SuppressWarnings("serial")
public class LogableException extends RuntimeException {

	@Getter
	private final Range range;
	private final String message;
	private final Object[] args;
	
	@Getter
	private boolean recoverable;
	
	public LogableException(Range range, @NonNull String message, Object...args) {
		this.range = range;
		this.message = message;
		this.args = args;
	}

	public LogableException(@NonNull String message, Object...args) {
		this(null, message, args);
	}

	public LogableException recoverable(boolean state) {
		recoverable = state;
		return this;
	}
	
	public LogableException recoverable() {
		recoverable = true;
		return this;
	}
	
	public void log() {
		Logger.log(Level.ERROR, null, range, message, args);
	}
	
}
