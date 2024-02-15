/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c;

import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxc.config.ConfigRegistry;
import at.syntaxerror.syntaxc.config.std.IStandard;
import at.syntaxerror.syntaxc.config.std.Standards;
import at.syntaxerror.syntaxc.frontend.IFrontend;
import at.syntaxerror.syntaxc.frontend.c.lexer.PreLexer;
import at.syntaxerror.syntaxc.frontend.c.preproc.Preprocessor;
import at.syntaxerror.syntaxc.io.CharSource;
import at.syntaxerror.syntaxc.io.IFileStream;
import at.syntaxerror.syntaxc.log.LogableException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 *
 * @author Thomas Kasper
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CFrontend implements IFrontend {

	public static final CFrontend INSTANCE = new CFrontend();

	static {
		ConfigRegistry.FLAGS.addAll(CFlag.class);
		ConfigRegistry.WARNINGS.addAll(CWarning.class);
		Standards.addAll(CStandard.class);
	}
	
	public final CArgs cArgs = new CArgs();
	public final CPreprocArgs cPreprocArgs = new CPreprocArgs();
	public final COutputArgs cOutputArgs = new COutputArgs();

	@Override
	public Object process(IFileStream file) {
		CharSource source = new CharSource(file);
		
		// TODO
		
		try {
			PreLexer preLexer = new PreLexer(source);
			Preprocessor preproc = new Preprocessor(preLexer);
			
			var tokens = preproc.preprocess();
			
			System.out.println(tokens);
		}
		catch (LogableException e) {
			e.log();
			e.printStackTrace();
			return null;
		}
		
		return null;
	}
	
	@Override
	public String getName() {
		return "C";
	}
	
	@Override
	public List<String> getLanguages() {
		return List.of("c");
	}
	
	@Override
	public Map<String, String> getFileExtensions() {
		return Map.of(
			"c", "c",
			"h", "c",
			"sych", "c_header"
		);
	}
	
	@Override
	public IStandard getStandard() {
		return CStandard.standard;
	}
	
	@Override
	public List<Object> getCommandLineOptions() {
		return List.of(
			cArgs, cPreprocArgs, cOutputArgs
		);
	}
	
}
