package dev.lukebemish.codecextras.minecraft.structured;

import com.mojang.datafixers.kinds.K1;
import dev.lukebemish.codecextras.stream.structured.StreamCodecInterpreter;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.Keys2;
import dev.lukebemish.codecextras.structured.MapCodecInterpreter;
import dev.lukebemish.codecextras.structured.ParametricKeyedValue;
import dev.lukebemish.codecextras.structured.schema.JsonSchemaInterpreter;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class MinecraftInterpreters {
    private MinecraftInterpreters() {}

    public static final Keys<MapCodecInterpreter.Holder.Mu, Object> MAP_CODEC_KEYS = Keys.<MapCodecInterpreter.Holder.Mu, Object>builder()
        .build();

    public static final Keys2<ParametricKeyedValue.Mu<MapCodecInterpreter.Holder.Mu>, K1, K1> MAP_CODEC_PARAMETRIC_KEYS = Keys2.<ParametricKeyedValue.Mu<MapCodecInterpreter.Holder.Mu>, K1, K1>builder()
        .build();

    public static final Keys<CodecInterpreter.Holder.Mu, Object> CODEC_KEYS = Keys.<CodecInterpreter.Holder.Mu, Object>builder()
        .build();

    public static final Keys2<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1> CODEC_PARAMETRIC_KEYS = Keys2.<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1>builder()
        .build();

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
        .build();

    public static final Keys2<ParametricKeyedValue.Mu<StreamCodecInterpreter.Holder.Mu<FriendlyByteBuf>>, K1, K1> FRIENDLY_STREAM_PARAMETRIC_KEYS = Keys2.<ParametricKeyedValue.Mu<StreamCodecInterpreter.Holder.Mu<FriendlyByteBuf>>, K1, K1>builder()
        .build();

    public static final StreamCodecInterpreter<FriendlyByteBuf> FRIENDLY_STREAM_CODEC_INTERPRETER = new StreamCodecInterpreter<>(
        StreamCodecInterpreter.FRIENDLY_BYTE_BUF_KEY,
        FRIENDLY_STREAM_KEYS,
        FRIENDLY_STREAM_PARAMETRIC_KEYS
    );

    public static final Keys<StreamCodecInterpreter.Holder.Mu<RegistryFriendlyByteBuf>, Object> REGISTRY_STREAM_KEYS = Keys.<StreamCodecInterpreter.Holder.Mu<RegistryFriendlyByteBuf>, Object>builder()
        .build();

    public static final Keys2<ParametricKeyedValue.Mu<StreamCodecInterpreter.Holder.Mu<RegistryFriendlyByteBuf>>, K1, K1> REGISTRY_STREAM_PARAMETRIC_KEYS = Keys2.<ParametricKeyedValue.Mu<StreamCodecInterpreter.Holder.Mu<RegistryFriendlyByteBuf>>, K1, K1>builder()
        .build();

    public static final StreamCodecInterpreter<RegistryFriendlyByteBuf> REGISTRY_STREAM_CODEC_INTERPRETER = new StreamCodecInterpreter<>(
        StreamCodecInterpreter.REGISTRY_FRIENDLY_BYTE_BUF_KEY,
        List.of(FRIENDLY_STREAM_CODEC_INTERPRETER),
        REGISTRY_STREAM_KEYS,
        REGISTRY_STREAM_PARAMETRIC_KEYS
    );

    public static final Keys<JsonSchemaInterpreter.Holder.Mu, Object> JSON_SCHEMA_KEYS = Keys.<JsonSchemaInterpreter.Holder.Mu, Object>builder()
        .build();

    // TODO: Add regex for schemas
    public static final Keys2<ParametricKeyedValue.Mu<JsonSchemaInterpreter.Holder.Mu>, K1, K1> JSON_SCHEMA_PARAMETRIC_KEYS = Keys2.<ParametricKeyedValue.Mu<JsonSchemaInterpreter.Holder.Mu>, K1, K1>builder()
        .build();
}
