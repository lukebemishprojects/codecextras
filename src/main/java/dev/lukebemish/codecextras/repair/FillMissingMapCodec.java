package dev.lukebemish.codecextras.repair;

import com.mojang.serialization.*;
import dev.lukebemish.codecextras.companion.AccompaniedOps;
import java.util.Optional;
import java.util.stream.Stream;

public final class FillMissingMapCodec<A> extends MapCodec<A> {
    private final MapCodec<A> delegate;
    private final MapRepair<A> fallback;

    private FillMissingMapCodec(MapCodec<A> delegate, MapRepair<A> fallback) {
        this.delegate = delegate;
        this.fallback = fallback;
    }

    public static <A> MapCodec<A> of(MapCodec<A> codec, MapRepair<A> fallback) {
        return new FillMissingMapCodec<>(codec, fallback);
    }

    public static <A> MapCodec<A> fieldOf(Codec<A> codec, String field, Repair<A> fallback) {
        return of(codec.fieldOf(field), new MapRepair<>() {
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
                return fallback.repair(ops, value);
            }
        });
    }

    public static <A> MapCodec<A> fieldOf(Codec<A> codec, String field, A fallback) {
        return fieldOf(codec, field, new Repair<>() {
            @Override
            public <T> A repair(DynamicOps<T> ops, T flawed) {
                return fallback;
            }
        });
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return delegate.keys(ops);
    }

    @Override
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
        var original = delegate.decode(ops, input);
        if (original.error().isPresent()) {
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
    }

    @Override
    public String toString() {
        return "FillMissing[" + delegate + "]";
    }
}
