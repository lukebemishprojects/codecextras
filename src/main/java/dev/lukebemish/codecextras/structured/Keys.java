package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public final class Keys<Mu extends K1, L> {
    private final IdentityHashMap<Key<? extends L>, App<Mu, ? extends L>> keys;

    private Keys(IdentityHashMap<Key<? extends L>, App<Mu, ? extends L>> keys) {
        this.keys = keys;
    }

    @SuppressWarnings("unchecked")
    public <A extends L> Optional<App<Mu, A>> get(Key<A> key) {
        return Optional.ofNullable((App<Mu, A>) keys.get(key));
    }

    public <N extends K1> Keys<N, L> map(Converter<Mu, N, L> converter) {
        var map = new IdentityHashMap<Key<? extends L>, App<N, ? extends L>>();
        keys.forEach((key, value) -> map.put(key, converter.convert(value)));
        return new Keys<>(map);
    }

    public interface Converter<Mu extends K1, N extends K1, L> {
        <A extends L> App<N, A> convert(App<Mu, A> input);
    }

    public static <Mu extends K1, L> Builder<Mu, L> builder() {
        return new Builder<>();
    }

    public Keys<Mu, L> join(Keys<Mu, L> other) {
        var map = new IdentityHashMap<>(this.keys);
        map.putAll(other.keys);
        return new Keys<>(map);
    }

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
