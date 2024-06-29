package dev.lukebemish.codecextras.test.structured;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.test.CodecAssertions;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestStructured {
	private record TestRecord(int a, String b, List<Boolean> c) {
		private static final Structure<TestRecord> STRUCTURE = Structure.record(i -> {
			var a = i.add("a", Structure.INT, TestRecord::a);
			var b = i.add("b", Structure.STRING, TestRecord::b);
			var c = i.add("c", Structure.BOOL.listOf(), TestRecord::c);
			return container -> new TestRecord(container.get(a), container.get(b), container.get(c));
		});

		private static final Codec<TestRecord> CODEC = CodecInterpreter.unbox(STRUCTURE.interpret(new CodecInterpreter()).getOrThrow());
	}

	private final String json = """
			{
				"a": 1,
				"b": "test",
				"c": [true, false, true]
			}""";

	private final TestRecord record = new TestRecord(1, "test", List.of(true, false, true));

	@Test
	void testDecodingCodec() {
		CodecAssertions.assertDecodes(JsonOps.INSTANCE, json, record, TestRecord.CODEC);
	}

	@Test
	void testEncodingCodec() {
		CodecAssertions.assertEncodes(JsonOps.INSTANCE, record, json, TestRecord.CODEC);
	}
}
