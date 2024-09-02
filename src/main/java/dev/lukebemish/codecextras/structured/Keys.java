package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A collection of keys and their associated values. Each key is parameterized by a type extending {@code L}, and a
 * value matching a given key will be of the type of {@code Mu} applied to the key's type.
 * @param <Mu> the type function mapping key type parameters to value types
 * @param <L> the bound on the key type parameters
 */
public final class Keys<Mu extends K1, L> {
    private final IdentityHashMap<Key<? extends L>, App<Mu, ? extends L>> keys;

    private Keys(IdentityHashMap<Key<? extends L>, App<Mu, ? extends L>> keys) {
        this.keys = keys;
    }

    /**
     * {@return the value associated with a key, if present}
     * @param key the key to search for
     * @param <A> the type parameter of the value associated with the key
     */
    @SuppressWarnings("unchecked")
    public <A extends L> Optional<App<Mu, A>> get(Key<A> key) {
        return Optional.ofNullable((App<Mu, A>) keys.get(key));
    }

    /**
     * {@return a new instance with the same keys, with values whose type is the application of a different type function}
     * @param converter converts {@code Mu<T>} to {@code N<T>} for each key's type parameter {@code T extends L}
     * @param <N> the type function associated with the new {@link Keys}
     */
    public <N extends K1> Keys<N, L> map(Converter<Mu, N, L> converter) {
        var map = new IdentityHashMap<Key<? extends L>, App<N, ? extends L>>();
        keys.forEach((key, value) -> map.put(key, converter.convert(value)));
        return new Keys<>(map);
    }

    /**
     * Effectively "lifts" values from {@code Mu<T>} to {@code N<T>}. Type parameters are bounded by {@code L}.
     * @param <Mu>
     * @param <N>
     * @param <L>
     */
    public interface Converter<Mu extends K1, N extends K1, L> {
        /**
         * {@return a single value, converted}
         * @param input the value to convert
         * @param <A> the type parameter of the value to convert
         */
        <A extends L> App<N, A> convert(App<Mu, A> input);
    }

    /**
     * {@return a new key set builder}
     * @param <Mu> the type function mapping key type parameters to value types
     * @param <L> the bound on the key type parameters
     */
    public static <Mu extends K1, L> Builder<Mu, L> builder() {
        return new Builder<>();
    }

    /**
     * {@return a new key set combining this set with another}
     * Values in the other set will overwrite values in this set if they share a key.
     * @param other the other key set to combine with this one
     */
    public Keys<Mu, L> join(Keys<Mu, L> other) {
        var map = new IdentityHashMap<>(this.keys);
        map.putAll(other.keys);
        return new Keys<>(map);
    }

    /**
     * {@return a new key set with the single key-value pair provided added}
     * @param key the key to add
     * @param value the value to associate with the key
     * @param <A> the type parameter for the key
     */
    public <A extends L> Keys<Mu, L> with(Key<A> key, App<Mu, A> value) {
        var map = new IdentityHashMap<>(this.keys);
        map.put(key, value);
        return new Keys<>(map);
    }

    public final static class Builder<Mu extends K1, L> {
        private final Map<Key<? extends L>, App<Mu, ? extends L>> keys = new IdentityHashMap<>();

        public <A extends L> Builder<Mu, L> add(Key<A> key, App<Mu, A> value) {
            keys.put(key, value);
            return this;
        }

        public Keys<Mu, L> build() {
            return new Keys<>(new IdentityHashMap<>(keys));
        }
    }
}
