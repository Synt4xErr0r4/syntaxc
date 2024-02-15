/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.argp;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import at.syntaxerror.syntaxc.collection.ArrayIterator;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 
 *
 * @author Thomas Kasper
 */
@Accessors(fluent = true, chain = true)
public class ArgParser {

	private static final String DEFAULT_CATEGORY = "argp.category.default";
	
	@Setter
	@Getter
	@NonNull
	private String programName = "program";

	@Setter
	@Getter
	private String epilogue;

	@Setter
	@Getter
	private String prologue;
	
	private final Map<String, Arg> argsByName = new HashMap<>();
	private final Map<Character, Arg> argsByMnemonic = new HashMap<>();
	private Arg positionalArg;
	private Arg helpArg;
	
	private final Map<String, ArgCategory> categories = new LinkedHashMap<>();
	
	private final ArgCategory defaultCategory = new ArgCategory(DEFAULT_CATEGORY, null, new ArrayList<>());
	
	public ArgParser() {
		categories.put(DEFAULT_CATEGORY, defaultCategory);
	}
	
	public ArgParser withObject(@NonNull Object obj) {
		Class<?> clazz = obj.getClass();
		
		ArgCategory category;
		
		Category cat = clazz.getDeclaredAnnotation(Category.class);
		
		if(cat != null)
			category = categories.computeIfAbsent(cat.value(), ArgCategory.of(getDescription(clazz)));
		else category = defaultCategory;
		
		for(Field field : clazz.getDeclaredFields()) {
			int mods = field.getModifiers();
			
			if(Modifier.isFinal(mods) || Modifier.isStatic(mods))
				continue;
			
			Parameter param = field.getDeclaredAnnotation(Parameter.class);
			
			if(param == null)
				continue;
			
			var name = param.name();
			var mnemonic = param.mnemonic();
			
			Arg arg = new Arg(this, field, obj);
			
			arg.type = field.getType();
			arg.name = name.length == 0 ? null : name[0];
			arg.mnemonic = mnemonic.length == 0 ? '\0' : mnemonic[0];
			arg.desc = getDescription(field);
			
			if(arg.name != null)
				argsByName.compute(arg.name, (key, val) -> {
					if(val != null)
						throw new IllegalArgumentException("Duplicate option »--" + key + "«");
					
					return arg;
				});
			
			if(arg.mnemonic != '\0')
				argsByMnemonic.compute(arg.mnemonic, (key, val) -> {
					if(val != null)
						throw new IllegalArgumentException("Duplicate option »-" + key + "«");
					
					return arg;
				});
			
			if(arg.name == null && arg.mnemonic == '\0') {
				if(positionalArg != null)
					throw new IllegalArgumentException("Duplicate positional argument");
				
				positionalArg = arg;
			}
			
			getIfPresent(field.getDeclaredAnnotation(Help.class), help -> {
				if(helpArg != null)
					throw new IllegalArgumentException("Duplicate help argument");

				helpArg = arg;
				return null;
			});
			
			arg.transformer = getIfPresent(field.getDeclaredAnnotation(Transformer.class), transformer -> {
				try {
					return transformer.value().getConstructor().newInstance();
				}
				catch (Exception e) {
					throw new IllegalArgumentException("Failed to instantiate argument transformer", e);
				}
			});
			
			arg.hasValue = arg.type != boolean.class && arg.type != Boolean.class;
			
			getIfPresent(field.getDeclaredAnnotation(Value.class), value -> {
				arg.hasValue = true;
				arg.value = value.value();
				arg.required = value.required();
				return null;
			});
			
			if(arg.hasValue && !arg.required)
				try {
					if(field.getType() != boolean.class && field.getType() != Boolean.class)
						throw new IllegalArgumentException("Expected boolean type for " + field.getName() + " in " + clazz);
					
					arg.fieldValue = clazz.getDeclaredField(field.getName() + "$value");
					arg.type = arg.fieldValue.getType();
				}
				catch (Exception e) {
					throw new IllegalArgumentException("Field with optional value requires additional field suffixed with »$value«", e);
				}
			else arg.fieldValue = arg.fieldState;
			
			category.args.add(arg);
		}
		
		return this;
	}
	
