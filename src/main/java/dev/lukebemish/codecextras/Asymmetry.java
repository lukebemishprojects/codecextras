package dev.lukebemish.codecextras;

import com.mojang.datafixers.util.Function10;
import com.mojang.datafixers.util.Function11;
import com.mojang.datafixers.util.Function12;
import com.mojang.datafixers.util.Function13;
import com.mojang.datafixers.util.Function14;
import com.mojang.datafixers.util.Function15;
import com.mojang.datafixers.util.Function16;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import com.mojang.datafixers.util.Function9;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Represents a type that can be encoded and decoded by {@link Codec}s in an asymmetric manner. Specifically, the value
 * this type wraps will have a different type while encoding than while decoding. A given asymmetry is moving in one
 * of two directions, encoding or decoding, and an attempt to access it in the wrong direction -- such as trying to get
 * at the decoding value of an encoding asymmetry -- will result in an error.
 * @param <D> the type of the value when decoding
 * @param <E> the type of the value when encoding
 */
public abstract sealed class Asymmetry<D, E> {

	/**
	 * {@return the stored value if this asymmetry is moving in the encoding direction, or an error otherwise}
	 */
	public abstract DataResult<E> encoding();

	/**
	 * {@return the stored value if this asymmetry is moving in the decoding direction, or an error otherwise}
	 */
	public abstract DataResult<D> decoding();

	private static final class Decoding<D, E> extends Asymmetry<D, E> {
		private final D decoding;

		private Decoding(D decoding) {
			this.decoding = decoding;
		}

		@Override
		public DataResult<E> encoding() {
			return DataResult.error(() -> "Attempted to access encoding value of a decoding asymmetry.");
		}

		@Override
		public DataResult<D> decoding() {
			return DataResult.success(decoding);
		}
	}

	private static final class Encoding<D, E> extends Asymmetry<D, E> {
		private final E encoding;

		private Encoding(E encoding) {
			this.encoding = encoding;
		}

		@Override
		public DataResult<E> encoding() {
			return DataResult.success(encoding);
		}

		@Override
		public DataResult<D> decoding() {
			return DataResult.error(() -> "Attempted to access decoding value of an encoding asymmetry.");
		}
	}

	private static final class Discontinuity<D, E> extends Asymmetry<D, E> {
		private static final Discontinuity<?, ?> INSTANCE = new Discontinuity<>(null);

		private final @Nullable Supplier<String> error;

		private Discontinuity(@Nullable Supplier<String> error) {
			this.error = error;
		}

		@Override
		public DataResult<E> encoding() {
			if (error == null) {
				return DataResult.error(() -> "Attempted to access encoding value of a discontinuous asymmetry.");
			}
			return DataResult.error(error);
		}

		@Override
		public DataResult<D> decoding() {
			if (error == null) {
				return DataResult.error(() -> "Attempted to access decoding value of a discontinuous asymmetry.");
			}
			return DataResult.error(error);
		}

		@SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent"})
		static <D, E> Asymmetry<D, E> withErrorOf(DataResult<?> result) {
			if (result.isError()) {
				return new Discontinuity<>(result.error().get().messageSupplier());
			}
			return (Asymmetry<D, E>) INSTANCE;
		}
	}

	/**
	 * {@return a new asymmetry moving in the encoding direction with the given value}
	 * @param encoding the value to wrap
	 * @param <D> the type of the value when decoding
	 * @param <E> the type of the value when encoding
	 */
	public static <D, E> Asymmetry<D, E> ofEncoding(E encoding) {
		return new Asymmetry.Encoding<>(encoding);
	}

	/**
	 * {@return a new asymmetry moving in the decoding direction with the given value}
	 * @param decoding the value to wrap
	 * @param <D> the type of the value when decoding
	 * @param <E> the type of the value when encoding
	 */
	public static <D, E> Asymmetry<D, E> ofDecoding(D decoding) {
		return new Asymmetry.Decoding<>(decoding);
	}

	/**
	 * {@return a new asymmetry representing an attempt to move in the wrong direction}
	 * @param <D> the type of the value when decoding
	 * @param <E> the type of the value when encoding
	 */
	@SuppressWarnings("unchecked")
	public static <D, E> Asymmetry<D, E> discontinuous() {
		return (Asymmetry<D, E>) Discontinuity.INSTANCE;
	}

