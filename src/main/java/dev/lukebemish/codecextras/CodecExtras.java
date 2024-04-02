package dev.lukebemish.codecextras;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class CodecExtras {
	public static <O> Codec<O> flatten(Codec<DataResult<O>> codec) {
		return codec.flatXmap(Function.identity(), o -> DataResult.success(DataResult.success(o)));
	}

	public static <O> MapCodec<O> flatten(MapCodec<DataResult<O>> codec) {
		return codec.flatXmap(Function.identity(), o -> DataResult.success(DataResult.success(o)));
	}

	public static <O> Codec<DataResult<O>> raise(Codec<O> codec) {
		return codec.flatXmap(o -> DataResult.success(DataResult.success(o)), Function.identity());
	}

	public static <O> MapCodec<DataResult<O>> raise(MapCodec<O> codec) {
		return codec.flatXmap(o -> DataResult.success(DataResult.success(o)), Function.identity());
	}

	public static <L, F> DataResult<Pair<L, F>> flattenLeft(Pair<DataResult<L>, F> pair) {
		return pair.getFirst().map(l -> Pair.of(l, pair.getSecond()));
	}

	public static <L, F> DataResult<Pair<L, F>> flattenRight(Pair<L, DataResult<F>> pair) {
		return pair.getSecond().map(f -> Pair.of(pair.getFirst(), f));
	}

	public static <L, F> DataResult<Pair<L, F>> flatten(Pair<DataResult<L>, DataResult<F>> pair) {
		return pair.getFirst().flatMap(l -> pair.getSecond().map(f -> Pair.of(l, f)));
	}

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
}
