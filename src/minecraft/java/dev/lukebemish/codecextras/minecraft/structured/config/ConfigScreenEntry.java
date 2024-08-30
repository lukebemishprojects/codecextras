package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.DynamicOps;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;

public record ConfigScreenEntry<T>(WidgetFactory<T> widget, ScreenFactory<T> screen, EntryCreationInfo<T> entryCreationInfo) implements App<ConfigScreenEntry.Mu, T> {

    public static final class Mu implements K1 { private Mu() {} }

    private static final int FULL_WIDTH = 310;

    public static <T> ConfigScreenEntry<T> unbox(App<ConfigScreenEntry.Mu, T> app) {
        return (ConfigScreenEntry<T>) app;
    }

    public static <T> ConfigScreenEntry<T> single(WidgetFactory<T> first, EntryCreationInfo<T> entryCreationInfo) {
        return new ConfigScreenEntry<>(first, (parent, ops, original, onClose, creationInfo) -> new OptionsSubScreen(parent, Minecraft.getInstance().options, creationInfo.componentInfo().title()) {
            private JsonElement value = original;

            @Override
            protected void addOptions() {
                this.list.addSmall(first.create(this, FULL_WIDTH, ops, value, newValue -> {
                    value = newValue;
                }, creationInfo), null);
            }

            @Override
            public void onClose() {
                onClose.accept(value);
                super.onClose();
            }
        }, entryCreationInfo);
    }

    public ConfigScreenEntry<T> withComponentInfo(UnaryOperator<ComponentInfo> function) {
        return new ConfigScreenEntry<>(this.widget, this.screen, this.entryCreationInfo.withComponentInfo(function));
    }

    public <A> ConfigScreenEntry<A> withEntryCreationInfo(Function<EntryCreationInfo<T>, EntryCreationInfo<A>> function, Function<EntryCreationInfo<A>, EntryCreationInfo<T>> reverse) {
        return new ConfigScreenEntry<>(
            (parent, width, ops, original, update, entry) -> {
                var entryCreationInfo = reverse.apply(entry);
                return this.widget.create(parent, width, ops, original, update, entryCreationInfo);
            },
            (parent, ops, original, onClose, entry) -> {
                var entryCreationInfo = reverse.apply(entry);
                return this.screen.open(parent, ops, original, onClose, entryCreationInfo);
            },
            function.apply(this.entryCreationInfo)
        );
    }

    public Screen rootScreen(Screen parent, Consumer<JsonElement> onClose, DynamicOps<JsonElement> ops, JsonElement initialData) {
        return screen().open(parent, ops, initialData, onClose, this.entryCreationInfo());
    }
}
