package dev.lukebemish.codecextras.minecraft.fabric.mixin;

import dev.lukebemish.codecextras.minecraft.fabric.CodecExtrasRegistriesRegistrar;
import dev.lukebemish.codecextras.minecraft.structured.CodecExtrasRegistries;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> implements WritableRegistry<T> {
    @SuppressWarnings({"RedundantCast", "rawtypes"})
    @Inject(method = "freeze()Lnet/minecraft/core/Registry;", at = @At("HEAD"))
    private void onFreeze(CallbackInfoReturnable<Registry<T>> cir) {
        if ((ResourceKey) this.key() == CodecExtrasRegistries.DATA_COMPONENT_STRUCTURES) {
            CodecExtrasRegistriesRegistrar.PREPARE_DATA_COMPONENT_STRUCTURES.run();
        }
    }
}
