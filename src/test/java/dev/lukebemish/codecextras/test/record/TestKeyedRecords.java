package dev.lukebemish.codecextras.test.record;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.record.KeyedRecordCodecBuilder;
import dev.lukebemish.codecextras.test.CodecAssertions;
import org.junit.jupiter.api.Test;

class TestKeyedRecords {
	private record TestRecord(int a, int b, float c) {
		public static final Codec<TestRecord> CODEC = KeyedRecordCodecBuilder.codec(i ->
			i.with(Codec.INT.fieldOf("a"), TestRecord::a, (aI, aK) ->
				aI.with(Codec.INT.fieldOf("b"), TestRecord::b, (bI, bK) ->
					bI.with(Codec.FLOAT.fieldOf("c"), TestRecord::c, (cI, cK) ->
						cI.build(container -> new TestRecord(container.get(aK), container.get(bK), container.get(cK)))
					)
				)
			)
		);
	}

	private final String json = """
			{
				"a": 1,
				"b": 2,
				"c": 3.0
			}""";

	@Test
	void testDecoding() {
		CodecAssertions.assertDecodes(JsonOps.INSTANCE, json, new TestRecord(1, 2, 3.0f), TestRecord.CODEC);
	}

	@Test
	void testEncoding() {
		CodecAssertions.assertEncodes(JsonOps.INSTANCE, new TestRecord(1, 2, 3.0f), json, TestRecord.CODEC);
	}
}
