package dev.lukebemish.codecextras.minecraft.structured;

import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.stream.structured.StreamCodecInterpreter;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.schema.SchemaAnnotations;
import dev.lukebemish.codecextras.types.Flip;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jspecify.annotations.Nullable;

public final class MinecraftStructures {
    private MinecraftStructures() {
    }

    public static final Structure<ResourceLocation> RESOURCE_LOCATION = Structure.keyed(
        MinecraftKeys.RESOURCE_LOCATION,
        Keys.<Flip.Mu<ResourceLocation>, K1>builder()
                    .add(CodecInterpreter.KEY, new Flip<>(new CodecInterpreter.Holder<>(ResourceLocation.CODEC)))
                    .build(),
        Structure.STRING.flatXmap(ResourceLocation::read, rl -> DataResult.success(rl.toString()))
    );

    public static final Structure<Integer> ARGB_COLOR = Structure.keyed(
            MinecraftKeys.ARGB_COLOR,
            Structure.INT
    );

    public static final Structure<Integer> RGB_COLOR = Structure.keyed(
            MinecraftKeys.RGB_COLOR,
            Structure.INT
    );

    private static final Structure<Map<DataComponentType<?>, Object>> DATA_COMPONENT_VALUE_MAP_FALLBACK = resourceKey(Registries.DATA_COMPONENT_TYPE)
        .<DataComponentType<?>>flatXmap(key -> {
            var type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(key);
            if (type == null) {
                return DataResult.error(() -> "Unknown data component type: " + key);
            }
            return DataResult.success(type);
        }, type -> {
            var location = BuiltInRegistries.DATA_COMPONENT_TYPE.getResourceKey(type);
            if (location.isPresent()) {
                return DataResult.success(location.orElseThrow());
            }
            return DataResult.error(() -> "Data component type " + type + " is not registered");
        })
        .dispatchedMap(() -> BuiltInRegistries.DATA_COMPONENT_TYPE.stream().collect(Collectors.toSet()), MinecraftStructures::dataComponentTypeStructure);

    public static final Structure<Map<DataComponentType<?>, Object>> DATA_COMPONENT_VALUE_MAP = Structure.keyed(
        MinecraftKeys.VALUE_MAP,
        Keys.<Flip.Mu<Map<DataComponentType<?>, Object>>, K1>builder()
            .add(CodecInterpreter.KEY, new Flip<>(new CodecInterpreter.Holder<>(DataComponentType.VALUE_MAP_CODEC)))
            .build(),
        DATA_COMPONENT_VALUE_MAP_FALLBACK
    );

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final Structure<DataComponentMap> DATA_COMPONENT_MAP = Structure.keyed(
        MinecraftKeys.DATA_COMPONENT_MAP,
        Keys.<Flip.Mu<DataComponentMap>, K1>builder()
            .add(CodecInterpreter.KEY, new Flip<>(new CodecInterpreter.Holder<>(DataComponentMap.CODEC)))
            .build(),
        DATA_COMPONENT_VALUE_MAP.xmap(values -> {
            var builder = DataComponentMap.builder();
            values.forEach((type, value) -> builder.set((DataComponentType) type, value));
            return builder.build();
        }, dataComponentMap -> dataComponentMap.stream().collect(Collectors.toMap(TypedDataComponent::type, TypedDataComponent::value)))
    );

