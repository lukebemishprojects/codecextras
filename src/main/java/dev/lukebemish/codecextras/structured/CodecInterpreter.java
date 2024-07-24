package dev.lukebemish.codecextras.structured;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import dev.lukebemish.codecextras.comments.CommentFirstListCodec;
import dev.lukebemish.codecextras.types.Identity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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
        ), parametricKeys);
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
    public <A> DataResult<App<Holder.Mu, A>> annotate(App<Holder.Mu, A> input, Keys<Identity.Mu, Object> annotations) {
        // No annotations handled here
        return DataResult.success(input);
    }

    @Override
    public <A> DataResult<App<Holder.Mu, A>> lazy(Structure<A> structure) {
        var supplier = Suppliers.memoize(() -> structure.interpret(this));
        return DataResult.success(new Holder<>(new Codec<A>() {
            @Override
            public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
                return supplier.get().map(CodecInterpreter::unbox).flatMap(c -> c.decode(ops, input));
            }

            @Override
            public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
                return supplier.get().map(CodecInterpreter::unbox).flatMap(c -> c.encode(input, ops, prefix));
            }
        }));
    }

    @Override
    public <E, A> DataResult<App<Holder.Mu, E>> dispatch(String key, Structure<A> keyStructure, Function<? super E, ? extends A> function, Map<? super A, ? extends Structure<? extends E>> structures) {
        return keyStructure.interpret(this).flatMap(keyCodecApp -> {
            var keyCodec = unbox(keyCodecApp);
            // Object here as it's the furthest super A and we have only ? super A
            Map<Object, MapCodec<? extends E>> codecMap = new HashMap<>();
            for (var entry : structures.entrySet()) {
                var result = entry.getValue().interpret(mapCodecInterpreter());
                if (result.error().isPresent()) {
                    return DataResult.error(result.error().get().messageSupplier());
                }
                codecMap.put(entry.getKey(), MapCodecInterpreter.unbox(result.result().orElseThrow()));
            }
            return DataResult.success(new Holder<>(keyCodec.dispatch(key, function, codecMap::get)));
        });
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
