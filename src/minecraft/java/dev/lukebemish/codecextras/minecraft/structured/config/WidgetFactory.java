package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

public interface WidgetFactory {
    AbstractWidget create(Screen parent, int width, JsonElement original, Consumer<JsonElement> update, ComponentInfo componentInfo);
}
