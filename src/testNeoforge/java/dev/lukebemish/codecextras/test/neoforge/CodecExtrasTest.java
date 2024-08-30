package dev.lukebemish.codecextras.test.neoforge;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.minecraft.structured.MinecraftStructures;
import dev.lukebemish.codecextras.minecraft.structured.config.ConfigScreenEntry;
import dev.lukebemish.codecextras.minecraft.structured.config.ConfigScreenInterpreter;
import dev.lukebemish.codecextras.structured.Structure;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod("codecextras_testmod")
public class CodecExtrasTest {
    private record TestRecord(int a, int b, int c) {
        private static final Structure<TestRecord> STRUCTURE = Structure.record(builder -> {
            var a = builder.add("a", Structure.INT, TestRecord::a);
            var b = builder.add("b", Structure.INT, TestRecord::b);
            var c = builder.add("c", Structure.INT, TestRecord::c);
            return container -> new TestRecord(a.apply(container), b.apply(container), c.apply(container));
        });
    }

    public CodecExtrasTest(ModContainer modContainer) {
        ConfigScreenEntry<TestRecord> entry = new ConfigScreenInterpreter(
            MinecraftStructures.CODEC_INTERPRETER
        ).interpret(TestRecord.STRUCTURE).getOrThrow();

        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> entry.rootScreen(parent, jsonElement -> {
            System.out.println("New JSON: "+jsonElement);
        }, JsonOps.INSTANCE, new JsonObject()));
    }
}
