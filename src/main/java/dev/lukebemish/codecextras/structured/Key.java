package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;

/**
 * A key which might be associated with a value. Keys carry a type parameter, and are compared by identity.
 * @param <A> the type parameter carried by the key, which may determine the type of the associated value
 */
public final class Key<A> implements App<Key.Mu, A> {
    public static final class Mu implements K1 {
        private Mu() {
        }
    }

    public static <A> Key<A> unbox(App<Mu, A> box) {
        return (Key<A>) box;
    }

    private final String name;

    private Key(String name) {
        this.name = name;
    }

    /**
     * {@return a new key with the given name}
     * Names are used for debugging purposes only, as keys are compared by identity. The name of the calling class will
     * also be included in the key's name.
     * @param name the name of the key
     * @param <A> the type parameter carried by the key
     */
    public static <A> Key<A> create(String name) {
        var className = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getSimpleName();
        return new Key<>(className + ":" + name);
    }

    /**
     * {@return the name of the key}
     * Names are used for debugging purposes only, as keys are compared by identity; two keys with the same name are not
     * necessarily the same key.
     */
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "Key[" + name + "]";
    }
}
