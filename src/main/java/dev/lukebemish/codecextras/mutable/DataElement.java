package dev.lukebemish.codecextras.mutable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A holdr for a mutable value.
 * @param <T> the type of the value
 */
public interface DataElement<T> {
    /**
     * Sets the contained value.
     * @param t the new value
     */
    void set(T t);

    /**
     * {@return the contained value}
     */
    T get();

    /**
     * {@return whether the value has been changed since it was last marked clean}
     */
    boolean dirty();

    /**
     * Marks the value as dirty or clean.
     * @param dirty whether the value is dirty
     */
    void setDirty(boolean dirty);

    /**
     * {@return whether this element should be included in an encoding of the full object it belongs in} For instance, if the
     * current value is the default value, it may not be necessary to include it in the full encoding.
     */
    default boolean includeInFullEncoding() {
        return true;
    }

    /**
     * Runs the provided consumer if the value is dirty in a partial encoding, or {@link #includeInFullEncoding()} in a
     * full encoding.
     * @param encodeFull whether this is a full encoding
     * @param consumer the consumer to run
     */
    default void ifEncoding(boolean encodeFull, Consumer<T> consumer) {
        synchronized (this) {
            if ((encodeFull && includeInFullEncoding()) || (!encodeFull && dirty())) {
                consumer.accept(get());
            }
        }
    }

    /**
     * Applies the provided function to the contained value if the value is dirty in a partial encoding, or
     * {@link #includeInFullEncoding()} in a full encoding.
     * @param encodeFull whether this is a full encoding
     * @param function the function to apply
     * @param orElse the value to return otherwise
     * @return the result of the function, or the provided value if the function was not applied
     * @param <O> the function's return type
     */
    default <O> O ifEncodingOrElse(boolean encodeFull, Function<T, O> function, Supplier<O> orElse) {
        synchronized (this) {
            if ((encodeFull && includeInFullEncoding()) || (!encodeFull && dirty())) {
                return function.apply(get());
            }
            return orElse.get();
        }
    }

    /**
     * A simple implementation of {@link DataElement} with a default value. This implementation is thread-safe, though
     * checks based on whether the value should be encoded should go through {@link #ifEncoding(boolean, Consumer)} or
     * {@link #ifEncodingOrElse(boolean, Function, Supplier)}.
     * @param <T> the type of the contained value
     */
    class Simple<T> implements DataElement<T> {
        private volatile T value;
        private volatile boolean dirty = false;
        private final T defaultValue;

        public Simple(T value) {
            this.value = value;
            this.defaultValue = value;
        }

        @Override
        public synchronized void set(T t) {
            if (!Objects.equals(this.value, t)) {
                this.value = t;
                setDirty(true);
            }
        }

        @Override
        public T get() {
            return this.value;
        }

        @Override
        public boolean dirty() {
            return this.dirty;
        }

        @Override
        public synchronized void setDirty(boolean dirty) {
            this.dirty = dirty;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Simple<?> simple)) return false;
            return Objects.equals(value, simple.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean includeInFullEncoding() {
            return !value.equals(defaultValue);
        }
    }
}
