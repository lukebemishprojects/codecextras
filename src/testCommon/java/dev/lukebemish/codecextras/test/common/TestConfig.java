package dev.lukebemish.codecextras.test.common;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.config.ConfigType;
import dev.lukebemish.codecextras.minecraft.structured.MinecraftInterpreters;
import dev.lukebemish.codecextras.minecraft.structured.MinecraftStructures;
import dev.lukebemish.codecextras.structured.Annotation;
import dev.lukebemish.codecextras.structured.IdentityInterpreter;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.schema.SchemaAnnotations;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.references.Items;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public record TestConfig(
    int a, float b, boolean c,
    String d, Optional<Boolean> e, Optional<String> f,
    Unit g, List<String> strings, Dispatches dispatches,
    int intInRange, float floatInRange, int argb,
    int rgb, ResourceKey<Item> item, Rarity rarity,
    Map<String, Integer> unbounded, Either<String, Integer> either, Map<String, Dispatches> dispatchedMap,
    DataComponentPatch patch
) {
    private static final Map<String, Structure<? extends Dispatches>> DISPATCHES = new HashMap<>();

    public interface Dispatches {
        Structure<Dispatches> STRUCTURE = Structure.STRING.dispatch(
            "type",
            d -> DataResult.success(d.key()),
            DISPATCHES::keySet,
            k -> DataResult.success(DISPATCHES.get(k))
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
        DISPATCHES.put("abc", Abc.STRUCTURE);
        DISPATCHES.put("xyz", Xyz.STRUCTURE);
    }

    public static final Structure<TestConfig> STRUCTURE = Structure.record(builder -> {
        var a = builder.addOptional("a", Structure.INT.annotate(Annotation.DESCRIPTION, "Describes the field!").annotate(Annotation.TITLE, "Field A"), TestConfig::a, () -> 34);
        var b = builder.addOptional("b", Structure.FLOAT, TestConfig::b, () -> 1.2f);
        var c = builder.addOptional("c", Structure.BOOL, TestConfig::c, () -> true);
        var d = builder.addOptional("d", Structure.STRING, TestConfig::d, () -> "test");
        var e = builder.addOptional("e", Structure.BOOL, TestConfig::e);
        var f = builder.addOptional("f", Structure.STRING, TestConfig::f);
        var g = builder.addOptional("g", Structure.UNIT, TestConfig::g, () -> Unit.INSTANCE);
        var strings = builder.addOptional("strings", Structure.STRING.listOf(), TestConfig::strings, () -> List.of("test1", "test2"));
        var dispatches = builder.addOptional("dispatches", Dispatches.STRUCTURE, TestConfig::dispatches, () -> IdentityInterpreter.INSTANCE.interpret(Abc.STRUCTURE).getOrThrow());
        var intInRange = builder.addOptional("intInRange", Structure.intInRange(10, 60), TestConfig::intInRange, () -> 50);
        var floatInRange = builder.addOptional("floatInRange", Structure.floatInRange(1.0f, 5.0f), TestConfig::floatInRange, () -> 3.0f);
        var argb = builder.addOptional("argb", MinecraftStructures.ARGB_COLOR, TestConfig::argb, () -> 0xFF0000FF);
        var rgb = builder.addOptional("rgb", MinecraftStructures.RGB_COLOR, TestConfig::rgb, () -> 0xFF0000);
        var item = builder.addOptional("item", MinecraftStructures.resourceKey(Registries.ITEM), TestConfig::item, () -> Items.MELON_SEEDS);
        var rarity = builder.addOptional("rarity", Structure.stringRepresentable(Rarity::values, Rarity::getSerializedName), TestConfig::rarity, () -> Rarity.COMMON);
        var unbounded = builder.addOptional("unbounded", Structure.unboundedMap(Structure.STRING, MinecraftStructures.RGB_COLOR), TestConfig::unbounded, () -> Map.of("test", 123));
        var either = builder.addOptional("either", Structure.either(Structure.STRING, MinecraftStructures.RGB_COLOR), TestConfig::either, () -> Either.right(0x00FFAA));
        var dispatchedMap = builder.addOptional("dispatchedMap", Structure.STRING.dispatchedMap(DISPATCHES::keySet, k -> DataResult.success(DISPATCHES.get(k))), TestConfig::dispatchedMap, Map::of);
        var patch = builder.addOptional("patch", MinecraftStructures.DATA_COMPONENT_PATCH, TestConfig::patch, () -> DataComponentPatch.EMPTY);
        return container -> new TestConfig(
            a.apply(container), b.apply(container), c.apply(container),
            d.apply(container), e.apply(container), f.apply(container),
            g.apply(container), strings.apply(container), dispatches.apply(container),
            intInRange.apply(container), floatInRange.apply(container), argb.apply(container),
            rgb.apply(container), item.apply(container), rarity.apply(container),
            unbounded.apply(container), either.apply(container), dispatchedMap.apply(container),
            patch.apply(container)
        );
    });

    public static final Codec<TestConfig> CODEC = MinecraftInterpreters.CODEC_INTERPRETER.interpret(STRUCTURE).getOrThrow();

    public static final ConfigType<TestConfig> CONFIG = new ConfigType<>() {
        @Override
        public Codec<TestConfig> codec() {
            return TestConfig.CODEC;
        }

        @Override
        public TestConfig defaultConfig() {
            return IdentityInterpreter.INSTANCE.interpret(TestConfig.STRUCTURE).getOrThrow();
        }
    };
}
