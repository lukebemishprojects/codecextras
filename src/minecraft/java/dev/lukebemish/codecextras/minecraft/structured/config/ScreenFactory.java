package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;

public interface ScreenFactory {
    Screen open(Screen parent, DynamicOps<JsonElement> ops, JsonElement original, Consumer<JsonElement> update, Consumer<JsonElement> onClose, ComponentInfo componentInfo);
}
