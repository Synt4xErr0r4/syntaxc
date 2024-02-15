/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.backend;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.Platform;

import at.syntaxerror.syntaxc.backend.x86.CPUID;
import at.syntaxerror.syntaxc.backend.x86.X86Backend;
import oshi.PlatformEnum;
import oshi.SystemInfo;

/**
 * 
 *
 * @author Thomas Kasper
 */
public class Backends {

	public static ByteOrder endianness = ByteOrder.nativeOrder();
	
	private static final List<IBackend> BACKENDS = new ArrayList<>();
	private static final Map<String, IBackend> BY_NAME = new LinkedHashMap<>();
	
	static {
		BACKENDS.add(X86Backend.INSTANCE);
		
		BACKENDS.forEach(backend -> {
			backend.getNames().forEach(name -> BY_NAME.put(name, backend));
		});
	}
	
	public static void init() {}
	
	public static boolean isUnixLike() {
		return !Platform.isWindows() && Platform.getOSType() != Platform.UNSPECIFIED;
	}
	
	public static IBackend getSystemBackend() {
		PlatformEnum platform = SystemInfo.getCurrentPlatform();
		
		if(Platform.isIntel()) {
			X86Backend backend = X86Backend.INSTANCE;
			CPUID.get();
			
			backend.bits = Platform.is64Bit() ? 64 : 32;
			backend.platform = platform;
			
			return backend;
		}
		
		throw new UnsupportedOperationException("Current platform »" + Platform.ARCH + "« is not supported.");
	}
	
	public static Map<String, IBackend> getNames() {
		return Collections.unmodifiableMap(BY_NAME);
	}
	
	public static List<IBackend> getBackends() {
		return Collections.unmodifiableList(BACKENDS);
	}
	
}
