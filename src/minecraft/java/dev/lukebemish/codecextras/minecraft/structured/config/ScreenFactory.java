package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;

public interface ScreenFactory<T> {
    Screen open(Screen parent, DynamicOps<JsonElement> ops, JsonElement original, Consumer<JsonElement> onClose, EntryCreationInfo<T> entry);
}
