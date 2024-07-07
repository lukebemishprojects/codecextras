package dev.lukebemish.codecextras.test.config;

import static dev.lukebemish.codecextras.test.CodecAssertions.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.RootSchema;
import dev.lukebemish.codecextras.config.ConfigType;
import dev.lukebemish.codecextras.config.GsonOpsIo;
import dev.lukebemish.codecextras.repair.FillMissingMapCodec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ConfigTypeTest {
    public record TestRecord(int a, int b, float c) {
        public static final TestRecord DEFAULT = new TestRecord(1, 2, 3.0f);

        public static final Codec<TestRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            FillMissingMapCodec.fieldOf(Codec.INT, "a", DEFAULT.a()).forGetter(TestRecord::a),
            FillMissingMapCodec.fieldOf(Codec.INT, "b", DEFAULT.b()).forGetter(TestRecord::b),
            FillMissingMapCodec.fieldOf(Codec.FLOAT, "c", DEFAULT.c()).forGetter(TestRecord::c)
        ).apply(i, TestRecord::new));
    }

    static class UnfixedConfigType extends ConfigType<ConfigTypeTest.TestRecord> {
        @Override
        public Codec<TestRecord> codec() {
            return TestRecord.CODEC;
        }

        @Override
        public TestRecord defaultConfig() {
            return TestRecord.DEFAULT;
        }
    }

    static class FixedConfigType extends UnfixedConfigType {
        @Override
        public int currentVersion() {
            return 1;
        }

        @Override
        public void addFixers(Supplier<DataFixerBuilder> builder) {
            super.addFixers(builder);
            builder.get().addSchema(0, V0::new);
            var schema1 = builder.get().addSchema(1, Schema::new);
            builder.get().addFixer(new RenameKeys(schema1, true));
        }

        static class V0 extends RootSchema {

            public V0(int versionKey, Schema parent) {
                super(versionKey, parent);
            }

            @Override
            public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
                super.registerTypes(schema, entityTypes, blockEntityTypes);
                schema.registerType(true, ConfigType.CONFIG, DSL::remainder);
            }
        }

        static class RenameKeys extends DataFix {

            public RenameKeys(Schema outputSchema, boolean changesType) {
                super(outputSchema, changesType);
            }

            @Override
            protected TypeRewriteRule makeRule() {
                var configType = this.getInputSchema().getType(ConfigType.CONFIG);
                return this.fixTypeEverywhereTyped(
                    "RenameKeys",
                    configType,
                    typed -> typed.update(
                        DSL.remainderFinder(),
                        dynamic -> {
                            var a = dynamic.get("e").asInt(TestRecord.DEFAULT.a());
                            var b = dynamic.get("f").asInt(TestRecord.DEFAULT.b());
                            var c = dynamic.get("g").asFloat(TestRecord.DEFAULT.c());
                            dynamic.remove("e");
                            dynamic.remove("f");
                            dynamic.remove("g");
                            return dynamic
                                .set("a", dynamic.createInt(a))
                                .set("b", dynamic.createInt(b))
                                .set("c", dynamic.createFloat(c));
                        }
                    )
                );
            }
        }
    }

    static final String CONFIG_0 = """
        {
            "config_version": 0,
            "e": 4,
            "f": 5,
            "g": 6.0
        }""";

    static final String CONFIG_1 = """
        {
            "config_version": 1,
            "a": 4,
            "b": 5,
            "c": 6.0
        }""";

    static final String CONFIG_UNVERSIONED = """
        {
            "a": 4,
            "b": 5,
            "c": 6.0
        }""";

    static final TestRecord TEST_RECORD = new TestRecord(4, 5, 6.0f);


    @Test
    void testReadDefault(@TempDir Path tempDir) {
        var configPath = tempDir.resolve("test.json");
        var handle = handle(configPath, UnfixedConfigType::new);
        assertEquals(TestRecord.DEFAULT, handle.load());
    }


    @Test
    void testReadUnfixed(@TempDir Path tempDir) {
        var configPath = tempDir.resolve("test.json");
        var handle = handle(configPath, UnfixedConfigType::new);
        write(configPath, CONFIG_UNVERSIONED);
        assertEquals(TEST_RECORD, handle.load());
    }

    @Test
    void testReadFixed(@TempDir Path tempDir) {
        var configPath = tempDir.resolve("test.json");
        var handle = handle(configPath, FixedConfigType::new);
        assertEquals(TestRecord.DEFAULT, handle.load());
        write(configPath, CONFIG_0);
        assertEquals(TEST_RECORD, handle.load());
        write(configPath, CONFIG_1);
        assertEquals(TEST_RECORD, handle.load());
    }

    @Test
    void testWriteUnfixed(@TempDir Path tempDir) {
        var configPath = tempDir.resolve("test.json");
        var handle = handle(configPath, UnfixedConfigType::new);
        handle.save(TEST_RECORD);
        assertJsonEquals(CONFIG_UNVERSIONED, read(configPath));
    }

    @Test
    void testWriteFixed(@TempDir Path tempDir) {
        var configPath = tempDir.resolve("test.json");
        var handle = handle(configPath, FixedConfigType::new);
        handle.save(TEST_RECORD);
        assertJsonEquals(CONFIG_1, read(configPath));
    }

    private ConfigType.ConfigHandle<TestRecord> handle(Path configPath, Supplier<ConfigType<TestRecord>> ctor) {
        return ctor.get().handle(configPath, GsonOpsIo.INSTANCE);
    }

    private void write(Path path, String content) {
        try {
            Files.write(path, content.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
