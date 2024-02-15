/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.backend.x86;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sun.jna.Platform;

import at.syntaxerror.syntaxc.backend.Backends;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

/**
 * 
 *
 * @author Thomas Kasper
 */
@ToString(exclude = { "eax", "ebx", "ecx", "edx" })
@Getter
public final class CPUID {

	private static final boolean IS_SUPPORTED;
	private static CPUID instance;

	static {
		boolean isSupported = false;
		
		if(Platform.isIntel()) {
			String prefix = null;
			String suffix = null;
			
			int bits = Platform.is64Bit() ? 64 : 32;
			
			if(Platform.isMac()) {
				prefix = "libx86cpuid";
				suffix = ".dylib";
			}
			if(Backends.isUnixLike()) {
				prefix = "libx86cpuid";
				suffix = ".so";
			}
			else if(Platform.isWindows()) {
				prefix = "x86cpuid";
				suffix = ".dll";
			}
			
			if(prefix != null) {
				var lib = CPUID.class.getResourceAsStream("/native/" + prefix + bits + suffix);
				
				if(lib != null) {
					FileOutputStream out = null;
					
					try {
						File file = File.createTempFile(prefix, suffix);
						file.deleteOnExit();
						
						out = new FileOutputStream(file);
						
						lib.transferTo(out);
						
						System.load(file.getAbsolutePath());
						isSupported = true;
					} catch (Exception e) {
					} finally {
						if(out != null)
							try {
								out.close();
							} catch (Exception e2) { }
					}
				}
			}
		}
		
		IS_SUPPORTED = isSupported;
	}
	
	public static boolean isSupported() {
		return IS_SUPPORTED;
	}
	
	public static CPUID get() {
		if(instance == null)
			instance = new CPUID(IS_SUPPORTED);
		
		return instance;
	}

	private native void cpuid2(int eax, int ecx);
	
	@Getter(AccessLevel.NONE)
	private volatile transient int eax, ebx, ecx, edx;
	
	public final Set<X86Feature> features;
	
	private int highestFunctionParameter;
	private int highestExtendedFunctionParameter;
	
	private CPUID(boolean x86) {
		if(!x86) {
			features = Set.of();
			return;
		}
		
		Set<X86Feature> features = new HashSet<>();
		
		cpuid(0);
		highestFunctionParameter = eax;
		
		cpuid(0x80000000);
		highestExtendedFunctionParameter = eax;
		
		if(cpuid(1))
			getFeatures(features, 0);
		
		if(cpuid(7, 0))
			getFeatures(features, 1);

		if(cpuid(7, 1))
			getFeatures(features, 2);

		if(cpuid(0x80000001))
			getFeatures(features, 3);

		if(cpuid(0xD, 1))
			getFeatures(features, 4);
		
		if(cpuid(0x14))
			getFeatures(features, 5);

		if(cpuid(0x19))
			getFeatures(features, 6);

		if(cpuid(0x24))
			getFeatures(features, 7);

		if(cpuid(0x80000008))
			getFeatures(features, 8);
		
		this.features = Collections.unmodifiableSet(features);
	}
	
	private boolean cpuid(int eax) {
		return cpuid(eax, 0);
	}

	private boolean cpuid(int eax, int ecx) {
		int func = eax & 0x7FFFFFFF;
		int max = func == eax ? highestFunctionParameter : highestExtendedFunctionParameter;
		
		if(func > max)
			return false;
		
		cpuid2(eax, ecx);
		return true;
	}
	
	private void getFeatures(Set<X86Feature> features, int page) {
		X86Feature.listFeatures(features, page, eax, ebx, ecx, edx);
	}
	
}
