package dev.lukebemish.codecextras.minecraft.structured;

import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.schema.SchemaAnnotations;
import dev.lukebemish.codecextras.types.Flip;
import java.util.function.Function;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public final class MinecraftStructures {
    private MinecraftStructures() {
    }

    public static final Structure<ResourceLocation> RESOURCE_LOCATION = Structure.keyed(
            MinecraftKeys.RESOURCE_LOCATION, Keys.<Flip.Mu<ResourceLocation>, K1>builder()
                    .add(CodecInterpreter.KEY, new Flip<>(new CodecInterpreter.Holder<>(ResourceLocation.CODEC)))
                    .build()
    );

    public static final Structure<Integer> ARGB_COLOR = Structure.keyed(
            MinecraftKeys.ARGB_COLOR,
            Structure.INT
    );

    public static final Structure<Integer> RGB_COLOR = Structure.keyed(
            MinecraftKeys.RGB_COLOR,
            Structure.INT
    );

    public static <T> Structure<ResourceKey<T>> resourceKey(ResourceKey<? extends Registry<T>> registry) {
        return Structure.parametricallyKeyed(
                MinecraftKeys.RESOURCE_KEY,
                new MinecraftKeys.RegistryKeyHolder<>(registry),
                MinecraftKeys.ResourceKeyHolder::unbox,
                Keys.<Flip.Mu<MinecraftKeys.ResourceKeyHolder<T>>, K1>builder()
                        .add(
                                CodecInterpreter.KEY,
                                new Flip<>(new CodecInterpreter.Holder<>(
                                        ResourceKey.codec(registry).xmap(MinecraftKeys.ResourceKeyHolder::new, MinecraftKeys.ResourceKeyHolder::value)
                                ))
                        )
                        .build()
        ).xmap(MinecraftKeys.ResourceKeyHolder::value, MinecraftKeys.ResourceKeyHolder::new);
    }

    public static <T> Structure<HolderSet<T>> homogenousList(ResourceKey<? extends Registry<T>> registry) {
        return Structure.parametricallyKeyed(
                MinecraftKeys.HOMOGENOUS_LIST_KEY,
                new MinecraftKeys.RegistryKeyHolder<>(registry),
                MinecraftKeys.HolderSetHolder::unbox,
                Keys.<Flip.Mu<MinecraftKeys.HolderSetHolder<T>>, K1>builder()
                        .add(
                                CodecInterpreter.KEY,
                                new Flip<>(new CodecInterpreter.Holder<>(
                                        RegistryCodecs.homogeneousList(registry).xmap(MinecraftKeys.HolderSetHolder::new, MinecraftKeys.HolderSetHolder::value)
                                ))
                        )
                        .build()
        ).xmap(MinecraftKeys.HolderSetHolder::value, MinecraftKeys.HolderSetHolder::new);
    }

    public static <T> Structure<TagKey<T>> tagKey(ResourceKey<? extends Registry<T>> registry, boolean hashPrefix) {
        return Structure.parametricallyKeyed(
                hashPrefix ? MinecraftKeys.HASHED_TAG_KEY : MinecraftKeys.TAG_KEY,
                new MinecraftKeys.RegistryKeyHolder<>(registry),
                MinecraftKeys.TagKeyHolder::unbox,
                Keys.<Flip.Mu<MinecraftKeys.TagKeyHolder<T>>, K1>builder()
                        .add(
                                CodecInterpreter.KEY,
                                new Flip<>(new CodecInterpreter.Holder<>(
                                        (hashPrefix ? TagKey.hashedCodec(registry) : TagKey.codec(registry)).xmap(MinecraftKeys.TagKeyHolder::new, MinecraftKeys.TagKeyHolder::value)
                                ))
                        )
                        .build()
        ).xmap(MinecraftKeys.TagKeyHolder::value, MinecraftKeys.TagKeyHolder::new);
    }

    public static <T> Structure<T> registryDispatch(String keyField, Function<T, DataResult<ResourceKey<Structure<? extends T>>>> structureFunction, Registry<Structure<? extends T>> registry) {
        var keyStructure = resourceKey(registry.key());
        return keyStructure.dispatch(keyField, structureFunction, registry::registryKeySet, k ->
                registry.getOrThrow(k).annotate(SchemaAnnotations.REUSE_KEY, toDefsKey(k.registry()) + "::" + toDefsKey(k.location()))
        );
    }

    private static String toDefsKey(ResourceLocation location) {
        return location.getNamespace().replace('/', '.') + ":" + location.getPath().replace('/', '.');
    }
}
