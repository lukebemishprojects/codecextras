package dev.lukebemish.codecextras.minecraft.structured.config;

import net.minecraft.client.gui.screens.Screen;

public interface ScreenEntryProvider {
    void onExit(EntryCreationContext context);
    void addEntries(ScreenEntryList list, Runnable rebuild, Screen parent);

    static Screen create(ScreenEntryProvider provider, Screen parent, EntryCreationContext context, ComponentInfo componentInfo) {
        return new EntryListScreen(parent, componentInfo.title(), provider, context);
    }
}
