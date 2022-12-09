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

import at.syntaxerror.syntaxc.misc.config.Flags;
import at.syntaxerror.syntaxc.misc.config.Warnings;
import at.syntaxerror.syntaxc.tracking.Positioned;

/**
 * @author Thomas Kasper
 * 
 */
public interface Logable extends Positioned {
	
	default Warnings getDefaultWarning() {
		return null;
	}
	
	default void log(LogLevel level, Positioned position, Warnings warnings, String message, Object...args) {
		Logger.log(level, position, warnings, message, args);
	}
	
	default void log(LogLevel level, Positioned position, String message, Object...args) {
		Logger.log(level, position, getDefaultWarning(), message, args);
	}

	default void log(LogLevel level, Warnings warnings, String message, Object...args) {
		Logger.log(level, getPosition(), warnings, message, args);
	}
	
	default void log(LogLevel level, String message, Object...args) {
		Logger.log(level, getPosition(), getDefaultWarning(), message, args);
	}
	

	default void note(Positioned position, Warnings warnings, String message, Object...args) {
		Logger.log(LogLevel.NOTE, position, warnings, message, args);
	}
	
	default void note(Positioned position, String message, Object...args) {
		Logger.log(LogLevel.NOTE, position, getDefaultWarning(), message, args);
	}

	default void note(Warnings warnings, String message, Object...args) {
		Logger.log(LogLevel.NOTE, getPosition(), warnings, message, args);
	}
	
	default void note(String message, Object...args) {
		Logger.log(LogLevel.NOTE, getPosition(), getDefaultWarning(), message, args);
	}
	

	default void info(Positioned position, Warnings warnings, String message, Object...args) {
		Logger.log(LogLevel.INFO, position, warnings, message, args);
	}
	
	default void info(Positioned position, String message, Object...args) {
		Logger.log(LogLevel.INFO, position, getDefaultWarning(), message, args);
	}

	default void info(Warnings warnings, String message, Object...args) {
		Logger.log(LogLevel.INFO, getPosition(), warnings, message, args);
	}
	
	default void info(String message, Object...args) {
		Logger.log(LogLevel.INFO, getPosition(), getDefaultWarning(), message, args);
	}
	

	default void warn(Positioned position, Warnings warnings, String message, Object...args) {
		Logger.log(LogLevel.WARN, position, warnings, message, args);
	}

	default void warn(Positioned position, String message, Object...args) {
		Logger.log(LogLevel.WARN, position, getDefaultWarning(), message, args);
	}

	default void warn(Warnings warnings, String message, Object...args) {
		Logger.log(LogLevel.WARN, getPosition(), warnings, message, args);
	}

	default void warn(String message, Object...args) {
		Logger.log(LogLevel.WARN, getPosition(), getDefaultWarning(), message, args);
	}
	

	default void error(Positioned position, Warnings warnings, String message, Object...args) {
		Logger.log(LogLevel.ERROR, position, warnings, message, args);
	}

	default void error(Positioned position, String message, Object...args) {
		Logger.log(LogLevel.ERROR, position, getDefaultWarning(), message, args);
	}

	default void error(Warnings warnings, String message, Object...args) {
		Logger.log(LogLevel.ERROR, getPosition(), warnings, message, args);
	}

	default void error(String message, Object...args) {
		Logger.log(LogLevel.ERROR, getPosition(), getDefaultWarning(), message, args);
	}

	
	default void error(Positioned position, Flags flags, String message, Object...args) {
		Logger.error(position, flags, message, args);
	}

	default void error(Flags flags, String message, Object...args) {
		Logger.error(flags, message, args);
	}
	

	default void softError(Positioned position, Warnings warnings, String message, Object...args) {
		if(!onBeforeSoftError())
			return;
		
		Logger.logRecover(LogLevel.ERROR, position, warnings, message, args);

		onAfterSoftError();
	}

	default void softError(Positioned position, String message, Object...args) {
		if(!onBeforeSoftError())
			return;
		
		Logger.logRecover(LogLevel.ERROR, position, getDefaultWarning(), message, args);

		onAfterSoftError();
	}

	default void softError(Warnings warnings, String message, Object...args) {
		if(!onBeforeSoftError())
			return;
		
		Logger.logRecover(LogLevel.ERROR, getPosition(), warnings, message, args);

		onAfterSoftError();
	}

	default void softError(String message, Object...args) {
		if(!onBeforeSoftError())
			return;
		
		Logger.logRecover(LogLevel.ERROR, getPosition(), getDefaultWarning(), message, args);

		onAfterSoftError();
	}

	
	default void softError(Positioned position, Flags flags, String message, Object...args) {
		if(!onBeforeSoftError())
			return;
		
		Logger.softError(position, flags, message, args);

		onAfterSoftError();
	}

	default void softError(Flags flags, String message, Object...args) {
		if(!onBeforeSoftError())
			return;
		
		Logger.softError(flags, message, args);
		
		onAfterSoftError();
	}
	
	
	default boolean onBeforeSoftError() {
		return true;
	}
	
	default void onAfterSoftError() {
		
	}
	
	
	default void terminate() {
		System.exit(1);
	}
	
}
