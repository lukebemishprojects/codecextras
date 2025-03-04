package dev.lukebemish.codecextras.structured.reflective;

import java.util.Collection;
import java.util.Set;

public final class CreationOptions {
    private final Set<CreationOption> options;

    CreationOptions(Collection<CreationOption> options) {
        this.options = Set.copyOf(options);
    }

    public boolean hasOption(CreationOption option) {
        return options.contains(option);
    }
}
