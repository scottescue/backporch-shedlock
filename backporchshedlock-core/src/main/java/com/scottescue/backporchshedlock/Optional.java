package com.scottescue.backporchshedlock;

import java.util.NoSuchElementException;

/**
 * A container object which may or may not contain a non-null value. If a value is present, {@code
 * isPresent()} will return {@code true} and {@code get()} will return the value.
 *
 * Additional methods that depend on the presence or absence of a contained value are provided, such
 * as {@code orElse()} (return a default value if value not present) and {@code ifPresent()}
 * (execute a block of code if the value is present).
 */
@SuppressWarnings("WeakerAccess")
public final class Optional<T> {

    @SuppressWarnings("unchecked")
    private static final Optional<?> EMPTY = new Optional(null);

    /**
     * Returns an empty {@code Optional} instance. No value is present for this Optional.
     *
     * Though it may be tempting to do so, avoid testing if an object is empty by comparing with
     * {@code ==} against instances returned by {@code Optional.empty()}. There is no guarantee that
     * it is a singleton. Instead, use {@link #isPresent()}
     *
     * @param <T> Type of the non-existent value
     * @return an empty {@code Optional}
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> empty() {
        return (Optional<T>) EMPTY;
    }

    private final T value;

    private Optional(T value) {
        this.value = value;
    }

    /**
     * Returns an Optional with the specified present non-null value.
     *
     * @param value the value to be present, which must be non-null
     * @param <T>   the class of the value
     * @return an Optional with the value present
     * @throws NullPointerException if value is null
     */
    public static <T> Optional<T> of(T value) {
        if (value == null) {
            throw new NullPointerException();
        }

        return new Optional<T>(value);
    }

    /**
     * Returns an Optional describing the specified value, if non-null otherwise returns an empty
     * Optional.
     *
     * @param value the possible-null value to describe
     * @param <T>   the class of the value
     * @return an Optional with a present value if the specified value is non-null, otherwise an
     * empty Optional
     */
    public static <T> Optional<T> ofNullable(T value) {
        if (value != null) {
            return new Optional<T>(value);
        }

        return empty();
    }

    /**
     * If a value is present in this {@code Optional}, returns the value, otherwise throws {@code
     * NoSuchElementException}.
     *
     * @return the non-null value held by this {@code Optional}
     * @throws NoSuchElementException if there is no value present
     * @see Optional#isPresent()
     */
    public T get() {
        if (isPresent()) {
            return value;
        }

        throw new NoSuchElementException("No value present");
    }

    public boolean isPresent() {
        return value != null;
    }

    /**
     * Return the value if present, otherwise return {@code other}.
     *
     * @param other the value to be returned if there is no value present, may be null
     * @return the value, if present, otherwise {@code other}
     */
    public T orElse(T other) {
        if (isPresent()) {
            return value;
        }

        return other;
    }

    /**
     * Indicates whether some other object is "equal to" this Optional. The other object is
     * considered equal if: <ul> <li>it is also an {@code Optional} and; <li>both instances have no
     * value present or; <li>the present values are "equal to" each other via {@code equals()}.
     * </ul>
     *
     * @param o an object to be tested for equality
     * @return {code true} if the other object is "equal to" this object otherwise {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Optional)) {
            return false;
        }

        if (isPresent()) {
            Optional<?> other = (Optional<?>) o;
            return value.equals(other.value);
        }

        return false;
    }

    /**
     * Returns the hash code value of the present value, if any, or 0 (zero) if no value is
     * present.
     *
     * @return hash code value of the present value or 0 if no value is present
     */
    @Override
    public int hashCode() {
        if (isPresent()) {
            return value.hashCode();
        }

        return 0;
    }

    /**
     * Returns a non-empty string representation of this Optional suitable for debugging. The exact
     * presentation format is unspecified and may vary between implementations and versions.
     *
     * <p> If a value is present the result must include its string representation in the result.
     * Empty and present Optionals must be unambiguously differentiable. </p>
     *
     * @return the string representation of this instance
     */
    @Override
    public String toString() {
        if (isPresent()) {
            return "Optional[" + value.toString() + "]";
        }

        return "Optional.empty";
    }
}
