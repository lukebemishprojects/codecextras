package dev.lukebemish.codecextras.test.structured;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.structured.Annotation;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.schema.JsonSchemaInterpreter;
import dev.lukebemish.codecextras.test.CodecAssertions;
import dev.lukebemish.codecextras.types.Identity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TestStructured {
    private record TestRecord(int a, String b, List<Boolean> c, Optional<String> d, Identity<String> e) {
        private static final Structure<TestRecord> STRUCTURE = Structure.record(i -> {
            var a = i.add("a", Structure.INT.annotate(Annotation.COMMENT, "Field A"), TestRecord::a);
            var b = i.add(Structure.STRING.fieldOf("b"), TestRecord::b);
            var c = i.add("c", Structure.BOOL.listOf(), TestRecord::c);
            var d = i.add(Structure.STRING.optionalFieldOf("d"), TestRecord::d);
            var e = i.addOptional("e", Structure.STRING.annotate(Annotation.PATTERN, "^[a-z]+$").xmap(Identity::new, Identity::value), TestRecord::e, () -> new Identity<>("default"));
            return container -> new TestRecord(a.apply(container), b.apply(container), c.apply(container), d.apply(container), e.apply(container));
        });

        private static final Codec<TestRecord> CODEC = CodecInterpreter.create().interpret(STRUCTURE).getOrThrow();
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
                    },
                    "e": {
                        "type": "string",
                        "default": "default",
                        "pattern": "^[a-z]+$"
                    }
                },
                "required": ["a", "b", "c"]
            }""";

    private final TestRecord record = new TestRecord(1, "test", List.of(true, false, true), Optional.empty(), new Identity<>("default"));

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
        CodecAssertions.assertJsonEquals(schema, new JsonSchemaInterpreter().rootSchema(TestRecord.STRUCTURE).getOrThrow().toString());
    }
}
