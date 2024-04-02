package dev.lukebemish.codecextras.test.polymorphic;

import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.polymorphic.BuilderException;
import dev.lukebemish.codecextras.test.CodecAssertions;
import org.junit.jupiter.api.Test;

class TestPolymorphicCodecs {

	@Test
	void testDecodingBuilder() {
		String json = """
			{
				"address": "123 Fake Street",
				"height": 180,
				"name": "John",
				"age": 21
			}""";
		CodecAssertions.assertDecodes(JsonOps.INSTANCE, json,
			new SubClass.Builder()
				.address("123 Fake Street")
				.height(180)
				.superClass(new SuperClass.Builder()
					.name("John")
					.age(21)
				)
				.buildUnsafe(),
			SubClass.CODEC
		);
	}

	@Test
	void testEncodingBuilder() {
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
		CodecAssertions.assertEncodes(JsonOps.INSTANCE, subClass, json, SubClass.CODEC);
	}

	@Test
	void testDecodingPolymorphicBuilder() {
		String json = """
			{
				"address": "123 Fake Street",
				"height": 180,
				"name": "John",
				"age": 21
			}""";
		CodecAssertions.assertDecodes(JsonOps.INSTANCE, json,
			new PolymorphicSubClass.Builder.Impl()
				.name("John")
				.age(21)
				.height(180)
				.address("123 Fake Street")
				.buildUnsafe(),
			PolymorphicSubClass.CODEC
		);
	}

	@Test
	void testEncodingPolymorphicBuilder() throws BuilderException {
		PolymorphicSubClass subClass = new PolymorphicSubClass.Builder.Impl()
			.name("John")
			.age(21)
			.height(180)
			.address("123 Fake Street")
			.build();
		String json = """
			{
				"name": "John",
				"age": 21,
				"address": "123 Fake Street",
				"height": 180
			}""";
		CodecAssertions.assertEncodes(JsonOps.INSTANCE, subClass, json, PolymorphicSubClass.CODEC);
	}
}
