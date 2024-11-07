package dev.lukebemish.codecextras;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.Objects;
import java.util.stream.Stream;

public final class XorMapCodec<F, S> extends MapCodec<Either<F, S>> {
    private final MapCodec<F> first;
    private final MapCodec<S> second;

    private XorMapCodec(MapCodec<F> first, MapCodec<S> second) {
        this.first = first;
        this.second = second;
    }

    public static <F, S> XorMapCodec<F, S> of(MapCodec<F> first, MapCodec<S> second) {
        return new XorMapCodec<>(first, second);
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
        return Stream.concat(this.first.keys(dynamicOps), this.second.keys(dynamicOps));
    }

    @Override
    public <T> DataResult<Either<F, S>> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
        var firstResult = this.first.decode(dynamicOps, mapLike);
        if (firstResult.isError()) {
            return this.second.decode(dynamicOps, mapLike).map(Either::right);
        }
        var secondResult = this.second.decode(dynamicOps, mapLike);
        if (secondResult.isError()) {
            return firstResult.map(Either::left);
        }
        return DataResult.error(() -> "Both alternatives read successfully, can not pick the correct one; first: " + firstResult.getOrThrow() + ", second: " + secondResult.getOrThrow());
    }

    @Override
    public <T> RecordBuilder<T> encode(Either<F, S> fsEither, DynamicOps<T> dynamicOps, RecordBuilder<T> recordBuilder) {
        return fsEither.map(
            f -> this.first.encode(f, dynamicOps, recordBuilder),
            s -> this.second.encode(s, dynamicOps, recordBuilder)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof XorMapCodec<?, ?> that)) return false;
        return Objects.equals(first, that.first) && Objects.equals(second, that.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "XorMapCodec[" + this.first + ", " + this.second + "]";
    }
}
