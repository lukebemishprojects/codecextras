package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public final class Keys2<Mu extends K2, L1, L2> {
    private final IdentityHashMap<Key2<? extends L1, ? extends L2>, App2<Mu, ? extends L1, ? extends L2>> keys;

    private Keys2(IdentityHashMap<Key2<? extends L1, ? extends L2>, App2<Mu, ? extends L1, ? extends L2>> keys) {
        this.keys = keys;
    }

    @SuppressWarnings("unchecked")
    public <A extends L1, B extends L2> Optional<App2<Mu, A, B>> get(Key2<A, B> key) {
        return Optional.ofNullable((App2<Mu, A, B>) keys.get(key));
    }

    public <N extends K2> Keys2<N, L1, L2> map(Converter<Mu, N, L1, L2> converter) {
        var map = new IdentityHashMap<Key2<? extends L1, ? extends L2>, App2<N, ? extends L1, ? extends L2>>();
        keys.forEach((key, value) -> map.put(key, converter.convert(value)));
        return new Keys2<>(map);
    }

    public interface Converter<Mu extends K2, N extends K2, L1, L2> {
        <A extends L1, B extends L2> App2<N, A, B> convert(App2<Mu, A, B> input);
    }

    public static <Mu extends K2, L1, L2> Builder<Mu, L1, L2> builder() {
        return new Builder<>();
    }

    public Keys2<Mu, L1, L2> join(Keys2<Mu, L1, L2> other) {
        var map = new IdentityHashMap<>(this.keys);
        map.putAll(other.keys);
        return new Keys2<>(map);
    }

    public <A extends L1, B extends L2> Keys2<Mu, L1, L2> with(Key2<A, B> key, App2<Mu, A, B> value) {
        var map = new IdentityHashMap<>(this.keys);
        map.put(key, value);
        return new Keys2<>(map);
    }

    public final static class Builder<Mu extends K2, L1, L2> {
        private final Map<Key2<? extends L1, ? extends L2>, App2<Mu, ? extends L1, ? extends L2>> keys = new IdentityHashMap<>();

        public <A extends L1, B extends L2> Builder<Mu, L1, L2> add(Key2<A, B> key, App2<Mu, A, B> value) {
            keys.put(key, value);
            return this;
        }

        public Keys2<Mu, L1, L2> build() {
            return new Keys2<>(new IdentityHashMap<>(keys));
        }

        public Builder<Mu, L1, L2> join(Keys2<Mu, L1, L2> other) {
            keys.putAll(other.keys);
            return this;
        }
    }
}
