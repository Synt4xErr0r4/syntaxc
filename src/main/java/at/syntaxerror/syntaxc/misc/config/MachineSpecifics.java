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
package at.syntaxerror.syntaxc.misc.config;

import java.nio.ByteOrder;
import java.util.function.Consumer;

import at.syntaxerror.syntaxc.SystemUtils.BitSize;
import at.syntaxerror.syntaxc.SystemUtils.OperatingSystem;
import at.syntaxerror.syntaxc.generator.arch.Architecture;
import at.syntaxerror.syntaxc.generator.arch.ArchitectureRegistry;
import at.syntaxerror.syntaxc.logger.Logger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
@RequiredArgsConstructor
public enum MachineSpecifics implements Configurable {
	
	_8	("8",	"Sets the bit size to 8",	x -> ArchitectureRegistry.setBitSize(BitSize.B8), false),
	_16	("16",	"Sets the bit size to 16",	x -> ArchitectureRegistry.setBitSize(BitSize.B16), false),
	_32	("32",	"Sets the bit size to 32",	x -> ArchitectureRegistry.setBitSize(BitSize.B32), false),
	_64	("64",	"Sets the bit size to 64",	x -> ArchitectureRegistry.setBitSize(BitSize.B64), false),
	_128("128",	"Sets the bit size to 128",	x -> ArchitectureRegistry.setBitSize(BitSize.B128), false),
	
	ARCH("arch=ARCH", "Specifies the target architecture (see below)") {
		
		@Override
		public void setValue(String value) {
			if(value.isBlank())
				Logger.warn("Missing value for assembler option -march");
			
			else {
				Architecture arch = ArchitectureRegistry.find(value);
				
				if(arch == null)
					Logger.warn("Unknown architecture »%s«", value);
				
				else ArchitectureRegistry.setArchitecture(arch);
			}
		}
		
	},
	
	ASM("asm=SYNTAX", "Specifies the assembly syntax (see below)") {
		
		@Override
		public void setValue(String value) {
			if(!ArchitectureRegistry.getArchitecture().setSyntax(value))
				Logger.warn(
					"Unknown assembly syntax »%s« for target architecture »%s«",
					value,
					ArchitectureRegistry.getArchitecture()
						.getNames()[0]
				);
		}
		
	},
	
	TARGET("target=TARGET", "Specifies the target system (see below)") {

		@Override
		public void setValue(String value) {
			try {
				ArchitectureRegistry.setOperatingSystem(OperatingSystem.valueOf(value.toUpperCase()));
			} catch(Exception e) {
				Logger.warn("Unknown target system »%s«", value);
			}
		}
		
	},
	
	ENDIAN("endian=ENDIANNESS", "Specifies the target endianness (see below)") {
		
		@Override
		public void setValue(String value) {
			if(value.equalsIgnoreCase("little"))
				ArchitectureRegistry.setEndianness(ByteOrder.LITTLE_ENDIAN);
			
			else if(value.equalsIgnoreCase("big"))
				ArchitectureRegistry.setEndianness(ByteOrder.BIG_ENDIAN);
			
			else if(value.isBlank())
				Logger.warn("Missing value for assembler option -mendian");
			
			else Logger.warn("Unknown endianness »%s«", value);
		}
		
	}
	
	;
	
	static {
		for(MachineSpecifics spec : values())
			ConfigRegistry.registerMachineSpecific(spec.name, spec);
	}
	
	public static void init() { }
	
	private final String name;
	private final String description;

	private final Consumer<String> action;

	private final boolean acceptsValue;
	
	@Setter
	private boolean enabled = true;
	
	private MachineSpecifics(String name, String description, Consumer<String> action) {
		this(name, description, action, true);
	}

	private MachineSpecifics(String name, String description) {
		this(name, description, null, true);
	}

	@Override
	public String getDescription() {
		return description + (enabled ? " §8(§aenabled§8)" : " §8(§9disabled§8)");
	}
	
	@Override
	public boolean acceptsValue() {
		return acceptsValue;
	}
	
	@Override
	public void setValue(String opt) {
		action.accept(null);
	}

}
