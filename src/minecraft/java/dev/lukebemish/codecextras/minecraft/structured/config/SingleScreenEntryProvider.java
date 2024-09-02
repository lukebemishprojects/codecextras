package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

class SingleScreenEntryProvider<T> implements ScreenEntryProvider {
    private static final int FULL_WIDTH = Button.DEFAULT_WIDTH * 2 + EntryListScreen.Entry.SPACING;
    private final DynamicOps<JsonElement> ops;
    private final EntryCreationInfo<T> creationInfo;
    private final Consumer<JsonElement> update;
    private JsonElement value;
    private final LayoutFactory<T> first;

    SingleScreenEntryProvider(JsonElement original, LayoutFactory<T> first, DynamicOps<JsonElement> ops, EntryCreationInfo<T> creationInfo, Consumer<JsonElement> update) {
        this.value = original;
        this.first = first;
        this.ops = ops;
        this.creationInfo = creationInfo;
        this.update = update;
    }

    @Override
    public void onExit() {
        update.accept(value);
    }

    @Override
    public void addEntries(ScreenEntryList list, Runnable rebuild, Screen parent) {
        list.addSingle(first.create(parent, FULL_WIDTH, ops, value, newValue -> value = newValue, creationInfo, false));
    }
}