package dev.lukebemish.codecextras.minecraft.fabric;

import com.google.auto.service.AutoService;
import dev.lukebemish.codecextras.minecraft.structured.CodecExtrasRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;

@AutoService(CodecExtrasRegistries.RegistryRegistrar.class)
public final class CodecExtrasRegistriesRegistrar implements CodecExtrasRegistries.RegistryRegistrar {
    @Override
    public void setup() {
        FabricRegistryBuilder.createSimple(CodecExtrasRegistries.DATA_COMPONENT_STRUCTURES).buildAndRegister();
    }
}
