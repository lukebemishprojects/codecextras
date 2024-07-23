package dev.lukebemish.codecextras.types;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;

public record Raised<F extends K1, T>(App<F, T> value) implements App<Raised.Mu<T>, F> {
    public static final class Mu<T> implements K1 { private Mu() {} }

    public static <M extends K1, T> Raised<M, T> unbox(App<Raised.Mu<T>, M> box) {
        return (Raised<M, T>) box;
    }
}
