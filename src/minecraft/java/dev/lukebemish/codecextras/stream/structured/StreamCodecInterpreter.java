package dev.lukebemish.codecextras.stream.structured;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.StringRepresentation;
import dev.lukebemish.codecextras.structured.Interpreter;
import dev.lukebemish.codecextras.structured.Key;
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
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

/**
 * Interprets a {@link Structure} into a {@link StreamCodec} for the same type.
 * @param <B> the type of the {@link ByteBuf} to encode and decode
 * @see #interpret(Structure)
 */
public class StreamCodecInterpreter<B extends ByteBuf> extends KeyStoringInterpreter<StreamCodecInterpreter.Holder.Mu<B>, StreamCodecInterpreter<B>> {
    private final Key<Holder.Mu<B>> key;
    private final List<KeyConsumer<?, Holder.Mu<B>>> parentConsumers;
    private final List<StreamCodecInterpreter<? super B>> parents;

    public StreamCodecInterpreter(Key<Holder.Mu<B>> key, List<StreamCodecInterpreter<? super B>> parents, Keys<Holder.Mu<B>, Object> keys, Keys2<ParametricKeyedValue.Mu<Holder.Mu<B>>, K1, K1> parametricKeys) {
        super(Keys.<Holder.Mu<B>, Object>builder()
            .add(Interpreter.UNIT, new Holder<>(StreamCodec.of((buf, data) -> {}, buf -> Unit.INSTANCE)))
            .add(Interpreter.BOOL, new Holder<>(ByteBufCodecs.BOOL.cast()))
            .add(Interpreter.BYTE, new Holder<>(ByteBufCodecs.BYTE.cast()))
            .add(Interpreter.SHORT, new Holder<>(ByteBufCodecs.SHORT.cast()))
            .add(Interpreter.INT, new Holder<>(ByteBufCodecs.VAR_INT.cast()))
            .add(Interpreter.LONG, new Holder<>(ByteBufCodecs.VAR_LONG.cast()))
            .add(Interpreter.FLOAT, new Holder<>(ByteBufCodecs.FLOAT.cast()))
            .add(Interpreter.DOUBLE, new Holder<>(ByteBufCodecs.DOUBLE.cast()))
            .add(Interpreter.STRING, new Holder<>(ByteBufCodecs.STRING_UTF8.cast()))
            .build().join(buildCombinedKeys(parents)).join(keys),
            Keys2.<ParametricKeyedValue.Mu<Holder.Mu<B>>, K1, K1>builder()
            .add(Interpreter.INT_IN_RANGE, numberRangeCodecParameter(ByteBufCodecs.VAR_INT.cast()))
            .add(Interpreter.BYTE_IN_RANGE, numberRangeCodecParameter(ByteBufCodecs.BYTE.cast()))
            .add(Interpreter.SHORT_IN_RANGE, numberRangeCodecParameter(ByteBufCodecs.SHORT.cast()))
            .add(Interpreter.LONG_IN_RANGE, numberRangeCodecParameter(ByteBufCodecs.VAR_LONG.cast()))
            .add(Interpreter.FLOAT_IN_RANGE, numberRangeCodecParameter(ByteBufCodecs.FLOAT.cast()))
            .add(Interpreter.DOUBLE_IN_RANGE, numberRangeCodecParameter(ByteBufCodecs.DOUBLE.cast()))
            .add(Interpreter.STRING_REPRESENTABLE, new ParametricKeyedValue<>() {
                @Override
                public <T> App<Holder.Mu<B>, App<Identity.Mu, T>> convert(App<StringRepresentation.Mu, T> parameter) {
                    var representation = StringRepresentation.unbox(parameter);
                    Supplier<StreamCodec<B, T>> lazy = Suppliers.memoize(() -> {
                        var values = representation.values().get();
                        Map<T, Integer> toIndexMap = new IdentityHashMap<>();
                        for (int i = 0; i < values.size(); i++) {
                            toIndexMap.put(values.get(i), i);
                        }
                        return new StreamCodec<>() {
                            @Override
                            public T decode(B buffer) {
                                var intValue = VarInt.read(buffer);
                                if (intValue < 0 || intValue >= values.size()) {
                                    throw new DecoderException("Unknown representation value: " + intValue);
                                }
                                return values.get(intValue);
                            }

                            @Override
                            public void encode(B buffer, T object) {
                                var index = toIndexMap.get(object);
                                if (index == null) {
                                    throw new DecoderException("Unknown representation value: " + object);
                                }
                                VarInt.write(buffer, index);
                            }
                        };
                    });
                    return new Holder<>(new StreamCodec<>() {
                        @Override
                        public App<Identity.Mu, T> decode(B buffer) {
                            return new Identity<>(lazy.get().decode(buffer));
                        }

                        @Override
                        public void encode(B buffer, App<Identity.Mu, T> object) {
                            var value = Identity.unbox(object).value();
                            lazy.get().encode(buffer, value);
                        }
                    });
                }
            })
            .build().join(buildCombinedParametricKeys(parents)).join(parametricKeys)
        );
        this.parents = parents;
        this.parentConsumers = new ArrayList<>();
        for (var parent : parents) {
            parent.parentConsumers.forEach(c -> addKeyConsumer(this.parentConsumers, c));
        }
        this.key = key;
    }

