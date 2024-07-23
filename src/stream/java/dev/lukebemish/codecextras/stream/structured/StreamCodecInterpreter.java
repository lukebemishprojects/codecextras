package dev.lukebemish.codecextras.stream.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.structured.Interpreter;
import dev.lukebemish.codecextras.structured.KeyStoringInterpreter;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.Keys2;
import dev.lukebemish.codecextras.structured.ParametricKeyedValue;
import dev.lukebemish.codecextras.structured.RecordStructure;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.types.Identity;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

public class StreamCodecInterpreter<B extends ByteBuf> extends KeyStoringInterpreter<StreamCodecInterpreter.Holder.Mu<B>> {
    public StreamCodecInterpreter(Keys<Holder.Mu<B>, Object> keys, Keys2<ParametricKeyedValue.Mu<Holder.Mu<B>>, K1, K1> parametricKeys) {
        super(keys.join(Keys.<Holder.Mu<B>, Object>builder()
            .add(Interpreter.UNIT, new Holder<>(StreamCodec.of((buf, data) -> {}, buf -> Unit.INSTANCE)))
            .add(Interpreter.BOOL, new Holder<>(ByteBufCodecs.BOOL.cast()))
            .add(Interpreter.BYTE, new Holder<>(ByteBufCodecs.BYTE.cast()))
            .add(Interpreter.SHORT, new Holder<>(ByteBufCodecs.SHORT.cast()))
            .add(Interpreter.INT, new Holder<>(ByteBufCodecs.VAR_INT.cast()))
            .add(Interpreter.LONG, new Holder<>(ByteBufCodecs.VAR_LONG.cast()))
            .add(Interpreter.FLOAT, new Holder<>(ByteBufCodecs.FLOAT.cast()))
            .add(Interpreter.DOUBLE, new Holder<>(ByteBufCodecs.DOUBLE.cast()))
            .add(Interpreter.STRING, new Holder<>(ByteBufCodecs.STRING_UTF8.cast()))
            .build()
        ), parametricKeys);
    }

    public StreamCodecInterpreter() {
        this(
            Keys.<Holder.Mu<B>, Object>builder().build(),
            Keys2.<ParametricKeyedValue.Mu<Holder.Mu<B>>, K1, K1>builder().build()
        );
    }

    @Override
    public <A> DataResult<App<Holder.Mu<B>, List<A>>> list(App<Holder.Mu<B>, A> single) {
        return DataResult.success(new Holder<>(StreamCodecInterpreter.list(unbox(single))));
    }

    private static <B extends ByteBuf, T> StreamCodec<B, List<T>> list(StreamCodec<B, T> elementCodec) {
        return ByteBufCodecs.<B, T>list().apply(elementCodec);
    }

    @Override
    public <A> DataResult<App<Holder.Mu<B>, A>> record(List<RecordStructure.Field<A, ?>> fields, Function<RecordStructure.Container, A> creator) {
        var streamFields = new ArrayList<Field<A, B, ?>>();
        for (var field : fields) {
            DataResult<App<Holder.Mu<B>, A>> result = recordSingleField(field, streamFields);
            if (result != null) return result;
        }
        return DataResult.success(new Holder<>(StreamCodec.of(
            (buf, data) -> {
                for (var field : streamFields) {
                    encodeSingleField(buf, field, data);
                }
            },
            buf -> {
                var builder = RecordStructure.Container.builder();
                for (var field : streamFields) {
                    decodeSingleField(buf, field, builder);
                }
                return creator.apply(builder.build());
            }
        )));
    }

    @Override
    public <X, Y> DataResult<App<Holder.Mu<B>, Y>> flatXmap(App<Holder.Mu<B>, X> input, Function<X, DataResult<Y>> deserializer, Function<Y, DataResult<X>> serializer) {
        var streamCodec = unbox(input);
        return DataResult.success(new Holder<>(streamCodec.map(
            x -> deserializer.apply(x).getOrThrow(),
            y -> serializer.apply(y).getOrThrow()
        )));
    }

    @Override
    public <A> DataResult<App<Holder.Mu<B>, A>> annotate(App<Holder.Mu<B>, A> input, Keys<Identity.Mu, Object> annotations) {
        // No annotations handled here
        return DataResult.success(input);
    }

    private static <B extends ByteBuf, A, F> void encodeSingleField(B buf, Field<A, B, F> field, A data) {
        var missingBehaviour = field.missingBehavior();
        if (missingBehaviour.isEmpty()) {
            field.codec.encode(buf, field.getter.apply(data));
        } else {
            var behavior = missingBehaviour.get();
            if (behavior.predicate().test(field.getter.apply(data))) {
                buf.writeBoolean(true);
                field.codec.encode(buf, field.getter.apply(data));
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    private static <B extends ByteBuf, A, F> void decodeSingleField(B buf, Field<A, B, F> field, RecordStructure.Container.Builder builder) {
        var missingBehaviour = field.missingBehavior();
        if (missingBehaviour.isEmpty()) {
            var value = field.codec.decode(buf);
            builder.add(field.key(), value);
        } else {
            if (buf.readBoolean()) {
                var value = field.codec.decode(buf);
                builder.add(field.key(), value);
            } else {
                builder.add(field.key(), missingBehaviour.get().missing().get());
            }
        }
    }

    private <A, F> @Nullable DataResult<App<Holder.Mu<B>, A>> recordSingleField(RecordStructure.Field<A, F> field, ArrayList<Field<A, B, ?>> streamFields) {
        var result = field.structure().interpret(this);
        if (result.error().isPresent()) {
            return DataResult.error(result.error().orElseThrow().messageSupplier());
        }
        streamFields.add(new Field<>(unbox(result.result().orElseThrow()), field.key(), field.getter(), field.missingBehavior()));
        return null;
    }

    public static <B extends ByteBuf, T> StreamCodec<B, T> unbox(App<Holder.Mu<B>, T> box) {
        return Holder.unbox(box).streamCodec();
    }

    public <T> DataResult<StreamCodec<B, T>> interpret(Structure<T> structure) {
        return structure.interpret(this).map(StreamCodecInterpreter::unbox);
    }

    public record Holder<B extends ByteBuf, T>(StreamCodec<B, T> streamCodec) implements App<StreamCodecInterpreter.Holder.Mu<B>, T> {
        public static final class Mu<B extends ByteBuf> implements K1 {}

        static <B extends ByteBuf, T> StreamCodecInterpreter.Holder<B, T> unbox(App<StreamCodecInterpreter.Holder.Mu<B>, T> box) {
            return (StreamCodecInterpreter.Holder<B, T>) box;
        }
    }

    private record Field<A, B, T>(StreamCodec<? super B, T> codec, RecordStructure.Key<T> key, Function<A, T> getter, Optional<RecordStructure.Field.MissingBehavior<T>> missingBehavior) {}
}
