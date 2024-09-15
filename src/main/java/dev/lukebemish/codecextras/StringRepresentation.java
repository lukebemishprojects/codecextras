package dev.lukebemish.codecextras;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * A representation of a type with finite possible values as strings. Values of the type should be comparable by identity
 * @param values provides the possible (ordered) values of the type
 * @param representation converts a value to a string
 * @param <T> the type of the values
 */
public record StringRepresentation<T>(Supplier<List<T>> values, Function<T, String> representation, Function<String, @Nullable T> inverse, boolean identity) implements App<StringRepresentation.Mu, T> {
    public static final class Mu implements K1 {
        private Mu() {
        }
    }

    public StringRepresentation(Supplier<List<T>> values, Function<T, String> representation) {
        this(values, representation, memoizeInverse(representation, values), false);
    }

    private static <T> Function<String, @Nullable T> memoizeInverse(Function<T, String> representation, Supplier<List<T>> values) {
        Supplier<Map<String, T>> mapSupplier = Suppliers.memoize(() -> {
            var map = new HashMap<String, T>();
            for (var value : values.get()) {
                map.put(representation.apply(value), value);
            }
            return map;
        });
        return t -> mapSupplier.get().get(t);
    }

    public static <T> StringRepresentation<T> ofArray(Supplier<T[]> values, Function<T, String> representation) {
        Supplier<List<T>> listSupplier = () -> List.of(values.get());
        return new StringRepresentation<>(listSupplier, representation);
    }

    public static <E> StringRepresentation<E> unbox(App<Mu, E> box) {
        return (StringRepresentation<E>) box;
    }

    public Codec<T> codec() {
        return Codec.lazyInitialized(() -> {
            var values = this.values().get();
            Function<T, String> toString;
            if (values.size() > 16) {
                toString = this.representation();
            } else {
                Map<T, String> representationMap = identity() ? new IdentityHashMap<>() : new HashMap<>();
                for (var value : values) {
                    representationMap.put(value, this.representation().apply(value));
                }
                toString = representationMap::get;
            }
            return Codec.STRING.comapFlatMap(string -> {
                T value = inverse.apply(string);
                if (value == null) {
                    return DataResult.error(() -> "Unknown string representation value: " + string);
                }
                return DataResult.success(value);
            }, toString);
        });
    }
}
