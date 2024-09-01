package dev.lukebemish.codecextras.test.neoforge;

import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.config.ConfigType;
import dev.lukebemish.codecextras.config.GsonOpsIo;
import dev.lukebemish.codecextras.minecraft.structured.MinecraftInterpreters;
import dev.lukebemish.codecextras.minecraft.structured.config.ConfigScreenEntry;
import dev.lukebemish.codecextras.minecraft.structured.config.ConfigScreenInterpreter;
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
        ConfigScreenEntry<TestConfig> entry = new ConfigScreenInterpreter(
            MinecraftInterpreters.CODEC_INTERPRETER
        ).interpret(TestConfig.STRUCTURE).getOrThrow();

        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) ->
            entry.rootScreen(parent, CONFIG::save, JsonOps.INSTANCE, CONFIG.load())
        );
    }
}
