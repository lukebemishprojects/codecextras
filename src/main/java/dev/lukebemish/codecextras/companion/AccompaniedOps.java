package dev.lukebemish.codecextras.companion;

import com.mojang.serialization.DynamicOps;
import org.jetbrains.annotations.Nullable;

public interface AccompaniedOps<T> extends DynamicOps<T> {
    default <O extends Companion.CompanionToken, C extends Companion<T, O>> @Nullable C getCompanion(O token) {
        return null;
    }
}
