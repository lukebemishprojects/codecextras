package dev.lukebemish.codecextras.minecraft.structured;

import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.stream.structured.StreamCodecInterpreter;
import dev.lukebemish.codecextras.structured.Annotation;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.schema.SchemaAnnotations;
import dev.lukebemish.codecextras.types.Flip;
import dev.lukebemish.codecextras.types.Identity;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class MinecraftStructures {
    private MinecraftStructures() {
    }

    public static final Structure<ResourceLocation> RESOURCE_LOCATION = Structure.keyed(
        MinecraftKeys.RESOURCE_LOCATION,
        Keys.<Flip.Mu<ResourceLocation>, K1>builder()
                    .add(CodecInterpreter.KEY, new Flip<>(new CodecInterpreter.Holder<>(ResourceLocation.CODEC)))
                    .add(StreamCodecInterpreter.FRIENDLY_BYTE_BUF_KEY, new Flip<>(new StreamCodecInterpreter.Holder<>(ResourceLocation.STREAM_CODEC.cast())))
                    .build(),
        Structure.STRING.flatXmap(ResourceLocation::read, rl -> DataResult.success(rl.toString()))
            .annotate(Annotation.PATTERN, "^([a-z0-9_.-]+:)?[a-z0-9_/.-]+$")
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
            .annotate(Annotation.PATTERN, "^[!]?([a-z0-9_.-]+:)?[a-z0-9_/.-]+$")
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

    @SuppressWarnings("deprecation")
    public static final Structure<Holder<Item>> ITEM_NON_AIR = Structure.keyed(
        MinecraftKeys.ITEM_NON_AIR,
        Keys.<Flip.Mu<Holder<Item>>, K1>builder()
            .add(CodecInterpreter.KEY, new Flip<>(new CodecInterpreter.Holder<>(ItemStack.ITEM_NON_AIR_CODEC)))
            .build(),
        registryOrderedHolder(BuiltInRegistries.ITEM)
            .validate(holder -> holder.is(Items.AIR.builtInRegistryHolder()) ? DataResult.error(() -> "Item must not be minecraft:air") : DataResult.success(holder))
    );

    public static final Structure<ItemStack> NON_EMPTY_ITEM_STACK = Structure.lazyInitialized(() -> Structure.keyed(
        MinecraftKeys.NON_EMPTY_ITEM_STACK,
        Keys.<Flip.Mu<ItemStack>, K1>builder()
            .add(CodecInterpreter.KEY, new Flip<>(new CodecInterpreter.Holder<>(ItemStack.CODEC)))
            .add(StreamCodecInterpreter.REGISTRY_FRIENDLY_BYTE_BUF_KEY, new Flip<>(new StreamCodecInterpreter.Holder<>(ItemStack.STREAM_CODEC)))
            .build(),
        Structure.record(builder -> {
            var item = builder.add("id", ITEM_NON_AIR, ItemStack::getItemHolder);
            var count = builder.addOptional("count", Structure.intInRange(1, 99), ItemStack::getCount, () -> 1);
            var patch = builder.addOptional("components", DATA_COMPONENT_PATCH, ItemStack::getComponentsPatch, () -> DataComponentPatch.EMPTY);
            return container -> new ItemStack(
                item.apply(container),
                count.apply(container),
                patch.apply(container)
            );
        })
    ));

    public static final Structure<ItemStack> OPTIONAL_ITEM_STACK = Structure.lazyInitialized(() -> Structure.keyed(
        MinecraftKeys.OPTIONAL_ITEM_STACK,
        Keys.<Flip.Mu<ItemStack>, K1>builder()
            .add(CodecInterpreter.KEY, new Flip<>(new CodecInterpreter.Holder<>(ItemStack.OPTIONAL_CODEC)))
            .add(StreamCodecInterpreter.REGISTRY_FRIENDLY_BYTE_BUF_KEY, new Flip<>(new StreamCodecInterpreter.Holder<>(ItemStack.OPTIONAL_STREAM_CODEC)))
            .build(),
        Structure.either(
            Structure.EMPTY_MAP,
                NON_EMPTY_ITEM_STACK
            )
            .xmap(e -> e.map(u -> ItemStack.EMPTY, Function.identity()), itemStack -> itemStack.isEmpty() ? Either.left(Unit.INSTANCE) : Either.right(itemStack))
    ));

    public static final Structure<ItemStack> STRICT_NON_EMPTY_ITEM_STACK = Structure.lazyInitialized(() -> Structure.keyed(
        MinecraftKeys.STRICT_NON_EMPTY_ITEM_STACK,
        Keys.<Flip.Mu<ItemStack>, K1>builder()
            .add(CodecInterpreter.KEY, new Flip<>(new CodecInterpreter.Holder<>(ItemStack.STRICT_CODEC)))
            .build(),
        NON_EMPTY_ITEM_STACK.validate(MinecraftStructures::validateItemStackStrict)
    ));

    private static DataResult<ItemStack> validateItemStackStrict(ItemStack itemStack) {
        return ItemStack.validateComponents(itemStack.getComponents())
            .flatMap(
                u -> itemStack.getCount() > itemStack.getMaxStackSize() ?
                    DataResult.error(() -> "Item stack with stack size of " + itemStack.getCount() + " was larger than maximum: " + itemStack.getMaxStackSize()) :
                    DataResult.success(itemStack)
            );
    }

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
                        .add(
                            StreamCodecInterpreter.FRIENDLY_BYTE_BUF_KEY,
                            new Flip<>(new StreamCodecInterpreter.Holder<>(
                                ResourceKey.streamCodec(registry).map(MinecraftKeys.ResourceKeyHolder::new, MinecraftKeys.ResourceKeyHolder::value).cast()
                            ))
                        )
                        .build(),
            RESOURCE_LOCATION.xmap(resourceLocation -> ResourceKey.create(registry, resourceLocation), ResourceKey::location)
                        .xmap(MinecraftKeys.ResourceKeyHolder::new, MinecraftKeys.ResourceKeyHolder::value)
        ).xmap(MinecraftKeys.ResourceKeyHolder::value, MinecraftKeys.ResourceKeyHolder::new);
    }

    public static <T> Structure<Holder<T>> registryOrderedHolder(Registry<T> registry) {
        return Structure.parametricallyKeyed(
            MinecraftKeys.ORDERED_HOLDER,
            new MinecraftKeys.RegistryHolder<>(registry),
            MinecraftKeys.HolderHolder::unbox,
            Keys.<Flip.Mu<MinecraftKeys.HolderHolder<T>>, K1>builder()
                .add(
                    CodecInterpreter.KEY,
                    new Flip<>(new CodecInterpreter.Holder<>(
                        registry.holderByNameCodec().xmap(MinecraftKeys.HolderHolder::new, MinecraftKeys.HolderHolder::value)
                    ))
                )
                .add(
                    StreamCodecInterpreter.REGISTRY_FRIENDLY_BYTE_BUF_KEY,
                    new Flip<>(new StreamCodecInterpreter.Holder<>(
                        ByteBufCodecs.holderRegistry(registry.key()).map(MinecraftKeys.HolderHolder::new, MinecraftKeys.HolderHolder::value)
                    ))
                )
                .build(),
            resourceKey(registry.key())
                .bounded(registry::registryKeySet)
                .flatXmap(key -> registry.getHolder(key).<DataResult<Holder<T>>>map(DataResult::success).orElse(DataResult.error(() -> "Unknown registry entry: " + key)), holder ->
                    keyForEntry(holder, registry).map(DataResult::success).orElse(DataResult.error(() -> "Unknown registry entry: " + holder)))
                .xmap(MinecraftKeys.HolderHolder::new, MinecraftKeys.HolderHolder::value)
        ).xmap(MinecraftKeys.HolderHolder::value, MinecraftKeys.HolderHolder::new);
    }

    public static <T> Structure<Holder<T>> registryUnorderedHolder(Registry<T> registry) {
        return Structure.parametricallyKeyed(
            MinecraftKeys.UNORDERED_HOLDER,
            new MinecraftKeys.RegistryHolder<>(registry),
            MinecraftKeys.HolderHolder::unbox,
            Keys.<Flip.Mu<MinecraftKeys.HolderHolder<T>>, K1>builder()
                .add(
                    CodecInterpreter.KEY,
                    new Flip<>(new CodecInterpreter.Holder<>(
                        registry.holderByNameCodec().xmap(MinecraftKeys.HolderHolder::new, MinecraftKeys.HolderHolder::value)
                    ))
                )
                .add(
                    StreamCodecInterpreter.FRIENDLY_BYTE_BUF_KEY,
                    new Flip<>(new StreamCodecInterpreter.Holder<>(
                        ResourceLocation.STREAM_CODEC.<Holder<T>>map(
                            rl -> registry.getHolder(rl).orElseThrow(() -> new DecoderException("Unknown registry entry: " + rl)),
                            holder -> keyForEntry(holder, registry).orElseThrow(() -> new EncoderException("Unknown registry entry: " + holder)).location()
                        ).<FriendlyByteBuf>cast().map(MinecraftKeys.HolderHolder::new, MinecraftKeys.HolderHolder::value)
                    ))
                )
                .build(),
            resourceKey(registry.key())
                .bounded(registry::registryKeySet)
                .flatXmap(key -> registry.getHolder(key).<DataResult<Holder<T>>>map(DataResult::success).orElse(DataResult.error(() -> "Unknown registry entry: " + key)), holder ->
                    keyForEntry(holder, registry).map(DataResult::success).orElse(DataResult.error(() -> "Unknown registry entry: " + holder)))
                .xmap(MinecraftKeys.HolderHolder::new, MinecraftKeys.HolderHolder::value)
        ).xmap(MinecraftKeys.HolderHolder::value, MinecraftKeys.HolderHolder::new);
    }

    private static <T> Optional<ResourceKey<T>> keyForEntry(Holder<T> entry, Registry<T> registry) {
        if (entry instanceof Holder.Reference<T> reference) {
            return Optional.of(reference.key());
        } else {
            var value = entry.value();
            return registry.getResourceKey(value);
        }
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
                        .build(),
            (hashPrefix ?
                    Structure.STRING.comapFlatMap(string -> string.startsWith("#") ? ResourceLocation.read(string.substring(1)).map(resourceLocation -> TagKey.create(registry, resourceLocation)) : DataResult.<TagKey<T>>error(() -> "Not a tag id"), tagKey -> "#" + tagKey.location()) :
                    RESOURCE_LOCATION.xmap(resourceLocation -> TagKey.create(registry, resourceLocation), TagKey::location))
                        .xmap(MinecraftKeys.TagKeyHolder::new, MinecraftKeys.TagKeyHolder::value)
                        .annotate(Annotation.PATTERN, "^"+(hashPrefix ? "#" : "")+"([a-z0-9_.-]+:)?[a-z0-9_/.-]+$")
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
        if (!CodecExtrasRegistries.REGISTRIES.dataComponentStructures().isReady()) {
            return DataResult.error(() -> "Data component structures registry is not frozen");
        }
        var structure = CodecExtrasRegistries.REGISTRIES.dataComponentStructures().get().get(resourceKey.orElseThrow().location());
        return fallbackDataComponentTypeStructure(type, structure);
    }

    private static <T> DataResult<Structure<?>> fallbackDataComponentTypeStructure(DataComponentType<T> type, @Nullable Structure<? extends DataComponentType<?>> fallback) {
        if (fallback != null) {
            return DataResult.success(fallback);
        }
        var key2 = MinecraftKeys.FALLBACK_DATA_COMPONENT_TYPE;

        var codec = type.codec();
        var streamCodec = type.streamCodec();
        var keysBuilder = Keys.<Flip.Mu<Identity<T>>, K1>builder()
            .add(StreamCodecInterpreter.REGISTRY_FRIENDLY_BYTE_BUF_KEY, new Flip<>(new StreamCodecInterpreter.Holder<>(streamCodec.<RegistryFriendlyByteBuf>cast().map(Identity::new, i -> Identity.unbox(i).value()))));;
        if (codec != null) {
            keysBuilder.add(CodecInterpreter.KEY, new Flip<>(new CodecInterpreter.Holder<>(codec.xmap(Identity::new, i -> Identity.unbox(i).value()))));
        }
        var keys = keysBuilder.build();
        return DataResult.success(Structure.parametricallyKeyed(
            key2,
            new MinecraftKeys.DataComponentTypeHolder<>(type),
            Identity::unbox,
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
