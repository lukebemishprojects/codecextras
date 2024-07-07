package dev.lukebemish.codecextras;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Various codec utilities that do not have homes other places.
 */
public final class CodecExtras {
    private CodecExtras() {}

    /**
     * Flattens a {@link Codec} that handles {@link DataResult} into one that handles the wrapped type..
     * @param codec the codec to flatten
     * @return a codec that handles the wrapped type
     * @param <O> the wrapped type
     */
    public static <O> Codec<O> flatten(Codec<DataResult<O>> codec) {
        return codec.flatXmap(Function.identity(), o -> DataResult.success(DataResult.success(o)));
    }

    /**
     * Flattens a {@link MapCodec} that handles {@link DataResult} into one that handles the wrapped type..
     * @param codec the map codec to flatten
     * @return a map codec that handles the wrapped type
     * @param <O> the wrapped type
     */
    public static <O> MapCodec<O> flatten(MapCodec<DataResult<O>> codec) {
        return codec.flatXmap(Function.identity(), o -> DataResult.success(DataResult.success(o)));
    }

    /**
     * Raises a {@link Codec} that handles a type into one that handles a {@link DataResult} of that type.
     * @param codec the codec to raise
     * @return a codec that handles a {@link DataResult} of the type
     * @param <O> the type
     */
    public static <O> Codec<DataResult<O>> raise(Codec<O> codec) {
        return codec.flatXmap(o -> DataResult.success(DataResult.success(o)), Function.identity());
    }

    /**
     * Raises a {@link MapCodec} that handles a type into one that handles a {@link DataResult} of that type.
     * @param codec the map codec to raise
     * @return a map codec that handles a {@link DataResult} of the type
     * @param <O> the type
     */
    public static <O> MapCodec<DataResult<O>> raise(MapCodec<O> codec) {
        return codec.flatXmap(o -> DataResult.success(DataResult.success(o)), Function.identity());
    }

    /**
     * Pulls a {@link DataResult} out of the left side of a {@link Pair}.
     * @param pair the pair to flatten
     * @return a flattened data result
     * @param <L> the left type of the pair
     * @param <F> the right type of the pair
     */
    public static <L, F> DataResult<Pair<L, F>> flattenLeft(Pair<DataResult<L>, F> pair) {
        return pair.getFirst().map(l -> Pair.of(l, pair.getSecond()));
    }

    /**
     * Pulls a {@link DataResult} out of the right side of a {@link Pair}.
     * @param pair the pair to flatten
     * @return a flattened data result
     * @param <L> the left type of the pair
     * @param <F> the right type of the pair
     */
    public static <L, F> DataResult<Pair<L, F>> flattenRight(Pair<L, DataResult<F>> pair) {
        return pair.getSecond().map(f -> Pair.of(pair.getFirst(), f));
    }

    /**
     * Pulls a {@link DataResult} out of both sides of a {@link Pair}.
     * @param pair the pair to flatten
     * @return a flattened data result
     * @param <L> the left type of the pair
     * @param <F> the right type of the pair
     */
    public static <L, F> DataResult<Pair<L, F>> flatten(Pair<DataResult<L>, DataResult<F>> pair) {
        return pair.getFirst().flatMap(l -> pair.getSecond().map(f -> Pair.of(l, f)));
    }

    /**
     * Pulls a {@link DataResult} out of the left side of a {@link Either}.
     * @param either the either to flatten
     * @return a flattened data result
     * @param <L> the left type of the either
     * @param <F> the right type of the either
     */
    public static <L, F> DataResult<Either<L, F>> flattenLeft(Either<DataResult<L>, F> either) {
        return either.map(d -> d.map(Either::left), r -> DataResult.success(Either.right(r)));
    }

    /**
     * Pulls a {@link DataResult} out of the right side of a {@link Either}.
     * @param either the either to flatten
     * @return a flattened data result
     * @param <L> the left type of the either
     * @param <F> the right type of the either
     */
    public static <L, F> DataResult<Either<L, F>> flattenRight(Either<L, DataResult<F>> either) {
        return either.map(l -> DataResult.success(Either.left(l)), d -> d.map(Either::right));
    }

    /**
     * Pulls a {@link DataResult} out of both sides of a {@link Either}.
     * @param either the either to flatten
     * @return a flattened data result
     * @param <L> the left type of the either
     * @param <F> the right type of the either
     */
    public static <L, F> DataResult<Either<L, F>> flatten(Either<DataResult<L>, DataResult<F>> either) {
        return either.map(l -> l.map(Either::left), r -> r.map(Either::right));
    }

    /**
     * {@return the first data result if no errors are present, otherwise the second result with all errors from both results}
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static <O> DataResult<O> orElseGet(DataResult<O> result, Supplier<DataResult<O>> other) {
        if (result.result().isPresent()) {
            return result;
        }
        var otherResult = other.get();
        if (otherResult.result().isPresent()) {
            return otherResult;
        }
        Optional<O> partial = otherResult.resultOrPartial(o -> {});
        if (partial.isEmpty()) {
            partial = result.resultOrPartial(o -> {});
        }
        Supplier<String> error = () -> result.error().get().message() + "; " + otherResult.error().get().message();
        return partial.map(o -> DataResult.error(error, o)).orElseGet(() -> DataResult.error(error));
    }

    /**
     * {@return a codec which encodes an optional value as a map with a single field}
     */
    public static <O> Codec<Optional<O>> optional(Codec<O> codec) {
        return codec.optionalFieldOf("value").codec();
    }

    /**
     * {@return the first data result if no errors are present, otherwise an error data result with all errors from all results}
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static <T> DataResult<T> withErrors(DataResult<T> result, DataResult<?>... others) {
        List<DataResult.Error<?>> pieces = new ArrayList<>();
        if (result.isError()) {
            pieces.add(result.error().get());
        }
        for (DataResult<?> other : others) {
            if (other.isError()) {
                pieces.add(other.error().get());
            }
        }
        if (pieces.isEmpty()) {
            return result;
        }
        return DataResult.error(() ->
            pieces.stream().map(DataResult.Error::message).collect(Collectors.joining("; "))
        );
    }
}
