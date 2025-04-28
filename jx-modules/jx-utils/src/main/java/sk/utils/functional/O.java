package sk.utils.functional;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Just short Optional
 *
 * @param <T>
 */
@SuppressWarnings("unused")
public final class O<T> implements Serializable {
    /**
     * Common instance for {@code empty()}.
     */
    private static final O<?> EMPTY = new O<>();

    /**
     * Returns an empty {@code O} instance.  No value is present for this
     * {@code O}.
     *
     * @param <T> The type of the non-existent value
     * @return an empty {@code O}
     * @apiNote Though it may be tempting to do so, avoid testing if an object is empty
     * by comparing with {@code ==} against instances returned by
     * {@code O.empty()}.  There is no guarantee that it is a singleton.
     * Instead, use {@link #isPresent()}.
     */
    public static <T> O<T> empty() {
        @SuppressWarnings("unchecked")
        O<T> t = (O<T>) EMPTY;
        return t;
    }

    public static <T> O<T> of(Optional<T> value) {
        return value == null ? empty() : value.map(O::of).orElse(empty());
    }

    /**
     * Returns an {@code O} describing the given non-{@code null}
     * value.
     *
     * @param value the value to describe, which must be non-{@code null}
     * @param <T>   the type of the value
     * @return an {@code O} with the value present
     * @throws NullPointerException if value is {@code null}
     */
    public static <T> O<T> of(T value) {
        return new O<>(value);
    }

    /**
     * Returns an {@code O} describing the given value, if
     * non-{@code null}, otherwise returns an empty {@code O}.
     *
     * @param value the possibly-{@code null} value to describe
     * @param <T>   the type of the value
     * @return an {@code O} with a present value if the specified value
     * is non-{@code null}, otherwise an empty {@code O}
     */
    public static <T> O<T> ofNullable(T value) {
        return value == null ? empty() : of(value);
    }

    /**
     * Returns an {@code O} describing the given value, if
     * non-{@code null}, otherwise returns an empty {@code O}.
     *
     * @param value the possibly-{@code null} value to describe
     * @param <T>   the type of the value
     * @return an {@code O} with a present value if the specified value
     * is non-{@code null}, otherwise an empty {@code O}
     */
    public static <T> O<T> ofNull(T value) {
        return ofNullable(value);
    }

    public static <A, B, C, D> O<D> allNotNull(O<A> oa, O<B> ob, O<C> oc, F3<A, B, C, O<D>> ifAllNotNull) {
        return oa.flatMap(a -> ob.flatMap(b -> oc.flatMap(c -> ifAllNotNull.apply(a, b, c))));
    }

    public static <A, B, C> O<C> allNotNull(O<A> oa, O<B> ob, F2<A, B, O<C>> ifAllNotNull) {
        return oa.flatMap(a -> ob.flatMap(b -> ifAllNotNull.apply(a, b)));
    }

    public Optional<T> toOpt() {
        return Optional.ofNullable(value);
    }

    public int size() {
        return isPresent() ? 1 : 0;
    }

    public O<T> or(Supplier<? extends Optional<? extends T>> supplier) {
        Objects.requireNonNull(supplier);
        if (isPresent()) {
            return this;
        } else {
            @SuppressWarnings("unchecked")
            O<T> r = of((Optional<T>) supplier.get());
            return Objects.requireNonNull(r);
        }
    }

    public O<T> or(F0<? extends O<? extends T>> supplier) {
        Objects.requireNonNull(supplier);
        if (isPresent()) {
            return this;
        } else {
            @SuppressWarnings("unchecked")
            O<T> r = (O<T>) supplier.get();
            return Objects.requireNonNull(r);
        }
    }

    public O<T> orVal(F0<? extends T> supplier) {
        Objects.requireNonNull(supplier);
        if (isPresent()) {
            return this;
        } else {
            O<T> r = O.ofNull(supplier.get());
            return Objects.requireNonNull(r);
        }
    }

