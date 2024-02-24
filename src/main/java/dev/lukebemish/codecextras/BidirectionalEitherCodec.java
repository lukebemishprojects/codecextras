package dev.lukebemish.codecextras;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.function.Function;

// TODO: test
public class BidirectionalEitherCodec<F, S> implements Codec<Either<F, S>> {
    private final Codec<Asymmetry<F, Either<F, S>>> first;
    private final Codec<Asymmetry<S, Either<F, S>>> second;

    public static <F, S> Codec<Either<F, S>> asymmetrical(Codec<Asymmetry<F, Either<F, S>>> first, Codec<Asymmetry<S, Either<F, S>>> second) {
        return new BidirectionalEitherCodec<>(
            first,
            second
        );
    }

    public static <F, S> Codec<Either<F, S>> simple(Codec<F> first, Codec<S> second) {
        return asymmetrical(
            Asymmetry.flatMapEncoding(
                Asymmetry.split(first, Function.identity(), Function.identity()),
                e -> e.map(DataResult::success, r -> DataResult.error(() -> "Attempted to encode right value with left codec"))
            ),
            Asymmetry.flatMapEncoding(
                Asymmetry.split(second, Function.identity(), Function.identity()),
                e -> e.map(r -> DataResult.error(() -> "Attempted to encode left value with right codec"), DataResult::success)
            )
        );
    }

    public static <F> Codec<F> orElse(Codec<F> first, Codec<F> second) {
        return asymmetrical(
            Asymmetry.flatMapEncoding(
                Asymmetry.split(first, Function.identity(), Function.identity()),
                e -> e.map(DataResult::success, DataResult::success)
            ),
            Asymmetry.flatMapEncoding(
                Asymmetry.split(second, Function.identity(), Function.identity()),
                e -> e.map(DataResult::success, DataResult::success)
            )
        ).xmap(e -> e.map(Function.identity(), Function.identity()), Either::left);
    }

    private BidirectionalEitherCodec(Codec<Asymmetry<F, Either<F, S>>> first, Codec<Asymmetry<S, Either<F, S>>> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> ops, T input) {
        return CodecExtras.orElseGet(
            first.decode(ops, input).flatMap(r -> CodecExtras.flattenLeft(r.mapFirst(Asymmetry::decoding))).map(r -> r.mapFirst(Either::left)),
            () -> second.decode(ops, input).flatMap(r -> CodecExtras.flattenLeft(r.mapFirst(Asymmetry::decoding))).map(r -> r.mapFirst(Either::right))
        );
    }

    @Override
    public <T> DataResult<T> encode(Either<F, S> input, DynamicOps<T> ops, T prefix) {
        return CodecExtras.orElseGet(
            first.encode(Asymmetry.ofEncoding(input), ops, prefix),
            () -> second.encode(Asymmetry.ofEncoding(input), ops, prefix)
        );
    }
}
