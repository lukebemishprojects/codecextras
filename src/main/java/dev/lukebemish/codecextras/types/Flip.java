package dev.lukebemish.codecextras.types;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;

/**
 * Allows the order of arguments to {@link App} to be "flipped", where {@code App F T = F[T]} becomes {@code App (Flip T) F = F[T]}
 * when boxed.
 * @param value the boxed value
 * @param <F> the type function to flip
 * @param <T> the type parameter for {@link F}
 */
public record Flip<F extends K1, T>(App<F, T> value) implements App<Flip.Mu<T>, F> {
    public static final class Mu<T> implements K1 { private Mu() {} }

    public static <M extends K1, T> Flip<M, T> unbox(App<Flip.Mu<T>, M> box) {
        return (Flip<M, T>) box;
    }
}