    public static final Structure<MinecraftKeys.DataComponentPatchKey<?>> DATA_COMPONENT_PATCH_KEY = Structure.keyed(
        MinecraftKeys.DATA_COMPONENT_PATCH_KEY,
        Structure.STRING
            .<MinecraftKeys.DataComponentPatchKey<?>>flatXmap(string -> {
                boolean removes = string.startsWith("!");
                string = removes ? string.substring(1) : string;
                return ResourceLocation.read(string).flatMap(rl -> {
                    var type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(rl);
                    if (type == null) {
                        return DataResult.error(() -> "Unknown data component type: " + rl);
                    }
                    return DataResult.success(new MinecraftKeys.DataComponentPatchKey<>(type, removes));
                });
            }, key -> {
                var rl = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(key.type());
                if (rl == null) {
                    return DataResult.error(() -> "Unknown data component type: " + key.type());
                }
                return DataResult.success((key.removes() ? "!" : "") + rl);
            })
            .bounded(MinecraftStructures::possibleDataComponentPatchKeys)
    );

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final Structure<DataComponentPatch> DATA_COMPONENT_PATCH = Structure.keyed(
        MinecraftKeys.DATA_COMPONENT_PATCH,
        Keys.<Flip.Mu<DataComponentPatch>, K1>builder()
            .add(CodecInterpreter.KEY, new Flip<>(new CodecInterpreter.Holder<>(DataComponentPatch.CODEC)))
            .add(StreamCodecInterpreter.REGISTRY_FRIENDLY_BYTE_BUF_KEY, new Flip<>(new StreamCodecInterpreter.Holder<>(DataComponentPatch.STREAM_CODEC)))
            .build(),
        DATA_COMPONENT_PATCH_KEY
            .dispatchedUnboundedMap(MinecraftStructures::possibleDataComponentPatchKeys, MinecraftStructures::dataComponentPatchValueCodec)
            .xmap(map -> {
                var builder = DataComponentPatch.builder();
                map.forEach((key, value) -> {
                    if (key.removes()) {
                        builder.remove(key.type());
                    } else {
                        builder.set((DataComponentType) key.type(), value);
                    }
                });
                return builder.build();
            }, patches -> patches.entrySet().stream().collect(Collectors.toMap(entry -> {
                var key = entry.getKey();
                var removes = entry.getValue().isEmpty();
                return new MinecraftKeys.DataComponentPatchKey<>(key, removes);
            }, entry -> {
                if (entry.getValue().isEmpty()) {
                    return Unit.INSTANCE;
                }
                return entry.getValue().get();
            })))
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
                MinecraftKeys.HOMOGENOUS_LIST,
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
                DataResult.success(registry.getOrThrow(k).annotate(SchemaAnnotations.REUSE_KEY, toDefsKey(k.registry()) + "::" + toDefsKey(k.location())))
        );
    }

    private static String toDefsKey(ResourceLocation location) {
        return location.getNamespace().replace('/', '.') + ":" + location.getPath().replace('/', '.');
    }

    private static DataResult<Structure<?>> dataComponentTypeStructure(DataComponentType<?> type) {
        var resourceKey = BuiltInRegistries.DATA_COMPONENT_TYPE.getResourceKey(type);
        if (resourceKey.isEmpty()) {
            return DataResult.error(() -> "Unregistered data component type: " + type);
        }
        var structure = CodecExtrasRegistries.Registries.DATA_COMPONENT_STRUCTURES.get().get(resourceKey.orElseThrow().location());
        return fallbackDataComponentTypeStructure(type, structure);
    }

    private static <T> DataResult<Structure<?>> fallbackDataComponentTypeStructure(DataComponentType<T> type, @Nullable Structure<? extends DataComponentType<?>> fallback) {
        if (fallback != null) {
            return DataResult.success(fallback);
        }
        var key = MinecraftKeys.dataComponentType(type);
        var codec = type.codec();
        var streamCodec = type.streamCodec();
        var keysBuilder = Keys.<Flip.Mu<T>, K1>builder()
            .add(StreamCodecInterpreter.REGISTRY_FRIENDLY_BYTE_BUF_KEY, new Flip<>(new StreamCodecInterpreter.Holder<>(streamCodec.cast())));;
        if (codec != null) {
            keysBuilder.add(CodecInterpreter.KEY, new Flip<>(new CodecInterpreter.Holder<>(codec)));
        }
        var keys = keysBuilder.build();
        return DataResult.success(Structure.keyed(
            key,
            keys
        ));
    }

    private static Set<MinecraftKeys.DataComponentPatchKey<?>> possibleDataComponentPatchKeys() {
        return BuiltInRegistries.DATA_COMPONENT_TYPE.stream()
            .flatMap(type -> Stream.of(new MinecraftKeys.DataComponentPatchKey<>(type, false), new MinecraftKeys.DataComponentPatchKey<>(type, true)))
            .collect(Collectors.toSet());
    }

    private static DataResult<Structure<?>> dataComponentPatchValueCodec(MinecraftKeys.DataComponentPatchKey<?> key) {
        if (key.removes()) {
            return DataResult.success(Structure.UNIT);
        }
        return dataComponentTypeStructure(key.type());
    }

}
