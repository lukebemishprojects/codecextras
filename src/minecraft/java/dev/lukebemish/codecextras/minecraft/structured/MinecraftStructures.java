package dev.lukebemish.codecextras.minecraft.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.stream.structured.StreamCodecInterpreter;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.schema.JsonSchemaInterpreter;
import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.Key2;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.Keys2;
import dev.lukebemish.codecextras.structured.MapCodecInterpreter;
import dev.lukebemish.codecextras.structured.ParametricKeyedValue;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.schema.SchemaAnnotations;
import dev.lukebemish.codecextras.types.Flip;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public final class MinecraftStructures {
    private MinecraftStructures() {}

    public static final Keys<MapCodecInterpreter.Holder.Mu, Object> MAP_CODEC_KEYS = Keys.<MapCodecInterpreter.Holder.Mu, Object>builder()
        .build();

    public static final Keys2<ParametricKeyedValue.Mu<MapCodecInterpreter.Holder.Mu>, K1, K1> MAP_CODEC_PARAMETRIC_KEYS = Keys2.<ParametricKeyedValue.Mu<MapCodecInterpreter.Holder.Mu>, K1, K1>builder()
        .build();

    public static final Keys<CodecInterpreter.Holder.Mu, Object> CODEC_KEYS = MAP_CODEC_KEYS.<CodecInterpreter.Holder.Mu>map(new Keys.Converter<>() {
        @Override
        public <B> App<CodecInterpreter.Holder.Mu, B> convert(App<MapCodecInterpreter.Holder.Mu, B> app) {
            return new CodecInterpreter.Holder<>(MapCodecInterpreter.unbox(app).codec());
        }
    }).join(Keys.<CodecInterpreter.Holder.Mu, Object>builder()
        .add(Types.RESOURCE_LOCATION, new CodecInterpreter.Holder<>(ResourceLocation.CODEC))
        .build()
    );

    public static final Keys2<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1> CODEC_PARAMETRIC_KEYS = MAP_CODEC_PARAMETRIC_KEYS.map(new Keys2.Converter<ParametricKeyedValue.Mu<MapCodecInterpreter.Holder.Mu>, ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1>() {
        @Override
        public <A extends K1, B extends K1> App2<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, A, B> convert(App2<ParametricKeyedValue.Mu<MapCodecInterpreter.Holder.Mu>, A, B> input) {
            var unboxed = ParametricKeyedValue.unbox(input);
            return new ParametricKeyedValue<>() {
                @Override
                public <T> App<CodecInterpreter.Holder.Mu, App<B, T>> convert(App<A, T> parameter) {
                    var mapCodec = MapCodecInterpreter.unbox(unboxed.convert(parameter));
                    return new CodecInterpreter.Holder<>(mapCodec.codec());
                }
            };
        }
    }).join(Keys2.<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1>builder()
        .add(Types.RESOURCE_KEY, new ParametricKeyedValue<>() {
            @Override
            public <T> App<CodecInterpreter.Holder.Mu, App<Types.ResourceKeyHolder.Mu, T>> convert(App<Types.RegistryKeyHolder.Mu, T> parameter) {
                return new CodecInterpreter.Holder<>(ResourceKey.codec(Types.RegistryKeyHolder.unbox(parameter).value()).xmap(Types.ResourceKeyHolder::new, a -> Types.ResourceKeyHolder.unbox(a).value()));
            }
        })
        .build()
    );

    public static final CodecInterpreter CODEC_INTERPRETER = CodecInterpreter.create().with(
        CODEC_KEYS,
        MAP_CODEC_KEYS,
        CODEC_PARAMETRIC_KEYS,
        MAP_CODEC_PARAMETRIC_KEYS
    );

    public static final MapCodecInterpreter MAP_CODEC_INTERPRETER = MapCodecInterpreter.create().with(
        CODEC_KEYS,
        MAP_CODEC_KEYS,
        CODEC_PARAMETRIC_KEYS,
        MAP_CODEC_PARAMETRIC_KEYS
    );

    public static final Keys<StreamCodecInterpreter.Holder.Mu<FriendlyByteBuf>, Object> FRIENDLY_STREAM_KEYS = Keys.<StreamCodecInterpreter.Holder.Mu<FriendlyByteBuf>, Object>builder()
        .add(Types.RESOURCE_LOCATION, new StreamCodecInterpreter.Holder<>(ResourceLocation.STREAM_CODEC.cast()))
        .build();

    public static final Keys2<ParametricKeyedValue.Mu<StreamCodecInterpreter.Holder.Mu<FriendlyByteBuf>>, K1, K1> FRIENDLY_STREAM_PARAMETRIC_KEYS = Keys2.<ParametricKeyedValue.Mu<StreamCodecInterpreter.Holder.Mu<FriendlyByteBuf>>, K1, K1>builder()
        .add(Types.RESOURCE_KEY, new ParametricKeyedValue<>() {
            @Override
            public <T> App<StreamCodecInterpreter.Holder.Mu<FriendlyByteBuf>, App<Types.ResourceKeyHolder.Mu, T>> convert(App<Types.RegistryKeyHolder.Mu, T> parameter) {
                return new StreamCodecInterpreter.Holder<>(
                    ResourceKey.streamCodec(Types.RegistryKeyHolder.unbox(parameter).value()).<App<Types.ResourceKeyHolder.Mu, T>>map(Types.ResourceKeyHolder::new, a -> Types.ResourceKeyHolder.unbox(a).value()).cast()
                );
            }
        })
        .build();

    public static final StreamCodecInterpreter<FriendlyByteBuf> FRIENDLY_STREAM_CODEC_INTERPRETER = new StreamCodecInterpreter<>(
        FRIENDLY_STREAM_KEYS,
        FRIENDLY_STREAM_PARAMETRIC_KEYS
    );

    public static final Keys<StreamCodecInterpreter.Holder.Mu<RegistryFriendlyByteBuf>, Object> REGISTRY_STREAM_KEYS = FRIENDLY_STREAM_KEYS.map(new Keys.Converter<StreamCodecInterpreter.Holder.Mu<FriendlyByteBuf>, StreamCodecInterpreter.Holder.Mu<RegistryFriendlyByteBuf>, Object>() {
        @Override
        public <A> App<StreamCodecInterpreter.Holder.Mu<RegistryFriendlyByteBuf>, A> convert(App<StreamCodecInterpreter.Holder.Mu<FriendlyByteBuf>, A> input) {
            return new StreamCodecInterpreter.Holder<>(StreamCodecInterpreter.unbox(input).cast());
        }
    }).join(Keys.<StreamCodecInterpreter.Holder.Mu<RegistryFriendlyByteBuf>, Object>builder()
        .build()
    );

    public static final Keys2<ParametricKeyedValue.Mu<StreamCodecInterpreter.Holder.Mu<RegistryFriendlyByteBuf>>, K1, K1> REGISTRY_STREAM_PARAMETRIC_KEYS = FRIENDLY_STREAM_PARAMETRIC_KEYS.map(new Keys2.Converter<ParametricKeyedValue.Mu<StreamCodecInterpreter.Holder.Mu<FriendlyByteBuf>>, ParametricKeyedValue.Mu<StreamCodecInterpreter.Holder.Mu<RegistryFriendlyByteBuf>>, K1, K1>() {
        @Override
        public <A extends K1, B extends K1> App2<ParametricKeyedValue.Mu<StreamCodecInterpreter.Holder.Mu<RegistryFriendlyByteBuf>>, A, B> convert(App2<ParametricKeyedValue.Mu<StreamCodecInterpreter.Holder.Mu<FriendlyByteBuf>>, A, B> input) {
            return new ParametricKeyedValue<>() {
                @Override
                public <T> App<StreamCodecInterpreter.Holder.Mu<RegistryFriendlyByteBuf>, App<B, T>> convert(App<A, T> parameter) {
                    return new StreamCodecInterpreter.Holder<>(StreamCodecInterpreter.unbox(ParametricKeyedValue.unbox(input).convert(parameter)).cast());
                }
            };
        }
    }).join(Keys2.<ParametricKeyedValue.Mu<StreamCodecInterpreter.Holder.Mu<RegistryFriendlyByteBuf>>, K1, K1>builder()
        .build()
    );

    public static final StreamCodecInterpreter<RegistryFriendlyByteBuf> REGISTRY_STREAM_CODEC_INTERPRETER = new StreamCodecInterpreter<>(
        REGISTRY_STREAM_KEYS,
        REGISTRY_STREAM_PARAMETRIC_KEYS
    );

    public static final Keys<JsonSchemaInterpreter.Holder.Mu, Object> JSON_SCHEMA_KEYS = Keys.<JsonSchemaInterpreter.Holder.Mu, Object>builder()
        .add(Types.RESOURCE_LOCATION, new JsonSchemaInterpreter.Holder<>(JsonSchemaInterpreter.STRING.get()))
        .build();

    public static final Keys2<ParametricKeyedValue.Mu<JsonSchemaInterpreter.Holder.Mu>, K1, K1> JSON_SCHEMA_PARAMETRIC_KEYS = Keys2.<ParametricKeyedValue.Mu<JsonSchemaInterpreter.Holder.Mu>, K1, K1>builder()
        .add(Types.RESOURCE_KEY, new ParametricKeyedValue<>() {
            @Override
            public <T> App<JsonSchemaInterpreter.Holder.Mu, App<Types.ResourceKeyHolder.Mu, T>> convert(App<Types.RegistryKeyHolder.Mu, T> parameter) {
                return new JsonSchemaInterpreter.Holder<>(JsonSchemaInterpreter.STRING.get());
            }
        })
        .build();

    public static final class Types {
        private Types() {}

        public static final Key<ResourceLocation> RESOURCE_LOCATION = Key.create("resource_location");

        public record ResourceKeyHolder<T>(ResourceKey<T> value) implements App<ResourceKeyHolder.Mu, T> {
            public static final class Mu implements K1 { private Mu() {} }

            public static <T> ResourceKeyHolder<T> unbox(App<Mu, T> box) {
                return (ResourceKeyHolder<T>) box;
            }
        }

        public record RegistryKeyHolder<T>(ResourceKey<? extends Registry<T>> value) implements App<RegistryKeyHolder.Mu, T> {
            public static final class Mu implements K1 { private Mu() {} }

            public static <T> RegistryKeyHolder<T> unbox(App<Mu, T> box) {
                return (RegistryKeyHolder<T>) box;
            }
        }

        public static final Key2<RegistryKeyHolder.Mu, ResourceKeyHolder.Mu> RESOURCE_KEY = Key2.create("resource_key");
    }

    public static final class Structures {
        private Structures() {}

        public static final Structure<ResourceLocation> RESOURCE_LOCATION = Structure.keyed(
            Types.RESOURCE_LOCATION, Keys.<Flip.Mu<ResourceLocation>, K1>builder()
                .add(CodecInterpreter.KEY, new Flip<>(new CodecInterpreter.Holder<>(ResourceLocation.CODEC)))
                .build()
        );

        public static <T> Structure<ResourceKey<T>> resourceKey(ResourceKey<? extends Registry<T>> registry) {
            return Structure.parametricallyKeyed(
                Types.RESOURCE_KEY,
                    new Types.RegistryKeyHolder<>(registry),
                    Types.ResourceKeyHolder::unbox,
                Keys.<Flip.Mu<Types.ResourceKeyHolder<T>>, K1>builder()
                    .add(
                        CodecInterpreter.KEY,
                        new Flip<>(new CodecInterpreter.Holder<>(
                            ResourceKey.codec(registry).xmap(Types.ResourceKeyHolder::new, Types.ResourceKeyHolder::value)
                        ))
                    )
                    .build()
                ).xmap(Types.ResourceKeyHolder::value, Types.ResourceKeyHolder::new);
        }

        public static <T> Structure<T> registryDispatch(String keyField, Function<T, DataResult<ResourceKey<Structure<? extends T>>>> structureFunction, Registry<Structure<? extends T>> registry) {
            var keyStructure = resourceKey(registry.key());
            return keyStructure.dispatch(keyField, structureFunction, registry::registryKeySet, k ->
                registry.getValueOrThrow(k).annotate(SchemaAnnotations.REUSE_KEY, toDefsKey(k.registry())+"::"+toDefsKey(k.location()))
            );
        }

        private static String toDefsKey(ResourceLocation location) {
            return location.getNamespace().replace('/', '.') + ":" + location.getPath().replace('/', '.');
        }
    }
}
