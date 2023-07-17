package dev.lukebemish.codecextras.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.ExtendedRecordCodecBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestExtendedRecords {
    record TestRecord(int a, int b, float c) {
        public static final Codec<TestRecord> CODEC = ExtendedRecordCodecBuilder
                .start(Codec.INT.fieldOf("a"), TestRecord::a)
                .field(Codec.INT.fieldOf("b"), TestRecord::b)
                .field(Codec.FLOAT.fieldOf("c"), TestRecord::c)
                .build(c -> b -> a -> new TestRecord(a, b, c));
    }

    @Test
    void testDecoding() {
        String json = """
                {
                    "a": 1,
                    "b": 2,
                    "c": 3.0
                }""";
        Gson gson = new GsonBuilder().create();
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        DataResult<TestRecord> dataResult = TestRecord.CODEC.parse(JsonOps.INSTANCE, jsonElement);
        Assertions.assertTrue(dataResult.result().isPresent());
        Assertions.assertEquals(dataResult.result().get(), new TestRecord(1, 2, 3.0f));
    }

    @Test
    void testEncoding() {
        TestRecord testRecord = new TestRecord(1, 2, 3.0f);
        String json = """
                {
                    "a": 1,
                    "b": 2,
                    "c": 3.0
                }""";
        Gson gson = new GsonBuilder().create();
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        DataResult<JsonElement> dataResult = TestRecord.CODEC.encodeStart(JsonOps.INSTANCE, testRecord);
        Assertions.assertTrue(dataResult.result().isPresent());
        Assertions.assertEquals(dataResult.result().get(), jsonElement);
    }
}
