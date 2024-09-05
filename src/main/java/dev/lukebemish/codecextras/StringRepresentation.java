package dev.lukebemish.codecextras;

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

/**
 * A representation of a type with finite possible values as strings. Values of the type should be comparable by identity
 * @param values provides the possible (ordered) values of the type
 * @param representation converts a value to a string
 * @param <T> the type of the values
 */
public record StringRepresentation<T>(Supplier<List<T>> values, Function<T, String> representation) implements App<StringRepresentation.Mu, T> {
    public static final class Mu implements K1 {
        private Mu() {
        }
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
            var map = new HashMap<String, T>();
            for (var value : values) {
                map.put(this.representation().apply(value), value);
            }
            Function<T, String> toString;
            if (values.size() > 16) {
                toString = this.representation();
            } else {
                Map<T, String> representationMap = new IdentityHashMap<>();
                for (var entry : map.entrySet()) {
                    representationMap.put(entry.getValue(), entry.getKey());
                }
                toString = representationMap::get;
            }
            return Codec.STRING.comapFlatMap(string -> {
                T value = map.get(string);
                if (value == null) {
                    return DataResult.error(() -> "Unknown string representation value: " + string);
                }
                return DataResult.success(value);
            }, toString);
        });
    }
}