	public void parse(String...args) {
		var iter = new ArrayIterator<>(args);
		
		boolean positionalOnly = false;
		
		while(iter.hasNext()) {
			String param = iter.next();
			
			if(param == null || param.isBlank())
				continue;
			
			if(positionalOnly || param.charAt(0) != '-' || param.length() == 1) {
				if(positionalArg == null)
					fail("unrecognized command-line option »&+&l%s&-«", param);
				
				positionalArg.setValue(param, param);
				continue;
			}
			
			String name = null;
			String value = null;
			Arg arg = null;
			
			if(param.startsWith("--")) {
				if(param.length() == 2) {
					positionalOnly = true;
					continue;
				}
				
				int equ = param.indexOf('=');
				
				if(equ != -1) {
					value = param.substring(equ + 1);
					param = param.substring(0, equ);
				}
				
				name = param.substring(2);
				arg = argsByName.get(name);
			}
			else if(param.startsWith("-")) {
				char mnemonic = param.charAt(1);
				
				if(param.length() > 2)
					value = param.substring(2);
				
				name = "-" + mnemonic;
				arg = argsByMnemonic.get(mnemonic);
			}
			
			if(arg == null)
				fail("unrecognized command-line option »&+&l%s&-«", name);
			
			if(arg == helpArg)
				showHelp();
			
			if(arg.hasValue && arg.required && iter.hasNext() && value == null)
				value = iter.next();
			
			arg.setValue(name, value);
		}
	}
	
	public void printPrologue() {
		if(prologue != null) {
			System.out.println(prologue);
			System.out.println();
		}
	}
	
	public void printEpilogue() {
		if(epilogue != null) {
			System.out.println();
			System.out.println(epilogue);
		}
	}
	
	public void showHelp() {
		if(prologue != null) {
			System.out.println(prologue);
			System.out.println();
		}
		
		System.out.printf("&rusage: &l%s &r[OPTIONS]%n", programName);
		System.out.println("Options:");
		
		for(var entry : categories.entrySet()) {
			ArgCategory category = entry.getValue();
			
			if(category != defaultCategory) {
				System.out.println();
				System.out.println(category.name + ":");
				
				if(category.description != null) {
					for(String line : category.description.split("\\R"))
						System.out.println("  " + line);
					
					System.out.println();
				}
			}
			
			category.args.forEach(Arg::print);
		}
		
		printEpilogue();
		System.exit(0);
	}
	
	public void fail(String message, Object...args) {
		System.err.printf("&9error:&r " + message + "&r%n", args);
		
		if(helpArg != null) {
			System.err.println();
			System.err.printf("\tShow all commands with »&l%s %s&r«%n", programName, helpArg.getName());
		}
		
		System.exit(1);
	}
	
	private static <R, T> R getIfPresent(T value, Function<T, R> getter) {
		return value == null ? null : getter.apply(value);
	}
	
	private static String getDescription(AnnotatedElement elem) {
		return getIfPresent(elem.getDeclaredAnnotation(Description.class), Description::value);
	}
	
	private static record ArgCategory(String name, String description, List<Arg> args) {
		
		public static Function<String, ArgCategory> of(String description) {
			return name -> new ArgCategory(name, description, new ArrayList<>());
		}
		
	}
	
	@RequiredArgsConstructor
	@ToString
	private static class Arg {
		
		public final ArgParser parser;
		public final Field fieldState;
		public final Object instance;
		
		public Class<?> type;

		public Field fieldValue;
		
		public String name;
		public char mnemonic;
		
		public String desc;
		public String value;
		
		public boolean hasValue;
		public boolean required = true;
		
		public IStringTransformer<?> transformer;
		
		public void print() {
			StringBuilder help = new StringBuilder();
			
			boolean positional = mnemonic == 0 && name == null;
			
			if(!positional) {
				if(mnemonic != 0)
					help.append("   -").append(mnemonic);
				else help.append("       ");
				
				if(name != null) {
					if(mnemonic != 0)
						help.append(", ");
					
					help.append("--").append(name);
				}
			}
			else help.append("  ");
			
			if(hasValue) {
				help.append(required && !positional ? " <" : " [");
				
				if(value != null)
					help.append(value);
				else help.append("VALUE");
					
				help.append(required && !positional ? '>' : ']');
			}
			
			if(desc != null && !desc.isBlank()) {
				final String indent = "\n" + " ".repeat(32);
				
				if(help.length() >= 32)
					help.append(indent);
				else help.append(" ".repeat(32 - help.length()));
				
				var lines = desc.split("\\R");
				
				help.append(lines[0]);
				
				for(int i = 1; i < lines.length; ++i)
					help.append(indent).append(lines[i]);
			}
			
			System.out.println(help);
		}
		
