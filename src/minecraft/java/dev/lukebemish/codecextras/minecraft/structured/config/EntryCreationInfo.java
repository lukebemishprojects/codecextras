package dev.lukebemish.codecextras.minecraft.structured.config;

import com.mojang.serialization.Codec;
import java.util.function.UnaryOperator;

public record EntryCreationInfo<T>(Codec<T> codec, ComponentInfo componentInfo) {
    public EntryCreationInfo<T> withComponentInfo(UnaryOperator<ComponentInfo> function) {
        return new EntryCreationInfo<>(this.codec, function.apply(this.componentInfo));
    }

    public <A> EntryCreationInfo<A> withCodec(Codec<A> codec) {
        return new EntryCreationInfo<>(codec, this.componentInfo);
    }
}
