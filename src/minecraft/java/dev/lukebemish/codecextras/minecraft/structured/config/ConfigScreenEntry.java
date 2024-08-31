package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.DynamicOps;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ConfigScreenEntry<T>(LayoutFactory<T> widget, ScreenFactory<T> screen, EntryCreationInfo<T> entryCreationInfo) implements App<ConfigScreenEntry.Mu, T> {

    public static final class Mu implements K1 { private Mu() {} }

    private static final int FULL_WIDTH = 310;

    public static <T> ConfigScreenEntry<T> unbox(App<ConfigScreenEntry.Mu, T> app) {
        return (ConfigScreenEntry<T>) app;
    }

    public static <T> ConfigScreenEntry<T> single(LayoutFactory<T> first, EntryCreationInfo<T> entryCreationInfo) {
        return new ConfigScreenEntry<>(first, (parent, ops, original, onClose, creationInfo) -> new EntryListScreen(parent, creationInfo.componentInfo().title()) {
            private JsonElement value = original;

            @Override
            protected void addEntries() {
                this.list.addSingle(first.create(this, FULL_WIDTH, ops, value, newValue -> value = newValue, creationInfo, false));
            }

            @Override
            public void onExit() {
                onClose.accept(value);
            }
        }, entryCreationInfo);
    }

    public ConfigScreenEntry<T> withComponentInfo(UnaryOperator<ComponentInfo> function) {
        return new ConfigScreenEntry<>(this.widget, this.screen, this.entryCreationInfo.withComponentInfo(function));
    }

    public <A> ConfigScreenEntry<A> withEntryCreationInfo(Function<EntryCreationInfo<T>, EntryCreationInfo<A>> function, Function<EntryCreationInfo<A>, EntryCreationInfo<T>> reverse) {
        return new ConfigScreenEntry<>(
            (parent, width, ops, original, update, entry, handleOptional) -> {
                var entryCreationInfo = reverse.apply(entry);
                return this.widget.create(parent, width, ops, original, update, entryCreationInfo, handleOptional);
            },
            (parent, ops, original, onClose, entry) -> {
                var entryCreationInfo = reverse.apply(entry);
                return this.screen.open(parent, ops, original, onClose, entryCreationInfo);
            },
            function.apply(this.entryCreationInfo)
        );
    }

    public Screen rootScreen(Screen parent, Consumer<T> onClose, DynamicOps<JsonElement> ops, T initialData) {
        return this.rootScreen(parent, onClose, ops, initialData, LoggerFactory.getLogger(ConfigScreenEntry.class));
    }

    public Screen rootScreen(Screen parent, Consumer<T> onClose, DynamicOps<JsonElement> ops, T initialData, Logger logger) {
        var initial = entryCreationInfo.codec().encodeStart(ops, initialData);
        JsonElement initialJson;
        if (initial.error().isPresent()) {
            logger.warn("Failed to encode `{}`: {}", initialData, initial.error().get().message());
            initialJson = JsonNull.INSTANCE;
        } else {
            initialJson = initial.getOrThrow();
        }
        return screen().open(parent, ops, initialJson, json -> {
            var decoded = entryCreationInfo.codec().parse(ops, json);
            if (decoded.error().isPresent()) {
                logger.warn("Failed to decode `{}`: {}", json, decoded.error().get().message());
            } else {
                onClose.accept(decoded.getOrThrow());
            }
        }, this.entryCreationInfo());
    }
}