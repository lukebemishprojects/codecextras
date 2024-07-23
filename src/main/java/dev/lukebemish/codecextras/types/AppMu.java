package dev.lukebemish.codecextras.types;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;

public record AppMu<M extends K1, T>(App<M, T> value) implements App<AppMu.Mu, M> {
    public static final class Mu implements K1 { private Mu() {} }

    public static <M extends K1, T> AppMu<M, T> unbox(App<AppMu.Mu, M> box) {
        return (AppMu<M, T>) box;
    }
}
