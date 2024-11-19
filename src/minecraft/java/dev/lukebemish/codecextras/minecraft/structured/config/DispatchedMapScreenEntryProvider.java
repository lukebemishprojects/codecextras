package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

class DispatchedMapScreenEntryProvider<K, V> implements ScreenEntryProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ConfigScreenEntry<K> keyEntry;
    private final List<String> keys;
    private final Map<String, Supplier<DataResult<ConfigScreenEntry<? extends V>>>> keyProviders;
    private final List<Pair<Optional<String>, JsonElement>> values = new ArrayList<>();
    private final JsonObject jsonValue = new JsonObject();
    private final Consumer<JsonElement> update;
    private final EntryCreationContext context;

    public DispatchedMapScreenEntryProvider(ConfigScreenEntry<K> keyEntry, JsonElement jsonValue, Consumer<JsonElement> update, EntryCreationContext context, Map<K, Supplier<DataResult<ConfigScreenEntry<? extends V>>>> entries) {
        this.keyEntry = keyEntry;
        if (jsonValue.isJsonObject()) {
            jsonValue.getAsJsonObject().entrySet().stream()
                .map(e -> Pair.of(Optional.of(e.getKey()), e.getValue()))
                .forEach(values::add);
            updateValue();
        } else {
            if (!jsonValue.isJsonNull()) {
                LOGGER.error("Value {} was not a JSON array", jsonValue);
            }
        }
        this.update = update;
        this.context = context;
        this.keys = new ArrayList<>();
        this.keyProviders = new HashMap<>();
        for (var entry : entries.entrySet()) {
            var keyResult = keyEntry.entryCreationInfo().codec().encodeStart(context.ops(), entry.getKey());
            if (keyResult.isError()) {
                LOGGER.error("Failed to encode key {}", entry.getKey());
                continue;
            }
            JsonElement keyElement = keyResult.getOrThrow();
            if (!keyElement.isJsonPrimitive()) {
                LOGGER.error("Key {} was not a JSON primitive", keyElement);
                continue;
            } else if (!keyElement.getAsJsonPrimitive().isString()) {
                LOGGER.error("Key {} was not a JSON string", keyElement);
                continue;
            }
            String keyAsString = keyElement.getAsString();
            keyProviders.put(keyAsString, entry.getValue());
            this.keys.add(keyAsString);
        }
        this.keys.sort(Comparator.naturalOrder());
    }

    private void updateValue() {
        jsonValue.entrySet().clear();
        for (var pair : values) {
            if (pair.getFirst().isEmpty()) {
                continue;
            }
            if (jsonValue.has(pair.getFirst().get())) {
                LOGGER.warn("Duplicate key {}", pair.getFirst().get());
            }
            jsonValue.add(pair.getFirst().get(), pair.getSecond());
        }
    }

    @Override
    public void onExit(EntryCreationContext context) {
        this.update.accept(jsonValue);
    }

    @Override
    public void addEntries(ScreenEntryList list, Runnable rebuild, Screen parent) {
        var fullWidth = Button.DEFAULT_WIDTH*2+ EntryListScreen.Entry.SPACING;
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
            var keyValue = values.get(index).getFirst();
            var keyLayout = keyEntry.layout().create(
                parent,
                keyAndValueWidth,
                context,
                keyValue.<JsonElement>map(JsonPrimitive::new).orElse(JsonNull.INSTANCE),
                newKeyValue -> {
                    String stringKeyValue = newKeyValue.isJsonNull() ? null : newKeyValue.isJsonPrimitive() && newKeyValue.getAsJsonPrimitive().isString() ? newKeyValue.getAsJsonPrimitive().getAsString() : null;
                    boolean shouldRebuild = keyProviders.containsKey(stringKeyValue);
                    if (!Objects.equals(stringKeyValue, keyValue.orElse(null))) {
                        values.set(index, Pair.of(Optional.ofNullable(stringKeyValue), JsonNull.INSTANCE));
                        updateValue();
                        if (shouldRebuild) {
                            rebuild.run();
                        }
                    }
                },
                keyEntry.entryCreationInfo(),
                false
            );
            layout.addChild(keyLayout, LayoutSettings.defaults().alignVerticallyMiddle());
            var valueEntry = keyValue.map(keyProviders::get).orElse(null);
            if (valueEntry != null) {
                var valueScreenEntry = valueEntry.get();
                if (valueScreenEntry.isError()) {
                    LOGGER.error("Failed to create screen entry for key {}: {}", keyValue.orElseThrow(), valueScreenEntry.error().orElseThrow().message());
                } else {
                    addValueEntry(parent, layout, valueScreenEntry.getOrThrow(), keyAndValueWidth, index);
                }
            } else {
                var disabled = Button.builder(Component.translatable("codecextras.config.dispatchedmap.icon.disabled"), b -> {}).width(keyAndValueWidth).build();
                disabled.active = false;
                layout.addChild(disabled, LayoutSettings.defaults().alignVerticallyMiddle());
            }
            list.addSingle(layout);
        }
        var addLayout = new FrameLayout(fullWidth, 0);
        addLayout.addChild(Button.builder(Component.translatable("codecextras.config.list.icon.add"), b -> {
            values.add(new Pair<>(Optional.empty(), JsonNull.INSTANCE));
            updateValue();
            rebuild.run();
        }).width(Button.DEFAULT_HEIGHT).tooltip(Tooltip.create(Component.translatable("codecextras.config.list.add"))).build(), LayoutSettings.defaults().alignHorizontallyLeft().alignVerticallyMiddle());
        list.addSingle(addLayout);
    }

    private <X extends V> void addValueEntry(Screen parent, EqualSpacingLayout layout, ConfigScreenEntry<X> valueEntry, int keyAndValueWidth, int index) {
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
    }
}
