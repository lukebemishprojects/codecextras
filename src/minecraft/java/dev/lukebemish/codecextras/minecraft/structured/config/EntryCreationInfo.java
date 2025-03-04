package dev.lukebemish.codecextras.minecraft.structured.config;

import com.mojang.serialization.Codec;
import dev.lukebemish.codecextras.utility.Lazy;
import java.util.function.UnaryOperator;

public record EntryCreationInfo<T>(Codec<T> codec, Lazy<ComponentInfo> componentInfo) {
    public EntryCreationInfo<T> withComponentInfo(UnaryOperator<ComponentInfo> function) {
        return new EntryCreationInfo<>(this.codec, componentInfo.andThen(function));
    }

    public <A> EntryCreationInfo<A> withCodec(Codec<A> codec) {
        return new EntryCreationInfo<>(codec, this.componentInfo);
    }
}
