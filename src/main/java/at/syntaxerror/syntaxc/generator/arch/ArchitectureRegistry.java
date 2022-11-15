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
package at.syntaxerror.syntaxc.generator.arch;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import at.syntaxerror.syntaxc.SystemUtils;
import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.SystemUtils.OperatingSystem;
import at.syntaxerror.syntaxc.generator.arch.x86.X86Architecture;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;

/**
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class ArchitectureRegistry {

	private static final Map<String, Architecture> ARCHITECTURES = new HashMap<>();
	
	@Getter @Setter
	private static OperatingSystem operatingSystem = SystemUtils.getOperatingSystem();
	
	@Getter @Setter
	private static BitSize bitSize = SystemUtils.getBitSize();
	
	@Getter @Setter
	private static Architecture architecture;
	
	@Getter @Setter
	private static ByteOrder endianness = SystemUtils.getByteOrder();

	@Getter @Setter
	private static int alignment = -1;
	
	static {
		register(new X86Architecture());
		
		String arch = SystemUtils.getArch().toString();
		
		architecture = Objects.requireNonNullElseGet(find(arch), () -> Architecture.unsupported(arch));
	}
	
	public static void register(Architecture arch) {
		for(String name : arch.getNames())
			ARCHITECTURES.put(name, arch);
	}
	
	public static Architecture find(String name) {
		return ARCHITECTURES.get(name);
	}
	
	public static Set<Architecture> getArchitectures() {
		return new HashSet<>(ARCHITECTURES.values());
	}

}
