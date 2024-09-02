package dev.lukebemish.codecextras.minecraft.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K1;
import dev.lukebemish.codecextras.stream.structured.StreamCodecInterpreter;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.Keys2;
import dev.lukebemish.codecextras.structured.MapCodecInterpreter;
import dev.lukebemish.codecextras.structured.ParametricKeyedValue;
import dev.lukebemish.codecextras.structured.schema.JsonSchemaInterpreter;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public final class MinecraftInterpreters {
    private MinecraftInterpreters() {}

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
        .add(MinecraftKeys.RESOURCE_LOCATION, new CodecInterpreter.Holder<>(ResourceLocation.CODEC))
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
        .add(MinecraftKeys.RESOURCE_KEY, new ParametricKeyedValue<>() {
            @Override
            public <T> App<CodecInterpreter.Holder.Mu, App<MinecraftKeys.ResourceKeyHolder.Mu, T>> convert(App<MinecraftKeys.RegistryKeyHolder.Mu, T> parameter) {
                return new CodecInterpreter.Holder<>(ResourceKey.codec(MinecraftKeys.RegistryKeyHolder.unbox(parameter).value()).xmap(MinecraftKeys.ResourceKeyHolder::new, a -> MinecraftKeys.ResourceKeyHolder.unbox(a).value()));
            }
        })
        .add(MinecraftKeys.TAG_KEY, new ParametricKeyedValue<>() {
            @Override
            public <T> App<CodecInterpreter.Holder.Mu, App<MinecraftKeys.TagKeyHolder.Mu, T>> convert(App<MinecraftKeys.RegistryKeyHolder.Mu, T> parameter) {
                return new CodecInterpreter.Holder<>(TagKey.codec(MinecraftKeys.RegistryKeyHolder.unbox(parameter).value()).xmap(MinecraftKeys.TagKeyHolder::new, a -> MinecraftKeys.TagKeyHolder.unbox(a).value()));
            }
        })
        .add(MinecraftKeys.HASHED_TAG_KEY, new ParametricKeyedValue<>() {
            @Override
            public <T> App<CodecInterpreter.Holder.Mu, App<MinecraftKeys.TagKeyHolder.Mu, T>> convert(App<MinecraftKeys.RegistryKeyHolder.Mu, T> parameter) {
                return new CodecInterpreter.Holder<>(TagKey.hashedCodec(MinecraftKeys.RegistryKeyHolder.unbox(parameter).value()).xmap(MinecraftKeys.TagKeyHolder::new, a -> MinecraftKeys.TagKeyHolder.unbox(a).value()));
            }
        })
        .add(MinecraftKeys.HOMOGENOUS_LIST_KEY, new ParametricKeyedValue<>() {
            @Override
            public <T> App<CodecInterpreter.Holder.Mu, App<MinecraftKeys.HolderSetHolder.Mu, T>> convert(App<MinecraftKeys.RegistryKeyHolder.Mu, T> parameter) {
                return new CodecInterpreter.Holder<>(RegistryCodecs.homogeneousList(MinecraftKeys.RegistryKeyHolder.unbox(parameter).value()).xmap(MinecraftKeys.HolderSetHolder::new, a -> MinecraftKeys.HolderSetHolder.unbox(a).value()));
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
        .add(MinecraftKeys.RESOURCE_LOCATION, new StreamCodecInterpreter.Holder<>(ResourceLocation.STREAM_CODEC.cast()))
        .build();

    public static final Keys2<ParametricKeyedValue.Mu<StreamCodecInterpreter.Holder.Mu<FriendlyByteBuf>>, K1, K1> FRIENDLY_STREAM_PARAMETRIC_KEYS = Keys2.<ParametricKeyedValue.Mu<StreamCodecInterpreter.Holder.Mu<FriendlyByteBuf>>, K1, K1>builder()
        .add(MinecraftKeys.RESOURCE_KEY, new ParametricKeyedValue<>() {
            @Override
            public <T> App<StreamCodecInterpreter.Holder.Mu<FriendlyByteBuf>, App<MinecraftKeys.ResourceKeyHolder.Mu, T>> convert(App<MinecraftKeys.RegistryKeyHolder.Mu, T> parameter) {
                return new StreamCodecInterpreter.Holder<>(
                    ResourceKey.streamCodec(MinecraftKeys.RegistryKeyHolder.unbox(parameter).value()).<App<MinecraftKeys.ResourceKeyHolder.Mu, T>>map(MinecraftKeys.ResourceKeyHolder::new, a -> MinecraftKeys.ResourceKeyHolder.unbox(a).value()).cast()
                );
            }
        })
        .build();

    public static final StreamCodecInterpreter<FriendlyByteBuf> FRIENDLY_STREAM_CODEC_INTERPRETER = new StreamCodecInterpreter<>(
        StreamCodecInterpreter.FRIENDLY_BYTE_BUF_KEY,
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
        StreamCodecInterpreter.REGISTRY_FRIENDLY_BYTE_BUF_KEY,
        REGISTRY_STREAM_KEYS,
        REGISTRY_STREAM_PARAMETRIC_KEYS
    );

    public static final Keys<JsonSchemaInterpreter.Holder.Mu, Object> JSON_SCHEMA_KEYS = Keys.<JsonSchemaInterpreter.Holder.Mu, Object>builder()
        .add(MinecraftKeys.RESOURCE_LOCATION, new JsonSchemaInterpreter.Holder<>(JsonSchemaInterpreter.STRING.get()))
        .build();

    // TODO: Add regex fo schemas
    public static final Keys2<ParametricKeyedValue.Mu<JsonSchemaInterpreter.Holder.Mu>, K1, K1> JSON_SCHEMA_PARAMETRIC_KEYS = Keys2.<ParametricKeyedValue.Mu<JsonSchemaInterpreter.Holder.Mu>, K1, K1>builder()
        .add(MinecraftKeys.RESOURCE_KEY, new ParametricKeyedValue<>() {
            @Override
            public <T> App<JsonSchemaInterpreter.Holder.Mu, App<MinecraftKeys.ResourceKeyHolder.Mu, T>> convert(App<MinecraftKeys.RegistryKeyHolder.Mu, T> parameter) {
                return new JsonSchemaInterpreter.Holder<>(JsonSchemaInterpreter.STRING.get());
            }
        })
        .add(MinecraftKeys.TAG_KEY, new ParametricKeyedValue<>() {
            @Override
            public <T> App<JsonSchemaInterpreter.Holder.Mu, App<MinecraftKeys.TagKeyHolder.Mu, T>> convert(App<MinecraftKeys.RegistryKeyHolder.Mu, T> parameter) {
                return new JsonSchemaInterpreter.Holder<>(JsonSchemaInterpreter.STRING.get());
            }
        })
        .add(MinecraftKeys.HASHED_TAG_KEY, new ParametricKeyedValue<>() {
            @Override
            public <T> App<JsonSchemaInterpreter.Holder.Mu, App<MinecraftKeys.TagKeyHolder.Mu, T>> convert(App<MinecraftKeys.RegistryKeyHolder.Mu, T> parameter) {
                return new JsonSchemaInterpreter.Holder<>(JsonSchemaInterpreter.STRING.get());
            }
        })
        .build();
}