    /**
     * If a value is present, returns the value, otherwise throws
     * {@code NoSuchElementException}.
     *
     * @return the non-{@code null} value described by this {@code O}
     * @throws NoSuchElementException if no value is present
     * @apiNote The preferred alternative to this method is {@link #orElseThrow()}.
     */
    public T get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    /**
     * If a value is present, returns {@code true}, otherwise {@code false}.
     *
     * @return {@code true} if a value is present, otherwise {@code false}
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * If a value is  not present, returns {@code true}, otherwise
     * {@code false}.
     *
     * @return {@code true} if a value is not present, otherwise {@code false}
     * @since 11
     */
    public boolean isEmpty() {
        return value == null;
    }

    /**
     * If a value is present, performs the given action with the value,
     * otherwise does nothing.
     *
     * @param action the action to be performed, if a value is present
     * @throws NullPointerException if value is present and the given action is
     *                              {@code null}
     */
    public void ifPresent(Consumer<? super T> action) {
        if (value != null) {
            action.accept(value);
        }
    }

    /**
     * If a value is present, performs the given action with the value,
     * otherwise performs the given empty-based action.
     *
     * @param action      the action to be performed, if a value is present
     * @param emptyAction the empty-based action to be performed, if no value is
     *                    present
     * @throws NullPointerException if a value is present and the given action
     *                              is {@code null}, or no value is present and the given empty-based
     *                              action is {@code null}.
     * @since 9
     */
    public void ifPresentOrElse(Consumer<? super T> action, R emptyAction) {
        if (value != null) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

    /**
     * If a value is present, and the value matches the given predicate,
     * returns an {@code O} describing the value, otherwise returns an
     * empty {@code O}.
     *
     * @param predicate the predicate to apply to a value, if present
     * @return an {@code O} describing the value of this
     * {@code O}, if a value is present and the value matches the
     * given predicate, otherwise an empty {@code O}
     * @throws NullPointerException if the predicate is {@code null}
     */
    public O<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        if (!isPresent()) {
            return this;
        } else {
            return predicate.test(value) ? this : empty();
        }
    }

