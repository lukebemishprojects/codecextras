package dev.lukebemish.codecextras.minecraft.structured.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns a list of {@link ConfigScreenEntry}s into a screen that can be opened by the user.
 */
public class ConfigScreenBuilder {
    private record SingleScreen<T>(ConfigScreenEntry<T> screenEntry, Consumer<T> onClose, Supplier<EntryCreationContext> context, Supplier<T> initialData) {}

    private final List<SingleScreen<?>> screens = new ArrayList<>();
    private Logger logger = LoggerFactory.getLogger(ConfigScreenBuilder.class);

    private ConfigScreenBuilder() {}

    public static ConfigScreenBuilder create() {
        return new ConfigScreenBuilder();
    }

    public ConfigScreenBuilder logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public <T> ConfigScreenBuilder add(ConfigScreenEntry<T> entry, Consumer<T> onClose, Supplier<EntryCreationContext> context, Supplier<T> initialData) {
        screens.add(new SingleScreen<>(entry, onClose, context, initialData));
        return this;
    }

    public UnaryOperator<Screen> factory() {
        if (screens.isEmpty()) {
            throw new IllegalStateException("No screens have been added to the builder");
        }
        return parent -> {
            if (screens.size() == 1) {
                var entry = screens.getFirst();
                return openSingleScreen(parent, entry);
            } else {
                return ScreenEntryProvider.create(new ScreenEntryProvider() {
                    @Override
                    public void onExit(EntryCreationContext context) {}

                    @Override
                    public void addEntries(ScreenEntryList list, Runnable rebuild, Screen parent) {
                        for (var screen : screens) {
                            var label = new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, screen.screenEntry().entryCreationInfo().componentInfo().title(), Minecraft.getInstance().font).alignLeft();
                            var button = Button.builder(Component.translatable("codecextras.config.configurerecord"), b -> {
                                var newScreen = openSingleScreen(parent, screen);
                                Minecraft.getInstance().setScreen(newScreen);
                            }).width(Button.DEFAULT_WIDTH).build();
                            screen.screenEntry().entryCreationInfo().componentInfo().maybeDescription().ifPresent(description -> {
                                var tooltip = Tooltip.create(description);
                                label.setTooltip(tooltip);
                                button.setTooltip(tooltip);
                            });
                            list.addPair(label, button);
                        }
                    }
                }, parent, EntryCreationContext.builder().build(), ComponentInfo.empty());
            }
        };
    }

    private <T> Screen openSingleScreen(Screen parent, SingleScreen<T> entry) {
        return entry.screenEntry().rootScreen(parent, entry.onClose(), entry.context().get(), entry.initialData().get(), logger);
    }
}
