package dev.lukebemish.codecextras.minecraft.structured.config;

import dev.lukebemish.codecextras.structured.Key;
import net.minecraft.network.chat.Component;

public final class ConfigAnnotations {
    private ConfigAnnotations() {}

    public static Key<Component> TITLE = Key.create("title");
    public static Key<Component> DESCRIPTION = Key.create("description");
}
