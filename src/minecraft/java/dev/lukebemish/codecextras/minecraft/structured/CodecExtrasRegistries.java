package dev.lukebemish.codecextras.minecraft.structured;

import dev.lukebemish.codecextras.structured.Structure;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

public class CodecExtrasRegistries {
    private static final String NAMESPACE = "codecextras_minecraft";

    public static final ResourceKey<Registry<Structure<? extends DataComponentType<?>>>> DATA_COMPONENT_STRUCTURES = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(NAMESPACE, "data_component__type"));

    static {
        // This MUST be the last static initializer in this class -- the registry registrar may depend on the keys defined earlier on
        ServiceLoader.load(RegistryRegistrar.class).stream().map(ServiceLoader.Provider::get).forEach(RegistryRegistrar::setup);
    }

    public static final class Registries {
        private Registries() {}

        @SuppressWarnings("unchecked")
        public static final Supplier<Registry<Structure<? extends DataComponentType<?>>>> DATA_COMPONENT_STRUCTURES = () ->
            (Registry<Structure<? extends DataComponentType<?>>>) Objects.requireNonNull(BuiltInRegistries.REGISTRY.get(CodecExtrasRegistries.DATA_COMPONENT_STRUCTURES.location()), "Registry does not exist");
    }

    @ApiStatus.Internal
    public interface RegistryRegistrar {
        void setup();
    }
}
