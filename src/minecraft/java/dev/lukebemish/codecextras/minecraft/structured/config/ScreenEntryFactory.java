package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import java.util.function.Consumer;

public interface ScreenEntryFactory<T> {
    ScreenEntryProvider open(EntryCreationContext context, JsonElement original, Consumer<JsonElement> onClose, EntryCreationInfo<T> entry);
}
