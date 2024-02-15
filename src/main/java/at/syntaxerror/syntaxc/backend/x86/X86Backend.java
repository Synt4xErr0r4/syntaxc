/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.backend.x86;

import java.util.List;

import com.sun.jna.Platform;

import at.syntaxerror.ieee754.binary.Binary80;
import at.syntaxerror.syntaxc.backend.IBackend;
import at.syntaxerror.syntaxc.frontend.c.string.EncodingPrefix;
import at.syntaxerror.syntaxc.frontend.c.type.BinaryFloatType;
import at.syntaxerror.syntaxc.frontend.c.type.IntType;
import at.syntaxerror.syntaxc.frontend.c.type.NumberType.NumberClass;
import at.syntaxerror.syntaxc.frontend.c.type.Types;
import at.syntaxerror.syntaxc.frontend.c.type.Types.DataModel;
import at.syntaxerror.syntaxc.io.Charsets;
import oshi.PlatformEnum;

/**
 * 
 *
 * @author Thomas Kasper
 */
public class X86Backend implements IBackend {
	
	public static final X86Backend INSTANCE = new X86Backend();
	
	public int bits;
	public PlatformEnum platform;
	
	@Override
	public String getName() {
		return "x86";
	}

	@Override
	public List<String> getNames() {
		return List.of(
			"x86", "x64", "x86-64", "amd64"
		);
	}

	@Override
	public void init() {
		if(X86Feature.FPU.isSupported())
			Types.LDOUBLE = BinaryFloatType.of(Binary80.FACTORY, Binary80.CODEC, NumberClass.LDOUBLE);
		
		switch(bits) {
		case 64:
			if(Platform.isWindows()) {
				DataModel.LLP64.apply();
				Types.WCHAR = (IntType) Types.USHORT.alias("wchar_t");
				EncodingPrefix.WIDE.charset = Charsets.UTF16LE;
			}
			else DataModel.LP64.apply();
			break;
			
		case 32:
			DataModel.ILP32.apply();
			break;
			
		case 16:
			throw new UnsupportedOperationException("x86 (16-bit) is not supported yet!");
		}
	}

}
