package dev.lukebemish.codecextras.test.comments;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.comments.CommentMapCodec;
import dev.lukebemish.codecextras.compat.nightconfig.TomlConfigOps;
import dev.lukebemish.codecextras.test.CodecAssertions;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class TestComments {
    private record TestRecord(int a) {
        static final Codec<TestRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            CommentMapCodec.of(Codec.INT.fieldOf("a"), "Commented field").forGetter(TestRecord::a)
        ).apply(i, TestRecord::new));
    }

    private final String toml = """
            #Commented field
            a = 1

            """;

    private final Config tomlConfig = new TomlParser().parse(toml);

    private static final Function<Object, String> TOML_TO_STRING = toml -> {
        if (toml instanceof Config config) {
            return new TomlWriter().writeToString(config);
        } else {
            return toml.toString();
        }
    };

    private final TestRecord testRecord = new TestRecord(1);

    @Test
    void testEncoding() {
        CodecAssertions.assertEncodesString(TomlConfigOps.COMMENTED, testRecord, toml, TOML_TO_STRING, TestRecord.CODEC);
    }

    @Test
    void testDecoding() {
        CodecAssertions.assertDecodes(TomlConfigOps.COMMENTED, tomlConfig, testRecord, TestRecord.CODEC);
    }
}
