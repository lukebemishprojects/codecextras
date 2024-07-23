package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.lukebemish.codecextras.comments.CommentMapCodec;
import dev.lukebemish.codecextras.types.Identity;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class MapCodecInterpreter extends KeyStoringInterpreter<MapCodecInterpreter.Holder.Mu> {
    private final CodecInterpreter codecInterpreter;

    public MapCodecInterpreter(
        Keys<Holder.Mu, Object> keys,
        Keys<CodecInterpreter.Holder.Mu, Object> codecKeys,
        Keys2<ParametricKeyedValue.Mu<Holder.Mu>, K1, K1> parametricKeys,
        Keys2<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1> parametricCodecKeys
    ) {
        super(keys, parametricKeys);
        this.codecInterpreter = new CodecInterpreter(keys.<CodecInterpreter.Holder.Mu>map(new Keys.Converter<>() {
            @Override
            public <B> App<CodecInterpreter.Holder.Mu, B> convert(App<Holder.Mu, B> app) {
                return new CodecInterpreter.Holder<>(unbox(app).codec());
            }
        }).join(codecKeys), parametricKeys.map(new Keys2.Converter<ParametricKeyedValue.Mu<Holder.Mu>, ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1>() {
            @Override
            public <A extends K1, B extends K1> App2<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, A, B> convert(App2<ParametricKeyedValue.Mu<Holder.Mu>, A, B> input) {
                var unboxed = ParametricKeyedValue.unbox(input);
                return new ParametricKeyedValue<>() {
                    @Override
                    public <T> App<CodecInterpreter.Holder.Mu, App<B, T>> convert(App<A, T> parameter) {
                        var mapCodec = unbox(unboxed.convert(parameter));
                        return new CodecInterpreter.Holder<>(mapCodec.codec());
                    }
                };
            }
        }).join(parametricCodecKeys));
    }

    public MapCodecInterpreter() {
        this(
            Keys.<Holder.Mu, Object>builder().build(),
            Keys.<CodecInterpreter.Holder.Mu, Object>builder().build(),
            Keys2.<ParametricKeyedValue.Mu<Holder.Mu>, K1, K1>builder().build(),
            Keys2.<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1>builder().build()
        );
    }

    @Override
    public <A> DataResult<App<Holder.Mu, List<A>>> list(App<Holder.Mu, A> single) {
        return DataResult.error(() -> "Cannot make a MapCodec for a list");
    }

    @Override
    public <A> DataResult<App<Holder.Mu, A>> record(List<RecordStructure.Field<A, ?>> fields, Function<RecordStructure.Container, A> creator) {
        return StructuredMapCodec.of(fields, creator, codecInterpreter, CodecInterpreter::unbox)
            .map(Holder::new);
    }

    @Override
    public <A, B> DataResult<App<Holder.Mu, B>> flatXmap(App<Holder.Mu, A> input, Function<A, DataResult<B>> deserializer, Function<B, DataResult<A>> serializer) {
        var mapCodec = unbox(input);
        return DataResult.success(new Holder<>(mapCodec.flatXmap(deserializer, serializer)));
    }

    @Override
    public <A> DataResult<App<Holder.Mu, A>> annotate(App<Holder.Mu, A> input, Keys<Identity.Mu, Object> annotations) {
        var mapCodec = new Object() {
            MapCodec<A> m = unbox(input);
        };
        mapCodec.m = Annotation.get(annotations, Annotation.COMMENT).map(comment -> CommentMapCodec.of(mapCodec.m, comment)).orElse(mapCodec.m);
        return DataResult.success(new Holder<>(mapCodec.m));
    }

    public static <T> MapCodec<T> unbox(App<Holder.Mu, T> box) {
        return Holder.unbox(box).mapCodec();
    }

    public <T> DataResult<MapCodec<T>> interpret(Structure<T> structure) {
        return structure.interpret(this).map(MapCodecInterpreter::unbox);
    }

    public record Holder<T>(MapCodec<T> mapCodec) implements App<Holder.Mu, T> {
        public static final class Mu implements K1 { private Mu() {} }

        static <T> Holder<T> unbox(App<Holder.Mu, T> box) {
            return (Holder<T>) box;
        }
    }

    public static final Key<Holder.Mu> KEY = Key.create("MapCodecInterpreter");

    @Override
    public Optional<Key<Holder.Mu>> key() {
        return Optional.of(KEY);
    }
}
