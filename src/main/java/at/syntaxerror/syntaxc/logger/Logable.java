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
package at.syntaxerror.syntaxc.logger;

import at.syntaxerror.syntaxc.misc.Warning;
import at.syntaxerror.syntaxc.tracking.Positioned;

/**
 * @author Thomas Kasper
 * 
 */
public interface Logable extends Positioned {
	
	default Warning getDefaultWarning() {
		return null;
	}
	
	default void log(LogLevel level, Positioned position, Warning warning, String message, Object...args) {
		Logger.log(level, position, warning, message, args);
	}
	
	default void log(LogLevel level, Positioned position, String message, Object...args) {
		Logger.log(level, position, getDefaultWarning(), message, args);
	}

	default void log(LogLevel level, Warning warning, String message, Object...args) {
		Logger.log(level, getPosition(), warning, message, args);
	}
	
	default void log(LogLevel level, String message, Object...args) {
		Logger.log(level, getPosition(), getDefaultWarning(), message, args);
	}
	

	default void note(Positioned position, Warning warning, String message, Object...args) {
		Logger.log(LogLevel.NOTE, position, warning, message, args);
	}
	
	default void note(Positioned position, String message, Object...args) {
		Logger.log(LogLevel.NOTE, position, getDefaultWarning(), message, args);
	}

	default void note(Warning warning, String message, Object...args) {
		Logger.log(LogLevel.NOTE, getPosition(), warning, message, args);
	}
	
	default void note(String message, Object...args) {
		Logger.log(LogLevel.NOTE, getPosition(), getDefaultWarning(), message, args);
	}
	

	default void info(Positioned position, Warning warning, String message, Object...args) {
		Logger.log(LogLevel.INFO, position, warning, message, args);
	}
	
	default void info(Positioned position, String message, Object...args) {
		Logger.log(LogLevel.INFO, position, getDefaultWarning(), message, args);
	}

	default void info(Warning warning, String message, Object...args) {
		Logger.log(LogLevel.INFO, getPosition(), warning, message, args);
	}
	
	default void info(String message, Object...args) {
		Logger.log(LogLevel.INFO, getPosition(), getDefaultWarning(), message, args);
	}
	

	default void warn(Positioned position, Warning warning, String message, Object...args) {
		Logger.log(LogLevel.WARN, position, warning, message, args);
	}

	default void warn(Positioned position, String message, Object...args) {
		Logger.log(LogLevel.WARN, position, getDefaultWarning(), message, args);
	}

	default void warn(Warning warning, String message, Object...args) {
		Logger.log(LogLevel.WARN, getPosition(), warning, message, args);
	}

	default void warn(String message, Object...args) {
		Logger.log(LogLevel.WARN, getPosition(), getDefaultWarning(), message, args);
	}
	

	default void error(Positioned position, Warning warning, String message, Object...args) {
		if(warning == Warning.CONTINUE) {
			Logger.dontTerminate = true;
			warning = getDefaultWarning();
		}
		
		Logger.log(LogLevel.ERROR, position, warning, message, args);
	}

	default void error(Positioned position, String message, Object...args) {
		Logger.log(LogLevel.ERROR, position, getDefaultWarning(), message, args);
	}

	default void error(Warning warning, String message, Object...args) {
		if(warning == Warning.CONTINUE) {
			Logger.dontTerminate = true;
			warning = getDefaultWarning();
		}
		
		Logger.log(LogLevel.ERROR, getPosition(), warning, message, args);
	}

	default void error(String message, Object...args) {
		Logger.log(LogLevel.ERROR, getPosition(), getDefaultWarning(), message, args);
	}
	
	default void terminate() {
		System.exit(1);
	}
	
}