    /**
     * If a value is present, returns an {@code O} describing (as if by
     * {@link #ofNullable}) the result of applying the given mapping function to
     * the value, otherwise returns an empty {@code O}.
     *
     * <p>If the mapping function returns a {@code null} result then this method
     * returns an empty {@code O}.
     *
     * @param mapper the mapping function to apply to a value, if present
     * @param <U>    The type of the value returned from the mapping function
     * @return an {@code O} describing the result of applying a mapping
     * function to the value of this {@code O}, if a value is
     * present, otherwise an empty {@code O}
     * @throws NullPointerException if the mapping function is {@code null}
     * @apiNote This method supports post-processing on {@code O} values, without
     * the need to explicitly check for a return status.  For example, the
     * following code traverses a stream of URIs, selects one that has not
     * yet been processed, and creates a path from that URI, returning
     * an {@code O<Path>}:
     *
     * <pre>{@code
     *     O<Path> p =
     *         uris.stream().filter(uri -> !isProcessedYet(uri))
     *                       .findFirst()
     *                       .map(Paths::get);
     * }</pre>
     * <p>
     * Here, {@code findFirst} returns an {@code O<URI>}, and then
     * {@code map} returns an {@code O<Path>} for the desired
     * URI if one exists.
     */
    public <U> O<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent()) {
            return empty();
        } else {
            return O.ofNullable(mapper.apply(value));
        }
    }

    public <U> U collect(F1<? super T, ? extends U> mapper, F0<? extends U> ifNone) {
        Objects.requireNonNull(mapper);
        Objects.requireNonNull(ifNone);
        if (!isPresent()) {
            return ifNone.apply();
        } else {
            return mapper.apply(value);
        }
    }

    /**
     * If a value is present, returns the result of applying the given
     * {@code O}-bearing mapping function to the value, otherwise returns
     * an empty {@code O}.
     *
     * <p>This method is similar to {@link #map(Function)}, but the mapping
     * function is one whose result is already an {@code O}, and if
     * invoked, {@code flatMap} does not wrap it within an additional
     * {@code O}.
     *
     * @param <U>    The type of value of the {@code O} returned by the
     *               mapping function
     * @param mapper the mapping function to apply to a value, if present
     * @return the result of applying an {@code O}-bearing mapping
     * function to the value of this {@code O}, if a value is
     * present, otherwise an empty {@code O}
     * @throws NullPointerException if the mapping function is {@code null} or
     *                              returns a {@code null} result
     */
    public <U> O<U> flatMap(Function<? super T, ? extends O<? extends U>> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent()) {
            return empty();
        } else {
            @SuppressWarnings("unchecked")
            O<U> r = (O<U>) mapper.apply(value);
            return Objects.requireNonNull(r);
        }
    }

    /**
     * If a value is present, returns a sequential {@link Stream} containing
     * only that value, otherwise returns an empty {@code Stream}.
     *
     * @return the O value as a {@code Stream}
     * @apiNote This method can be used to transform a {@code Stream} of O
     * elements to a {@code Stream} of present value elements:
     * <pre>{@code
     *     Stream<O<T>> os = ..
     *     Stream<T> s = os.flatMap(O::stream)
     * }</pre>
     * @since 9
     */
    public Stream<T> stream() {
        if (!isPresent()) {
            return Stream.empty();
        } else {
            return Stream.of(value);
        }
    }

    /**
     * If a value is present, returns the value, otherwise returns
     * {@code other}.
     *
     * @param other the value to be returned, if no value is present.
     *              May be {@code null}.
     * @return the value, if present, otherwise {@code other}
     */
    public T orElse(T other) {
        return value != null ? value : other;
    }

    /**
     * If a value is present, returns the value, otherwise returns the result
     * produced by the supplying function.
     *
     * @param supplier the supplying function that produces a value to be returned
     * @return the value, if present, otherwise the result produced by the
     * supplying function
     * @throws NullPointerException if no value is present and the supplying
     *                              function is {@code null}
     */
    public T orElseGet(Supplier<? extends T> supplier) {
        return value != null ? value : supplier.get();
    }

    /**
     * If a value is present, returns the value, otherwise throws
     * {@code NoSuchElementException}.
     *
     * @return the non-{@code null} value described by this {@code O}
     * @throws NoSuchElementException if no value is present
     * @since 10
     */
    public T orElseThrow() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    /**
     * If a value is present, returns the value, otherwise throws an exception
     * produced by the exception supplying function.
     *
     * @param <X>               Type of the exception to be thrown
     * @param exceptionSupplier the supplying function that produces an
     *                          exception to be thrown
     * @return the value, if present
     * @throws X                    if no value is present
     * @throws NullPointerException if no value is present and the exception
     *                              supplying function is {@code null}
     * @apiNote A method reference to the exception constructor with an empty argument
     * list can be used as the supplier. For example,
     * {@code IllegalStateException::new}
     */
    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (value != null) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Indicates whether some other object is "equal to" this {@code O}.
     * The other object is considered equal if:
     * <ul>
     * <li>it is also an {@code O} and;
     * <li>both instances have no value present or;
     * <li>the present values are "equal to" each other via {@code equals()}.
     * </ul>
     *
     * @param obj an object to be tested for equality
     * @return {@code true} if the other object is "equal to" this object
     * otherwise {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof O)) {
            return false;
        }

        O<?> other = (O<?>) obj;
        return Objects.equals(value, other.value);
    }

    /**
     * Returns the hash code of the value, if present, otherwise {@code 0}
     * (zero) if no value is present.
     *
     * @return hash code value of the present value or {@code 0} if no value is
     * present
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    /**
     * Returns a non-empty string representation of this {@code O}
     * suitable for debugging.  The exact presentation format is unspecified and
     * may vary between implementations and versions.
     *
     * @return the string representation of this instance
     * @implSpec If a value is present the result must include its string representation
     * in the result.  Empty and present {@code O}s must be unambiguously
     * differentiable.
     */
    @Override
    public String toString() {
        return value != null
               ? String.format("O[%s]", value)
               : "O.empty";
    }


    /**
     * If non-null, the value; if null, indicates no value is present
     */
    private final T value;

    /**
     * Constructs an empty instance.
     *
     * @implNote Generally only one empty instance, {@link O#EMPTY},
     * should exist per VM.
     */
    private O() {
        this.value = null;
    }

    /**
     * Constructs an instance with the described value.
     *
     * @param value the non-{@code null} value to describe
     * @throws NullPointerException if value is {@code null}
     */
    private O(T value) {
        this.value = Objects.requireNonNull(value);
    }
}
