package dev.lukebemish.codecextras.minecraft.companion;

import com.google.auto.service.AutoService;
import com.mojang.serialization.DynamicOps;
import dev.lukebemish.codecextras.companion.AccompaniedOps;
import dev.lukebemish.codecextras.companion.AlternateCompanionRetriever;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import net.minecraft.resources.DelegatingOps;
import net.minecraft.resources.RegistryOps;

@AutoService(AlternateCompanionRetriever.class)
public class RegistryOpsCompanionRetriever implements AlternateCompanionRetriever {
    static final MethodHandle DELEGATE_FIELD;

    static {
        try {
            var lookup = MethodHandles.privateLookupIn(DelegatingOps.class, MethodHandles.lookup());
            DELEGATE_FIELD = lookup.unreflectGetter(DelegatingOps.class.getDeclaredField("delegate"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<AccompaniedOps<T>> locateCompanionDelegate(DynamicOps<T> ops) {
        if (ops instanceof RegistryOps<T> registryOps) {
            try {
                return Optional.of((AccompaniedOps<T>) DELEGATE_FIELD.invoke(registryOps));
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> DynamicOps<T> delegate(DynamicOps<T> ops, AccompaniedOps<T> delegate) {
        var registryOps = (RegistryOps<T>) ops;
        return registryOps.withParent(delegate);
    }
}
