package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;

/**
 * A range of values
 *
 * @param min minimum value, inclusive
 * @param max maximum value, inclusive
 * @param <N> number type of the range
 */
public record Range<N extends Number & Comparable<N>>(N min, N max) implements App<Range.Mu, N> {
    public static final class Mu implements K1 {
        private Mu() {
        }
    }

    public static <N extends Number & Comparable<N>> Range<N> unbox(App<Range.Mu, N> app) {
        return (Range<N>) app;
    }
}
