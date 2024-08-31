package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import java.util.function.Consumer;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;

public interface LayoutFactory<T> {
    LayoutElement create(Screen parent, int width, DynamicOps<JsonElement> ops, JsonElement original, Consumer<JsonElement> update, EntryCreationInfo<T> creationInfo, boolean handleOptional);
}
