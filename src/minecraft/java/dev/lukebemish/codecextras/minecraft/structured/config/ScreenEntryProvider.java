package dev.lukebemish.codecextras.minecraft.structured.config;

import net.minecraft.client.gui.screens.Screen;

public interface ScreenEntryProvider {
    void onExit();
    void addEntries(ScreenEntryList list, Runnable rebuild, Screen parent);

    static Screen create(ScreenEntryProvider provider, Screen parent, ComponentInfo componentInfo) {
        return new EntryListScreen(parent, componentInfo.title(), provider);
    }
}
