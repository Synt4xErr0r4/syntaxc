/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import at.syntaxerror.syntaxc.argp.IStringTransformer;
import at.syntaxerror.syntaxc.frontend.Frontends;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 *
 * @author Thomas Kasper
 */
@ToString
public abstract sealed class IFileStream permits PathStream, StdoutStream, StringStream {

	public static IFileStream ofFile(String path) {
		return new PathStream(path);
	}
	
	public static IFileStream ofString(String string) {
		return new StringStream(string.getBytes(StandardCharsets.UTF_8));
	}

	public static IFileStream ofStdout() {
		return StdoutStream.INSTANCE;
	}

	@Getter
	@Setter
	protected String language;

	public IFileStream() {
		language = Frontends.language;
	}
	
	public abstract void determineLanguage();
	
	public abstract IFileStream forInput();
	public abstract IFileStream forOutput(String filename);
	
	public abstract int nextByte();
	public abstract void writeByte(int b);
	
	public abstract void seek(long pos);
	
	public abstract String getName();

	public void writeBytes(byte[] bytes) {
		for (byte b : bytes)
			writeByte(b);
	}

	public static class Transformer implements IStringTransformer<IFileStream> {

		@Override
		public IFileStream apply(String path) {
			return ofFile(path).forInput();
		}
		
	}
	
}

@RequiredArgsConstructor
@ToString(callSuper = true)
final class PathStream extends IFileStream {

	private final String path;
	private String filename;
	private RandomAccessFile file;
	
	@Override
	public void determineLanguage() {
		if(language == null)
			language = Frontends.determineByExtension(path);
	}

	@Override
	public IFileStream forInput() {
		try {
			file = new RandomAccessFile(path, "r");
			return this;
		}
		catch (IOException e) {
			throw new IllegalArgumentException("invalid path: " + e.getMessage());
		}
	}

	@Override
	public IFileStream forOutput(String filename) {
		String path = this.path;

		if (path.endsWith("/")) {
			path += filename;
			this.filename = filename;
		}

		File file = new File(path).getAbsoluteFile();

		try {
			file.getParentFile().mkdirs();

			this.file = new RandomAccessFile(file, "rw");
			return this;
		}
		catch (IOException e) {
			throw new IllegalArgumentException("invalid path: " + e.getMessage());
		}
	}

	@Override
	public int nextByte() {
		try {
			return file.read();
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Failed to read from file", e);
		}
	}

	@Override
	public void writeByte(int b) {
		try {
			file.write(b);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Failed to write to file", e);
		}
	}
	
	@Override
	public void seek(long pos) {
		try {
			file.seek(pos);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Failed to set file pointer", e);
		}
	}
	
	@Override
	public String getName() {
		if(filename != null)
			return path + "/" + filename;
		
		return path;
	}

}

@ToString(callSuper = true)
final class StdoutStream extends IFileStream {

	static final StdoutStream INSTANCE = new StdoutStream();

	@Override
	public void determineLanguage() {
		throw new UnsupportedOperationException("Cannot determine language of stdout");
	}
	
	@Override
	public IFileStream forInput() {
		throw new UnsupportedOperationException("Cannot read from stdout");
	}
	
	@Override
	public IFileStream forOutput(String filename) {
		return this;
	}
	
	@Override
	public int nextByte() {
		throw new UnsupportedOperationException("Cannot read from stdout");
	}

	@Override
	public void writeByte(int b) {
		System.out.write(b);
	}

	@Override
	public void seek(long pos) {
		throw new UnsupportedOperationException("Cannot seek in stdout");
	}
	
	@Override
	public String getName() {
		return "stdout";
	}

}

@RequiredArgsConstructor
@ToString(callSuper = true)
final class StringStream extends IFileStream {

	private final byte[] bytes;
	private int offset;

	@Override
	public void determineLanguage() {
		throw new UnsupportedOperationException("Cannot determine language of string");
	}
	
	@Override
	public IFileStream forInput() {
		return this;
	}
	
	@Override
	public IFileStream forOutput(String filename) {
		throw new UnsupportedOperationException("Cannot write to string");
	}
	
	@Override
	public int nextByte() {
		if(offset < 0 || offset >= bytes.length)
			return -1;
		
		return bytes[offset++];
	}

	@Override
	public void writeByte(int b) {
		throw new UnsupportedOperationException("Cannot write to string");
	}

	@Override
	public void seek(long pos) {
		offset = (int) pos;
	}
	
	@Override
	public String getName() {
		return "<command-line>";
	}

}
