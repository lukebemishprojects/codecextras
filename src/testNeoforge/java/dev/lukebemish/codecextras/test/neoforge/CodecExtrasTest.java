package dev.lukebemish.codecextras.test.neoforge;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.config.ConfigType;
import dev.lukebemish.codecextras.config.GsonOpsIo;
import dev.lukebemish.codecextras.minecraft.structured.MinecraftStructures;
import dev.lukebemish.codecextras.minecraft.structured.config.ConfigScreenEntry;
import dev.lukebemish.codecextras.minecraft.structured.config.ConfigScreenInterpreter;
import dev.lukebemish.codecextras.structured.Annotation;
import dev.lukebemish.codecextras.structured.IdentityInterpreter;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.schema.SchemaAnnotations;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod("codecextras_testmod")
public class CodecExtrasTest {
    private interface Dispatches {
        Map<String, Structure<? extends Dispatches>> MAP = new HashMap<>();
        Structure<Dispatches> STRUCTURE = Structure.STRING.dispatch(
            "type",
            d -> DataResult.success(d.key()),
            MAP::keySet,
            MAP::get
        );
        String key();
    }

    private record Abc(int a, String b, float c) implements Dispatches {
        private static final Structure<Abc> STRUCTURE = Structure.<Abc>record(i -> {
            var a = i.addOptional("a", Structure.INT, Abc::a, () -> 123);
            var b = i.addOptional("b", Structure.STRING, Abc::b, () ->"gizmo");
            var c = i.addOptional("c", Structure.FLOAT, Abc::c, () -> 1.23f);
            return container -> new Abc(a.apply(container), b.apply(container), c.apply(container));
        }).annotate(SchemaAnnotations.REUSE_KEY, "abc");

        @Override
        public String key() {
            return "abc";
        }
    }

    private record Xyz(String x, int y, float z) implements Dispatches {
        private static final Structure<Xyz> STRUCTURE = Structure.<Xyz>record(i -> {
            var x = i.addOptional("x", Structure.STRING, Xyz::x, () -> "gadget");
            var y = i.addOptional("y", Structure.INT, Xyz::y, () -> 345);
            var z = i.addOptional("z", Structure.FLOAT, Xyz::z, () -> 3.45f);
            return container -> new Xyz(x.apply(container), y.apply(container), z.apply(container));
        }).annotate(SchemaAnnotations.REUSE_KEY, "xyz");

        @Override
        public String key() {
            return "xyz";
        }
    }

    static {
        Dispatches.MAP.put("abc", Abc.STRUCTURE);
        Dispatches.MAP.put("xyz", Xyz.STRUCTURE);
    }

    private record TestRecord(int a, float b, boolean c, String d, Optional<Boolean> e, Optional<String> f, Unit g, List<String> strings, Dispatches dispatches) {
        private static final Structure<TestRecord> STRUCTURE = Structure.record(builder -> {
            var a = builder.addOptional("a", Structure.INT.annotate(Annotation.DESCRIPTION, "Describes the field!").annotate(Annotation.TITLE, "Field A"), TestRecord::a, () -> 34);
            var b = builder.addOptional("b", Structure.FLOAT, TestRecord::b, () -> 1.2f);
            var c = builder.addOptional("c", Structure.BOOL, TestRecord::c, () -> true);
            var d = builder.addOptional("d", Structure.STRING, TestRecord::d, () -> "test");
            var e = builder.addOptional("e", Structure.BOOL, TestRecord::e);
            var f = builder.addOptional("f", Structure.STRING, TestRecord::f);
            var g = builder.addOptional("g", Structure.UNIT, TestRecord::g, () -> Unit.INSTANCE);
            var strings = builder.addOptional("strings", Structure.STRING.listOf(), TestRecord::strings, () -> List.of("test1", "test2"));
            var dispatches = builder.addOptional("dispatches", Dispatches.STRUCTURE, TestRecord::dispatches, () -> IdentityInterpreter.INSTANCE.interpret(Abc.STRUCTURE).getOrThrow());
            return container -> new TestRecord(
                a.apply(container), b.apply(container), c.apply(container),
                d.apply(container), e.apply(container), f.apply(container),
                g.apply(container), strings.apply(container), dispatches.apply(container)
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
            return IdentityInterpreter.INSTANCE.interpret(TestRecord.STRUCTURE).getOrThrow();
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
