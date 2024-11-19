package dev.lukebemish.codecextras.minecraft.structured;

import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.types.Preparable;
import dev.lukebemish.codecextras.types.PreparableView;
import java.util.Objects;
import java.util.ServiceLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

public class CodecExtrasRegistries {
    private static final String NAMESPACE = "codecextras_minecraft";
    private static final RegistryRegistrar.RegistriesImpl REGISTRIES_IMPL = new RegistryRegistrar.RegistriesImpl();

    public static final ResourceKey<Registry<Structure<? extends DataComponentType<?>>>> DATA_COMPONENT_STRUCTURES = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(NAMESPACE, "data_component_type"));
    public static final Registries REGISTRIES = REGISTRIES_IMPL;

    static {
        // This MUST be the last static initializer in this class -- the registry registrar may depend on the keys defined earlier on
        ServiceLoader.load(RegistryRegistrar.class).stream().map(ServiceLoader.Provider::get).forEach(registryRegistrar -> registryRegistrar.setup(REGISTRIES_IMPL));
    }

    public abstract sealed static class Registries {
        public abstract PreparableView<Registry<Structure<? extends DataComponentType<?>>>> dataComponentStructures();
    }

    @ApiStatus.Internal
    public interface RegistryRegistrar {
        @ApiStatus.Internal
        final class RegistriesImpl extends Registries {
            private RegistriesImpl() {}

            @Override
            public PreparableView<Registry<Structure<? extends DataComponentType<?>>>> dataComponentStructures() {
                return dataComponentStructures;
            }

            @SuppressWarnings("unchecked")
            public final Preparable<Registry<Structure<? extends DataComponentType<?>>>> dataComponentStructures = Preparable.memoize(() ->
                (Registry<Structure<? extends DataComponentType<?>>>) Objects.requireNonNull(BuiltInRegistries.REGISTRY.getValue(CodecExtrasRegistries.DATA_COMPONENT_STRUCTURES.location()), "Registry does not exist"));
        }

        void setup(RegistriesImpl registries);
    }
}
