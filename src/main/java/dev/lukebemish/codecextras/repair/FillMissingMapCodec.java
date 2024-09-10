package dev.lukebemish.codecextras.repair;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.lukebemish.codecextras.companion.AccompaniedOps;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class FillMissingMapCodec<A> extends MapCodec<A> {
    private final MapCodec<A> delegate;
    private final MapRepair<A> fallback;
    private final boolean lenient;

    private FillMissingMapCodec(MapCodec<A> delegate, MapRepair<A> fallback, boolean lenient) {
        this.delegate = delegate;
        this.fallback = fallback;
        this.lenient = lenient;
    }

    public static <A> MapCodec<A> fieldOf(MapCodec<A> codec, MapRepair<A> fallback, boolean lenient) {
        return new FillMissingMapCodec<>(codec, fallback, lenient);
    }

    public static <A> MapCodec<A> fieldOf(MapCodec<A> codec, MapRepair<A> fallback) {
        return fieldOf(codec, fallback, true);
    }

    public static <A> MapCodec<A> strictFieldOf(MapCodec<A> codec, MapRepair<A> fallback) {
        return fieldOf(codec, fallback, false);
    }

    public static <A> MapCodec<A> fieldOf(Codec<A> codec, String field, Repair<A> fallback) {
        return fieldOf(codec, field, fallback, true);
    }

    public static <A> MapCodec<A> strictFieldOf(Codec<A> codec, String field, Repair<A> fallback) {
        return fieldOf(codec, field, fallback, false);
    }

    public static <A> MapCodec<A> fieldOf(Codec<A> codec, String field, Repair<A> fallback, boolean lenient) {
        return fieldOf(codec.fieldOf(field), fallback.fieldOf(field), lenient);
    }

    public static <A> MapCodec<A> fieldOf(Codec<A> codec, String field, A fallback) {
        return fieldOf(codec, field, fallback, true);
    }

    public static <A> MapCodec<A> strictFieldOf(Codec<A> codec, String field, A fallback) {
        return fieldOf(codec, field, fallback, false);
    }

    public static <A> MapCodec<A> fieldOf(Codec<A> codec, String field, A fallback, boolean lenient) {
        return fieldOf(codec, field, new Repair<>() {
            @Override
            public <T> A repair(DynamicOps<T> ops, T flawed) {
                return fallback;
            }
        }, lenient);
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return delegate.keys(ops);
    }

    @Override
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
        boolean allEmpty = delegate.keys(ops).allMatch(key -> input.get(key) == null);
        if (allEmpty) {
            return DataResult.success(fallback.repair(ops, input));
        }
        var original = delegate.decode(ops, input);
        if (lenient && original.error().isPresent()) {
            return DataResult.success(fallback.repair(ops, input));
        }
        return original;
    }

    @Override
    public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return delegate.encode(input, ops, prefix);
    }

    public interface MapRepair<A> {
        <T> A repair(DynamicOps<T> ops, MapLike<T> flawed);
    }

    public interface Repair<A> {
        <T> A repair(DynamicOps<T> ops, T flawed);

        default MapRepair<A> fieldOf(String field) {
            return new MapRepair<>() {
                @Override
                public <T> A repair(DynamicOps<T> ops, MapLike<T> flawed) {
                    T value = flawed.get(field);
                    if (value == null) {
                        value = ops.empty();
                    }
                    if (ops instanceof AccompaniedOps<T> accompaniedOps) {
                        Optional<FillMissingLogOps<T>> fillMissingLogOps = accompaniedOps.getCompanion(FillMissingLogOps.TOKEN);
                        if (fillMissingLogOps.isPresent()) {
                            fillMissingLogOps.get().logMissingField(field, value);
                        }
                    }
                    return Repair.this.repair(ops, value);
                }
            };
        }
    }

    public static <A> Repair<A> lazyRepair(Supplier<A> supplier) {
        return new Repair<A>() {
            @Override
            public <T> A repair(DynamicOps<T> ops, T flawed) {
                return supplier.get();
            }
        };
    }

    @Override
    public String toString() {
        return "FillMissing[" + delegate + "]";
    }
}
