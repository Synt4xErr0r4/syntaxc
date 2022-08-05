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
public class Pair<L, R> {

	private static final Pair<?, ?> EMPTY = new Pair<>(null, null);
	
	@SuppressWarnings("unchecked")
	public static <A, B> Pair<A, B> empty() {
		return (Pair<A, B>) EMPTY;
	}
	
	public static <A, B> Pair<A, B> ofLeft(A left) {
		return left == null
			? empty()
			: new Pair<>(left, null);
	}

	public static <A, B> Pair<A, B> ofRight(B right) {
		return right == null
			? empty()
			: new Pair<>(null, right);
	}
	
	public static <A, B> Pair<A, B> of(A left, B right) {
		return left == null && right == null
			? empty()
			: new Pair<>(left, right);
	}
	
	private final L left;
	private final R right;

	public boolean hasNeither() {
		return left == null
			&& right == null;
	}
	
	public boolean hasBoth() {
		return left != null
			&& right != null;
	}

	public boolean hasAny() {
		return left != null
			|| right != null;
	}
	
	public boolean hasLeft() {
		return left != null;
	}
	
	public boolean hasRight() {
		return right != null;
	}
	
	public boolean hasOnlyLeft() {
		return left != null
			&& right == null;
	}
	
	public boolean hasOnlyRight() {
		return right != null
			&& left == null;
	}
	
	public Pair<L, R> withLeft(L left) {
		return of(left, right);
	}

	public Pair<L, R> withRight(R right) {
		return of(left, right);
	}
	
	public Optional<L> getLeftOptional() {
		return Optional.ofNullable(left);
	}

	public Optional<R> getRightOptional() {
		return Optional.ofNullable(right);
	}

	public void ifBothPresent(BiConsumer<L, R> consumer) {
		if(hasBoth()) consumer.accept(left, right);
	}

	public void ifAnyPresent(BiConsumer<L, R> consumer) {
		if(hasAny()) consumer.accept(left, right);
	}

	public void ifPresent(Consumer<L> consumerLeft, Consumer<R> consumerRight) {
		ifLeftPresent(consumerLeft);
		ifRightPresent(consumerRight);
	}
	
	public void ifLeftPresent(Consumer<L> consumer) {
		if(hasLeft()) consumer.accept(left);
	}
	
	public void ifRightPresent(Consumer<R> consumer) {
		if(hasRight()) consumer.accept(right);
	}
	
	public <X, Y> Pair<X, Y> mapFlat(BiFunction<L, R, Pair<X, Y>> map) {
		return map.apply(left, right);
	}
	
	public <X, Y> Pair<X, Y> map(@NonNull Function<L, X> mapFirst, @NonNull Function<R, Y> mapSecond) {
		return of(mapFirst.apply(left), mapSecond.apply(right));
	}
	
	public <X> Pair<X, R> mapLeft(@NonNull Function<L, X> map) {
		return of(map.apply(left), right);
	}
	
	public <Y> Pair<L, Y> mapRight(@NonNull Function<R, Y> map) {
		return of(left, map.apply(right));
	}
	
	public Pair<R, L> swap() {
		return of(right, left);
	}
	
}
