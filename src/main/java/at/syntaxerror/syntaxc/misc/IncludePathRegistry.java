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
package at.syntaxerror.syntaxc.misc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import at.syntaxerror.syntaxc.SystemUtils;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class IncludePathRegistry {

	private static final Set<Path> INCLUDE_PATH = new HashSet<>();

	static {
		switch(SystemUtils.getOperatingSystem()) {
		case LINUX:
			INCLUDE_PATH.add(Paths.get("/usr/local"));
			INCLUDE_PATH.add(Paths.get("/usr/local/include"));
			break;
		
		case WINDOWS:
			INCLUDE_PATH.add(Paths.get("C:\\Program Files\\syntaxc\\include"));
			break;
		
		default:
			break;
		}
	}
	
	public static Set<Path> getIncludePath() {
		return Collections.unmodifiableSet(INCLUDE_PATH);
	}
	
	public static void clear() {
		INCLUDE_PATH.clear();
	}
	
	public static void add(String path) {
		Path p = Paths.get(path.strip());
		
		if(!Files.exists(p) || !Files.isDirectory(p)) {
			System.out.printf("§9Cannot add file »%s« to include path: file does not exist or is not a directory", p);
			return;
		}
		
		INCLUDE_PATH.add(p);
	}
	
	private static boolean isValidFile(Path path) {
		return Files.exists(path) && Files.isRegularFile(path);
	}
	
	public static Path resolve(Path sibling, String file) {
		Path path = Paths.get(file.strip());
		
		if(path.isAbsolute())
			return isValidFile(path) ? path : null;
		
		Path target = sibling.resolveSibling(path);
		
		if(isValidFile(target))
			return target;
		
		return INCLUDE_PATH
			.stream()
			.map(p -> p.resolve(path))
			.filter(IncludePathRegistry::isValidFile)
			.findFirst()
			.orElse(null);
	}
	
}
