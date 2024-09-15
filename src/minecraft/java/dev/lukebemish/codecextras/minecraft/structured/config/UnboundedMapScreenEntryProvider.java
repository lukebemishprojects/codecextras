package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

class UnboundedMapScreenEntryProvider<K, V> implements ScreenEntryProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ConfigScreenEntry<K> keyEntry;
    private final ConfigScreenEntry<V> valueEntry;
    private final List<Pair<JsonElement, JsonElement>> values = new ArrayList<>();
    private final JsonObject jsonValue = new JsonObject();
    private final Consumer<JsonElement> update;
    private final EntryCreationContext context;

    UnboundedMapScreenEntryProvider(ConfigScreenEntry<K> keyEntry, ConfigScreenEntry<V> valueEntry, EntryCreationContext context, JsonElement jsonValue, Consumer<JsonElement> update) {
        this.keyEntry = keyEntry;
        this.valueEntry = valueEntry;
        if (jsonValue.isJsonObject()) {
            jsonValue.getAsJsonObject().entrySet().stream()
                .<Pair<JsonElement, JsonElement>>map(e -> Pair.of(new JsonPrimitive(e.getKey()), e.getValue()))
                .forEach(values::add);
            updateValue();
        } else {
            if (!jsonValue.isJsonNull()) {
                LOGGER.error("Value {} was not a JSON object", jsonValue);
            }
        }
        this.update = update;
        this.context = context;
    }

    private void updateValue() {
        jsonValue.entrySet().clear();
        for (var pair : values) {
            if (pair.getFirst().isJsonPrimitive()) {
                if (pair.getFirst().getAsJsonPrimitive().isString()) {
                    if (jsonValue.has(pair.getFirst().getAsString())) {
                        LOGGER.warn("Duplicate key {}", pair.getFirst().getAsString());
                    }
                    jsonValue.add(pair.getFirst().getAsString(), pair.getSecond());
                } else {
                    LOGGER.error("Key {} was not a JSON string", pair.getFirst());
                }
            } else if (!pair.getFirst().isJsonNull()) {
                LOGGER.error("Key {} was not a JSON primitive", pair.getFirst());
            }
        }
    }

    @Override
    public void onExit(EntryCreationContext context) {
        this.update.accept(jsonValue);
    }

    @Override
    public void addEntries(ScreenEntryList list, Runnable rebuild, Screen parent) {
        var fullWidth = Button.DEFAULT_WIDTH*2 + EntryListScreen.Entry.SPACING;
        for (int i = 0; i < values.size(); i++) {
            var index = i;
            var layout = new EqualSpacingLayout(fullWidth, 0, EqualSpacingLayout.Orientation.HORIZONTAL);
            // 5 gives us good spacing here
            var keyAndValueWidth = (fullWidth - (Button.DEFAULT_HEIGHT + 5) - 5)/2;
            layout.addChild(Button.builder(Component.translatable("codecextras.config.list.icon.remove"), b -> {
                values.remove(index);
                updateValue();
                rebuild.run();
            }).width(Button.DEFAULT_HEIGHT).tooltip(Tooltip.create(Component.translatable("codecextras.config.list.remove"))).build(), LayoutSettings.defaults().alignVerticallyMiddle());
            layout.addChild(keyEntry.layout().create(
                parent,
                keyAndValueWidth,
                context, values.get(index).getFirst(),
                newKey -> {
                    this.values.set(index, this.values.get(index).mapFirst(old -> newKey));
                    updateValue();
                }, keyEntry.entryCreationInfo(),
                false
            ), LayoutSettings.defaults().alignVerticallyMiddle());
            layout.addChild(valueEntry.layout().create(
                parent,
                keyAndValueWidth,
                context, values.get(index).getSecond(),
                newValue -> {
                    this.values.set(index, this.values.get(index).mapSecond(old -> newValue));
                    updateValue();
                }, valueEntry.entryCreationInfo(),
                false
            ), LayoutSettings.defaults().alignVerticallyMiddle());
            list.addSingle(layout);
        }
        var addLayout = new FrameLayout(fullWidth, 0);
        addLayout.addChild(Button.builder(Component.translatable("codecextras.config.list.icon.add"), b -> {
            values.add(new Pair<>(JsonNull.INSTANCE, JsonNull.INSTANCE));
            updateValue();
            rebuild.run();
        }).width(Button.DEFAULT_HEIGHT).tooltip(Tooltip.create(Component.translatable("codecextras.config.list.add"))).build(), LayoutSettings.defaults().alignHorizontallyLeft().alignVerticallyMiddle());
        list.addSingle(addLayout);
    }
}
