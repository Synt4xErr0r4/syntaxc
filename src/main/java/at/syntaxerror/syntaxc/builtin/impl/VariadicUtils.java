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
package at.syntaxerror.syntaxc.builtin.impl;

import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.config.Warnings;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.FunctionType;
import at.syntaxerror.syntaxc.type.Type;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class VariadicUtils {

	/**
	 * Ensures that the given type is '__builtin_va_list'
	 * 
	 * @param pos the position
	 * @param type the type to be checked
	 */
	public static void ensureVaListType(Positioned pos, Type type) {
		if(type.isVaList())
			return;
		
		Logger.error(pos, Warnings.SEM_NONE, "Expected »__builtin_va_list«, got »%s« instead", type);
	}

	/**
	 * Ensures that the given function is a variadic function
	 * 
	 * @param pos the position
	 * @param type the function to be checked
	 * @param name the name of the builtin function
	 */
	public static void ensureVariadic(Positioned pos, FunctionType type, String name) {
		if(type.isVariadic())
			return;
		
		Logger.error(pos, Warnings.SEM_NONE, "Cannot use »%s« within non-variadic function", name);
	}

}
