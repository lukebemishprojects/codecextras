package dev.lukebemish.codecextras.test.neoforge;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.config.ConfigType;
import dev.lukebemish.codecextras.config.GsonOpsIo;
import dev.lukebemish.codecextras.minecraft.structured.MinecraftStructures;
import dev.lukebemish.codecextras.minecraft.structured.config.ConfigScreenEntry;
import dev.lukebemish.codecextras.minecraft.structured.config.ConfigScreenInterpreter;
import dev.lukebemish.codecextras.structured.Annotation;
import dev.lukebemish.codecextras.structured.Structure;
import java.util.List;
import java.util.Optional;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod("codecextras_testmod")
public class CodecExtrasTest {
    private record TestRecord(int a, float b, boolean c, String d, Optional<Boolean> e, Optional<String> f, Unit g, List<String> strings) {
        private static final Structure<TestRecord> STRUCTURE = Structure.record(builder -> {
            var a = builder.add("a", Structure.INT.annotate(Annotation.DESCRIPTION, "Describes the field!").annotate(Annotation.TITLE, "Field A"), TestRecord::a);
            var b = builder.add("b", Structure.FLOAT, TestRecord::b);
            var c = builder.add("c", Structure.BOOL, TestRecord::c);
            var d = builder.add("d", Structure.STRING, TestRecord::d);
            var e = builder.addOptional("e", Structure.BOOL, TestRecord::e);
            var f = builder.addOptional("f", Structure.STRING, TestRecord::f);
            var g = builder.add("g", Structure.UNIT, TestRecord::g);
            var strings = builder.addOptional("strings", Structure.STRING.listOf(), TestRecord::strings, () -> List.of("test1", "test2"));
            return container -> new TestRecord(
                a.apply(container), b.apply(container), c.apply(container),
                d.apply(container), e.apply(container), f.apply(container),
                g.apply(container), strings.apply(container)
            );
        });

        private static final Codec<TestRecord> CODEC = MinecraftStructures.CODEC_INTERPRETER.interpret(STRUCTURE).getOrThrow();
    }

    private static final ConfigType.ConfigHandle<TestRecord> CONFIG = new ConfigType<TestRecord>() {
        @Override
        public Codec<TestRecord> codec() {
            return TestRecord.CODEC;
        }

        @Override
        public TestRecord defaultConfig() {
            return new TestRecord(1, 2.0f, true, "test", Optional.empty(), Optional.empty(), Unit.INSTANCE, List.of("test1", "test2"));
        }
    }.handle(FMLPaths.CONFIGDIR.get().resolve("codecextras_testmod.json"), GsonOpsIo.INSTANCE);

    public CodecExtrasTest(ModContainer modContainer) {
        ConfigScreenEntry<TestRecord> entry = new ConfigScreenInterpreter(
            MinecraftStructures.CODEC_INTERPRETER
        ).interpret(TestRecord.STRUCTURE).getOrThrow();

        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) ->
            entry.rootScreen(parent, CONFIG::save, JsonOps.INSTANCE, CONFIG.load())
        );
    }
}
