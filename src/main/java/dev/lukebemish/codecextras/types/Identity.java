package dev.lukebemish.codecextras.types;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;

public record Identity<T>(T value) implements App<Identity.Mu, T> {
    public static final class Mu implements K1 {}

    public static <T> Identity<T> unbox(App<Mu, T> input) {
        return (Identity<T>) input;
    }
}
