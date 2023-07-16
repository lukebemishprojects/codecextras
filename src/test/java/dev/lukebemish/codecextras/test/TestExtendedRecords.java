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
    record TestBuilder(int a, int b, float c) {
        public static final Codec<TestBuilder> CODEC = ExtendedRecordCodecBuilder
                .start(Codec.INT.fieldOf("a"), TestBuilder::a)
                .field(Codec.INT.fieldOf("b"), TestBuilder::b)
                .field(Codec.FLOAT.fieldOf("c"), TestBuilder::c)
                .build(c -> b -> a -> new TestBuilder(a, b, c));
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
        DataResult<TestBuilder> dataResult = TestBuilder.CODEC.parse(JsonOps.INSTANCE, jsonElement);
        Assertions.assertEquals(dataResult.result().get(), new TestBuilder(1, 2, 3.0f));
    }

    @Test
    void testEncoding() {
        TestBuilder testBuilder = new TestBuilder(1, 2, 3.0f);
        String json = """
                {
                    "a": 1,
                    "b": 2,
                    "c": 3.0
                }""";
        Gson gson = new GsonBuilder().create();
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        DataResult<JsonElement> dataResult = TestBuilder.CODEC.encodeStart(JsonOps.INSTANCE, testBuilder);
        Assertions.assertEquals(dataResult.result().get(), jsonElement);
    }
}
