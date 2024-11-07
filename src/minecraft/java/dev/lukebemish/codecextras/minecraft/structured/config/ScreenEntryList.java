package dev.lukebemish.codecextras.minecraft.structured.config;

import net.minecraft.client.gui.layouts.LayoutElement;

public interface ScreenEntryList {
    void addPair(LayoutElement left, LayoutElement right);
    void addSingle(LayoutElement layoutElement);
}
