package dev.lukebemish.codecextras.record;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.ExtendedRecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.ApiStatus;

/**
 * Similar to {@link ExtendedRecordCodecBuilder}, an alternative to {@link RecordCodecBuilder} that allows for any
 * number of fields. Unlike {@link ExtendedRecordCodecBuilder}, this does not require massively curried lambdas and so
 * is less likely to make IDEs cry, and may be slightly faster in some scenarios.
 */
@ApiStatus.Experimental
public final class KeyedRecordCodecBuilder<A> {
	private final List<Field<A, ?>> fields = new ArrayList<>();

	private KeyedRecordCodecBuilder() {}

	/**
	 * A key for a field in a {@link KeyedRecordCodecBuilder}; can be used to retrieve the value of that field from a
	 * {@link Container} created by the corresponding {@link KeyedRecordCodecBuilder}.
	 * @param <T> the type of the matching field
	 */
	public static final class Key<T> {
		private final int count;

		private Key(int i) {
			this.count = i;
		}
	}

	/**
	 * Holds the values of fields for the {@link KeyedRecordCodecBuilder} being built, when an object is being decoded.
	 */
	public static final class Container {
		private final Key<?>[] keys;
		private final Object[] array;

		private Container(Key<?>[] keys, Object[] array) {
			this.array = array;
			this.keys = keys;
		}

		@SuppressWarnings("unchecked")
		public <T> T get(Key<T> key) {
			if (key.count >= array.length || key != keys[key.count]) {
				throw new IllegalArgumentException("Key does not belong to the container");
			}
			return (T) array[key.count];
		}
	}

	private record Field<A, T>(Key<T> key, Function<A, T> getter, MapCodec<T> partial) {}

	/**
	 * Creates a codec given a {@link KeyedRecordCodecBuilder} building function.
	 * @param function should add all necessary fields and return the function to assemble the object on decode
	 * @return a {@link Codec} for the type {@code A}
	 * @param <A> the type of the object being encoded/decoded
	 */
	public static <A> Codec<A> codec(Function<KeyedRecordCodecBuilder<A>, Function<Container, A>> function) {
		return codecFlat(function.andThen(f -> f.andThen(DataResult::success)));
	}

	/**
	 * An equivalent to {@link #codec(Function)} that returns a {@link MapCodec} instead of a {@link Codec}.
	 * @param function should add all necessary fields and return the function to assemble the object on decode
	 * @return a {@link MapCodec} for the type {@code A}
	 * @param <A> the type of the object being encoded/decoded
	 */
	public static <A> MapCodec<A> mapCodec(Function<KeyedRecordCodecBuilder<A>, Function<Container, A>> function) {
		return mapCodecFlat(function.andThen(f -> f.andThen(DataResult::success)));
	}

	/**
	 * An equivalent to {@link #codec(Function)} that allows for optionally-successful combining of fields.
	 * @param function should add all necessary fields and return the function to assemble the object on decode
	 * @return a {@link MapCodec} for the type {@code A}
	 * @param <A> the type of the object being encoded/decoded
	 */
	public static <A> Codec<A> codecFlat(Function<KeyedRecordCodecBuilder<A>, Function<Container, DataResult<A>>> function) {
		return mapCodecFlat(function).codec();
	}

	/**
	 * An equivalent to {@link #mapCodec(Function)} that allows for optionally-successful combining of fields.
	 * @param function should add all necessary fields and return the function to assemble the object on decode
	 * @return a {@link MapCodec} for the type {@code A}
	 * @param <A> the type of the object being encoded/decoded
	 */
	public static <A> MapCodec<A> mapCodecFlat(Function<KeyedRecordCodecBuilder<A>, Function<Container, DataResult<A>>> function) {
		KeyedRecordCodecBuilder<A> builder = new KeyedRecordCodecBuilder<>();
		var combiner = function.apply(builder);
		List<Field<A, ?>> fields = List.copyOf(builder.fields);
		return new MapCodec<>() {
			@Override
			public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
				for (Field<A, ?> field : fields) {
					prefix = encodePartial(input, ops, prefix, field);
				}
				return prefix;
			}

			private <T, P> RecordBuilder<T> encodePartial(A input, DynamicOps<T> ops, RecordBuilder<T> prefix, Field<A, P> field) {
				P value = field.getter.apply(input);
				return field.partial.encode(value, ops, prefix);
			}

			@Override
			public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
				Key<?>[] keys = new Key[fields.size()];
				Container container = new Container(keys, new Object[fields.size()]);
				List<DataResult.Error<?>> errors = new ArrayList<>();
				for (Field<A, ?> field : fields) {
					keys[field.key.count] = field.key;
					decodePartial(ops, input, container, field, errors);
				}
				if (!errors.isEmpty()) {
					return DataResult.error(() -> "Failed to decode object: " + errors.stream().map(DataResult.Error::message).collect(Collectors.joining("; ")));
				}
				return combiner.apply(container);
			}

			private <T, P> void decodePartial(DynamicOps<T> ops, MapLike<T> input, Container container, Field<A, P> field, List<DataResult.Error<?>> errors) {
				DataResult<P> result = field.partial.decode(ops, input);
				result.result().ifPresent(value -> container.array[field.key.count] = value);
				result.error().ifPresent(errors::add);
			}

			@Override
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return fields.stream().flatMap(field -> field.partial.keys(ops));
			}
		};
	}

	/**
	 * Adds a field to the builder, and provides a key which can be used to retrieve that field's value from a
	 * {@link Container} when the object is being assembled.
	 * @param partial the codec for the field
	 * @param getter the getter for the field
	 * @return a {@link Key} that can be used to retrieve the value of the field
	 * @param <T> the type of the field
	 */
	public <T> Key<T> add(MapCodec<T> partial, Function<A, T> getter) {
		Key<T> key = new Key<>(fields.size());
		Field<A, T> field = new Field<>(key, getter, partial);
		fields.add(field);
		return key;
	}
}
