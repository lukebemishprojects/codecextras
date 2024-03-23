package dev.lukebemish.codecextras.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.repair.FillMissingLogOps;
import dev.lukebemish.codecextras.repair.FillMissingMapCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class TestLogMissing {
    private record TestRecord(int a, int b, float c) {
        public static final Codec<TestRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            FillMissingMapCodec.fieldOf(Codec.INT, "a", 1).forGetter(TestRecord::a),
            FillMissingMapCodec.fieldOf(Codec.INT, "b", 2).forGetter(TestRecord::b),
            FillMissingMapCodec.fieldOf(Codec.FLOAT, "c", 3.0f).forGetter(TestRecord::c)
        ).apply(i, TestRecord::new));
    }

    @Test
    void testDecoding() {
        String json = """
            {
                "a": "abc",
                "b": 5
            }""";
        Map<String, JsonElement> logged = new HashMap<>();
        DynamicOps<JsonElement> ops = FillMissingLogOps.of(logged::put, JsonOps.INSTANCE);
        Map<String, JsonElement> expected = Map.of(
            "a", new JsonPrimitive("abc"),
            "c", JsonOps.INSTANCE.empty()
        );
        CodecAssertions.assertDecodes(ops, json, new TestRecord(1, 5, 3.0f), TestRecord.CODEC);
        Assertions.assertEquals(expected, logged);
    }

    @Test
    void testEncoding() {
        // To make sure that encoding is unaffected
        String json = """
            {
                "a": 1,
                "b": 5,
                "c": 3.0
            }""";
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, new TestRecord(1, 5, 3.0f), json, TestRecord.CODEC);
    }
}
