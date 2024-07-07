package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.lukebemish.codecextras.comments.CommentMapCodec;
import java.util.List;
import java.util.function.Function;

public class MapCodecInterpreter extends KeyStoringInterpreter<MapCodecInterpreter.Holder.Mu> {
    private final CodecInterpreter codecInterpreter;

    public MapCodecInterpreter(Keys<Holder.Mu> keys, Keys<CodecInterpreter.Holder.Mu> codecKeys) {
        super(keys);
        this.codecInterpreter = new CodecInterpreter(codecKeys.join(keys.map(new Keys.Converter<>() {
            @Override
            public <B> App<CodecInterpreter.Holder.Mu, B> convert(App<Holder.Mu, B> app) {
                return new CodecInterpreter.Holder<>(unbox(app).codec());
            }
        })));
    }

    public MapCodecInterpreter() {
        this(Keys.<Holder.Mu>builder().build(), Keys.<CodecInterpreter.Holder.Mu>builder().build());
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
    public <A> DataResult<App<Holder.Mu, A>> annotate(App<Holder.Mu, A> input, Annotations annotations) {
        var mapCodec = new Object() {
            MapCodec<A> m = unbox(input);
        };
        mapCodec.m = annotations.get(Annotations.COMMENT).map(comment -> CommentMapCodec.of(mapCodec.m, comment)).orElse(mapCodec.m);
        return DataResult.success(new Holder<>(mapCodec.m));
    }

    public static <T> MapCodec<T> unbox(App<Holder.Mu, T> box) {
        return Holder.unbox(box).mapCodec();
    }

    public <T> DataResult<MapCodec<T>> interpret(Structure<T> structure) {
        return structure.interpret(this).map(MapCodecInterpreter::unbox);
    }

    public record Holder<T>(MapCodec<T> mapCodec) implements App<Holder.Mu, T> {
        public static final class Mu implements K1 {}

        static <T> Holder<T> unbox(App<Holder.Mu, T> box) {
            return (Holder<T>) box;
        }
    }
}