		private Object transform(String name, String strVal) {
			Object value = strVal;
			
			if(transformer != null)
				value = transformer.apply(strVal);
			
			else if(value != null && !Collection.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type)) {
				
				if(is(type, boolean.class, Boolean.class)) {
					if(!strVal.matches("(?i)^(true|false|[01])$"))
						parser.fail("command-line option »&+&l%s&-« expects a boolean value", name);
					
					value = strVal.matches("(?i)^(true|1)$");
				}
				
				else if(is(type, float.class, Float.class, double.class, Double.class))
					try {
						BigDecimal num = new BigDecimal(strVal);

						if(is(type, byte.class, Byte.class))
							value = num.floatValue();
						
						else if(is(type, short.class, Short.class))
							value = num.doubleValue();
					}
					catch (Exception e) {
						parser.fail("command-line option »&+&l%s&-« expects a floating-point value", name);
					}
				
				else if(is(type, byte.class, Byte.class, short.class, Short.class, int.class, Integer.class, long.class, Long.class))
					try {
						BigInteger num = new BigInteger(strVal);
						
						if(is(type, byte.class, Byte.class))
							value = num.byteValueExact();
						
						else if(is(type, short.class, Short.class))
							value = num.shortValueExact();
						
						else if(is(type, int.class, Integer.class))
							value = num.intValueExact();
						
						else if(is(type, long.class, Long.class))
							value = num.longValueExact();
					}
					catch (Exception e) {
						parser.fail("command-line option »&+&l%s&-« expects an integer value", name);
					}
			}
			
			return value;
		}
		
		@SuppressWarnings("unchecked")
		public void setValue(String name, String strVal) {
			if(strVal != null && !hasValue)
				parser.fail("command-line option »&+&l%s&-« does not accept values", name);
			
			if(strVal == null && hasValue && required)
				parser.fail("command-line option »&+&l%s&-« requires a value", name);
			
			if(!hasValue || !required)
				try {
					fieldState.setBoolean(instance, true);
				}
				catch (Exception e) {
					throw new IllegalArgumentException("Failed to set field's value", e);
				}
			
			if(!hasValue || strVal == null)
				return;

			String key = null;
			Object value = strVal;
			
			if(Map.class.isAssignableFrom(type)) {
				int equ = strVal.indexOf('=');
				
				if(equ != -1) {
					key = strVal.substring(0, equ);
					value = strVal = strVal.substring(equ + 1);
				}
				else {
					key = strVal;
					value = strVal = null;
				}
			}

			value = transform(name, strVal);

			if(Collection.class.isAssignableFrom(type)) {
				Object collection;
				
				try {
					collection = fieldValue.get(instance);
				}
				catch (Exception e) {
					throw new IllegalArgumentException("Failed to get field's value", e);
				}
				
				if(collection == null)
					collection = new ArrayList<>();
				
				((Collection<Object>) collection).add(value);
				value = collection;
			}
			
			else if(Map.class.isAssignableFrom(type)) {
				Object map;
				
				try {
					map = fieldValue.get(instance);
				}
				catch (Exception e) {
					throw new IllegalArgumentException("Failed to get field's value", e);
				}
				
				if(map == null)
					map = new LinkedHashMap<>();

				((Map<String, Object>) map).put(key, value);
				value = map;
			}

			try {
				fieldValue.set(instance, value);
				return;
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Failed to set field's value", e);
			}
		}
		
		public String getName() {
			return name == null ? "-" + mnemonic : "--" + name;
		}
		
		private static boolean is(Class<?> clazz, Class<?>...classes) {
			for(Class<?> cls : classes)
				if(cls.isAssignableFrom(clazz))
					return false;
			
			return false;
		}
		
	}
	
}
