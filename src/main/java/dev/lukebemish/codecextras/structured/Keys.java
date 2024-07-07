package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public final class Keys<Mu extends K1> {
    private final IdentityHashMap<Key<?>, App<Mu, ?>> keys;

    private Keys(IdentityHashMap<Key<?>, App<Mu, ?>> keys) {
        this.keys = keys;
    }

    @SuppressWarnings("unchecked")
    public <A> Optional<App<Mu, A>> get(Key<A> key) {
        return Optional.ofNullable((App<Mu, A>) keys.get(key));
    }

    public <N extends K1> Keys<N> map(Converter<Mu, N> converter) {
        var map = new IdentityHashMap<Key<?>, App<N, ?>>();
        keys.forEach((key, value) -> map.put(key, converter.convert(value)));
        return new Keys<>(map);
    }

    public interface Converter<Mu extends K1, N extends K1> {
        <A> App<N, A> convert(App<Mu, A> input);
    }

    public static <Mu extends K1> Builder<Mu> builder() {
        return new Builder<>();
    }

    public Keys<Mu> join(Keys<Mu> other) {
        var map = new IdentityHashMap<>(this.keys);
        map.putAll(other.keys);
        return new Keys<>(map);
    }

    public final static class Builder<Mu extends K1> {
        private final Map<Key<?>, App<Mu, ?>> keys = new IdentityHashMap<>();

        public <A> Builder<Mu> add(Key<A> key, App<Mu, A> value) {
            keys.put(key, value);
            return this;
        }

        public Keys<Mu> build() {
            return new Keys<>(new IdentityHashMap<>(keys));
        }
    }
}
