package dev.lukebemish.codecextras.test.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.lukebemish.codecextras.config.ConfigType;
import dev.lukebemish.codecextras.config.GsonOpsIo;
import dev.lukebemish.codecextras.minecraft.structured.MinecraftInterpreters;
import dev.lukebemish.codecextras.minecraft.structured.config.ConfigScreenBuilder;
import dev.lukebemish.codecextras.minecraft.structured.config.ConfigScreenEntry;
import dev.lukebemish.codecextras.minecraft.structured.config.ConfigScreenInterpreter;
import dev.lukebemish.codecextras.minecraft.structured.config.EntryCreationContext;
import dev.lukebemish.codecextras.test.common.TestConfig;
import net.fabricmc.loader.api.FabricLoader;

public class CodecExtrasModMenu implements ModMenuApi {
    private static final ConfigType.ConfigHandle<TestConfig> CONFIG = TestConfig.CONFIG
        .handle(FabricLoader.getInstance().getConfigDir().resolve("codecextras_testmod.json"), GsonOpsIo.INSTANCE);

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        ConfigScreenEntry<TestConfig> entry = new ConfigScreenInterpreter(
            MinecraftInterpreters.CODEC_INTERPRETER
        ).interpret(TestConfig.STRUCTURE).getOrThrow();

        return parent -> ConfigScreenBuilder.create()
            .add(entry, CONFIG::save, () -> EntryCreationContext.builder().build(), CONFIG::load)
            .factory().apply(parent);
    }
}
