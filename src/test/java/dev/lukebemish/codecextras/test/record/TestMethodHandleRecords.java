package dev.lukebemish.codecextras.test.record;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.record.MethodHandleRecordCodecBuilder;
import dev.lukebemish.codecextras.test.CodecAssertions;
import java.lang.invoke.MethodHandles;
import org.junit.jupiter.api.Test;

class TestMethodHandleRecords {
    private record TestRecord(int a, int b, float c) {
        public static final Codec<TestRecord> CODEC = MethodHandleRecordCodecBuilder.<TestRecord>start()
            .with(Codec.INT.fieldOf("a"), TestRecord::a)
            .with(Codec.INT.fieldOf("b"), TestRecord::b)
            .with(Codec.FLOAT.fieldOf("c"), TestRecord::c)
            .buildWithConstructor(MethodHandles.lookup(), TestRecord.class);
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
