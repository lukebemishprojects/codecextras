package dev.lukebemish.codecextras.test.structured;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.schema.JsonSchemaInterpreter;
import dev.lukebemish.codecextras.structured.schema.SchemaAnnotations;
import dev.lukebemish.codecextras.test.CodecAssertions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TestDispatch {
    private interface Dispatches {
        Map<String, Structure<? extends Dispatches>> MAP = new HashMap<>();
        Structure<Dispatches> STRUCTURE = Structure.STRING.<Dispatches>dispatch(
            "type",
            d -> DataResult.success(d.key()),
            MAP::keySet,
            k -> DataResult.success(MAP.get(k))
        ).annotate(SchemaAnnotations.REUSE_KEY, "dispatches");
        String key();
    }

    private record Abc(int a, String b, float c) implements Dispatches {
        private static final Structure<Abc> STRUCTURE = Structure.<Abc>record(i -> {
            var a = i.add("a", Structure.INT, Abc::a);
            var b = i.add("b", Structure.STRING, Abc::b);
            var c = i.add("c", Structure.FLOAT, Abc::c);
            return container -> new Abc(a.apply(container), b.apply(container), c.apply(container));
        }).annotate(SchemaAnnotations.REUSE_KEY, "abc");

        @Override
        public String key() {
            return "abc";
        }
    }

    private record Xyz(String x, int y, float z) implements Dispatches {
        private static final Structure<Xyz> STRUCTURE = Structure.<Xyz>record(i -> {
            var x = i.add("x", Structure.STRING, Xyz::x);
            var y = i.add("y", Structure.INT, Xyz::y);
            var z = i.add("z", Structure.FLOAT, Xyz::z);
            return container -> new Xyz(x.apply(container), y.apply(container), z.apply(container));
        }).annotate(SchemaAnnotations.REUSE_KEY, "xyz");

        @Override
        public String key() {
            return "xyz";
        }
    }

    static {
        Dispatches.MAP.put("abc", Abc.STRUCTURE);
        Dispatches.MAP.put("xyz", Xyz.STRUCTURE);
    }

    private static final Structure<List<Dispatches>> LIST_STRUCTURE = Dispatches.STRUCTURE.listOf();
    private static final Codec<List<Dispatches>> CODEC = CodecInterpreter.create().interpret(LIST_STRUCTURE).getOrThrow();

    private final String json = """
            [
                {
                    "type": "abc",
                    "a": 1,
                    "b": "test",
                    "c": 1.0
                },
                {
                    "type": "xyz",
                    "x": "test",
                    "y": 1,
                    "z": 1.0
                }
            ]""";

    private final String schema = """
            {
                "$ref": "#/$defs/dispatches",
                "$defs": {
                    "dispatches": {
                        "properties": {
                            "type": {
                                "type": "string",
                                "enum":["abc","xyz"]
                            }
                        },
                        "required": [
                            "type"
                        ],
                        "allOf": [
                            {
                                "if": {
                                    "properties": {
                                        "type": {
                                            "const": "abc"
                                        }
                                    }
                                },
                                "then": {
                                    "$ref": "#/$defs/abc"
                                }
                            },
                            {
                                "if": {
                                    "properties": {
                                        "type": {
                                            "const": "xyz"
                                        }
                                    }
                                },
                                "then": {
                                    "$ref": "#/$defs/xyz"
                                }
                            }
                        ]
                    },
                    "xyz": {
                        "type": "object",
                        "properties": {
                            "x": {
                                "type": "string"
                            },
                            "y": {
                                "type": "integer"
                            },
                            "z": {
                                "type": "number"
                            }
                        },
                        "required": [
                            "x",
                            "y",
                            "z"
                        ]
                    },
                    "abc": {
                        "type": "object",
                        "properties": {
                            "a": {
                                "type": "integer"
                            },
                            "b": {
                                "type": "string"
                            },
                            "c": {
                                "type": "number"
                            }
                        },
                        "required": [
                            "a",
                            "b",
                            "c"
                        ]
                    }
                }
            }""";

    private final List<Dispatches> list = List.of(new Abc(1, "test", 1.0f), new Xyz("test", 1, 1.0f));

    @Test
    void testDecoding() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, json, list, CODEC);
    }

    @Test
    void testEncoding() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, list, json, CODEC);
    }

    @Test
    void testJsonSchema() {
        CodecAssertions.assertJsonEquals(schema, new JsonSchemaInterpreter().rootSchema(Dispatches.STRUCTURE).getOrThrow().toString());
    }
}
