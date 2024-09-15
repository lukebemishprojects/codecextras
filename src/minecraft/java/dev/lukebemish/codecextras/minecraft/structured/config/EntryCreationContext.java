package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class EntryCreationContext {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final DynamicOps<JsonElement> ops;
    private final RegistryAccess registryAccess;
    final Map<ProblemMarker, String> problems = new IdentityHashMap<>();

    public static final class ProblemMarker {
        private ProblemMarker() {}
    }

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

    public ProblemMarker problem(@Nullable ProblemMarker old, String message) {
        ProblemMarker marker = old == null ? new ProblemMarker() : old;
        problems.put(marker, message);
        LOGGER.error(message);
        return marker;
    }

    public EntryCreationContext subContext() {
        return new EntryCreationContext(ops, registryAccess);
    }

    public void resolve(@Nullable ProblemMarker problem) {
        if (problem != null) {
            problems.remove(problem);
        }
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
