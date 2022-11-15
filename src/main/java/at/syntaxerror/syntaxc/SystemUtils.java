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
package at.syntaxerror.syntaxc;

import java.nio.ByteOrder;

import com.sun.jna.Platform;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

/**
 * This class is used to gather information about the current machine's specifications
 * 
 * @author Thomas Kasper
 * 
 */
@UtilityClass
public class SystemUtils {

	private static final OperatingSystem SYSTEM;
	private static final BitSize BITS;
	private static final Arch ARCH;
	private static final ByteOrder BYTE_ORDER;
	
	static {
		SYSTEM = switch(Platform.getOSType()) {
		case Platform.MAC ->		OperatingSystem.MAC;
		case Platform.LINUX ->		OperatingSystem.LINUX;
		case Platform.WINDOWS ->	OperatingSystem.WINDOWS;
		case Platform.SOLARIS ->	OperatingSystem.SOLARIS;
		case Platform.FREEBSD ->	OperatingSystem.FREEBSD;
		case Platform.OPENBSD ->	OperatingSystem.OPENBSD;
		case Platform.WINDOWSCE ->	OperatingSystem.WINDOWS;
		case Platform.AIX ->		OperatingSystem.AIX;
		case Platform.ANDROID ->	OperatingSystem.ANDROID;
		case Platform.GNU ->		OperatingSystem.GNU;
		case Platform.KFREEBSD ->	OperatingSystem.KFREEBSD;
		case Platform.NETBSD ->		OperatingSystem.NETBSD;
		default ->					OperatingSystem.UNSPECIFIED;
		};
		
		BITS = Platform.is64Bit()
			? BitSize.B64
			: BitSize.B32;
		
		BYTE_ORDER = ByteOrder.nativeOrder();
		
			 if(Platform.isIntel())	ARCH = Arch.X86;
		else if(Platform.isARM())	ARCH = Arch.ARM;
		else if(Platform.isPPC())	ARCH = Arch.PPC;
		else if(Platform.isSPARC())	ARCH = Arch.SPARC;
		else if(Platform.isMIPS())	ARCH = Arch.MIPS;
		else						ARCH = Arch.UNSPECIFIED;
	}
	
	public static OperatingSystem getOperatingSystem() {
		return SYSTEM;
	}
	
	public static BitSize getBitSize() {
		return BITS;
	}
	
	public static Arch getArch() {
		return ARCH;
	}
	
	public static ByteOrder getByteOrder() {
		return BYTE_ORDER;
	}

	public static boolean isBigEndian() {
		return BYTE_ORDER == ByteOrder.BIG_ENDIAN;
	}

	public static boolean isLittleEndian() {
		return BYTE_ORDER == ByteOrder.LITTLE_ENDIAN;
	}
	
	public static boolean isMac() {
		return SYSTEM == OperatingSystem.MAC;
	}
	
	public static boolean isLinux() {
		return SYSTEM == OperatingSystem.LINUX;
	}
	
	public static boolean isWindows() {
		return SYSTEM == OperatingSystem.WINDOWS;
	}
	
	public static boolean isSolaris() {
		return SYSTEM == OperatingSystem.SOLARIS;
	}
	
	public static boolean isFreeBSD() {
		return SYSTEM == OperatingSystem.FREEBSD;
	}
	
	public static boolean isOpenBSD() {
		return SYSTEM == OperatingSystem.OPENBSD;
	}
	
	public static boolean isAIX() {
		return SYSTEM == OperatingSystem.AIX;
	}
	
	public static boolean isAndroid() {
		return SYSTEM == OperatingSystem.ANDROID;
	}
	
	public static boolean isGNU() {
		return SYSTEM == OperatingSystem.GNU;
	}
	
	public static boolean isKFreeBSD() {
		return SYSTEM == OperatingSystem.KFREEBSD;
	}
	
	public static boolean isNetBSD() {
		return SYSTEM == OperatingSystem.NETBSD;
	}
	
	public static boolean isUnixLike() {
		return !isWindows()
			&& SYSTEM != OperatingSystem.UNSPECIFIED;
	}
	
	public static enum OperatingSystem {
		UNSPECIFIED,
		MAC,
		LINUX,
		WINDOWS,
		SOLARIS,
		FREEBSD,
		OPENBSD,
		AIX,
		ANDROID,
		GNU,
		KFREEBSD,
		NETBSD;
		
		@Override
		public String toString() {
			return name().toLowerCase();
		}
		
	}

	@Getter
	@RequiredArgsConstructor
	public static enum BitSize {
		B8		(8),
		B16		(16),
		B32		(32),
		B64		(64),
		B128	(128);
		
		private final int bits;
		
		@Override
		public String toString() {
			return String.valueOf(bits);
		}
		
	}

	@RequiredArgsConstructor
	public static enum Arch {
		X86			("x86"),
		PPC			("PowerPC"),
		ARM			("ARM"),
		SPARC		("SPARC"),
		MIPS		("MIPS"),
		UNSPECIFIED	("unspecified");
		
		private final String name;
		
		@Override
		public String toString() {
			return name;
		}
		
	}
	
}
