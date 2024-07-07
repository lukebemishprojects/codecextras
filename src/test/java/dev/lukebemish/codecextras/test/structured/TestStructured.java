package dev.lukebemish.codecextras.test.structured;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.structured.Annotations;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.JsonSchemaInterpreter;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.test.CodecAssertions;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TestStructured {
    private record TestRecord(int a, String b, List<Boolean> c, Optional<String> d) {
        private static final Structure<TestRecord> STRUCTURE = Structure.record(i -> {
            var a = i.add("a", Structure.INT.annotate(Annotations.COMMENT, "Field A"), TestRecord::a);
            var b = i.add(Structure.STRING.fieldOf("b"), TestRecord::b);
            var c = i.add("c", Structure.BOOL.listOf(), TestRecord::c);
            var d = i.add(Structure.STRING.optionalFieldOf("d"), TestRecord::d);
            return container -> new TestRecord(a.apply(container), b.apply(container), c.apply(container), d.apply(container));
        });

        private static final Codec<TestRecord> CODEC = new CodecInterpreter().interpret(STRUCTURE).getOrThrow();
    }

    private final String json = """
            {
                "a": 1,
                "b": "test",
                "c": [true, false, true]
            }""";

    private final String schema = """
            {
                "type": "object",
                "properties": {
                    "a": {
                        "type": "integer",
                        "description": "Field A"
                    },
                    "b": {
                        "type": "string"
                    },
                    "c": {
                        "type": "array",
                        "items": {
                            "type": "boolean"
                        }
                    },
                    "d": {
                        "type": "string"
                    }
                },
                "required": ["a", "b", "c"]
            }""";

    private final TestRecord record = new TestRecord(1, "test", List.of(true, false, true), Optional.empty());

    @Test
    void testDecodingCodec() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, json, record, TestRecord.CODEC);
    }

    @Test
    void testEncodingCodec() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, record, json, TestRecord.CODEC);
    }

    @Test
    void testJsonSchema() {
        CodecAssertions.assertJsonEquals(schema, new JsonSchemaInterpreter().interpret(TestRecord.STRUCTURE).getOrThrow().toString());
    }
}
