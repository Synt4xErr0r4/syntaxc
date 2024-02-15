/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.collection;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import lombok.Getter;
import lombok.NonNull;

/**
 * 
 *
 * @author Thomas Kasper
 */
@Getter
public class ArrayIterator<E> implements Iterator<E> {

	public static <E> ArrayIterator<E> of(E[] array) {
		return new ArrayIterator<>(array);
	}
	
	public static ArrayIterator<Boolean> of(boolean[] array) {
		return of(Arrays.asList(array).toArray(Boolean[]::new));
	}

	public static ArrayIterator<Byte> of(byte[] array) {
		return of(Arrays.asList(array).toArray(Byte[]::new));
	}
	
	public static ArrayIterator<Short> of(short[] array) {
		return of(Arrays.asList(array).toArray(Short[]::new));
	}
	
	public static ArrayIterator<Integer> of(int[] array) {
		return of(Arrays.asList(array).toArray(Integer[]::new));
	}
	
	public static ArrayIterator<Long> of(long[] array) {
		return of(Arrays.asList(array).toArray(Long[]::new));
	}
	
	public static ArrayIterator<Float> of(float[] array) {
		return of(Arrays.asList(array).toArray(Float[]::new));
	}
	
	public static ArrayIterator<Double> of(double[] array) {
		return of(Arrays.asList(array).toArray(Double[]::new));
	}
	
	private E[] array;
	private int index;

	public ArrayIterator(@NonNull E[] array) {
		this.array = array;
	}
	
	@Override
	public boolean hasNext() {
		return index < array.length;
	}

	@Override
	public E next() {
		if(!hasNext())
			throw new NoSuchElementException();
		
		return array[index++];
	}
	
}
