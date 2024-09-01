package dev.lukebemish.codecextras.stream.structured;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.structured.Interpreter;
import dev.lukebemish.codecextras.structured.KeyStoringInterpreter;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.Keys2;
import dev.lukebemish.codecextras.structured.ParametricKeyedValue;
import dev.lukebemish.codecextras.structured.Range;
import dev.lukebemish.codecextras.structured.RecordStructure;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.types.Identity;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

public class StreamCodecInterpreter<B extends ByteBuf> extends KeyStoringInterpreter<StreamCodecInterpreter.Holder.Mu<B>, StreamCodecInterpreter<B>> {
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
        ), parametricKeys.join(Keys2.<ParametricKeyedValue.Mu<Holder.Mu<B>>, K1, K1>builder()
            .add(Interpreter.INT_IN_RANGE, numberRangeCodecParameter(ByteBufCodecs.VAR_INT.cast()))
            .add(Interpreter.BYTE_IN_RANGE, numberRangeCodecParameter(ByteBufCodecs.BYTE.cast()))
            .add(Interpreter.SHORT_IN_RANGE, numberRangeCodecParameter(ByteBufCodecs.SHORT.cast()))
            .add(Interpreter.LONG_IN_RANGE, numberRangeCodecParameter(ByteBufCodecs.VAR_LONG.cast()))
            .add(Interpreter.FLOAT_IN_RANGE, numberRangeCodecParameter(ByteBufCodecs.FLOAT.cast()))
            .add(Interpreter.DOUBLE_IN_RANGE, numberRangeCodecParameter(ByteBufCodecs.DOUBLE.cast()))
            .build()
        ));
    }

    private static <N extends Number & Comparable<N>, B extends ByteBuf> ParametricKeyedValue<StreamCodecInterpreter.Holder.Mu<B>, Const.Mu<Range<N>>, Const.Mu<N>> numberRangeCodecParameter(StreamCodec<B, N> codec) {
        return new ParametricKeyedValue<>() {
            @Override
            public <T> App<StreamCodecInterpreter.Holder.Mu<B>, App<Const.Mu<N>, T>> convert(App<Const.Mu<Range<N>>, T> parameter) {
                var range = Const.unbox(parameter);
                return new StreamCodecInterpreter.Holder<>(new StreamCodec<B, App<Const.Mu<N>, T>>() {
                    @Override
                    public App<Const.Mu<N>, T> decode(B buffer) {
                        var n = codec.decode(buffer);
                        if (n.compareTo(range.min()) < 0) {
                            throw new DecoderException("Value " + n + " is larger than max " + range.max());
                        } else if (n.compareTo(range.max()) > 0) {
                            throw new DecoderException("Value " + n + " is smaller than min " + range.min());
                        }
                        return Const.create(n);
                    }

                    @Override
                    public void encode(B buffer, App<Const.Mu<N>, T> object2) {
                        var value = Const.unbox(object2);
                        if (value.compareTo(range.min()) < 0) {
                            throw new DecoderException("Value " + value + " is larger than max " + range.max());
                        } else if (value.compareTo(range.max()) > 0) {
                            throw new DecoderException("Value " + value + " is smaller than min " + range.min());
                        }
                        codec.encode(buffer, value);
                    }
                });
            }
        };
    }

    public StreamCodecInterpreter() {
        this(
            Keys.<Holder.Mu<B>, Object>builder().build(),
            Keys2.<ParametricKeyedValue.Mu<Holder.Mu<B>>, K1, K1>builder().build()
        );
    }

    @Override
    public StreamCodecInterpreter<B> with(Keys<Holder.Mu<B>, Object> keys, Keys2<ParametricKeyedValue.Mu<Holder.Mu<B>>, K1, K1> parametricKeys) {
        return new StreamCodecInterpreter<>(keys().join(keys), parametricKeys().join(parametricKeys));
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
    public <A> DataResult<App<Holder.Mu<B>, A>> annotate(Structure<A> original, Keys<Identity.Mu, Object> annotations) {
        // No annotations handled here
        return original.interpret(this);
    }

    @Override
    public <E, A> DataResult<App<Holder.Mu<B>, E>> dispatch(String key, Structure<A> keyStructure, Function<? super E, ? extends DataResult<A>> function, Set<A> keys, Function<A, Structure<? extends E>> structures) {
        return keyStructure.interpret(this).flatMap(keyCodecApp -> {
            var keyStreamCodec = unbox(keyCodecApp);
            Supplier<Map<Object, DataResult<StreamCodec<B, ? extends E>>>> codecMapSupplier = Suppliers.memoize(() -> {
                Map<Object, DataResult<StreamCodec<B, ? extends E>>> codecMap = new HashMap<>();
                for (var entryKey : keys) {
                    var result = structures.apply(entryKey).interpret(this);
                    if (result.error().isPresent()) {
                        codecMap.put(entryKey, DataResult.error(result.error().get().messageSupplier()));
                    }
                    codecMap.put(entryKey, DataResult.success(StreamCodecInterpreter.unbox(result.result().orElseThrow())));
                }
                return codecMap;
            });
            return DataResult.success(new Holder<>(
                keyStreamCodec.dispatch(function.andThen(DataResult::getOrThrow), k -> codecMapSupplier.get().get(k).getOrThrow())
            ));
        });
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
