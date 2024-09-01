package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;

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

    public static <A> Key<A> create(String name) {
        var className = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getSimpleName();
        return new Key<>(className + ":" + name);
    }

    public String name() {
        return name;
    }
}
