package dev.lukebemish.codecextras.test.neoforge;

import dev.lukebemish.codecextras.config.ConfigType;
import dev.lukebemish.codecextras.config.GsonOpsIo;
import dev.lukebemish.codecextras.minecraft.structured.MinecraftInterpreters;
import dev.lukebemish.codecextras.minecraft.structured.config.ConfigScreenBuilder;
import dev.lukebemish.codecextras.minecraft.structured.config.ConfigScreenEntry;
import dev.lukebemish.codecextras.minecraft.structured.config.ConfigScreenInterpreter;
import dev.lukebemish.codecextras.minecraft.structured.config.EntryCreationContext;
import dev.lukebemish.codecextras.test.common.TestConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod("codecextras_testmod")
public class CodecExtrasTest {
    private static final ConfigType.ConfigHandle<TestConfig> CONFIG = TestConfig.CONFIG
            .handle(FMLPaths.CONFIGDIR.get().resolve("codecextras_testmod.json"), GsonOpsIo.INSTANCE);

    public CodecExtrasTest(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> {
            var interpreter = new ConfigScreenInterpreter(MinecraftInterpreters.CODEC_INTERPRETER);
            ConfigScreenEntry<TestConfig> entry = interpreter.interpret(TestConfig.STRUCTURE).getOrThrow();

            return ConfigScreenBuilder.create()
                .add(entry, CONFIG::save, () -> EntryCreationContext.builder().build(), CONFIG::load)
                .factory().apply(parent);
        });
    }
}
