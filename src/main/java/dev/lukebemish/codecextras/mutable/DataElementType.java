package dev.lukebemish.codecextras.mutable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.Asymmetry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A tool for retrieving {@link DataElement}s from a holder object, and encoding/decoding them.
 * @param <D> the type of the holder object
 * @param <T> the type of data being retrieved
 */
public interface DataElementType<D, T> {
    /**
     * {@return a matching {@link DataElement} retrieved from the provided object}
     * @param data the object to retrieve the element from
     */
    DataElement<T> from(D data);

    /**
     * {@return the codec to (de)serialize the element}
     */
    Codec<T> codec();

    /**
     * {@return the name of the data type} Used when encoding; should be unique within a given set of data types.
     */
    String name();

    /**
     * {@return a new {@link DataElementType} with the provided name, codec, and getter}
     * @param name the name of the data type
     * @param codec the codec to (de)serialize the data type
     * @param getter a function to retrieve the data element from the object
     * @param <D> the type of the holder object
     * @param <T> the type of the data
     */
    static <D, T> DataElementType<D, T> create(String name, Codec<T> codec, Function<D, DataElement<T>> getter) {
        return new DataElementType<>() {
            @Override
            public DataElement<T> from(D data) {
                return getter.apply(data);
            }

            @Override
            public Codec<T> codec() {
                return codec;
            }

            @Override
            public String name() {
                return name;
            }
        };
    }

    /**
     * {@return a {@link Consumer} that marks all the provided data elements as clean}
     * @param types the data elements to mark as clean
     * @param <D> the type of object containing the data elements
     */
    @SafeVarargs
    static <D> Consumer<D> cleaner(DataElementType<D, ?>... types) {
        List<DataElementType<D, ?>> list = List.of(types);
        return cleaner(list);
    }

    /**
     * {@return a {@link Consumer} that marks all the provided data elements as clean}
     * @param types the data elements to mark as clean
     * @param <D> the type of object containing the data elements
     */
    static <D> Consumer<D> cleaner(List<? extends DataElementType<D, ?>> types) {
        return data -> {
            for (var type : types) {
                type.from(data).setDirty(false);
            }
        };
    }

    /**
     * Creates a {@link Codec} for a series of data elements. This codec will encode from an instance of the type that
     * holds the data elements, and will decode to a {@link Consumer} that can be applied to an instance of that type to
     * set the data elements' values. The codec can encode either the full state of the data elements, or just the changes
     * since the last time they were marked as clean. Elements are encoded as a name to value map.
     * @param encodeFull whether to encode the full state of the data elements
     * @param elements the data elements to encode, which must have unique {@link #name() names}
     * @return a new {@link Codec}
     * @param <D> the type of the holder object
     */
    @SafeVarargs
    static <D> Codec<Asymmetry<Consumer<D>, D>> codec(boolean encodeFull, DataElementType<D, ?>... elements) {
        List<DataElementType<D, ?>> list = List.of(elements);
        return codec(encodeFull, list);
    }

    /**
     * Creates a {@link Codec} for a series of data elements. This codec will encode from an instance of the type that
     * holds the data elements, and will decode to a {@link Consumer} that can be applied to an instance of that type to
     * set the data elements' values. The codec can encode either the full state of the data elements, or just the changes
     * since the last time they were marked as clean. Elements are encoded as a name to value map.
     * @param encodeFull whether to encode the full state of the data elements
     * @param elements the data elements to encode, which must have unique {@link #name() names}
     * @return a new {@link Codec}
     * @param <D> the type of the holder object
     */
    static <D> Codec<Asymmetry<Consumer<D>, D>> codec(boolean encodeFull, List<? extends DataElementType<D, ?>> elements) {
        Map<String, DataElementType<D, ?>> elementTypeMap = new HashMap<>();
        for (var element : elements) {
            if (elementTypeMap.containsKey(element.name())) {
                throw new IllegalArgumentException("Duplicate name for DataElementType: " + element.name());
            }
            elementTypeMap.put(element.name(), element);
        }

        Codec<Map<String, Dynamic<?>>> partial = Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH);

        return Asymmetry.flatSplit(
            partial,
            map -> {
                record Mutation<D, T>(DataElementType<D, T> element, T value) {
                    private static <D, T> DataResult<Mutation<D, T>> of(DataElementType<D, T> type, Dynamic<?> dynamic) {
                        return type.codec().parse(dynamic).map(value -> new Mutation<>(type, value));
                    }

                    public void set(D data) {
                        element.from(data).set(value);
                    }
                }

                List<Mutation<D, ?>> mutations = new ArrayList<>();
                for (var pair : map.entrySet()) {
                    String name = pair.getKey();
                    var type = elementTypeMap.get(name);
                    if (type == null) {
                        return DataResult.error(() -> "Invalid name for DataElementType: " + name);
                    }
                    var mutation = Mutation.of(type, pair.getValue());
                    if (mutation.error().isPresent()) {
                        return DataResult.error(mutation.error().get().messageSupplier());
                    }
                    mutations.add(mutation.result().get());
                }

                return DataResult.success(data -> {
                    for (var mutation : mutations) {
                        mutation.set(data);
                    }
                });
            },
            data -> {
                Map<String, Dynamic<?>> map = new HashMap<>();
                for (DataElementType<D, ?> type : elements) {
                    Optional<DataResult<Map<String, Dynamic<?>>>> result = forElement(type, data, map, encodeFull);
                    if (result.isPresent()) {
                        return result.get();
                    }
                }
                return DataResult.success(map);
            }
        );
    }

    private static <D, T> Optional<DataResult<Map<String, Dynamic<?>>>> forElement(DataElementType<D, T> type, D data, Map<String, Dynamic<?>> map, boolean encodeFull) {
        var element = type.from(data);
        return element.ifEncodingOrElse(encodeFull, value -> {
            var result = type.codec().encodeStart(JsonOps.INSTANCE, element.get()).map(r -> new Dynamic<>(JsonOps.INSTANCE, r));
            if (result.result().isPresent()) {
                map.put(type.name(), result.result().get());
            } else {
                return Optional.of(DataResult.error(result.error().get().messageSupplier(), map));
            }
            return Optional.empty();
        }, Optional::empty);
    }
}
