package dev.lukebemish.codecextras.test.mutalbe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.Asymmetry;
import dev.lukebemish.codecextras.mutable.DataElement;
import dev.lukebemish.codecextras.mutable.DataElementType;
import dev.lukebemish.codecextras.test.CodecAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

class TestDataElements {
	private static class WithDataElements {
		private static final DataElementType<WithDataElements, String> STRING = DataElementType.defaulted(Codec.STRING, "", d -> d.string);
		private static final DataElementType<WithDataElements, Integer> INTEGER = DataElementType.defaulted(Codec.INT, 0, d -> d.integer);

		private static final List<DataElementType<WithDataElements, ?>> ELEMENTS = List.of(STRING, INTEGER);
		private static final Codec<Asymmetry<Consumer<WithDataElements>, WithDataElements>> CODEC = DataElementType.codec(ELEMENTS, true);
		private static final Codec<Asymmetry<Consumer<WithDataElements>, WithDataElements>> CHANGED_CODEC = DataElementType.codec(ELEMENTS, false);

		private final DataElement<String> string = STRING.create();
		private final DataElement<Integer> integer = INTEGER.create();

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
			[
				{
					"index": 0,
					"value": "Hello"
				},
				{
					"index": 1,
					"value": 42
				}
			]""";

	private final String onlyChanged = """
			[
				{
					"index": 1,
					"value": 42
				}
			]""";

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
