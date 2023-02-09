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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import at.syntaxerror.syntaxc.SyntaxC;
import at.syntaxerror.syntaxc.SystemUtils;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class IncludePathRegistry {

	private static Path syntaxcLibraryPath;

	private static final String[] STDLIB_HEADERS = {
		"assert.h",
		"ctype.h",
		"errno.h",
		"float.h",
		"limits.h",
		"locale.h",
		"math.h",
		"setjmp.h",
		"signal.h",
		"stdarg.h",
		"stddef.h",
		"stdio.h",
		"stdlib.h",
		"string.h",
		"time.h"
	};
	
	private static final Set<Path> INCLUDE_PATH = new LinkedHashSet<>();

	static {
		String ver = SyntaxC.Version.VERSION;
		
		switch(SystemUtils.getOperatingSystem()) {
		case LINUX:
			INCLUDE_PATH.add(syntaxcLibraryPath = Paths.get("/opt/syntaxc/" + ver + "/include"));
			INCLUDE_PATH.add(Paths.get("/usr/local"));
			INCLUDE_PATH.add(Paths.get("/usr/local/include"));
			break;
		
		case WINDOWS:
			INCLUDE_PATH.add(syntaxcLibraryPath = Paths.get("C:\\Program Files\\syntaxc\\" + ver + "\\include"));
			break;
		
		default:
			break;
		}
	}
	
	public static Path getSyntaxCLibraryPath() {
		return syntaxcLibraryPath;
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
	
	public static void uninstall() throws IOException {
		if(!Files.exists(syntaxcLibraryPath))
			throw new IOException("Standard library is not installed");
		
		if(!Files.isDirectory(syntaxcLibraryPath))
			throw new IOException("Standard library path is not a directory");
		
		for(String header : STDLIB_HEADERS)
			Files.deleteIfExists(syntaxcLibraryPath.resolve(header));
		
		try {
			Files.delete(syntaxcLibraryPath);
		} catch(DirectoryNotEmptyException e) {
			// ignore
		}
	}
	
	public static void install() throws IOException {
		if(Files.exists(syntaxcLibraryPath) && !Files.isDirectory(syntaxcLibraryPath))
			throw new IOException("Standard library path is not a directory");
		
		Files.createDirectories(syntaxcLibraryPath);
		
		for(String header : STDLIB_HEADERS)
			try(OutputStream out = Files.newOutputStream(
					syntaxcLibraryPath.resolve(header),
					StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.CREATE
				);
				InputStream in = IncludePathRegistry.class.getResourceAsStream("/resources/stdlib/" + header)) {
				
				in.transferTo(out);
			}
	}
	
}
