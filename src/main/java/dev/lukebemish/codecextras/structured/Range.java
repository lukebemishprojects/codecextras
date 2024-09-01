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
    public Range {
        if (min.compareTo(max) >= 0) {
            throw new IllegalArgumentException("min >= max");
        }
    }

    public N clamp(N value) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }

    public static final class Mu implements K1 {
        private Mu() {
        }
    }

    public static <N extends Number & Comparable<N>> Range<N> unbox(App<Range.Mu, N> app) {
        return (Range<N>) app;
    }
}
