package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.lukebemish.codecextras.comments.CommentMapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

class StructuredMapCodec<A> extends MapCodec<A> {
    private record Field<A, T>(String name, MapCodec<T> codec, RecordStructure.Key<T> key, Function<A, T> getter) {}

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
        boolean lenient = Annotation.get(field.structure().annotations(), Annotation.LENIENT).isPresent();
        MapCodec<F> fieldMapCodec = Annotation.get(field.structure().annotations(), Annotation.COMMENT)
            .map(comment -> CommentMapCodec.of(makeFieldCodec(fieldCodec, field, lenient), comment))
            .orElseGet(() -> makeFieldCodec(fieldCodec, field, lenient));
        mapCodecFields.add(new StructuredMapCodec.Field<>(field.name(), fieldMapCodec, field.key(), field.getter()));
        return null;
    }

    private static <A,F> MapCodec<F> makeFieldCodec(Codec<F> fieldCodec, RecordStructure.Field<A,F> field, boolean lenient) {
        return field.missingBehavior().map(behavior -> (lenient ? fieldCodec.lenientOptionalFieldOf(field.name()) : fieldCodec.optionalFieldOf(field.name())).xmap(
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
        boolean isPartial = false;
        boolean isError = false;
        Lifecycle errorLifecycle = Lifecycle.stable();
        Supplier<String> errorMessage = null;
        for (var field : fields) {
            DataResult<?> result = singleField(ops, input, field, builder);
            if (result.isError()) {
                if (result.hasResultOrPartial()) {
                    isPartial = true;
                }
                isError = true;
                errorLifecycle = errorLifecycle.add(result.lifecycle());
                if (errorMessage == null) {
                    errorMessage = result.error().orElseThrow().messageSupplier();
                } else {
                    var oldMessage = errorMessage;
                    errorMessage = () -> oldMessage.get() + ": " + result.error().orElseThrow().messageSupplier().get();
                }
            }
        }
        if (isError) {
            if (isPartial) {
                return DataResult.error(errorMessage, creator.apply(builder.build()), errorLifecycle);
            } else {
                return DataResult.error(errorMessage, errorLifecycle);
            }
        } else {
            return DataResult.success(creator.apply(builder.build()));
        }
    }

    private static <A, T, F> DataResult<F> singleField(DynamicOps<T> ops, MapLike<T> input, Field<A, F> field, RecordStructure.Container.Builder builder) {
        var key = field.key();
        var codec = field.codec();
        var result = codec.decode(ops, input);
        if (result.hasResultOrPartial()) {
            builder.add(key, result.resultOrPartial().orElseThrow());
        }
        return result;
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

    @Override
    public String toString() {
        var fields = this.fields.stream().map(f -> f.codec().toString()).reduce((a, b) -> a + ", " + b).orElse("");
        return "StructuredMapCodec[" + fields + "]";
    }
}
