package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K2;

public final class Key2<A, B> implements App2<Key2.Mu, A, B> {
    public static final class Mu implements K2 {
        private Mu() {
        }
    }

    public static <A, B> Key2<A, B> unbox(App2<Mu, A, B> box) {
        return (Key2<A, B>) box;
    }

    private final String name;

    private Key2(String name) {
        this.name = name;
    }

    public static <A, B> Key2<A, B> create(String name) {
        var className = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getSimpleName();
        return new Key2<>(className + ":" + name);
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "Key2[" + name + "]";
    }
}
