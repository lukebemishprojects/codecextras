package dev.lukebemish.codecextras.test.polymorphic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestPolymorphicCodecs {

    @Test
    void testDecoding() {
        String json = """
            {
                "address": "123 Fake Street",
                "height": 180,
                "name": "John",
                "age": 21
            }""";
        Gson gson = new GsonBuilder().create();
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        DataResult<SubClass> dataResult = SubClass.CODEC.parse(JsonOps.INSTANCE, jsonElement);
        Assertions.assertTrue(dataResult.result().isPresent());
        SubClass subClass = dataResult.result().get();
        Assertions.assertEquals(subClass.address(), "123 Fake Street");
        Assertions.assertEquals(subClass.height(), 180);
        Assertions.assertEquals(subClass.name(), "John");
        Assertions.assertEquals(subClass.age(), 21);
    }

    @Test
    void testEncoding() {
        SubClass subClass = new SubClass.Builder()
            .address("123 Fake Street")
            .height(180)
            .superClass(new SuperClass.Builder()
                .name("John")
                .age(21)
            )
            .buildUnsafe();
        String json = """
            {
                "name": "John",
                "age": 21,
                "address": "123 Fake Street",
                "height": 180
            }""";
        Gson gson = new GsonBuilder().create();
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        DataResult<JsonElement> dataResult = SubClass.CODEC.encodeStart(JsonOps.INSTANCE, subClass);
        Assertions.assertTrue(dataResult.result().isPresent());
        Assertions.assertEquals(dataResult.result().get(), jsonElement);
    }
}
