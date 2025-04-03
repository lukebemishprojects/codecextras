package dev.lukebemish.codecextras.test.structured.reflective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import dev.lukebemish.codecextras.structured.reflective.annotations.SerializedProperty;
import dev.lukebemish.codecextras.test.CodecAssertions;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TestGenericRecord {
    public record GenericRecord<T>(T value) {}
    public record TestRecord(GenericRecord<List<String>> generic) {
        private static final Structure<TestRecord> STRUCTURE = ReflectiveStructureCreator.create(TestRecord.class);
    }
    public static class TestGenericArray {
        private final GenericRecord<String>[][][] array;

        private static final Structure<TestGenericArray> STRUCTURE = ReflectiveStructureCreator.create(TestGenericArray.class);

        public TestGenericArray(@SerializedProperty("array") GenericRecord<String>[][][] array) {
            this.array = array;
        }

        public GenericRecord<String>[][][] getArray() {
            return array;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TestGenericArray that)) return false;
            return Arrays.deepEquals(array, that.array);
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(array);
        }
    }

    private static final Codec<TestRecord> CODEC = CodecInterpreter.create().interpret(TestRecord.STRUCTURE).getOrThrow();

    private final String json = """
            {
                "generic": {
                    "value": ["a", "b", "c"]
                }
            }""";

    private final TestRecord object = new TestRecord(new GenericRecord<>(List.of("a", "b", "c")));

    private final String genericArrayJson = """
            {
                "array": [[[
                    {
                        "value": "string"
                    }
                ]]]
            }""";

    @SuppressWarnings("unchecked")
    private final TestGenericArray genericArrayObject = new TestGenericArray(new GenericRecord[][][]{new GenericRecord[][]{new GenericRecord[]{
        new GenericRecord<>("string")
    }}});

    private static final Codec<TestGenericArray> GENERIC_ARRAY_CODEC = CodecInterpreter.create().interpret(TestGenericArray.STRUCTURE).getOrThrow();

    @Test
    void testDecoding() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, json, object, CODEC);
    }

    @Test
    void testEncoding() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, object, json, CODEC);
    }

    @Test
    void testGenericArrayDecoding() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, genericArrayJson, genericArrayObject, GENERIC_ARRAY_CODEC);
    }

    @Test
    void testGenericArrayEncoding() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, genericArrayObject, genericArrayJson, GENERIC_ARRAY_CODEC);
    }
}
