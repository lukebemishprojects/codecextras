package dev.lukebemish.codecextras.types;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;

public record AppMu<Mu extends K1, T>(App<Mu, T> value) implements App<AppMu.Mu, Mu> {
    public static final class Mu implements K1 { private Mu() {} }

    public static <Mu extends K1, T> AppMu<Mu, T> unbox(App<AppMu.Mu, Mu> box) {
        return (AppMu<Mu, T>) box;
    }
}
