package dev.lukebemish.codecextras.test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.BidirectionalEitherCodec;
import org.junit.jupiter.api.Test;

class TestBidirectionalEither {
	private record TestRecord(int a, int b, float c) {
		public static final Codec<TestRecord> CODEC = BidirectionalEitherCodec.orElse(
			RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("a").forGetter(TestRecord::a),
				Codec.INT.fieldOf("b").forGetter(TestRecord::b),
				Codec.FLOAT.fieldOf("c").forGetter(TestRecord::c)
			).apply(i, TestRecord::new)),
			RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("d").forGetter(TestRecord::a),
				Codec.INT.fieldOf("e").forGetter(TestRecord::b),
				Codec.FLOAT.fieldOf("f").forGetter(TestRecord::c)
			).apply(i, TestRecord::new))
		);
	}

	private final String primary = """
			{
				"a": 1,
				"b": 2,
				"c": 3.0
			}""";

	@Test
	void testDecodingPrimary() {
		CodecAssertions.assertDecodes(JsonOps.INSTANCE, primary, new TestRecord(1, 2, 3.0f), TestRecord.CODEC);
	}

	@Test
	void testDecodingSecondary() {
		String secondary = """
				{
					"d": 1,
					"e": 2,
					"f": 3.0
				}""";
		CodecAssertions.assertDecodes(JsonOps.INSTANCE, secondary, new TestRecord(1, 2, 3.0f), TestRecord.CODEC);
	}

	@Test
	void testEncoding() {
		CodecAssertions.assertEncodes(JsonOps.INSTANCE, new TestRecord(1, 2, 3.0f), primary, TestRecord.CODEC);
	}
}
