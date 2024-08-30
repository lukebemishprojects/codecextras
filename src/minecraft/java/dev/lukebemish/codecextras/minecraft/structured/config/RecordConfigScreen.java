package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

class RecordConfigScreen extends OptionsSubScreen {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final List<RecordEntry<?>> entries;
    private final JsonObject jsonValue;
    private final Consumer<JsonElement> update;
    private final DynamicOps<JsonElement> ops;

    public RecordConfigScreen(Screen screen, Component component, List<RecordEntry<?>> entries, DynamicOps<JsonElement> ops, JsonElement jsonValue, Consumer<JsonElement> update) {
        super(screen, Minecraft.getInstance().options, component);
        this.entries = entries;
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
    }

    @Override
    public void onClose() {
        this.update.accept(jsonValue);
        super.onClose();
    }

    @Override
    protected void addOptions() {
        for (var entry: this.entries) {
            JsonElement specificValue = this.jsonValue.has(entry.key()) ? this.jsonValue.get(entry.key()) : JsonNull.INSTANCE;
            this.list.addSmall(
                new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, entry.entry().componentInfo().title(), font).alignLeft(),
                entry.entry().widget().create(this, Button.DEFAULT_WIDTH, specificValue, newValue -> {
                    if (shouldUpdate(newValue, specificValue, entry)) {
                        this.jsonValue.add(entry.key(), newValue);
                    } else {
                        this.jsonValue.remove(entry.key());
                    }
                }, entry.entry().componentInfo())
            );
        }
    }

    <T> boolean shouldUpdate(JsonElement newValue, JsonElement oldValue, RecordEntry<T> entry) {
        if (newValue.isJsonNull() || newValue.equals(oldValue)) {
            return false;
        }
        var decoded = entry.codec().parse(this.ops, newValue);
        if (decoded.isError()) {
            LOGGER.warn("Could not encode new value {}", newValue);
            return false;
        }
        return entry.shouldEncode().test(decoded.result().orElseThrow());
    }
}
