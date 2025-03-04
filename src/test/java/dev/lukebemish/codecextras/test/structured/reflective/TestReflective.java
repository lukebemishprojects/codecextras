package dev.lukebemish.codecextras.test.structured.reflective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import dev.lukebemish.codecextras.test.CodecAssertions;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SequencedSet;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

public class TestReflective {
    public record TestRecord(int a, String b, TestEnum c, OptionalInt d, Optional<String> e, @Nullable String f, SequencedSet<Integer> g) {
        private static final Structure<TestRecord> STRUCTURE = ReflectiveStructureCreator.create(TestRecord.class);
    }

    public enum TestEnum {
        A,
        B,
        C
    }

    private static final Codec<TestRecord> CODEC = CodecInterpreter.create().interpret(TestRecord.STRUCTURE).getOrThrow();

    private static final Codec<TestRecord[]> ARRAY_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestRecord[].class)).getOrThrow();
    private static final Codec<int[]> PRIMITIVE_ARRAY_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(int[].class)).getOrThrow();

    private final String json = """
            {
                "a": 1,
                "b": "test",
                "c": "A",
                "d": 2,
                "e": "test",
                "g": [1, 2, 3]
            }""";

    private final String arrayJson = """
            [
                {
                    "a": 1,
                    "b": "test",
                    "c": "A",
                    "d": 2,
                    "e": "test",
                    "g": [1, 2, 3]
                }
            ]""";

    private final String primitiveArrayJson = """
            [1, 2, 3]""";

    private final TestRecord object = new TestRecord(1, "test", TestEnum.A, OptionalInt.of(2), Optional.of("test"), null, new LinkedHashSet<>(List.of(1, 2, 3)));
    private final TestRecord[] array = new TestRecord[] { object };
    private final int[] primitiveArray = new int[] { 1, 2, 3 };

    @Test
    void testDecoding() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, json, object, CODEC);
    }

    @Test
    void testEncoding() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, object, json, CODEC);
    }

    @Test
    void testDecodingArray() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, arrayJson, array, ARRAY_CODEC);
    }

    @Test
    void testEncodingArray() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, array, arrayJson, ARRAY_CODEC);
    }

    @Test
    void testDecodingPrimitiveArray() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, primitiveArrayJson, primitiveArray, PRIMITIVE_ARRAY_CODEC);
    }

    @Test
    void testEncodingPrimitiveArray() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, primitiveArray, primitiveArrayJson, PRIMITIVE_ARRAY_CODEC);
    }
}
