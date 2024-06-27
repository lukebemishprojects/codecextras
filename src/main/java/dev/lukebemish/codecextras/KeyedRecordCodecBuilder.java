package dev.lukebemish.codecextras;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jetbrains.annotations.ApiStatus;

/**
 * Similar to {@link ExtendedRecordCodecBuilder}, an alternative to {@link RecordCodecBuilder} that allows for any
 * number of fields. Unlike {@link ExtendedRecordCodecBuilder}, this does not require massively curried lambdas and so
 * is less likely to make IDEs cry, and may be slightly faster in some scenarios; the tradeoff is some particularly
 * nested lambdas to build it.
 */
@ApiStatus.Experimental
public final class KeyedRecordCodecBuilder<A> {
	private final List<Field<A, ?>> fields;

	private KeyedRecordCodecBuilder(List<Field<A, ?>> fields) {
		this.fields = fields;
	}

	public static final class Key<T> {
		private final int count;

		private Key(int i) {
			this.count = i;
		}
	}

	public static final class Built<A> {
		private final KeyedRecordCodecBuilder<A> builder;
		private final Function<Container, DataResult<A>> function;

		private Built(KeyedRecordCodecBuilder<A> builder, Function<Container, DataResult<A>> function) {
			this.builder = builder;
			this.function = function;
		}
	}

	public static final class Container {
		private final Object[] array;

		private Container(Object[] array) {
			this.array = array;
		}

		@SuppressWarnings("unchecked")
		public <T> T get(Key<T> key) {
			return (T) array[key.count];
		}
	}

	private record Field<A, T>(Key<T> key, Function<A, T> getter, MapCodec<T> partial) {}

	public static <A> Codec<A> codec(Function<KeyedRecordCodecBuilder<A>, Built<A>> function) {
		return mapCodec(function).codec();
	}

	public static <A> MapCodec<A> mapCodec(Function<KeyedRecordCodecBuilder<A>, Built<A>> function) {
		KeyedRecordCodecBuilder<A> builder = new KeyedRecordCodecBuilder<>(List.of());
		Built<A> built = function.apply(builder);
		return new MapCodec<>() {
			@Override
			public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
				for (Field<A, ?> field : built.builder.fields) {
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
				Container container = new Container(new Object[built.builder.fields.size()]);
				for (Field<A, ?> field : built.builder.fields) {
					decodePartial(ops, input, container, field);
				}
				return built.function.apply(container);
			}

			private <T, P> void decodePartial(DynamicOps<T> ops, MapLike<T> input, Container container, Field<A, P> field) {
				DataResult<P> result = field.partial.decode(ops, input);
				result.result().ifPresent(value -> container.array[field.key.count] = value);
			}

			@Override
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return built.builder.fields.stream().flatMap(field -> field.partial.keys(ops));
			}
		};
	}

	public Built<A> flatBuild(Function<Container, DataResult<A>> function) {
		return new Built<>(this, function);
	}

	public Built<A> build(Function<Container, A> function) {
		return flatBuild(function.andThen(DataResult::success));
	}

	public <T> Built<A> with(MapCodec<T> partial, Function<A, T> getter, BiFunction<KeyedRecordCodecBuilder<A>, Key<T>, Built<A>> rest) {
		Key<T> key = new Key<>(fields.size());
		Field<A, T> field = new Field<>(key, getter, partial);
		List<Field<A, ?>> newFields = new ArrayList<>(fields);
		newFields.add(field);
		KeyedRecordCodecBuilder<A> newBuilder = new KeyedRecordCodecBuilder<>(newFields);
		return rest.apply(newBuilder, key);
	}
}
