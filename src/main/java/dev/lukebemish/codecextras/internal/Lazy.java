package dev.lukebemish.codecextras.internal;

import com.google.common.base.Suppliers;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Lazy<T> implements Supplier<T> {
    private final Supplier<T> memoized;

    private Lazy(Supplier<T> memoized) {
        this.memoized = Suppliers.memoize(memoized::get);
    }

    @Override
    public T get() {
        return memoized.get();
    }

    public <S> Lazy<S> andThen(Function<T, S> function) {
        return new Lazy<>(() -> function.apply(get()));
    }

    public static <T> Lazy<T> of(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }
}
