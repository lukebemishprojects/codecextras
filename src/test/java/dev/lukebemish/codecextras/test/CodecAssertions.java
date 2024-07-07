package dev.lukebemish.codecextras.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import org.junit.jupiter.api.Assertions;

public final class CodecAssertions {
    private CodecAssertions() {}

    public static <O> void assertDecodes(DynamicOps<JsonElement> jsonOps, String json, O expected, Codec<O> codec) {
        Gson gson = new GsonBuilder().create();
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        assertDecodes(jsonOps, jsonElement, expected, codec);
    }

    public static <O, T> void assertDecodes(DynamicOps<T> ops, T data, O expected, Codec<O> codec) {
        DataResult<O> dataResult = codec.parse(ops, data);
        Assertions.assertTrue(dataResult.result().isPresent(), () -> dataResult.error().orElseThrow().message());
        Assertions.assertEquals(expected, dataResult.result().get());
    }

    public static <O> void assertEncodes(DynamicOps<JsonElement> jsonOps, O value, String json, Codec<O> codec) {
        Gson gson = new GsonBuilder().create();
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        assertEncodes(jsonOps, value, jsonElement, codec);
    }

    public static <O, T> void assertEncodes(DynamicOps<T> ops, O value, T expected, Codec<O> codec) {
        DataResult<T> dataResult = codec.encodeStart(ops, value);
        Assertions.assertTrue(dataResult.result().isPresent(), () -> dataResult.error().orElseThrow().message());
        Assertions.assertEquals(expected, dataResult.result().get());
    }

    public static void assertJsonEquals(String expected, String actual) {
        Gson gson = new GsonBuilder().create();
        JsonElement expectedElement = gson.fromJson(expected, JsonElement.class);
        JsonElement actualElement = gson.fromJson(actual, JsonElement.class);
        Assertions.assertEquals(expectedElement, actualElement);
    }
}
