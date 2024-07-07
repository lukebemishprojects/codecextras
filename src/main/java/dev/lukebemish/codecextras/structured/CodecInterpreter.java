package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.comments.CommentFirstListCodec;
import java.util.List;
import java.util.function.Function;

public class CodecInterpreter extends KeyStoringInterpreter<CodecInterpreter.Holder.Mu> {
    public CodecInterpreter(Keys<Holder.Mu> keys) {
        super(keys.join(Keys.<Holder.Mu>builder()
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
        ));
    }

    public CodecInterpreter() {
        this(Keys.<Holder.Mu>builder().build());
    }

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
    public <A> DataResult<App<Holder.Mu, A>> annotate(App<Holder.Mu, A> input, Annotations annotations) {
        // No annotations handled here
        return DataResult.success(input);
    }

    public static <T> Codec<T> unbox(App<Holder.Mu, T> box) {
        return Holder.unbox(box).codec();
    }

    public <T> DataResult<Codec<T>> interpret(Structure<T> structure) {
        return structure.interpret(this).map(CodecInterpreter::unbox);
    }

    public record Holder<T>(Codec<T> codec) implements App<Holder.Mu, T> {
        public static final class Mu implements K1 {}

        static <T> Holder<T> unbox(App<Mu, T> box) {
            return (Holder<T>) box;
        }
    }
}