    private static <B extends ByteBuf> Keys<Holder.Mu<B>, Object> buildCombinedKeys(List<StreamCodecInterpreter<? super B>> parents) {
        var builder = Keys.<Holder.Mu<B>, Object>builder();
        for (var parent : parents) {
            addConvertedKeysFromParent(builder, parent);
        }
        return builder.build();
    }

    private static <B extends ByteBuf> Keys2<ParametricKeyedValue.Mu<Holder.Mu<B>>, K1, K1> buildCombinedParametricKeys(List<StreamCodecInterpreter<? super B>> parents) {
        var builder = Keys2.<ParametricKeyedValue.Mu<Holder.Mu<B>>, K1, K1>builder();
        for (var parent : parents) {
            addConvertedParametricKeysFromParent(builder, parent);
        }
        return builder.build();
    }

    private static <P extends ByteBuf, B extends P> void addConvertedParametricKeysFromParent(Keys2.Builder<ParametricKeyedValue.Mu<Holder.Mu<B>>, K1, K1> builder, StreamCodecInterpreter<P> parent) {
        builder.join(parent.parametricKeys().map(new Keys2.Converter<>() {
            @Override
            public <X extends K1, Y extends K1> App2<ParametricKeyedValue.Mu<Holder.Mu<B>>, X, Y> convert(App2<ParametricKeyedValue.Mu<Holder.Mu<P>>, X, Y> input) {
                var value = ParametricKeyedValue.unbox(input);
                return value.map(new ParametricKeyedValue.Converter<>() {
                    @Override
                    public <T, MuO extends K1> App<Holder.Mu<B>, App<MuO, T>> convert(App<Holder.Mu<P>, App<MuO, T>> app) {
                        return new Holder<>(unbox(app).cast());
                    }
                });
            }
        }));
    }

    private static <P extends ByteBuf, B extends P> void addConvertedKeysFromParent(Keys.Builder<Holder.Mu<B>, Object> builder, StreamCodecInterpreter<P> parent) {
        builder.join(parent.keys().map(new Keys.Converter<>() {
            @Override
            public <A> App<Holder.Mu<B>, A> convert(App<Holder.Mu<P>, A> input) {
                return new Holder<>(unbox(input).cast());
            }
        }));
    }

    private static <P extends ByteBuf, B extends P, MuA extends K1> void addKeyConsumer(List<KeyConsumer<?, Holder.Mu<B>>> keyConsumers, KeyConsumer<MuA, Holder.Mu<P>> original) {
        keyConsumers.add(new KeyConsumer<MuA, Holder.Mu<B>>() {
            @Override
            public Key<MuA> key() {
                return original.key();
            }

            @Override
            public <T> App<Holder.Mu<B>, T> convert(App<MuA, T> input) {
                var converted = original.convert(input);
                var stream = unbox(converted);
                return new StreamCodecInterpreter.Holder<>(stream.cast());
            }
        });
    }

