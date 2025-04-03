package dev.lukebemish.codecextras.mutable;

import java.util.List;
import java.util.function.Consumer;

public interface GenericDataElementType<D, T> {
    /**
     * {@return a matching {@link DataElement} retrieved from the provided object}
     * @param data the object to retrieve the element from
     */
    DataElement<T> from(D data);

    /**
     * {@return the name of the data type} Used when encoding; should be unique within a given set of data types.
     */
    String name();

    /**
     * {@return a {@link Consumer } that marks all the provided data elements as clean}
     * @param types the data elements to mark as clean
     * @param <D> the type of object containing the data elements
     */
    @SafeVarargs
    static <D> Consumer<D> cleaner(GenericDataElementType<D, ?>... types) {
        List<GenericDataElementType<D, ?>> list = List.of(types);
        return cleaner(list);
    }

    /**
     * {@return a {@link Consumer} that marks all the provided data elements as clean}
     * @param types the data elements to mark as clean
     * @param <D> the type of object containing the data elements
     */
    static <D> Consumer<D> cleaner(List<? extends GenericDataElementType<D, ?>> types) {
        return data -> {
            for (var type : types) {
                type.from(data).setDirty(false);
            }
        };
    }
}
