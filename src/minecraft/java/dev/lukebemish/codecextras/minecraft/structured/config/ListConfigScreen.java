package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
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

public class ListConfigScreen<T> extends EntryListScreen {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ConfigScreenEntry<T> entry;
    private final JsonArray jsonValue;
    private final Consumer<JsonElement> update;
    private final DynamicOps<JsonElement> ops;

    @Override
    protected void onExit() {
        this.update.accept(jsonValue);
    }

    @Override
    protected void addEntries() {
        var fullWidth = Button.DEFAULT_WIDTH*2+Entry.SPACING;
        for (int i = 0; i < jsonValue.size(); i++) {
            var index = i;
            var layout = new EqualSpacingLayout(fullWidth, 0, EqualSpacingLayout.Orientation.HORIZONTAL);
            // 5 gives us good spacing here
            var remainingWidth = fullWidth - (Button.DEFAULT_HEIGHT + 5)*3;
            layout.addChild(Button.builder(Component.translatable("codecextras.config.list.icon.remove"), b -> {
                jsonValue.remove(index);
                ListConfigScreen.this.rebuildWidgets();
            }).width(Button.DEFAULT_HEIGHT).tooltip(Tooltip.create(Component.translatable("codecextras.config.list.remove"))).build(), LayoutSettings.defaults().alignVerticallyMiddle());
            var upButton = Button.builder(Component.translatable("codecextras.config.list.icon.up"), b -> {
                if (index == 0) {
                    return;
                }
                var oldAbove = jsonValue.get(index - 1);
                var old = jsonValue.get(index);
                jsonValue.set(index - 1, old);
                jsonValue.set(index, oldAbove);
                ListConfigScreen.this.rebuildWidgets();
            }).width(Button.DEFAULT_HEIGHT).tooltip(Tooltip.create(Component.translatable("codecextras.config.list.up"))).build();
            if (index == 0) {
                upButton.active = false;
            }
            layout.addChild(upButton, LayoutSettings.defaults().alignVerticallyMiddle());
            var downButton = Button.builder(Component.translatable("codecextras.config.list.icon.down"), b -> {
                if (index == jsonValue.size()-1) {
                    return;
                }
                var oldBelow = jsonValue.get(index + 1);
                var old = jsonValue.get(index);
                jsonValue.set(index + 1, old);
                jsonValue.set(index, oldBelow);
                ListConfigScreen.this.rebuildWidgets();
            }).width(Button.DEFAULT_HEIGHT).tooltip(Tooltip.create(Component.translatable("codecextras.config.list.down"))).build();
            if (index == jsonValue.size()-1) {
                downButton.active = false;
            }
            layout.addChild(downButton, LayoutSettings.defaults().alignVerticallyMiddle());
            layout.addChild(entry.widget().create(
                this,
                remainingWidth,
                ops,
                jsonValue.get(index),
                newValue -> this.jsonValue.set(index, newValue), entry.entryCreationInfo(),
                false
            ), LayoutSettings.defaults().alignVerticallyMiddle());
            this.list.addSingle(layout);
        }
        var addLayout = new FrameLayout(fullWidth, 0);
        addLayout.addChild(Button.builder(Component.translatable("codecextras.config.list.icon.add"), b -> {
            jsonValue.add(JsonNull.INSTANCE);
            ListConfigScreen.this.rebuildWidgets();
        }).width(Button.DEFAULT_HEIGHT).tooltip(Tooltip.create(Component.translatable("codecextras.config.list.add"))).build(), LayoutSettings.defaults().alignHorizontallyLeft().alignVerticallyMiddle());
        this.list.addSingle(addLayout);
    }

    public ListConfigScreen(Screen screen, EntryCreationInfo<List<T>> creationInfo, ConfigScreenEntry<T> entry, DynamicOps<JsonElement> ops, JsonElement jsonValue, Consumer<JsonElement> update) {
        super(screen, creationInfo.componentInfo().title());
        this.entry = entry;
        if (jsonValue.isJsonArray()) {
            this.jsonValue = jsonValue.getAsJsonArray();
        } else {
            if (!jsonValue.isJsonNull()) {
                LOGGER.warn("Value {} was not a JSON array", jsonValue);
            }
            this.jsonValue = new JsonArray();
        }
        this.update = update;
        this.ops = ops;
    }
}
