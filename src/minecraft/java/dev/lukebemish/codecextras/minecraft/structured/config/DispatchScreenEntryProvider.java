package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

class DispatchScreenEntryProvider<K, T> implements ScreenEntryProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ConfigScreenEntry<K> keyEntry;
    private final String key;
    private JsonElement keyValue = JsonNull.INSTANCE;
    private JsonElement oldKeyValue = JsonNull.INSTANCE;
    private JsonObject jsonValue;
    private final Consumer<JsonElement> update;
    private final List<JsonElement> keys;
    private final Map<JsonElement, ConfigScreenEntry<? extends T>> keyProviders;
    private final EntryCreationContext context;

    public DispatchScreenEntryProvider(ConfigScreenEntry<K> keyEntry, JsonElement jsonValue, String key, Consumer<JsonElement> update, EntryCreationContext context, Map<K, DataResult<ConfigScreenEntry<? extends T>>> entries) {
        this.keyEntry = keyEntry;
        this.key = key;
        if (jsonValue.isJsonObject()) {
            this.jsonValue = jsonValue.getAsJsonObject();
        } else {
            if (!jsonValue.isJsonNull()) {
                LOGGER.warn("Value {} was not a JSON object", jsonValue);
            }
            this.jsonValue = new JsonObject();
        }
        this.update = update;
        this.context = context;
        this.keys = new ArrayList<>();
        this.keyProviders = new HashMap<>();
        for (var entry : entries.entrySet()) {
            var keyResult = keyEntry.entryCreationInfo().codec().encodeStart(context.ops(), entry.getKey());
            if (keyResult.isError()) {
                LOGGER.warn("Failed to encode key {}", entry.getKey());
                continue;
            }
            JsonElement keyElement = keyResult.getOrThrow();
            if (entry.getValue().isError()) {
                LOGGER.warn("Failed to create screen entry for key {}: {}", entry.getKey(), entry.getValue().error().orElseThrow().message());
            }
            keyProviders.put(keyElement, entry.getValue().getOrThrow());
            this.keys.add(keyElement);
        }
        this.keys.sort(JsonComparator.INSTANCE);
        if (this.jsonValue.has(key)) {
            this.keyValue = this.jsonValue.get(key);
        }
    }

    @Override
    public void onExit() {
        if (nestedOnExit != null) {
            nestedOnExit.run();
        }
        update.accept(jsonValue);
    }

    private @Nullable Runnable nestedOnExit;

    @Override
    public void addEntries(ScreenEntryList list, Runnable rebuild, Screen parent) {
        var label = new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, keyEntry.entryCreationInfo().componentInfo().title(), Minecraft.getInstance().font).alignLeft();
        var contents = keyEntry.layout().create(parent, Button.DEFAULT_WIDTH, context, keyValue, newKeyValue -> {
            if (!Objects.equals(newKeyValue, oldKeyValue)) {
                keyValue = newKeyValue;
                boolean shouldRebuild = keyProviders.containsKey(keyValue) && !Objects.equals(oldKeyValue, keyValue);
                if (shouldRebuild) {
                    oldKeyValue = keyValue;
                    jsonValue = new JsonObject();
                }
                if (!keyValue.isJsonNull()) {
                    jsonValue.add(key, keyValue);
                } else {
                    jsonValue.remove(key);
                }
                if (shouldRebuild) {
                    rebuild.run();
                }
            }
        }, keyEntry.entryCreationInfo(), false);
        keyEntry.entryCreationInfo().componentInfo().maybeDescription().ifPresent(description -> {
            var tooltip = Tooltip.create(description);
            label.setTooltip(tooltip);
        });
        list.addPair(label, contents);
        if (!keyValue.isJsonNull()) {
            var provider = keyProviders.get(keyValue);
            if (provider != null) {
                JsonObject valueCopy = new JsonObject();
                valueCopy.asMap().putAll(jsonValue.asMap());
                addEntry(provider, valueCopy, list, rebuild, parent);
            }
        }
    }

    private <F extends T> void addEntry(ConfigScreenEntry<F> provider, JsonObject valueCopy, ScreenEntryList list, Runnable rebuild, Screen parent) {
        var entryProvider = provider.screenEntryProvider().open(context, valueCopy, newValue -> {
            if (newValue.isJsonObject()) {
                for (var entry : newValue.getAsJsonObject().entrySet()) {
                    if (entry.getKey().equals(key)) {
                        continue;
                    }
                    this.jsonValue.add(entry.getKey(), entry.getValue());
                }
            } else {
                LOGGER.warn("Value {} was not a JSON object", newValue);
            }
        }, provider.entryCreationInfo());
        this.nestedOnExit = entryProvider::onExit;
        entryProvider.addEntries(list, rebuild, parent);
    }
}
