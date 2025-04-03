package dev.lukebemish.codecextras.structured;

import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.Asymmetry;
import dev.lukebemish.codecextras.mutable.DataElement;
import dev.lukebemish.codecextras.mutable.GenericDataElementType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface StructuredDataElementType<D, T> extends GenericDataElementType<D, T> {
    Structure<T> structure();

    static <D, T> StructuredDataElementType<D, T> create(String name, Structure<T> structure, Function<D, DataElement<T>> getter) {
        return new StructuredDataElementType<>() {
            @Override
            public Structure<T> structure() {
                return structure;
            }

            @Override
            public DataElement<T> from(D data) {
                return getter.apply(data);
            }

            @Override
            public String name() {
                return name;
            }
        };
    }

    @SafeVarargs
    static <D> Structure<Asymmetry<Consumer<D>, D>> structure(boolean encodeFull, StructuredDataElementType<D, ?>... elements) {
        List<StructuredDataElementType<D, ?>> list = List.of(elements);
        return structure(encodeFull, list);
    }

    static <D> Structure<Asymmetry<Consumer<D>, D>> structure(boolean encodeFull, List<? extends StructuredDataElementType<D, ?>> elements) {
        Map<String, StructuredDataElementType<D, ?>> elementTypeMap = new HashMap<>();

        for (var element : elements) {
            if (elementTypeMap.containsKey(element.name())) {
                throw new IllegalArgumentException("Duplicate name for DataElementType: " + element.name());
            }
            elementTypeMap.put(element.name(), element);
        }

        return Structure.flatRecord(builder -> {
            record Mutation<D, T>(StructuredDataElementType<D, T> elementType, T value) {
                public void set(D data) {
                    elementType.from(data).set(value);
                }

                static <D, T> RecordStructure.Key<Optional<DataResult<Mutation<D, T>>>> of(boolean encodeFull, StructuredDataElementType<D, T> elementType, RecordStructure<Asymmetry<Consumer<D>, D>> asymmetryBuilder) {
                    return asymmetryBuilder.addOptional(
                        elementType.name(),
                        elementType.structure().flatComapMap(
                            t -> DataResult.success(new Mutation<>(elementType, t)),
                            r -> r.map(Mutation::value)
                        ),
                        asymmetry -> {
                            DataResult<Optional<Mutation<D, T>>> nested = asymmetry.encoding().map(d ->
                                elementType.from(d).ifEncodingOrElse(encodeFull, t ->
                                    Optional.of(new Mutation<>(elementType, t)),
                                    Optional::empty
                                )
                            );
                            return nested.mapOrElse(
                                optional -> optional.map(DataResult::success),
                                error -> Optional.of(DataResult.error(error.messageSupplier()))
                            );
                        }
                    );
                }
            }

            Map<Object, RecordStructure.Key<? extends Optional<? extends DataResult<? extends Mutation<D, ?>>>>> containerKeys = new IdentityHashMap<>();
            List<Object> keysInOrder = new ArrayList<>();

            for (var element : elements) {
                RecordStructure.Key<? extends Optional<? extends DataResult<? extends Mutation<D, ?>>>> containerKey = Mutation.of(encodeFull, element, builder);

                var key = new Object();
                containerKeys.put(key, containerKey);
                keysInOrder.add(key);
            }

            return container -> {
                Map<Object, Mutation<D, ?>> mutations = new IdentityHashMap<>();
                List<Object> foundKeys = new ArrayList<>();
                List<Supplier<String>> errors = new ArrayList<>();
                for (var key : keysInOrder) {
                    var containerKey = containerKeys.get(key);
                    var value = containerKey.apply(container);
                    value.ifPresent(result -> {
                        result.ifError(e -> errors.add(e.messageSupplier()));
                        result.ifSuccess(mutation -> {
                            mutations.put(key, mutation);
                            foundKeys.add(key);
                        });
                    });
                }
                Consumer<D> consumer = d -> {
                    for (var key : foundKeys) {
                        mutations.get(key).set(d);
                    }
                };
                if (errors.isEmpty()) {
                    return DataResult.success(Asymmetry.ofDecoding(consumer));
                } else {
                    var error = errors.getFirst();
                    var result = DataResult.<Asymmetry<Consumer<D>, D>>error(error, Asymmetry.ofDecoding(consumer));
                    for (var e : errors.subList(1, errors.size())) {
                        result = result.mapError(s -> s + "; " + e.get());
                    }
                    return result;
                }
            };
        });
    }
}