	/**
	 * {@return a new asymmetry moving in the encoding direction with the given value or a propagated error}
	 * @param encoding the value to wrap
	 * @param <D> the type of the value when decoding
	 * @param <E> the type of the value when encoding
	 */
	public static <D, E> Asymmetry<D, E> ofEncodingResult(DataResult<E> encoding) {
		return encoding.result().map(Asymmetry::<D,E>ofEncoding).orElseGet(() -> Discontinuity.withErrorOf(encoding));
	}

	/**
	 * {@return a new asymmetry moving in the decoding direction with the given value or a propagated error}
	 * @param decoding the value to wrap
	 * @param <D> the type of the value when decoding
	 * @param <E> the type of the value when encoding
	 */
	public static <D, E> Asymmetry<D, E> ofDecodingResult(DataResult<D> decoding) {
		return decoding.result().map(Asymmetry::<D,E>ofDecoding).orElseGet(() -> Discontinuity.withErrorOf(decoding));
	}

	/**
	 * {@return a codec that can encode and decode asymmetries using the given encoder and decoder}
	 * @param decoder the decoder to use
	 * @param encoder the encoder to use
	 * @param <D> the type of the value when decoding
	 * @param <E> the type of the value when encoding
	 */
	public static <D, E> Codec<Asymmetry<D, E>> codec(Decoder<D> decoder, Encoder<E> encoder) {
		return new Codec<>() {
			@Override
			public <T> DataResult<Pair<Asymmetry<D, E>, T>> decode(DynamicOps<T> ops, T input) {
				return decoder.decode(ops, input).map(p -> p.mapFirst(Asymmetry::ofDecoding));
			}

			@Override
			public <T> DataResult<T> encode(Asymmetry<D, E> input, DynamicOps<T> ops, T prefix) {
				return input.encoding().flatMap(e -> encoder.encode(e, ops, prefix));
			}
		};
	}