    public StreamCodecInterpreter(Key<Holder.Mu<B>> key, Keys<Holder.Mu<B>, Object> keys, Keys2<ParametricKeyedValue.Mu<Holder.Mu<B>>, K1, K1> parametricKeys) {
        this(key, List.of(), keys, parametricKeys);
    }

    public static final Key<Holder.Mu<FriendlyByteBuf>> FRIENDLY_BYTE_BUF_KEY = Key.create("StreamCodecInterpreter<FriendlyByteBuf>");
    public static final Key<Holder.Mu<RegistryFriendlyByteBuf>> REGISTRY_FRIENDLY_BYTE_BUF_KEY = Key.create("StreamCodecInterpreter<RegistryFriendlyByteBuf>");

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

    public StreamCodecInterpreter(Key<Holder.Mu<B>> key) {
        this(
            key,
            Keys.<Holder.Mu<B>, Object>builder().build(),
            Keys2.<ParametricKeyedValue.Mu<Holder.Mu<B>>, K1, K1>builder().build()
        );
    }

    @Override
    public StreamCodecInterpreter<B> with(Keys<Holder.Mu<B>, Object> keys, Keys2<ParametricKeyedValue.Mu<Holder.Mu<B>>, K1, K1> parametricKeys) {
        return new StreamCodecInterpreter<>(key, parents, keys().join(keys), parametricKeys().join(parametricKeys));
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
    public <X, Y> DataResult<App<Holder.Mu<B>, Y>> flatXmap(App<Holder.Mu<B>, X> input, Function<X, DataResult<Y>> to, Function<Y, DataResult<X>> from) {
        var streamCodec = unbox(input);
        return DataResult.success(new Holder<>(streamCodec.map(
            x -> to.apply(x).getOrThrow(),
            y -> from.apply(y).getOrThrow()
        )));
    }

    @Override
    public <A> DataResult<App<Holder.Mu<B>, A>> annotate(Structure<A> original, Keys<Identity.Mu, Object> annotations) {
        // No annotations handled here
        return original.interpret(this);
    }

    @Override
    public <E, A> DataResult<App<Holder.Mu<B>, E>> dispatch(String key, Structure<A> keyStructure, Function<? super E, ? extends DataResult<A>> function, Supplier<Set<A>> keys, Function<A, DataResult<Structure<? extends E>>> structures) {
        return keyStructure.interpret(this).flatMap(keyCodecApp -> {
            var keyStreamCodec = unbox(keyCodecApp);
            var map = new ConcurrentHashMap<A, DataResult<StreamCodec<B, ? extends E>>>();
            Function<A, DataResult<StreamCodec<B, ? extends E>>> cache = k -> map.computeIfAbsent(k , structures.andThen(result -> result.flatMap(s -> s.interpret(this)).map(StreamCodecInterpreter::unbox)));
            return DataResult.success(new Holder<>(
                keyStreamCodec.dispatch(function.andThen(DataResult::getOrThrow), cache.andThen(DataResult::getOrThrow))
            ));
        });
    }

    @Override
    public <K, V> DataResult<App<Holder.Mu<B>, Map<K, V>>> dispatchedMap(Structure<K> keyStructure, Supplier<Set<K>> keys, Function<K, DataResult<Structure<? extends V>>> valueStructures) {
        return keyStructure.interpret(this).map(StreamCodecInterpreter::unbox).flatMap(keyCodec -> {
            var map = new ConcurrentHashMap<K, DataResult<StreamCodec<B, ? extends V>>>();
            Function<K, DataResult<StreamCodec<B, ? extends V>>> cache = k -> map.computeIfAbsent(k , valueStructures.andThen(result -> result.flatMap(s -> s.interpret(this)).map(StreamCodecInterpreter::unbox)));
            return DataResult.success(new Holder<>(new StreamCodec<>() {
                @Override
                public Map<K, V> decode(B buffer) {
                    var map = new HashMap<K, V>();
                    var size = VarInt.read(buffer);
                    for (int i = 0; i < size; i++) {
                        var key = keyCodec.decode(buffer);
                        var valueCodec = cache.apply(key).getOrThrow(s -> new DecoderException("Could not find StreamCodec for key "+key+": "+s));
                        var value = valueCodec.decode(buffer);
                        map.put(key, value);
                    }
                    return map;
                }

                @Override
                public void encode(B buffer, Map<K, V> object) {
                    buffer.writeInt(object.size());
                    object.forEach((key, value) -> {
                        keyCodec.encode(buffer, key);
                        var valueCodec = cache.apply(key).getOrThrow(s -> new EncoderException("Could not find StreamCodec for key "+ key +": "+s));
                        encodeValue(buffer, valueCodec, value);
                    });
                }

                @SuppressWarnings("unchecked")
                private <X extends V> void encodeValue(B buffer, StreamCodec<B, X> valueCodec, V value) {
                    valueCodec.encode(buffer, (X) value);
                }
            }));
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

    @Override
    public Stream<KeyConsumer<?, Holder.Mu<B>>> keyConsumers() {
        return Stream.concat(
            Stream.of(new KeyConsumer<Holder.Mu<B>, Holder.Mu<B>>() {
                @Override
                public Key<Holder.Mu<B>> key() {
                    return key;
                }

                @Override
                public <T> App<Holder.Mu<B>, T> convert(App<Holder.Mu<B>, T> input) {
                    return input;
                }
            }),
            parentConsumers.stream()
        );
    }

    @Override
    public <K, V> DataResult<App<Holder.Mu<B>, Map<K, V>>> unboundedMap(App<Holder.Mu<B>, K> k, App<Holder.Mu<B>, V> v) {
        return DataResult.success(new Holder<>(new StreamCodec<>() {
            @Override
            public Map<K, V> decode(B buffer) {
                var map = new HashMap<K, V>();
                int size = VarInt.read(buffer);
                for (int i = 0; i < size; i++) {
                    K key = unbox(k).decode(buffer);
                    V value = unbox(v).decode(buffer);
                    map.put(key, value);
                }
                return map;
            }

            @Override
            public void encode(B buffer, Map<K, V> object) {
                VarInt.write(buffer, object.size());
                object.forEach((key, value) -> {
                    unbox(k).encode(buffer, key);
                    unbox(v).encode(buffer, value);
                });
            }
        }));
    }

    @Override
    public <L, R> DataResult<App<Holder.Mu<B>, Either<L, R>>> either(App<Holder.Mu<B>, L> left, App<Holder.Mu<B>, R> right) {
        var leftCodec = unbox(left);
        var rightCodec = unbox(right);
        return DataResult.success(new Holder<>(ByteBufCodecs.either(leftCodec, rightCodec)));
    }

    @Override
    public <L, R> DataResult<App<Holder.Mu<B>, Either<L, R>>> xor(App<Holder.Mu<B>, L> left, App<Holder.Mu<B>, R> right) {
        // For stream codecs, xor is just either
        return either(left, right);
    }

    public record Holder<B extends ByteBuf, T>(StreamCodec<B, T> streamCodec) implements App<StreamCodecInterpreter.Holder.Mu<B>, T> {
        public static final class Mu<B extends ByteBuf> implements K1 {}

        static <B extends ByteBuf, T> StreamCodecInterpreter.Holder<B, T> unbox(App<StreamCodecInterpreter.Holder.Mu<B>, T> box) {
            return (StreamCodecInterpreter.Holder<B, T>) box;
        }
    }

    private record Field<A, B, T>(StreamCodec<? super B, T> codec, RecordStructure.Key<T> key, Function<A, T> getter, Optional<RecordStructure.Field.MissingBehavior<T>> missingBehavior) {}
}
