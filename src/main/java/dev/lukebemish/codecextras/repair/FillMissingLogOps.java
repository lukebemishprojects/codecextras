package dev.lukebemish.codecextras.repair;

import com.mojang.serialization.DynamicOps;
import dev.lukebemish.codecextras.companion.AccompaniedOps;
import dev.lukebemish.codecextras.companion.Companion;
import dev.lukebemish.codecextras.companion.DelegatingOps;

public interface FillMissingLogOps<T> extends Companion<T, FillMissingLogOps.RepairLogOpsToken> {
    FillMissingLogOps.RepairLogOpsToken TOKEN = new RepairLogOpsToken();

    static <T> AccompaniedOps<T> of(FillMissingLogOps<T> logOps, DynamicOps<T> delegate) {
        return DelegatingOps.of(TOKEN, logOps, delegate);
    }

    void logMissingField(String field, T original);
    final class RepairLogOpsToken implements Companion.CompanionToken {
        private RepairLogOpsToken() {}
    }
}
