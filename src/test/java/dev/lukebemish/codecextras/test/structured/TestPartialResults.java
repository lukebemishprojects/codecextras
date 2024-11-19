package dev.lukebemish.codecextras.test.structured;

import static dev.lukebemish.codecextras.test.CodecAssertions.*;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Structure;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class TestPartialResults {
    @Test
    void testPartialResults() {
        final var codec = RecordCodecBuilder.<List<String>>create(
            instance -> instance.group(
                Codec.STRING.listOf().fieldOf("example").forGetter(Function.identity())
            ).apply(instance, Function.identity())
        );

        final var structure = Structure.<List<String>>record(i ->
            i.add("example", Structure.STRING.listOf(),Function.identity())
        );

        final var json = """
            {
                "example": [
                    "abc",
                    123,
                    "def"
                ]
            }""";
        final var expected = List.of("abc", "def");

        assertDecodesOrPartial(JsonOps.INSTANCE, json, expected, codec);
        assertDecodesOrPartial(JsonOps.INSTANCE, json, expected, CodecInterpreter.create()
            .interpret(structure)
            .getOrThrow()
        );
    }
}
