package dev.lukebemish.codecextras.test.mutable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.Asymmetry;
import dev.lukebemish.codecextras.mutable.DataElement;
import dev.lukebemish.codecextras.mutable.DataElementType;
import dev.lukebemish.codecextras.test.CodecAssertions;
import java.util.Objects;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestDataElements {
	private static class WithDataElements {
		private static final DataElementType<WithDataElements, String> STRING = DataElementType.create("string", Codec.STRING, d -> d.string);
		private static final DataElementType<WithDataElements, Integer> INTEGER = DataElementType.create("integer", Codec.INT, d -> d.integer);
		private static final Codec<Asymmetry<Consumer<WithDataElements>, WithDataElements>> CODEC = DataElementType.codec(true, STRING, INTEGER);
		private static final Codec<Asymmetry<Consumer<WithDataElements>, WithDataElements>> CHANGED_CODEC = DataElementType.codec(false, STRING, INTEGER);

		private final DataElement<String> string = new DataElement.Simple<>("");
		private final DataElement<Integer> integer = new DataElement.Simple<>(0);

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof WithDataElements that)) return false;
			return Objects.equals(string, that.string) && Objects.equals(integer, that.integer);
		}

		@Override
		public int hashCode() {
			return Objects.hash(string, integer);
		}
	}

	private final String both = """
			{
				"string": "Hello",
				"integer": 42
			}""";

	private final String onlyChanged = """
			{
				"integer": 42
			}""";

	@Test
	void testEncodeMutable() {
		WithDataElements data = new WithDataElements();
		data.string.set("Hello");
		data.integer.set(42);
		CodecAssertions.assertEncodes(JsonOps.INSTANCE, Asymmetry.ofEncoding(data), both, WithDataElements.CODEC);
	}

	@Test
	void testEncodeMutableOnlyDirty() {
		WithDataElements data = new WithDataElements();
		data.string.set("Hello");
		data.string.setDirty(false);
		data.integer.set(42);
		CodecAssertions.assertEncodes(JsonOps.INSTANCE, Asymmetry.ofEncoding(data), onlyChanged, WithDataElements.CHANGED_CODEC);
	}

	@Test
	void testDecodeMutable() {
		Gson gson = new GsonBuilder().create();
		JsonElement jsonElement = gson.fromJson(both, JsonElement.class);
		Consumer<WithDataElements> decoded = WithDataElements.CODEC.parse(JsonOps.INSTANCE, jsonElement).flatMap(Asymmetry::decoding).result().orElseThrow();
		WithDataElements data = new WithDataElements();
		decoded.accept(data);
		WithDataElements original = new WithDataElements();
		original.string.set("Hello");
		original.integer.set(42);
		Assertions.assertEquals(original, data);
	}

	@Test
	void testDecodeMutableOnlyDirty() {
		Gson gson = new GsonBuilder().create();
		JsonElement jsonElement = gson.fromJson(onlyChanged, JsonElement.class);
		Consumer<WithDataElements> decoded = WithDataElements.CHANGED_CODEC.parse(JsonOps.INSTANCE, jsonElement).flatMap(Asymmetry::decoding).result().orElseThrow();
		WithDataElements data = new WithDataElements();
		data.string.set("Hello");
		decoded.accept(data);
		WithDataElements original = new WithDataElements();
		original.string.set("Hello");
		original.integer.set(42);
		Assertions.assertEquals(original, data);
	}
}
