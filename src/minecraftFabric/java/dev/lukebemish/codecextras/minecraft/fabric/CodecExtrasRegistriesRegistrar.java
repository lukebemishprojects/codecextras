package dev.lukebemish.codecextras.minecraft.fabric;

import com.google.auto.service.AutoService;
import dev.lukebemish.codecextras.minecraft.structured.CodecExtrasRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;

@AutoService(CodecExtrasRegistries.RegistryRegistrar.class)
public final class CodecExtrasRegistriesRegistrar implements CodecExtrasRegistries.RegistryRegistrar {
    public static Runnable PREPARE_DATA_COMPONENT_STRUCTURES = () -> {
        throw new IllegalStateException("Registry not created yet");
    };

    @Override
    public void setup(RegistriesImpl registries) {
        FabricRegistryBuilder.createSimple(CodecExtrasRegistries.DATA_COMPONENT_STRUCTURES).buildAndRegister();
        PREPARE_DATA_COMPONENT_STRUCTURES = registries.dataComponentStructures::prepare;
    }
}
