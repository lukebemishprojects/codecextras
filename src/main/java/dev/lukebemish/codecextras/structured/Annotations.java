package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.K1;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public final class Annotations {
    /**
     * A comment that a field in a structure should be serialized with.
     */
    public static final Key<String> COMMENT = Key.create("comment");
    /**
     * A human-readable title for a part of a structure.
     */
    public static final Key<String> TITLE = Key.create("title");
    /**
     * A human-readable description for a part of a structure; if missing, falls back to {@link #COMMENT}.
     */
    public static final Key<String> DESCRIPTION = Key.create("description");

    @SuppressWarnings("unchecked")
    public <A> Optional<A> get(Key<A> key) {
        return Optional.ofNullable((A) keys.get(key));
    }

    public static <Mu extends K1> Keys.Builder<Mu> builder() {
        return new Keys.Builder<>();
    }

    public Annotations join(Annotations other) {
        var map = new IdentityHashMap<>(this.keys);
        map.putAll(other.keys);
        return new Annotations(map);
    }

    public <A> Annotations with(Key<A> key, A value) {
        var map = new IdentityHashMap<>(this.keys);
        map.put(key, value);
        return new Annotations(map);
    }

    public static Annotations empty() {
        return EMPTY;
    }

    public final static class Builder {
        private final Map<Key<?>, Object> keys = new IdentityHashMap<>();

        public <A> Builder add(Key<A> key, A value) {
            keys.put(key, value);
            return this;
        }

        public Annotations build() {
            return new Annotations(new IdentityHashMap<>(keys));
        }
    }

    private static final Annotations EMPTY = new Annotations(new IdentityHashMap<>());

    private final IdentityHashMap<Key<?>, Object> keys;

    private Annotations(IdentityHashMap<Key<?>, Object> keys) {
        this.keys = keys;
    }
}