	/**
	 * Independently map both the encoding and decoding directions.
	 * @param codec the codec to map
	 * @param mapDecoding the function to apply to the decoding value when decoding
	 * @param mapEncoding the function to apply to the encoding value when encoding
	 * @return a codec with the given mapping applied
	 * @param <E0> the type of the encoding value before mapping
	 * @param <D0> the type of the decoding value before mapping
	 * @param <E1> the type of the encoding value after mapping
	 * @param <D1> the type of the decoding value after mapping
	 */
	public static <E0, D0, E1, D1> Codec<Asymmetry<D1, E1>> map(Codec<Asymmetry<D0, E0>> codec, Function<D0, D1> mapDecoding, Function<E1, E0> mapEncoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(mapEncoding).map(Asymmetry::ofEncoding));
	}

	/**
	 * An equivalent to {@link #map(Codec, Function, Function)} that maps to {@link DataResult}s and propagates errors.
	 * @see #map(Codec, Function, Function)
	 */
	public static <E0, D0, E1, D1> Codec<Asymmetry<D1, E1>> flatMap(Codec<Asymmetry<D0, E0>> codec, Function<D0, DataResult<D1>> mapDecoding, Function<E1, DataResult<E0>> mapEncoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding).map(Asymmetry::ofEncoding));
	}

	/**
	 * Map only the encoding direction of the given codec.
	 * @param codec the codec to map
	 * @param mapEncoding the function to apply to the encoding value when encoding
	 * @return a codec with the given mapping applied
	 * @param <E0> the type of the encoding value before mapping
	 * @param <D> the type of the decoding value
	 * @param <E1> the type of the encoding value after mapping
	 */
	public static <E0, D, E1> Codec<Asymmetry<D, E1>> mapEncoding(Codec<Asymmetry<D, E0>> codec, Function<E1, E0> mapEncoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(mapEncoding).map(Asymmetry::ofEncoding));
	}

	/**
	 * An equivalent to {@link #mapEncoding(Codec, Function)} that maps to {@link DataResult}s and propagates errors.
	 * @see #mapEncoding(Codec, Function)
	 */
	public static <E0, D, E1> Codec<Asymmetry<D, E1>> flatMapEncoding(Codec<Asymmetry<D, E0>> codec, Function<E1, DataResult<E0>> mapEncoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding).map(Asymmetry::ofEncoding));
	}

	/**
	 * Map the decoding direction of the given codec.
	 * @param codec the codec to map
	 * @param mapDecoding the function to apply to the decoding value when decoding
	 * @return a codec with the given mapping applied
	 * @param <E> the type of the encoding value
	 * @param <D0> the type of the decoding value before mapping
	 * @param <D1> the type of the decoding value after mapping
	 */
	public static <E, D0, D1> Codec<Asymmetry<D1, E>> mapDecoding(Codec<Asymmetry<D0, E>> codec, Function<D0, D1> mapDecoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(Asymmetry::ofEncoding));
	}

	/**
	 * An equivalent to {@link #mapDecoding(Codec, Function)} that maps to {@link DataResult}s and propagates errors.
	 * @see #mapDecoding(Codec, Function)
	 */
	public static <E, D0, D1> Codec<Asymmetry<D1, E>> flatMapDecoding(Codec<Asymmetry<D0, E>> codec, Function<D0, DataResult<D1>> mapDecoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(Asymmetry::ofEncoding));
	}

	/**
	 * Join an asymmetrical codec into a single type by providing a pair of functions applied on the end of the codec
	 * furthest from the data.
	 * @param codec the codec to join
	 * @param mapDecoding the function to apply to the decoding value when decoding
	 * @param mapEncoding the function to apply to the encoding value when encoding
	 * @return a codec of a single type
	 * @param <E> the type of the encoding value
	 * @param <D> the type of the decoding value
	 * @param <O> the type of the joined codec
	 */
	public static <E, D, O> Codec<O> join(Codec<Asymmetry<D, E>> codec, Function<D, O> mapDecoding, Function<O, E> mapEncoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding), object -> DataResult.success(ofEncoding(mapEncoding.apply(object))));
	}

	/**
	 * An equivalent to {@link #join(Codec, Function, Function)} that maps to {@link DataResult}s and propagates errors.
	 * @see #join(Codec, Function, Function)
	 */
	public static <E, D, O> Codec<O> flatJoin(Codec<Asymmetry<D, E>> codec, Function<D, DataResult<O>> mapDecoding, Function<O, DataResult<E>> mapEncoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding), object -> mapEncoding.apply(object).map(Asymmetry::ofEncoding));
	}

	/**
	 * Split an asymmetrical codec into two types by providing a pair of functions applied on the end of the codec
	 * closest to the data.
	 * @param codec the codec to split
	 * @param mapDecoding the function to apply to the decoding value when decoding
	 * @param mapEncoding the function to apply to the encoding value when encoding
	 * @return a codec of a single type
	 * @param <E> the type of the encoding value
	 * @param <D> the type of the decoding value
	 * @param <O> the type of the split codec
	 */
	public static <E, D, O> Codec<Asymmetry<D, E>> split(Codec<O> codec, Function<O, D> mapDecoding, Function<E, O> mapEncoding) {
		return codec.flatXmap(object -> DataResult.success(ofDecoding(mapDecoding.apply(object))), asymmetry -> asymmetry.encoding().map(mapEncoding));
	}

	/**
	 * An equivalent to {@link #split(Codec, Function, Function)} that maps to {@link DataResult}s and propagates errors.
	 * @see #split(Codec, Function, Function)
	 */
	public static <E, D, O> Codec<Asymmetry<D, E>> flatSplit(Codec<O> codec, Function<O, DataResult<D>> mapDecoding, Function<E, DataResult<O>> mapEncoding) {
		return codec.flatXmap(object -> mapDecoding.apply(object).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding));
	}

	/**
	 * A version of {@link #map(Codec, Function, Function)} that works with {@link MapCodec}s.
	 * @see #map(Codec, Function, Function)
	 */
	public static <E0, D0, E1, D1> MapCodec<Asymmetry<D1, E1>> map(MapCodec<Asymmetry<D0, E0>> codec, Function<D0, D1> mapDecoding, Function<E1, E0> mapEncoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(mapEncoding).map(Asymmetry::ofEncoding));
	}

	/**
	 * A version of {@link #flatMap(Codec, Function, Function)} that works with {@link MapCodec}s.
	 * @see #flatMap(Codec, Function, Function)
	 */
	public static <E0, D0, E1, D1> MapCodec<Asymmetry<D1, E1>> flatMap(MapCodec<Asymmetry<D0, E0>> codec, Function<D0, DataResult<D1>> mapDecoding, Function<E1, DataResult<E0>> mapEncoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding).map(Asymmetry::ofEncoding));
	}

	/**
	 * A version of {@link #mapEncoding(Codec, Function)} that works with {@link MapCodec}s.
	 * @see #mapEncoding(Codec, Function)
	 */
	public static <E0, D, E1> MapCodec<Asymmetry<D, E1>> mapEncoding(MapCodec<Asymmetry<D, E0>> codec, Function<E1, E0> mapEncoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(mapEncoding).map(Asymmetry::ofEncoding));
	}

	/**
	 * A version of {@link #flatMapEncoding(Codec, Function)} that works with {@link MapCodec}s.
	 * @see #flatMapEncoding(Codec, Function)
	 */
	public static <E0, D, E1> MapCodec<Asymmetry<D, E1>> flatMapEncoding(MapCodec<Asymmetry<D, E0>> codec, Function<E1, DataResult<E0>> mapEncoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding).map(Asymmetry::ofEncoding));
	}

	/**
	 * A version of {@link #mapDecoding(Codec, Function)} that works with {@link MapCodec}s.
	 * @see #mapDecoding(Codec, Function)
	 */
	public static <E, D0, D1> MapCodec<Asymmetry<D1, E>> mapDecoding(MapCodec<Asymmetry<D0, E>> codec, Function<D0, D1> mapDecoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(Asymmetry::ofEncoding));
	}

	/**
	 * A version of {@link #flatMapDecoding(Codec, Function)} that works with {@link MapCodec}s.
	 * @see #flatMapDecoding(Codec, Function)
	 */
	public static <E, D0, D1> MapCodec<Asymmetry<D1, E>> flatMapDecoding(MapCodec<Asymmetry<D0, E>> codec, Function<D0, DataResult<D1>> mapDecoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(Asymmetry::ofEncoding));
	}

	/**
	 * A version of {@link #join(Codec, Function, Function)} that works with {@link MapCodec}s.
	 * @see #join(Codec, Function, Function)
	 */
	public static <E, D, O> MapCodec<O> join(MapCodec<Asymmetry<D, E>> codec, Function<D, O> mapDecoding, Function<O, E> mapEncoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding), object -> DataResult.success(ofEncoding(mapEncoding.apply(object))));
	}

	/**
	 * A version of {@link #flatJoin(Codec, Function, Function)} that works with {@link MapCodec}s.
	 * @see #flatJoin(Codec, Function, Function)
	 */
	public static <E, D, O> MapCodec<O> flatJoin(MapCodec<Asymmetry<D, E>> codec, Function<D, DataResult<O>> mapDecoding, Function<O, DataResult<E>> mapEncoding) {
		return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding), object -> mapEncoding.apply(object).map(Asymmetry::ofEncoding));
	}

	/**
	 * A version of {@link #split(Codec, Function, Function)} that works with {@link MapCodec}s.
	 * @see #split(Codec, Function, Function)
	 */
	public static <E, D, O> MapCodec<Asymmetry<D, E>> split(MapCodec<O> codec, Function<O, D> mapDecoding, Function<E, O> mapEncoding) {
		return codec.flatXmap(object -> DataResult.success(ofDecoding(mapDecoding.apply(object))), asymmetry -> asymmetry.encoding().map(mapEncoding));
	}

	/**
	 * A version of {@link #flatSplit(Codec, Function, Function)} that works with {@link MapCodec}s.
	 * @see #flatSplit(Codec, Function, Function)
	 */
	public static <E, D, O> MapCodec<Asymmetry<D, E>> flatSplit(MapCodec<O> codec, Function<O, DataResult<D>> mapDecoding, Function<E, DataResult<O>> mapEncoding) {
		return codec.flatXmap(object -> mapDecoding.apply(object).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding));
	}

	/**
	 * Raises a function to a function of encoding asymmetries.
	 * @param getter the function to raise
	 * @return a function that applies the given function to the encoding value of an asymmetry
	 * @param <D> the type of the value when decoding
	 * @param <E> the type of the value when encoding
	 * @param <FD> the new type of the value when decoding
	 * @param <FE> type of the value when encoding, after applying the function
	 */
	public static <D, E, FD, FE> Function<Asymmetry<D, E>, Asymmetry<FD, FE>> wrapGetter(Function<E, FE> getter) {
		return asymmetryResult -> ofEncodingResult(asymmetryResult.encoding().map(getter));
	}

	/**
	 * Raises a function to a function of decoding asymmetries.
	 * @param function the function to raise
	 * @return a function that applies the given function to the decoding value of an asymmetry
	 * @param <E> the type of the value when encoding
	 * @param <D> the type of the value when decoding
	 * @param <F1> the new type of the value when encoding
	 * @param <FE1> type of the value when encoding, after applying the function
	 */
	public static <E,D,F1,FE1> Function<Asymmetry<F1,FE1>, Asymmetry<D, E>> wrapJoiner(Function<F1, DataResult<D>> function) {
		return f1 -> {
			var d1 = f1.decoding().result();
			//noinspection OptionalIsPresent
			if (d1.isPresent()) {
				return ofDecodingResult(function.apply(d1.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 2 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,FE1,FE2> BiFunction<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<D, E>> wrapJoiner(BiFunction<F1,F2,DataResult<D>> function) {
		return (f1, f2) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			if (d1.isPresent() && d2.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 3 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,F3,FE1,FE2,FE3> Function3<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<F3,FE3>,
		Asymmetry<D, E>> wrapJoiner(Function3<F1,F2,F3,DataResult<D>> function) {
		return (f1, f2, f3) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			var d3 = f3.decoding().result();
			if (d1.isPresent() && d2.isPresent() && d3.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding(),
					f3.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 4 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,F3,F4,FE1,FE2,FE3,FE4> Function4<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<F3,FE3>,
		Asymmetry<F4,FE4>,
		Asymmetry<D, E>> wrapJoiner(Function4<F1,F2,F3,F4,DataResult<D>> function) {
		return (f1, f2, f3, f4) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			var d3 = f3.decoding().result();
			var d4 = f4.decoding().result();
			if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding(),
					f3.decoding(),
					f4.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 5 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,F3,F4,F5,FE1,FE2,FE3,FE4,FE5> Function5<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<F3,FE3>,
		Asymmetry<F4,FE4>,
		Asymmetry<F5,FE5>,
		Asymmetry<D, E>> wrapJoiner(Function5<F1,F2,F3,F4,F5,DataResult<D>> function) {
		return (f1, f2, f3, f4, f5) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			var d3 = f3.decoding().result();
			var d4 = f4.decoding().result();
			var d5 = f5.decoding().result();
			if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding(),
					f3.decoding(),
					f4.decoding(),
					f5.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 6 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,F3,F4,F5,F6,FE1,FE2,FE3,FE4,FE5,FE6> Function6<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<F3,FE3>,
		Asymmetry<F4,FE4>,
		Asymmetry<F5,FE5>,
		Asymmetry<F6,FE6>,
		Asymmetry<D, E>> wrapJoiner(Function6<F1,F2,F3,F4,F5,F6,DataResult<D>> function) {
		return (f1, f2, f3, f4, f5, f6) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			var d3 = f3.decoding().result();
			var d4 = f4.decoding().result();
			var d5 = f5.decoding().result();
			var d6 = f6.decoding().result();
			if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding(),
					f3.decoding(),
					f4.decoding(),
					f5.decoding(),
					f6.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 7 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,F3,F4,F5,F6,F7,FE1,FE2,FE3,FE4,FE5,FE6,FE7> Function7<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<F3,FE3>,
		Asymmetry<F4,FE4>,
		Asymmetry<F5,FE5>,
		Asymmetry<F6,FE6>,
		Asymmetry<F7,FE7>,
		Asymmetry<D, E>> wrapJoiner(Function7<F1,F2,F3,F4,F5,F6,F7,DataResult<D>> function) {
		return (f1, f2, f3, f4, f5, f6, f7) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			var d3 = f3.decoding().result();
			var d4 = f4.decoding().result();
			var d5 = f5.decoding().result();
			var d6 = f6.decoding().result();
			var d7 = f7.decoding().result();
			if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding(),
					f3.decoding(),
					f4.decoding(),
					f5.decoding(),
					f6.decoding(),
					f7.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 8 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8> Function8<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<F3,FE3>,
		Asymmetry<F4,FE4>,
		Asymmetry<F5,FE5>,
		Asymmetry<F6,FE6>,
		Asymmetry<F7,FE7>,
		Asymmetry<F8,FE8>,
		Asymmetry<D, E>> wrapJoiner(Function8<F1,F2,F3,F4,F5,F6,F7,F8,DataResult<D>> function) {
		return (f1, f2, f3, f4, f5, f6, f7, f8) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			var d3 = f3.decoding().result();
			var d4 = f4.decoding().result();
			var d5 = f5.decoding().result();
			var d6 = f6.decoding().result();
			var d7 = f7.decoding().result();
			var d8 = f8.decoding().result();
			if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding(),
					f3.decoding(),
					f4.decoding(),
					f5.decoding(),
					f6.decoding(),
					f7.decoding(),
					f8.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 9 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9> Function9<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<F3,FE3>,
		Asymmetry<F4,FE4>,
		Asymmetry<F5,FE5>,
		Asymmetry<F6,FE6>,
		Asymmetry<F7,FE7>,
		Asymmetry<F8,FE8>,
		Asymmetry<F9,FE9>,
		Asymmetry<D, E>> wrapJoiner(Function9<F1,F2,F3,F4,F5,F6,F7,F8,F9,DataResult<D>> function) {
		return (f1, f2, f3, f4, f5, f6, f7, f8, f9) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			var d3 = f3.decoding().result();
			var d4 = f4.decoding().result();
			var d5 = f5.decoding().result();
			var d6 = f6.decoding().result();
			var d7 = f7.decoding().result();
			var d8 = f8.decoding().result();
			var d9 = f9.decoding().result();
			if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding(),
					f3.decoding(),
					f4.decoding(),
					f5.decoding(),
					f6.decoding(),
					f7.decoding(),
					f8.decoding(),
					f9.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 10 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9,FE10> Function10<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<F3,FE3>,
		Asymmetry<F4,FE4>,
		Asymmetry<F5,FE5>,
		Asymmetry<F6,FE6>,
		Asymmetry<F7,FE7>,
		Asymmetry<F8,FE8>,
		Asymmetry<F9,FE9>,
		Asymmetry<F10,FE10>,
		Asymmetry<D, E>> wrapJoiner(Function10<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,DataResult<D>> function) {
		return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			var d3 = f3.decoding().result();
			var d4 = f4.decoding().result();
			var d5 = f5.decoding().result();
			var d6 = f6.decoding().result();
			var d7 = f7.decoding().result();
			var d8 = f8.decoding().result();
			var d9 = f9.decoding().result();
			var d10 = f10.decoding().result();
			if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding(),
					f3.decoding(),
					f4.decoding(),
					f5.decoding(),
					f6.decoding(),
					f7.decoding(),
					f8.decoding(),
					f9.decoding(),
					f10.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 11 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9,FE10,FE11> Function11<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<F3,FE3>,
		Asymmetry<F4,FE4>,
		Asymmetry<F5,FE5>,
		Asymmetry<F6,FE6>,
		Asymmetry<F7,FE7>,
		Asymmetry<F8,FE8>,
		Asymmetry<F9,FE9>,
		Asymmetry<F10,FE10>,
		Asymmetry<F11,FE11>,
		Asymmetry<D, E>> wrapJoiner(Function11<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,DataResult<D>> function) {
		return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			var d3 = f3.decoding().result();
			var d4 = f4.decoding().result();
			var d5 = f5.decoding().result();
			var d6 = f6.decoding().result();
			var d7 = f7.decoding().result();
			var d8 = f8.decoding().result();
			var d9 = f9.decoding().result();
			var d10 = f10.decoding().result();
			var d11 = f11.decoding().result();
			if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding(),
					f3.decoding(),
					f4.decoding(),
					f5.decoding(),
					f6.decoding(),
					f7.decoding(),
					f8.decoding(),
					f9.decoding(),
					f10.decoding(),
					f11.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 12 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9,FE10,FE11,FE12> Function12<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<F3,FE3>,
		Asymmetry<F4,FE4>,
		Asymmetry<F5,FE5>,
		Asymmetry<F6,FE6>,
		Asymmetry<F7,FE7>,
		Asymmetry<F8,FE8>,
		Asymmetry<F9,FE9>,
		Asymmetry<F10,FE10>,
		Asymmetry<F11,FE11>,
		Asymmetry<F12,FE12>,
		Asymmetry<D, E>> wrapJoiner(Function12<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,DataResult<D>> function) {
		return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			var d3 = f3.decoding().result();
			var d4 = f4.decoding().result();
			var d5 = f5.decoding().result();
			var d6 = f6.decoding().result();
			var d7 = f7.decoding().result();
			var d8 = f8.decoding().result();
			var d9 = f9.decoding().result();
			var d10 = f10.decoding().result();
			var d11 = f11.decoding().result();
			var d12 = f12.decoding().result();
			if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding(),
					f3.decoding(),
					f4.decoding(),
					f5.decoding(),
					f6.decoding(),
					f7.decoding(),
					f8.decoding(),
					f9.decoding(),
					f10.decoding(),
					f11.decoding(),
					f12.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 13 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9,FE10,FE11,FE12,FE13> Function13<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<F3,FE3>,
		Asymmetry<F4,FE4>,
		Asymmetry<F5,FE5>,
		Asymmetry<F6,FE6>,
		Asymmetry<F7,FE7>,
		Asymmetry<F8,FE8>,
		Asymmetry<F9,FE9>,
		Asymmetry<F10,FE10>,
		Asymmetry<F11,FE11>,
		Asymmetry<F12,FE12>,
		Asymmetry<F13,FE13>,
		Asymmetry<D, E>> wrapJoiner(Function13<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,DataResult<D>> function) {
		return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			var d3 = f3.decoding().result();
			var d4 = f4.decoding().result();
			var d5 = f5.decoding().result();
			var d6 = f6.decoding().result();
			var d7 = f7.decoding().result();
			var d8 = f8.decoding().result();
			var d9 = f9.decoding().result();
			var d10 = f10.decoding().result();
			var d11 = f11.decoding().result();
			var d12 = f12.decoding().result();
			var d13 = f13.decoding().result();
			if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent() && d13.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get(), d13.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding(),
					f3.decoding(),
					f4.decoding(),
					f5.decoding(),
					f6.decoding(),
					f7.decoding(),
					f8.decoding(),
					f9.decoding(),
					f10.decoding(),
					f11.decoding(),
					f12.decoding(),
					f13.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 14 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9,FE10,FE11,FE12,FE13,FE14> Function14<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<F3,FE3>,
		Asymmetry<F4,FE4>,
		Asymmetry<F5,FE5>,
		Asymmetry<F6,FE6>,
		Asymmetry<F7,FE7>,
		Asymmetry<F8,FE8>,
		Asymmetry<F9,FE9>,
		Asymmetry<F10,FE10>,
		Asymmetry<F11,FE11>,
		Asymmetry<F12,FE12>,
		Asymmetry<F13,FE13>,
		Asymmetry<F14,FE14>,
		Asymmetry<D, E>> wrapJoiner(Function14<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,DataResult<D>> function) {
		return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			var d3 = f3.decoding().result();
			var d4 = f4.decoding().result();
			var d5 = f5.decoding().result();
			var d6 = f6.decoding().result();
			var d7 = f7.decoding().result();
			var d8 = f8.decoding().result();
			var d9 = f9.decoding().result();
			var d10 = f10.decoding().result();
			var d11 = f11.decoding().result();
			var d12 = f12.decoding().result();
			var d13 = f13.decoding().result();
			var d14 = f14.decoding().result();
			if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent() && d13.isPresent() && d14.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get(), d13.get(), d14.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding(),
					f3.decoding(),
					f4.decoding(),
					f5.decoding(),
					f6.decoding(),
					f7.decoding(),
					f8.decoding(),
					f9.decoding(),
					f10.decoding(),
					f11.decoding(),
					f12.decoding(),
					f13.decoding(),
					f14.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 15 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,F15,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9,FE10,FE11,FE12,FE13,FE14,FE15> Function15<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<F3,FE3>,
		Asymmetry<F4,FE4>,
		Asymmetry<F5,FE5>,
		Asymmetry<F6,FE6>,
		Asymmetry<F7,FE7>,
		Asymmetry<F8,FE8>,
		Asymmetry<F9,FE9>,
		Asymmetry<F10,FE10>,
		Asymmetry<F11,FE11>,
		Asymmetry<F12,FE12>,
		Asymmetry<F13,FE13>,
		Asymmetry<F14,FE14>,
		Asymmetry<F15,FE15>,
		Asymmetry<D, E>> wrapJoiner(Function15<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,F15,DataResult<D>> function) {
		return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			var d3 = f3.decoding().result();
			var d4 = f4.decoding().result();
			var d5 = f5.decoding().result();
			var d6 = f6.decoding().result();
			var d7 = f7.decoding().result();
			var d8 = f8.decoding().result();
			var d9 = f9.decoding().result();
			var d10 = f10.decoding().result();
			var d11 = f11.decoding().result();
			var d12 = f12.decoding().result();
			var d13 = f13.decoding().result();
			var d14 = f14.decoding().result();
			var d15 = f15.decoding().result();
			if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent() && d13.isPresent() && d14.isPresent() && d15.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get(), d13.get(), d14.get(), d15.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding(),
					f3.decoding(),
					f4.decoding(),
					f5.decoding(),
					f6.decoding(),
					f7.decoding(),
					f8.decoding(),
					f9.decoding(),
					f10.decoding(),
					f11.decoding(),
					f12.decoding(),
					f13.decoding(),
					f14.decoding(),
					f15.decoding()
				));
			}
		};
	}

	/**
	 * An equivalent of {@link #wrapJoiner(Function)} for 16 arguments.
	 * @see #wrapJoiner(Function)
	 */
	public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,F15,F16,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9,FE10,FE11,FE12,FE13,FE14,FE15,FE16> Function16<
		Asymmetry<F1,FE1>,
		Asymmetry<F2,FE2>,
		Asymmetry<F3,FE3>,
		Asymmetry<F4,FE4>,
		Asymmetry<F5,FE5>,
		Asymmetry<F6,FE6>,
		Asymmetry<F7,FE7>,
		Asymmetry<F8,FE8>,
		Asymmetry<F9,FE9>,
		Asymmetry<F10,FE10>,
		Asymmetry<F11,FE11>,
		Asymmetry<F12,FE12>,
		Asymmetry<F13,FE13>,
		Asymmetry<F14,FE14>,
		Asymmetry<F15,FE15>,
		Asymmetry<F16,FE16>,
		Asymmetry<D, E>> wrapJoiner(Function16<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,F15,F16,DataResult<D>> function) {
		return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16) -> {
			var d1 = f1.decoding().result();
			var d2 = f2.decoding().result();
			var d3 = f3.decoding().result();
			var d4 = f4.decoding().result();
			var d5 = f5.decoding().result();
			var d6 = f6.decoding().result();
			var d7 = f7.decoding().result();
			var d8 = f8.decoding().result();
			var d9 = f9.decoding().result();
			var d10 = f10.decoding().result();
			var d11 = f11.decoding().result();
			var d12 = f12.decoding().result();
			var d13 = f13.decoding().result();
			var d14 = f14.decoding().result();
			var d15 = f15.decoding().result();
			var d16 = f16.decoding().result();
			if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent() && d13.isPresent() && d14.isPresent() && d15.isPresent() && d16.isPresent()) {
				return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get(), d13.get(), d14.get(), d15.get(), d16.get()));
			} else {
				return Discontinuity.withErrorOf(CodecExtras.withErrors(
					f1.decoding(),
					f2.decoding(),
					f3.decoding(),
					f4.decoding(),
					f5.decoding(),
					f6.decoding(),
					f7.decoding(),
					f8.decoding(),
					f9.decoding(),
					f10.decoding(),
					f11.decoding(),
					f12.decoding(),
					f13.decoding(),
					f14.decoding(),
					f15.decoding(),
					f16.decoding()
				));
			}
		};
	}
}
