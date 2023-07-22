package dev.lukebemish.codecextras.repair;

import com.mojang.serialization.DynamicOps;
import dev.lukebemish.codecextras.companion.AccompaniedOps;
import dev.lukebemish.codecextras.companion.Companion;
import dev.lukebemish.codecextras.companion.DelegatingOps;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface RepairLogOps<T> extends Companion<T, RepairLogOps.RepairLogOpsToken> {
    RepairLogOps.RepairLogOpsToken TOKEN = new RepairLogOpsToken();

    static <T> AccompaniedOps<T> of(Consumer<String> logger, DynamicOps<T> delegate) {
        RepairLogOps<T> logOps = (field, original) -> logger.accept("Could not parse entry "+original+" for field "+field+"; replacing with default.");
        return new DelegatingOps<>(delegate) {
            @SuppressWarnings("unchecked")
            @Override
            public <O extends CompanionToken, C extends Companion<T, O>> @Nullable C getCompanion(O token) {
                if (token == RepairLogOps.TOKEN) {
                    return (C) logOps;
                }
                return super.getCompanion(token);
            }
        };
    }

    void logMissingField(String field, T original);
    final class RepairLogOpsToken implements Companion.CompanionToken {
        private RepairLogOpsToken() {}
    }
}
