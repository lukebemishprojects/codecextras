package dev.lukebemish.codecextras.test.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Key2;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.Keys2;
import dev.lukebemish.codecextras.structured.ParametricKeyedValue;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.test.CodecAssertions;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class TestParametricKeys {
    private record WithType<T>(String string) implements App<WithType.Mu, T> {
        public static class Mu implements K1 {}

        private static <T> WithType<T> unbox(App<Mu, T> box) {
            return (WithType<T>) box;
        }
    }

    private record Prefix<T>(String string) implements App<Prefix.Mu, T> {
        public static class Mu implements K1 {}

        private static  <T> Prefix<T> unbox(App<Mu, T> box) {
            return (Prefix<T>) box;
        }
    }

    private static <T> Codec<WithType<T>> withTypeCodec(Prefix<T> prefix) {
        return Codec.STRING.comapFlatMap(
            s -> s.startsWith(prefix.string()) ?
                DataResult.success(new WithType<>(s.substring(prefix.string().length()))) :
                DataResult.error(() -> "Provided string \""+s+"\" does not start with prefix \""+prefix.string()+"\""),
            w -> prefix.string()+w.string()
        );
    }

    private static final Key2<Prefix.Mu, WithType.Mu> WITH_TYPE = Key2.create("with_type");

    private static final Structure<WithType<Integer>> STRUCTURE = Structure.parametricallyKeyed(
        WITH_TYPE,
        new Prefix<>("prefix:"),
        WithType::unbox
    );

    private static final Codec<WithType<Integer>> CODEC = new CodecInterpreter(
        Keys.<CodecInterpreter.Holder.Mu, Object>builder().build(),
        Keys2.<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1>builder()
            .add(WITH_TYPE, new ParametricKeyedValue<>(new ParametricKeyedValue.Converter<>() {
                @Override
                public <T> App<CodecInterpreter.Holder.Mu, App<WithType.Mu, T>> convert(App<Prefix.Mu, T> parameter) {
                    return new CodecInterpreter.Holder<>(withTypeCodec(Prefix.unbox(parameter)).xmap(Function.identity(), WithType::unbox));
                }
            }))
            .build()
    ).interpret(STRUCTURE).getOrThrow();

    private final String json = "\"prefix:123\"";

    private final WithType<Integer> data = new WithType<>("123");

    @Test
    void testEncodingCodec() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, data, json, CODEC);
    }

    @Test
    void testDecodingCodec() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, json, data, CODEC);
    }
}
