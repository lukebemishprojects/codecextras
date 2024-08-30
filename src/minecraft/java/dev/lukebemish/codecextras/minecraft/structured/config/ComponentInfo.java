package dev.lukebemish.codecextras.minecraft.structured.config;

import java.util.Optional;
import net.minecraft.network.chat.Component;

public record ComponentInfo(Optional<Component> maybeTitle, Optional<Component> maybeDescription) {
    private static final ComponentInfo EMPTY = new ComponentInfo(Optional.empty(), Optional.empty());

    public static ComponentInfo empty() {
        return EMPTY;
    }

    public Component title() {
        return maybeTitle.orElseGet(Component::empty);
    }

    public Component description() {
        return maybeDescription.orElseGet(Component::empty);
    }

    public ComponentInfo fallbackTitle(Component fallback) {
        if (maybeTitle.isPresent()) {
            return this;
        } else {
            return new ComponentInfo(Optional.of(fallback), maybeDescription);
        }
    }

    public ComponentInfo fallbackDescription(Component fallback) {
        if (maybeDescription.isPresent()) {
            return this;
        } else {
            return new ComponentInfo(maybeTitle, Optional.of(fallback));
        }
    }
}
