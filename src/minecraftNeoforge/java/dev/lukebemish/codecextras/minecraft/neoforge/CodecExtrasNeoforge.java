package dev.lukebemish.codecextras.minecraft.neoforge;

import dev.lukebemish.codecextras.minecraft.structured.CodecExtrasRegistries;
import dev.lukebemish.codecextras.structured.Structure;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@Mod("codecextras_minecraft")
public final class CodecExtrasNeoforge {
    private static final Registry<Structure<? extends DataComponentType<?>>> DATA_COMPONENT_STRUCTURE_REGISTRY = new RegistryBuilder<>(CodecExtrasRegistries.DATA_COMPONENT_STRUCTURES).create();

    CodecExtrasNeoforge(IEventBus modBus) {
        modBus.addListener(NewRegistryEvent.class, event -> {
            event.register(DATA_COMPONENT_STRUCTURE_REGISTRY);
        });
        modBus.addListener(FMLCommonSetupEvent.class, event -> {
            event.enqueueWork(((CodecExtrasRegistries.RegistryRegistrar.RegistriesImpl) CodecExtrasRegistries.REGISTRIES).dataComponentStructures::prepare);
        });
    }
}
