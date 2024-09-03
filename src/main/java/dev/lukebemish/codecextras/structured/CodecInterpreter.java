package dev.lukebemish.codecextras.structured;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.lukebemish.codecextras.StringRepresentation;
import dev.lukebemish.codecextras.comments.CommentFirstListCodec;
import dev.lukebemish.codecextras.types.Identity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class CodecInterpreter extends KeyStoringInterpreter<CodecInterpreter.Holder.Mu, CodecInterpreter> {
    public CodecInterpreter(Keys<Holder.Mu, Object> keys, Keys2<ParametricKeyedValue.Mu<Holder.Mu>, K1, K1> parametricKeys) {
        super(keys.join(Keys.<Holder.Mu, Object>builder()
            .add(Interpreter.UNIT, new Holder<>(Codec.unit(Unit.INSTANCE)))
            .add(Interpreter.BOOL, new Holder<>(Codec.BOOL))
            .add(Interpreter.BYTE, new Holder<>(Codec.BYTE))
            .add(Interpreter.SHORT, new Holder<>(Codec.SHORT))
            .add(Interpreter.INT, new Holder<>(Codec.INT))
            .add(Interpreter.LONG, new Holder<>(Codec.LONG))
            .add(Interpreter.FLOAT, new Holder<>(Codec.FLOAT))
            .add(Interpreter.DOUBLE, new Holder<>(Codec.DOUBLE))
            .add(Interpreter.STRING, new Holder<>(Codec.STRING))
            .build()
        ), parametricKeys.join(Keys2.<ParametricKeyedValue.Mu<Holder.Mu>, K1, K1>builder()
            .add(Interpreter.INT_IN_RANGE, numberRangeCodecParameter(Codec.INT))
            .add(Interpreter.BYTE_IN_RANGE, numberRangeCodecParameter(Codec.BYTE))
            .add(Interpreter.SHORT_IN_RANGE, numberRangeCodecParameter(Codec.SHORT))
            .add(Interpreter.LONG_IN_RANGE, numberRangeCodecParameter(Codec.LONG))
            .add(Interpreter.FLOAT_IN_RANGE, numberRangeCodecParameter(Codec.FLOAT))
            .add(Interpreter.DOUBLE_IN_RANGE, numberRangeCodecParameter(Codec.DOUBLE))
            .add(Interpreter.STRING_REPRESENTABLE, new ParametricKeyedValue<>() {
                @Override
                public <T> App<Holder.Mu, App<Identity.Mu, T>> convert(App<StringRepresentation.Mu, T> parameter) {
                    var representation = StringRepresentation.unbox(parameter);
                    return new Holder<>(representation.codec().xmap(Identity::new, app -> Identity.unbox(app).value()));
                }
            })
            .build()
        ));
    }

    private static <N extends Number & Comparable<N>> ParametricKeyedValue<Holder.Mu, Const.Mu<Range<N>>, Const.Mu<N>> numberRangeCodecParameter(Codec<N> codec) {
        return new ParametricKeyedValue<>() {
            @Override
            public <T> App<Holder.Mu, App<Const.Mu<N>, T>> convert(App<Const.Mu<Range<N>>, T> parameter) {
                return new Holder<>(codec.validate(Codec.checkRange(Const.unbox(parameter).min(), Const.unbox(parameter).max())).xmap(Const::create, Const::unbox));
            }
        };
    }

    public static CodecInterpreter create(
        Keys<CodecInterpreter.Holder.Mu, Object> codecKeys,
        Keys<MapCodecInterpreter.Holder.Mu, Object> mapCodecKeys,
        Keys2<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1> parametricCodecKeys,
        Keys2<ParametricKeyedValue.Mu<MapCodecInterpreter.Holder.Mu>, K1, K1> parametricMapCodecKeys
    ) {
        return new CodecAndMapInterpreters(codecKeys, mapCodecKeys, parametricCodecKeys, parametricMapCodecKeys).codecInterpreter();
    }

    public static CodecInterpreter create() {
        return new CodecAndMapInterpreters().codecInterpreter();
    }

    protected abstract MapCodecInterpreter mapCodecInterpreter();

    @Override
    public <A> DataResult<App<Holder.Mu, List<A>>> list(App<Holder.Mu, A> single) {
        return DataResult.success(new Holder<>(CommentFirstListCodec.of(Holder.unbox(single).codec)));
    }

    @Override
    public <A> DataResult<App<Holder.Mu, A>> record(List<RecordStructure.Field<A, ?>> fields, Function<RecordStructure.Container, A> creator) {
        return StructuredMapCodec.of(fields, creator, this, CodecInterpreter::unbox)
            .map(mapCodec -> new Holder<>(mapCodec.codec()));
    }

    @Override
    public <A, B> DataResult<App<Holder.Mu, B>> flatXmap(App<Holder.Mu, A> input, Function<A, DataResult<B>> deserializer, Function<B, DataResult<A>> serializer) {
        var codec = Holder.unbox(input).codec();
        return DataResult.success(new Holder<>(codec.flatXmap(deserializer, serializer)));
    }

    @Override
    public <A> DataResult<App<Holder.Mu, A>> annotate(Structure<A> original, Keys<Identity.Mu, Object> annotations) {
        // No annotations handled here
        return original.interpret(this);
    }

    @Override
    public <E, A> DataResult<App<Holder.Mu, E>> dispatch(String key, Structure<A> keyStructure, Function<? super E, ? extends DataResult<A>> function, Supplier<Set<A>> keys, Function<A, DataResult<Structure<? extends E>>> structures) {
        return keyStructure.interpret(this).flatMap(keyCodecApp -> {
            var keyCodec = unbox(keyCodecApp);
            // Object here as it's the furthest super A and we have only ? super A
            Supplier<Map<A, DataResult<MapCodec<? extends E>>>> codecMapSupplier = Suppliers.memoize(() -> {
                Map<A, DataResult<MapCodec<? extends E>>> codecMap = new HashMap<>();
                for (var entryKey : keys.get()) {
                    var result = structures.apply(entryKey).interpret(mapCodecInterpreter());
                    if (result.error().isPresent()) {
                        codecMap.put(entryKey, DataResult.error(result.error().get().messageSupplier()));
                    } else {
                        codecMap.put(entryKey, DataResult.success(MapCodecInterpreter.unbox(result.result().orElseThrow())));
                    }
                }
                return codecMap;
            });
            return DataResult.success(new Holder<>(keyCodec.partialDispatch(key, function, k -> codecMapSupplier.get().get(k))));
        });
    }

    @Override
    public <K, V> DataResult<App<Holder.Mu, Map<K, V>>> unboundedMap(App<Holder.Mu, K> key, App<Holder.Mu, V> value) {
        var keyCodec = unbox(key);
        var valueCodec = unbox(value);
        return DataResult.success(new Holder<>(Codec.unboundedMap(keyCodec, valueCodec)));
    }

    @Override
    public <L, R> DataResult<App<Holder.Mu, Either<L, R>>> either(App<Holder.Mu, L> left, App<Holder.Mu, R> right) {
        var leftCodec = unbox(left);
        var rightCodec = unbox(right);
        return DataResult.success(new Holder<>(Codec.either(leftCodec, rightCodec)));
    }

    @Override
    public CodecInterpreter with(Keys<Holder.Mu, Object> keys, Keys2<ParametricKeyedValue.Mu<Holder.Mu>, K1, K1> parametricKeys) {
        return new CodecAndMapInterpreters(keys().join(keys), mapCodecInterpreter().keys(), parametricKeys().join(parametricKeys), mapCodecInterpreter().parametricKeys()).codecInterpreter();
    }

    public CodecInterpreter with(
        Keys<CodecInterpreter.Holder.Mu, Object> codecKeys,
        Keys<MapCodecInterpreter.Holder.Mu, Object> mapCodecKeys,
        Keys2<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1> parametricCodecKeys,
        Keys2<ParametricKeyedValue.Mu<MapCodecInterpreter.Holder.Mu>, K1, K1> parametricMapCodecKeys
    ) {
        return new CodecAndMapInterpreters(
            keys().join(codecKeys),
            mapCodecInterpreter().keys().join(mapCodecKeys),
            parametricKeys().join(parametricCodecKeys),
            mapCodecInterpreter().parametricKeys().join(parametricMapCodecKeys)
        ).codecInterpreter();
    }

    public static final Key<Holder.Mu> KEY = Key.create("CodecInterpreter");

    @Override
    public Optional<Key<Holder.Mu>> key() {
        return Optional.of(KEY);
    }

    public static <T> Codec<T> unbox(App<Holder.Mu, T> box) {
        return Holder.unbox(box).codec();
    }

    public <T> DataResult<Codec<T>> interpret(Structure<T> structure) {
        return structure.interpret(this).map(CodecInterpreter::unbox);
    }

    public record Holder<T>(Codec<T> codec) implements App<Holder.Mu, T> {
        public static final class Mu implements K1 { private Mu() {} }

        static <T> Holder<T> unbox(App<Mu, T> box) {
            return (Holder<T>) box;
        }
    }
}
