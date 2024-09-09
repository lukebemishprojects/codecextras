package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.KeyDispatchCodec;
import dev.lukebemish.codecextras.comments.CommentMapCodec;
import dev.lukebemish.codecextras.types.Identity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class MapCodecInterpreter extends KeyStoringInterpreter<MapCodecInterpreter.Holder.Mu, MapCodecInterpreter> {
    public MapCodecInterpreter(
        Keys<Holder.Mu, Object> keys,
        Keys2<ParametricKeyedValue.Mu<Holder.Mu>, K1, K1> parametricKeys
    ) {
        super(keys, parametricKeys);
    }

    public static MapCodecInterpreter create(
        Keys<CodecInterpreter.Holder.Mu, Object> codecKeys,
        Keys<MapCodecInterpreter.Holder.Mu, Object> mapCodecKeys,
        Keys2<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1> parametricCodecKeys,
        Keys2<ParametricKeyedValue.Mu<MapCodecInterpreter.Holder.Mu>, K1, K1> parametricMapCodecKeys
    ) {
        return new CodecAndMapInterpreters(codecKeys, mapCodecKeys, parametricCodecKeys, parametricMapCodecKeys).mapCodecInterpreter();
    }

    public static MapCodecInterpreter create() {
        return new CodecAndMapInterpreters().mapCodecInterpreter();
    }

    protected abstract CodecInterpreter codecInterpreter();

    @Override
    public <A> DataResult<App<Holder.Mu, List<A>>> list(App<Holder.Mu, A> single) {
        return DataResult.error(() -> "Cannot make a MapCodec for a list");
    }

    @Override
    public <A> DataResult<App<Holder.Mu, A>> record(List<RecordStructure.Field<A, ?>> fields, Function<RecordStructure.Container, A> creator) {
        return StructuredMapCodec.of(fields, creator, codecInterpreter(), CodecInterpreter::unbox)
            .map(Holder::new);
    }

    @Override
    public <A, B> DataResult<App<Holder.Mu, B>> flatXmap(App<Holder.Mu, A> input, Function<A, DataResult<B>> to, Function<B, DataResult<A>> from) {
        var mapCodec = unbox(input);
        return DataResult.success(new Holder<>(mapCodec.flatXmap(to, from)));
    }

    @Override
    public <A> DataResult<App<Holder.Mu, A>> annotate(Structure<A> original, Keys<Identity.Mu, Object> annotations) {
        return original.interpret(this).map(input -> {
            var mapCodec = new Object() {
                MapCodec<A> m = unbox(input);
            };
            mapCodec.m = Annotation.get(annotations, Annotation.COMMENT).map(comment -> CommentMapCodec.of(mapCodec.m, comment)).orElse(mapCodec.m);
            return new Holder<>(mapCodec.m);
        });
    }

    public static <T> MapCodec<T> unbox(App<Holder.Mu, T> box) {
        return Holder.unbox(box).mapCodec();
    }

    public <T> DataResult<MapCodec<T>> interpret(Structure<T> structure) {
        return structure.interpret(this).map(MapCodecInterpreter::unbox);
    }

    @Override
    public <E, A> DataResult<App<Holder.Mu, E>> dispatch(String key, Structure<A> keyStructure, Function<? super E, ? extends DataResult<A>> function, Supplier<Set<A>> keys, Function<A, DataResult<Structure<? extends E>>> structures) {
        return keyStructure.interpret(codecInterpreter()).flatMap(keyCodecApp -> {
            var keyCodec = CodecInterpreter.unbox(keyCodecApp);
            var map = new ConcurrentHashMap<A, DataResult<MapCodec<? extends E>>>();
            Function<A, DataResult<MapCodec<? extends E>>> cache = k -> map.computeIfAbsent(k , structures.andThen(result -> result.flatMap(s -> s.interpret(this)).map(MapCodecInterpreter::unbox)));
            return DataResult.success(new MapCodecInterpreter.Holder<>(new KeyDispatchCodec<>(key, keyCodec, function, cache)));
        });
    }

    @Override
    public <K, V> DataResult<App<Holder.Mu, Map<K, V>>> dispatchedMap(Structure<K> keyStructure, Supplier<Set<K>> keys, Function<K, DataResult<Structure<? extends V>>> valueStructures) {
        return DataResult.error(() -> "Cannot make a MapCodec for a dispatched map");
    }

    @Override
    public <K, V> DataResult<App<Holder.Mu, Map<K, V>>> unboundedMap(App<Holder.Mu, K> key, App<Holder.Mu, V> value) {
        return DataResult.error(() -> "Cannot make a MapCodec for an unbounded map");
    }

    @Override
    public <L, R> DataResult<App<Holder.Mu, Either<L, R>>> either(App<Holder.Mu, L> left, App<Holder.Mu, R> right) {
        var leftCodec = unbox(left);
        var rightCodec = unbox(right);
        return DataResult.success(new Holder<>(Codec.mapEither(leftCodec, rightCodec)));
    }

    @Override
    public MapCodecInterpreter with(Keys<Holder.Mu, Object> keys, Keys2<ParametricKeyedValue.Mu<Holder.Mu>, K1, K1> parametricKeys) {
        return new CodecAndMapInterpreters(codecInterpreter().keys(), keys().join(keys), codecInterpreter().parametricKeys(), parametricKeys().join(parametricKeys)).mapCodecInterpreter();
    }

    public MapCodecInterpreter with(
        Keys<CodecInterpreter.Holder.Mu, Object> codecKeys,
        Keys<MapCodecInterpreter.Holder.Mu, Object> mapCodecKeys,
        Keys2<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1> parametricCodecKeys,
        Keys2<ParametricKeyedValue.Mu<MapCodecInterpreter.Holder.Mu>, K1, K1> parametricMapCodecKeys
    ) {
        return new CodecAndMapInterpreters(
            codecInterpreter().keys().join(codecKeys),
            keys().join(mapCodecKeys),
            codecInterpreter().parametricKeys().join(parametricCodecKeys),
            parametricKeys().join(parametricMapCodecKeys)
        ).mapCodecInterpreter();
    }

    public record Holder<T>(MapCodec<T> mapCodec) implements App<Holder.Mu, T> {
        public static final class Mu implements K1 { private Mu() {} }

        static <T> Holder<T> unbox(App<Holder.Mu, T> box) {
            return (Holder<T>) box;
        }
    }

    public static final Key<Holder.Mu> KEY = Key.create("MapCodecInterpreter");

    @Override
    public Stream<KeyConsumer<?, Holder.Mu>> keyConsumers() {
        return Stream.of(
            new KeyConsumer<Holder.Mu, Holder.Mu>() {
                @Override
                public Key<Holder.Mu> key() {
                    return KEY;
                }

                @Override
                public <T> App<Holder.Mu, T> convert(App<Holder.Mu, T> input) {
                    return input;
                }
            }
        );
    }
}
