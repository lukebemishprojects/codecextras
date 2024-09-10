package dev.lukebemish.codecextras.companion;

import com.mojang.serialization.DynamicOps;
import java.util.Optional;

public interface AccompaniedOps<T> extends DynamicOps<T> {
    default <O extends Companion.CompanionToken, C extends Companion<T, O>> Optional<C> getCompanion(O token) {
        return Optional.empty();
    }

    static <T> Optional<AccompaniedOps<T>> find(DynamicOps<T> ops) {
        for (var retriever : DelegatingOps.ALTERNATE_COMPANION_RETRIEVERS) {
            var companion = retriever.locateCompanionDelegate(ops);
            if (companion.isPresent()) {
                return companion;
            }
        }
        return Optional.empty();
    }
}
