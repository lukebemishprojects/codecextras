package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.Comparator;
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
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

class DispatchScreenEntryProvider<K, T> implements ScreenEntryProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final EntryCreationInfo<K> keyInfo;
    private final String key;
    private @Nullable String keyValue;
    private JsonObject jsonValue;
    private final Consumer<JsonElement> update;
    private final List<String> keys;
    private final Map<String, JsonElement> keyValues;
    private final Map<String, ConfigScreenEntry<? extends T>> keyProviders;
    private final DynamicOps<JsonElement> ops;

    public DispatchScreenEntryProvider(EntryCreationInfo<K> keyInfo, JsonElement jsonValue, String key, Consumer<JsonElement> update, DynamicOps<JsonElement> ops, Map<K, DataResult<ConfigScreenEntry<? extends T>>> entries) {
        this.keyInfo = keyInfo;
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
        this.ops = ops;
        this.keys = new ArrayList<>();
        this.keyValues = new HashMap<>();
        this.keyProviders = new HashMap<>();
        for (var entry : entries.entrySet()) {
            var keyResult = keyInfo.codec().encodeStart(ops, entry.getKey());
            if (keyResult.isError()) {
                LOGGER.warn("Failed to encode key {}", entry.getKey());
                continue;
            }
            JsonElement keyElement = keyResult.getOrThrow();
            String keyAsString = stringify(keyElement);
            keyValues.put(keyAsString, keyElement);
            if (entry.getValue().isError()) {
                LOGGER.warn("Failed to create screen entry for key {}: {}", entry.getKey(), entry.getValue().error().orElseThrow().message());
            }
            keyProviders.put(keyAsString, entry.getValue().getOrThrow());
            this.keys.add(keyAsString);
        }
        this.keys.sort(Comparator.naturalOrder());
        if (this.jsonValue.has(key)) {
            this.keyValue = stringify(this.jsonValue.get(key));
        }
    }

    private String stringify(JsonElement keyElement) {
        String keyAsString;
        if (keyElement.isJsonPrimitive()) {
            keyAsString = keyElement.getAsString();
        } else {
            keyAsString = keyElement.toString();
        }
        return keyAsString;
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
        var label = new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, keyInfo.componentInfo().title(), Minecraft.getInstance().font).alignLeft();
        var contents = Button.builder(Component.literal(keyValue == null ? "" : keyValue), b -> {
            Minecraft.getInstance().setScreen(new DispatchPickScreen(parent, keyInfo.componentInfo().title(), keys, keyValue, newKeyValue -> {
                if (!Objects.equals(newKeyValue, keyValue)) {
                    keyValue = newKeyValue;
                    jsonValue = new JsonObject();
                    if (keyValue != null) {
                        jsonValue.add(key, keyValues.get(keyValue));
                    } else {
                        jsonValue.remove(key);
                    }
                    rebuild.run();
                }
            }));
        }).build();
        keyInfo.componentInfo().maybeDescription().ifPresent(description -> {
            var tooltip = Tooltip.create(description);
            label.setTooltip(tooltip);
            contents.setTooltip(tooltip);
        });
        list.addPair(label, contents);
        if (keyValue != null) {
            var provider = keyProviders.get(keyValue);
            JsonObject valueCopy = new JsonObject();
            valueCopy.asMap().putAll(jsonValue.asMap());
            addEntry(provider, valueCopy, list, rebuild, parent);
        }
    }

    private <F extends T> void addEntry(ConfigScreenEntry<F> provider, JsonObject valueCopy, ScreenEntryList list, Runnable rebuild, Screen parent) {
        var entryProvider = provider.screenEntryProvider().open(ops, valueCopy, newValue -> {
            if (newValue.isJsonObject()) {
                for (var entry : newValue.getAsJsonObject().entrySet()) {
                    if (entry.getKey().equals(key)) {
                        continue;
                    }
                    jsonValue.add(entry.getKey(), entry.getValue());
                }
            } else {
                LOGGER.warn("Value {} was not a JSON object", newValue);
            }
        }, provider.entryCreationInfo());
        this.nestedOnExit = entryProvider::onExit;
        entryProvider.addEntries(list, rebuild, parent);
    }
}
