package dev.lukebemish.codecextras.companion;

import com.mojang.serialization.DynamicOps;
import java.util.Optional;

public interface AlternateCompanionRetriever {
    <T> Optional<AccompaniedOps<T>> locateCompanionDelegate(DynamicOps<T> ops);

    <T> DynamicOps<T> delegate(DynamicOps<T> ops, AccompaniedOps<T> delegate);
}
