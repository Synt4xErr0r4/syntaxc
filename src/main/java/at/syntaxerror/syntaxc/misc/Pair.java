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
package at.syntaxerror.syntaxc.misc;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * @author Thomas Kasper
 * 
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class Pair<A, B> {

	private static final Pair<?, ?> EMPTY = new Pair<>(null, null);
	
	@SuppressWarnings("unchecked")
	public static <A, B> Pair<A, B> empty() {
		return (Pair<A, B>) EMPTY;
	}
	
	public static <A, B> Pair<A, B> ofFirst(A first) {
		return first == null
			? empty()
			: new Pair<A, B>(first, null);
	}

	public static <A, B> Pair<A, B> ofSecond(B second) {
		return second == null
			? empty()
			: new Pair<A, B>(null, second);
	}
	
	public static <A, B> Pair<A, B> of(A first, B second) {
		return first == null && second == null
			? empty()
			: new Pair<A, B>(first, second);
	}
	
	private final A first;
	private final B second;

	public boolean hasNone() {
		return first == null
			&& second == null;
	}
	
	public boolean hasBoth() {
		return first != null
			&& second != null;
	}

	public boolean hasAny() {
		return first != null
			|| second != null;
	}
	
	public boolean hasFirst() {
		return first != null;
	}
	
	public boolean hasSecond() {
		return second != null;
	}
	
	public boolean hasOnlyFirst() {
		return first != null
			&& second == null;
	}
	
	public boolean hasOnlySecond() {
		return second != null
			&& first == null;
	}
	
	public Pair<A, B> withFirst(A first) {
		return of(first, second);
	}

	public Pair<A, B> withSecond(B second) {
		return of(first, second);
	}
	
	public Optional<A> getFirstOptional() {
		return Optional.ofNullable(first);
	}

	public Optional<B> getSecondOptional() {
		return Optional.ofNullable(second);
	}

	public void ifBothPresent(BiConsumer<A, B> consumer) {
		if(hasBoth()) consumer.accept(first, second);
	}

	public void ifAnyPresent(BiConsumer<A, B> consumer) {
		if(hasAny()) consumer.accept(first, second);
	}

	public void ifPresent(Consumer<A> consumerFirst, Consumer<B> consumerSecond) {
		ifFirstPresent(consumerFirst);
		ifSecondPresent(consumerSecond);
	}
	
	public void ifFirstPresent(Consumer<A> consumer) {
		if(hasFirst()) consumer.accept(first);
	}
	
	public void ifSecondPresent(Consumer<B> consumer) {
		if(hasSecond()) consumer.accept(second);
	}
	
	public <X, Y> Pair<X, Y> mapFlat(BiFunction<A, B, Pair<X, Y>> map) {
		return map.apply(first, second);
	}
	
	public <X, Y> Pair<X, Y> map(@NonNull Function<A, X> mapFirst, @NonNull Function<B, Y> mapSecond) {
		return of(mapFirst.apply(first), mapSecond.apply(second));
	}
	
	public <X> Pair<X, B> mapFirst(@NonNull Function<A, X> map) {
		return of(map.apply(first), second);
	}
	
	public <Y> Pair<A, Y> mapSecond(@NonNull Function<B, Y> map) {
		return of(first, map.apply(second));
	}
	
	public Pair<B, A> swap() {
		return of(second, first);
	}
	
}
