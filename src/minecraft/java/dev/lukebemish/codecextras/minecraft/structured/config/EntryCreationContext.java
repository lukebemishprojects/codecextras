package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.util.Objects;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;

public class EntryCreationContext {
    private final DynamicOps<JsonElement> ops;
    private final RegistryAccess registryAccess;

    private EntryCreationContext(DynamicOps<JsonElement> ops, RegistryAccess registryAccess) {
        this.ops = ops;
        this.registryAccess = registryAccess;
    }

    public DynamicOps<JsonElement> ops() {
        return ops;
    }

    public RegistryAccess registryAccess() {
        return registryAccess;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private DynamicOps<JsonElement> ops = JsonOps.INSTANCE;
        private RegistryAccess registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

        private Builder() {}

        public Builder ops(DynamicOps<JsonElement> ops) {
            this.ops = ops;
            return this;
        }

        public Builder registryAccess(RegistryAccess registryAccess) {
            this.registryAccess = registryAccess;
            return this;
        }

        public EntryCreationContext build() {
            return new EntryCreationContext(Objects.requireNonNull(ops), registryAccess);
        }
    }
}
