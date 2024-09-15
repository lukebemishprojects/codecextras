package dev.lukebemish.codecextras.types;

import java.util.function.Supplier;

public interface PreparableView<T> extends Supplier<T> {
    boolean isReady();
}
