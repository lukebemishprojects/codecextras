package dev.lukebemish.codecextras;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * An equivalent to {@link RecordCodecBuilder} that allows for any number of fields.
 * @param <A> the type of the object being encoded/decoded
 * @param <F> the type of the highest level field
 * @param <B> the type of the final builder function used during decoding
 */
public abstract sealed class ExtendedRecordCodecBuilder<A, F, B extends ExtendedRecordCodecBuilder.AppFunction> {

	/**
	 * Creates a new {@link ExtendedRecordCodecBuilder} with the given codec and getter as the bottom-most field.
	 * @param codec the codec for the bottom-most field
	 * @param getter the getter for the bottom-most field
	 * @return a new {@link ExtendedRecordCodecBuilder}
	 * @param <O> the type of the object being encoded/decoded
	 * @param <F> the type of the bottom-most field
	 */
	public static <O, F> ExtendedRecordCodecBuilder<O, F, FinalAppFunction<O, F>> start(MapCodec<F> codec, Function<O, F> getter) {
		return new Endpoint<>(codec, getter);
	}

	/**
	 * Creates a new {@link ExtendedRecordCodecBuilder} with the given codec and getter as the next field above the
	 * current one.
	 * @param codec the codec for the next field
	 * @param getter the getter for the next field
	 * @return a new {@link ExtendedRecordCodecBuilder}
	 * @param <N> the type of the next field
	 */
	public <N> ExtendedRecordCodecBuilder<A, N, FromAppFunction<N, B>> field(MapCodec<N> codec, Function<A, N> getter) {
		return new Delegating<>(codec, getter, this);
	}

	/**
	 * Builds a codec with the provided builder function for the final step of decoding.
	 * @param b the builder function to use in the final step of decoding; can be expressed as a nested lambda function
	 *          with fields in the opposite order that they were built in
	 * @return a codec for the type {@code A}
	 */
	public final Codec<A> build(B b) {
		return buildMap(b).codec();
	}

	/**
	 * Builds a map codec with the provided builder function for the final step of decoding.
	 * @param b the builder function to use in the final step of decoding; can be expressed as a nested lambda function
	 *          with fields in the opposite order that they were built in
	 * @return a map codec for the type {@code A}
	 */
	public abstract MapCodec<A> buildMap(B b);

	public non-sealed interface FinalAppFunction<A, B> extends AppFunction {
		A create(B b);
	}

	public non-sealed interface FromAppFunction<B, C extends AppFunction> extends AppFunction {
		C create(B b);
	}

	public sealed interface AppFunction {}

	protected final MapCodec<F> codec;
	protected final Function<A, F> getter;

	private ExtendedRecordCodecBuilder(MapCodec<F> codec, Function<A, F> getter) {
		this.codec = codec;
		this.getter = getter;
	}

	protected abstract <T> RecordBuilder<T> encodeChildren(A input, DynamicOps<T> ops, RecordBuilder<T> prefix);
	protected abstract <T> DataResult<A> decodePartial(DynamicOps<T> ops, MapLike<T> input, B b);
	protected abstract <T> Stream<T> keysPartial(DynamicOps<T> ops);

	private static final class Endpoint<A, F, B extends ExtendedRecordCodecBuilder.FinalAppFunction<A, F>> extends ExtendedRecordCodecBuilder<A, F, B> {
		private Endpoint(MapCodec<F> codec, Function<A, F> getter) {
			super(codec, getter);
		}

		@Override
		protected <T> RecordBuilder<T> encodeChildren(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
			F field = getter.apply(input);
			prefix = codec.encode(field, ops, prefix);
			return prefix;
		}

		@Override
		protected <T> DataResult<A> decodePartial(DynamicOps<T> ops, MapLike<T> input, B b) {
			DataResult<F> field = codec.decode(ops, input);
			return field.map(b::create);
		}

		@Override
		protected <T> Stream<T> keysPartial(DynamicOps<T> ops) {
			return codec.keys(ops);
		}


		@Override
		public MapCodec<A> buildMap(B b) {
			return new MapCodec<>() {

				@Override
				public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
					return encodeChildren(input, ops, prefix);
				}

				@Override
				public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
					return decodePartial(ops, input, b);
				}

				@Override
				public <T> Stream<T> keys(DynamicOps<T> ops) {
					return keysPartial(ops);
				}

				@Override
				public String toString() {
					return Endpoint.this.toString();
				}
			};
		}

		@Override
		public String toString() {
			return "ExtendedRecordCodec[" + codec + "]";
		}
	}

	private static final class Delegating<A, F, D extends ExtendedRecordCodecBuilder.AppFunction, B extends ExtendedRecordCodecBuilder.FromAppFunction<F, D>> extends ExtendedRecordCodecBuilder<A, F, B> {
		private final ExtendedRecordCodecBuilder<A, ?, D> delegate;
		private Delegating(MapCodec<F> codec, Function<A, F> getter, ExtendedRecordCodecBuilder<A, ?, D> delegate) {
			super(codec, getter);
			this.delegate = delegate;
		}

		@Override
		protected <T> RecordBuilder<T> encodeChildren(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
			F field = getter.apply(input);
			prefix = codec.encode(field, ops, prefix);
			return delegate.encodeChildren(input, ops, prefix);
		}

		@Override
		protected <T> DataResult<A> decodePartial(DynamicOps<T> ops, MapLike<T> input, B b) {
			DataResult<F> field = codec.decode(ops, input);
			return field.flatMap(f -> {
				D d = b.create(f);
				return delegate.decodePartial(ops, input, d);
			});
		}

		@SuppressWarnings("InfiniteRecursion")
		@Override
		protected <T> Stream<T> keysPartial(DynamicOps<T> ops) {
			return Stream.concat(keysPartial(ops), delegate.keysPartial(ops));
		}

		@Override
		public MapCodec<A> buildMap(B b) {
			return new MapCodec<>() {

				@Override
				public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
					return encodeChildren(input, ops, prefix);
				}

				@Override
				public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
					return decodePartial(ops, input, b);
				}

				@Override
				public <T> Stream<T> keys(DynamicOps<T> ops) {
					return keysPartial(ops);
				}

				@Override
				public String toString() {
					return Delegating.this.toString();
				}
			};
		}

		@Override
		public String toString() {
			return "ExtendedRecordCodec[" + codec + "] -> " + delegate.toString();
		}
	}
}
