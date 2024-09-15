package dev.lukebemish.codecextras.types;

import com.google.common.base.Suppliers;
import java.util.function.Supplier;

public interface Preparable<T> extends PreparableView<T> {
    void prepare();

    static <T> Preparable<T> memoize(Supplier<T> supplier) {
        var memoized = Suppliers.memoize(supplier::get);
        return new Preparable<>() {
            private volatile boolean prepared = false;

            @Override
            public boolean isReady() {
                return prepared;
            }

            @Override
            public T get() {
                if (!prepared) {
                    throw new IllegalStateException("Not ready!");
                }
                return memoized.get();
            }

            @Override
            public void prepare() {
                prepared = true;
            }
        };
    }
}
