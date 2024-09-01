package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import java.util.function.Consumer;

public interface ScreenEntryFactory<T> {
    ScreenEntryProvider open(DynamicOps<JsonElement> ops, JsonElement original, Consumer<JsonElement> onClose, EntryCreationInfo<T> entry);
}
