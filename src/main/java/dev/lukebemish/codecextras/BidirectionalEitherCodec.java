package dev.lukebemish.codecextras;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.function.Function;

/**
 * A codec for {@link Either} that, unlike the one created by {@link Codec#either(Codec, Codec)}, attempts to use both
 * constituent codecs on encode in addition to decode. Where a {@link Codec#either(Codec, Codec)} codec will only use
 * the left or right codec when encoding, depending on whether the provided value is a left or right value, a
 * {@link BidirectionalEitherCodec} will attempt to use both codecs.
 * @param <F>
 * @param <S>
 */
public final class BidirectionalEitherCodec<F, S> implements Codec<Either<F, S>> {
	private final Codec<Asymmetry<F, Either<F, S>>> first;
	private final Codec<Asymmetry<S, Either<F, S>>> second;

	/**
	 * Creates a new {@link BidirectionalEitherCodec} that will attempt to decode and encode with both codecs. The
	 * constituent codecs have asymmetrical types as they decode to different types but must encode from the same type.
	 * @param first the first codec to attempt to encode or decode with
	 * @param second the second codec to attempt to encode or decode with
	 * @return a new codec
	 * @param <F> the left type of the {@link Either} this codec will handle
	 * @param <S> the right type of the {@link Either} this codec will handle
	 */
	public static <F, S> Codec<Either<F, S>> asymmetrical(Codec<Asymmetry<F, Either<F, S>>> first, Codec<Asymmetry<S, Either<F, S>>> second) {
		return new BidirectionalEitherCodec<>(
			first,
			second
		);
	}

	/**
	 * An equivalent to {@link Codec#either(Codec, Codec)} backed by a {@link BidirectionalEitherCodec}.
	 * @param first the first codec to attempt to encode or decode with
	 * @param second the second codec to attempt to encode or decode with
	 * @return a new codec
	 * @param <F> the left type of the {@link Either} this codec will handle
	 * @param <S> the right type of the {@link Either} this codec will handle
	 */
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

	/**
	 * Attempts to decode and encode a value of a given type with both codecs, in order.
	 * @param first the first codec to attempt to encode or decode with
	 * @param second the second codec to attempt to encode or decode with
	 * @return a new codec
	 * @param <F> the type of the value this codec will handle
	 */
	public static <F> Codec<F> orElse(Codec<F> first, Codec<F> second) {
		return asymmetrical(
			Asymmetry.split(first, Function.identity(), e -> e.map(Function.identity(), Function.identity())),
			Asymmetry.split(second, Function.identity(), e -> e.map(Function.identity(), Function.identity()))
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
