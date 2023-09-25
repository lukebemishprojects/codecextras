package dev.lukebemish.codecextras.repair;

import com.mojang.serialization.DynamicOps;
import dev.lukebemish.codecextras.companion.AccompaniedOps;
import dev.lukebemish.codecextras.companion.Companion;
import dev.lukebemish.codecextras.companion.DelegatingOps;
import org.jetbrains.annotations.Nullable;

public interface FillMissingLogOps<T> extends Companion<T, FillMissingLogOps.RepairLogOpsToken> {
    FillMissingLogOps.RepairLogOpsToken TOKEN = new RepairLogOpsToken();

    static <T> AccompaniedOps<T> of(FillMissingLogOps<T> logOps, DynamicOps<T> delegate) {
        return new DelegatingOps<>(delegate) {
            @SuppressWarnings("unchecked")
            @Override
            public <O extends CompanionToken, C extends Companion<T, O>> @Nullable C getCompanion(O token) {
                if (token == FillMissingLogOps.TOKEN) {
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
