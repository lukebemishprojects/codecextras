package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.DynamicOps;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;

public record OptionsEntry<T>(WidgetFactory widget, ScreenFactory screen, ComponentInfo componentInfo) implements App<OptionsEntry.Mu, T> {
    public static final class Mu implements K1 { private Mu() {} }

    private static final int FULL_WIDTH = 310;

    public static <T> OptionsEntry<T> unbox(App<OptionsEntry.Mu, T> app) {
        return (OptionsEntry<T>) app;
    }

    public static <T> OptionsEntry<T> single(WidgetFactory first, ComponentInfo componentInfo) {
        return new OptionsEntry<>(first, (parent, ops, original, onClose, info) -> new OptionsSubScreen(parent, Minecraft.getInstance().options, info.title()) {
            private JsonElement value = original;

            @Override
            protected void addOptions() {
                this.list.addSmall(first.create(this, FULL_WIDTH, value, newValue -> {
                    value = newValue;
                }, componentInfo), null);
            }

            @Override
            public void onClose() {
                onClose.accept(value);
                super.onClose();
            }
        }, componentInfo);
    }

    public OptionsEntry<T> withComponentInfo(UnaryOperator<ComponentInfo> function) {
        return new OptionsEntry<>(this.widget, this.screen, function.apply(this.componentInfo));
    }

    public Screen rootScreen(Screen parent, Consumer<JsonElement> onClose, DynamicOps<JsonElement> ops, JsonElement initialData) {
        return screen().open(parent, ops, initialData, onClose, componentInfo);
    }
}