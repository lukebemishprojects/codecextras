package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.lukebemish.codecextras.comments.CommentMapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

class StructuredMapCodec<A> extends MapCodec<A> {
	private record Field<A, T>(MapCodec<T> codec, RecordStructure.Key<T> key, Function<A, T> getter) {}

	private final List<Field<A, ?>> fields;
	private final Function<RecordStructure.Container, A> creator;

	private StructuredMapCodec(List<Field<A, ?>> fields, Function<RecordStructure.Container, A> creator) {
		this.fields = fields;
		this.creator = creator;
	}

	public interface Unboxer<Mu extends K1> {
		<A> Codec<A> unbox(App<Mu, A> box);
	}

	public static <A, Mu extends K1> DataResult<MapCodec<A>> of(List<RecordStructure.Field<A, ?>> fields, Function<RecordStructure.Container, A> creator, Interpreter<Mu> interpreter, Unboxer<Mu> unboxer) {
		var mapCodecFields = new ArrayList<Field<A, ?>>();
		for (var field : fields) {
			DataResult<MapCodec<A>> result = recordSingleField(field, mapCodecFields, interpreter, unboxer);
			if (result != null) return result;
		}
		return DataResult.success(new StructuredMapCodec<>(mapCodecFields, creator));
	}

	private static <A, F, Mu extends K1> @Nullable DataResult<MapCodec<A>> recordSingleField(RecordStructure.Field<A, F> field, ArrayList<StructuredMapCodec.Field<A, ?>> mapCodecFields, Interpreter<Mu> interpreter, Unboxer<Mu> unboxer) {
		var result = field.structure().interpret(interpreter);
		if (result.error().isPresent()) {
			return DataResult.error(result.error().orElseThrow().messageSupplier());
		}
		Codec<F> fieldCodec = unboxer.unbox(result.result().orElseThrow());
		MapCodec<F> fieldMapCodec = field.structure().annotations().get(Annotations.COMMENT)
			.map(comment -> CommentMapCodec.of(makeFieldCodec(fieldCodec, field), comment))
			.orElseGet(() -> makeFieldCodec(fieldCodec, field));
		mapCodecFields.add(new StructuredMapCodec.Field<>(fieldMapCodec, field.key(), field.getter()));
		return null;
	}

	private static <A,F> MapCodec<F> makeFieldCodec(Codec<F> fieldCodec, RecordStructure.Field<A,F> field) {
		return field.missingBehavior().map(behavior -> fieldCodec.optionalFieldOf(field.name()).xmap(
			optional -> optional.orElseGet(behavior.missing()),
			value -> behavior.predicate().test(value) ? Optional.of(value) : Optional.empty()
		)).orElseGet(() -> fieldCodec.fieldOf(field.name()));
	}

	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		return fields.stream().flatMap(f -> f.codec().keys(ops));
	}

	@Override
	public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
		var builder = RecordStructure.Container.builder();
		for (var field : fields) {
			DataResult<A> result = singleField(ops, input, field, builder);
			if (result != null) return result;
		}
		return DataResult.success(creator.apply(builder.build()));
	}

	private static <A, T, F> @Nullable DataResult<A> singleField(DynamicOps<T> ops, MapLike<T> input, Field<A, F> field, RecordStructure.Container.Builder builder) {
		var key = field.key();
		var codec = field.codec();
		var result = codec.decode(ops, input);
		if (result.error().isPresent()) {
			return DataResult.error(result.error().orElseThrow().messageSupplier());
		}
		builder.add(key, result.result().orElseThrow());
		return null;
	}

	@Override
	public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		for (var field : fields) {
			prefix = encodeSingleField(input, ops, prefix, field);
		}
		return prefix;
	}

	private <T, F> RecordBuilder<T> encodeSingleField(A input, DynamicOps<T> ops, RecordBuilder<T> prefix, Field<A, F> field) {
		var codec = field.codec();
		var value = field.getter().apply(input);
		return codec.encode(value, ops, prefix);
	}
}
