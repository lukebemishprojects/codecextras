package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;

/**
 * Represents both a single level of a configuration screen, and the entry it would become when nested in another config
 * screen. To turn into a screen, use {@link ConfigScreenBuilder}.
 * @param layout prevides the layout used when this data is nested in another config screen
 * @param screenEntryProvider provides the entries present on this screen
 * @param entryCreationInfo the information needed to create this entry
 * @param <T> the type of data this entry represents
 */
public record ConfigScreenEntry<T>(LayoutFactory<T> layout, ScreenEntryFactory<T> screenEntryProvider, EntryCreationInfo<T> entryCreationInfo) implements App<ConfigScreenEntry.Mu, T> {

    public static final class Mu implements K1 { private Mu() {} }

    public static <T> ConfigScreenEntry<T> unbox(App<ConfigScreenEntry.Mu, T> app) {
        return (ConfigScreenEntry<T>) app;
    }

    public static <T> ConfigScreenEntry<T> single(LayoutFactory<T> first, EntryCreationInfo<T> entryCreationInfo) {
        return new ConfigScreenEntry<>(first, (context, original, onClose, creationInfo) -> new SingleScreenEntryProvider<>(original, first, context, creationInfo, onClose), entryCreationInfo);
    }

    public ConfigScreenEntry<T> withComponentInfo(UnaryOperator<ComponentInfo> function) {
        return new ConfigScreenEntry<>(this.layout, this.screenEntryProvider, this.entryCreationInfo.withComponentInfo(function));
    }

    public <A> ConfigScreenEntry<A> withEntryCreationInfo(Function<EntryCreationInfo<T>, EntryCreationInfo<A>> function, Function<EntryCreationInfo<A>, EntryCreationInfo<T>> reverse) {
        return new ConfigScreenEntry<>(
            (parent, width, context, original, update, entry, handleOptional) -> {
                var entryCreationInfo = reverse.apply(entry);
                return this.layout.create(parent, width, context, original, update, entryCreationInfo, handleOptional);
            },
            (context, original, onClose, entry) -> {
                var entryCreationInfo = reverse.apply(entry);
                return this.screenEntryProvider.openChecked(context, original, onClose, entryCreationInfo);
            },
            function.apply(this.entryCreationInfo)
        );
    }

    Screen rootScreen(Screen parent, Consumer<T> onClose, EntryCreationContext context, T initialData, Logger logger) {
        var initial = entryCreationInfo.codec().encodeStart(context.ops(), initialData);
        JsonElement initialJson;
        if (initial.error().isPresent()) {
            logger.error("Failed to encode `{}`: {}", initialData, initial.error().get().message());
            initialJson = JsonNull.INSTANCE;
        } else {
            initialJson = initial.getOrThrow();
        }
        var provider = screenEntryProvider().openChecked(context, initialJson, json -> {
            var decoded = entryCreationInfo.codec().parse(context.ops(), json);
            if (decoded.error().isPresent()) {
                logger.error("Failed to decode `{}`: {}", json, decoded.error().get().message());
            } else {
                onClose.accept(decoded.getOrThrow());
            }
        }, this.entryCreationInfo());
        return ScreenEntryProvider.create(provider, parent, context, entryCreationInfo.componentInfo());
    }
}
