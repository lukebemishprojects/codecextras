package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Used to assemble a set of key-structure pairs, potentially optionally present, into a structure. Most often you will
 * create a {@link RecordStructure.Builder} and pass it to {@link Structure#record(Builder)}.
 * @param <A> The type of the record represented.
 */
public class RecordStructure<A> {
    private final List<Field<A, ?>> fields = new ArrayList<>();
    private final Set<String> fieldNames = new HashSet<>();
    private int count = 0;

    /**
     * When assembling an object from the layout defined in a record structure, values that have been read in for known
     * keys are available in a {@link Container}.
     */
    public static final class Container {
        private final Key<?>[] keys;
        private final Object[] array;

        private Container(Key<?>[] keys, Object[] array) {
            this.array = array;
            this.keys = keys;
        }

        @SuppressWarnings("unchecked")
        private  <T> T get(Key<T> key) {
            if (key.count >= array.length || key != keys[key.count]) {
                throw new IllegalArgumentException("Key does not belong to the container");
            }
            return (T) array[key.count];
        }

        /**
         * {@return a container builder that has not read any keys yet}
         * {@link Interpreter}s may need to assemble a container to use with a record structure.
         */
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final List<Object> values = new ArrayList<>();
            private final List<Key<?>> keys = new ArrayList<>();

            private Builder() {}

            /**
             * Add a key-value pair to the container.
             * @param key the key
             * @param value the value
             * @param <T> the type of the value
             */
            public <T> void add(Key<T> key, T value) {
                keys.add(key);
                values.add(value);
            }

            /**
             * {@return a new container with the keys and values added so far}
             */
            public Container build() {
                return new Container(keys.toArray(Key<?>[]::new), values.toArray());
            }
        }
    }

    /**
     * A handle to the value of a single field as read into a {@link Container}
     * @param <T> the type of the field
     */
    public static final class Key<T> implements Function<Container, T> {
        private final int count;

        private Key(int i) {
            this.count = i;
        }

        @Override
        public T apply(Container container) {
            return container.get(this);
        }
    }

    /**
     * A single field in a record structure.
     * @param <A> the type of the complete type
     * @param <T> the type of the field
     */
    public interface Field<A, T> {
        /**
         * {@return the name of the field}
         */
        String name();

        /**
         * {@return the structure for the field's contents}
         */
        Structure<T> structure();

        /**
         * {@return a function to extract the field's value from the full data type}
         */
        Function<A, T> getter();

        /**
         * {@return how the field should behave if it is missing from data}
         */
        Optional<MissingBehavior<T>> missingBehavior();

        /**
         * {@return the key to access the field's value once it is read in}
         */
        Key<T> key();

        /**
         * Represents how a field should behave if it is missing from data.
         * @param <T> the type of the field
         */
        interface MissingBehavior<T> {
            /**
             * {@return the default value to use if the field is missing}
             */
            Supplier<T> missing();

            /**
             * {@return whether a given field value should be re-encoded or just left missing}
             */
            Predicate<T> predicate();
        }
    }

    private record MissingBehaviorImpl<T>(Supplier<T> missing, Predicate<T> predicate) implements Field.MissingBehavior<T> {}

    private record FieldImpl<A, T>(String name, Structure<T> structure, Function<A, T> getter, Optional<MissingBehavior<T>> missingBehavior, Key<T> key) implements Field<A, T> {}

    /**
     * Add a field to the record structure.
     * @param name the name of the field
     * @param structure the structure of the field's contents
     * @param getter a function to extract the field's value from the full data type
     * @return a key to access the field's value once it is read in to a {@link Container}
     * @param <T> the type of the field
     */
    public <T> Key<T> add(String name, Structure<T> structure, Function<A, T> getter) {
        var key = new Key<T>(count);
        count++;
        fields.add(new FieldImpl<>(name, structure, getter, Optional.empty(), key));
        fieldNames.add(name);
        return key;
    }

    /**
     * Add an optional field to the record structure.
     * @param name the name of the field
     * @param structure the structure of the field's contents
     * @param getter a function to extract the field's value from the full data type
     * @return a key to access the field's value once it is read in to a {@link Container}
     * @param <T> the type of the field
     */
    public <T> Key<Optional<T>> addOptional(String name, Structure<T> structure, Function<A, Optional<T>> getter) {
        var key = new Key<Optional<T>>(count);
        count++;
        fields.add(new FieldImpl<>(
            name,
            structure.flatXmap(
                t -> DataResult.success(Optional.of(t)),
                o -> o.map(DataResult::success).orElseGet(() -> DataResult.error(() ->"Optional default value not handled by interpreter"))
            ),
            getter,
            Optional.of(new MissingBehaviorImpl<>(Optional::empty, Optional::isPresent)),
            key
        ));
        fieldNames.add(name);
        return key;
    }

    /**
     * Add a field to the record structure with a default value. The field will not be encoded if equal to its default value.
     * @param name the name of the field
     * @param structure the structure of the field's contents
     * @param getter a function to extract the field's value from the full data type
     * @param defaultValue the default value to use if the field is missing
     * @return a key to access the field's value once it is read in to a {@link Container}
     * @param <T> the type of the field
     */
    public <T> Key<T> addOptional(String name, Structure<T> structure, Function<A, T> getter, Supplier<T> defaultValue) {
        var key = new Key<T>(count);
        count++;
        fields.add(new FieldImpl<>(name, structure, getter, Optional.of(new MissingBehaviorImpl<>(defaultValue, t -> !t.equals(defaultValue.get()))), key));
        fieldNames.add(name);
        return key;
    }

    /**
     * Add a sub-record to the record structure. All that record's fields will be located at the top level, but can be
     * retrieved as one from a {@link Container}.
     * @param part the record structure to add
     * @param getter a function to extract the sub-record from the full data type
     * @return a function to create the sub-record from a {@link Container}
     * @param <T> the type of the sub-record
     */
    public <T> Function<Container, T> add(RecordStructure.Builder<T> part, Function<A, T> getter) {
        RecordStructure<T> partial = new RecordStructure<>();
        partial.count = this.count;
        var creator = part.build(partial);
        for (var field : partial.fields) {
            partialField(getter, field);
        }
        return creator;
    }

    private <T, F> void partialField(Function<A, T> getter, Field<T, F> field) {
        if (fieldNames.contains(field.name())) {
            throw new IllegalArgumentException("Duplicate field name: " + field.name());
        }
        fields.add(new FieldImpl<>(field.name(), field.structure(), a -> field.getter().apply(getter.apply(a)), field.missingBehavior(), field.key()));
        count++;
        fieldNames.add(field.name());
    }

    /**
     * Turn a record structure builder into a {@link Structure}.
     * @param builder the record structure builder
     * @return a new structure
     * @param <A> the type of the data represented
     */
    static <A> Structure<A> create(RecordStructure.Builder<A> builder) {
        RecordStructure<A> instance = new RecordStructure<>();
        var creator = builder.build(instance);
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
                return interpreter.record(instance.fields, creator);
            }
        };
    }

    /**
     * A builder for a record structure. This is the fundamental unit used to assemble record structures, and much as
     * {@link com.mojang.serialization.MapCodec}s are reusable, record structure builders can be combined and reused via
     * {@link #add(Builder, Function)}.
     * @param <A> the type of the record represented
     */
    @FunctionalInterface
    public interface Builder<A> {
        /**
         * Assemble a record structure for the given type. Should collect {@link Key}s for every field needed and return
         * a function that uses those keys to assemble the final type from a {@link Container}.
         * @param builder a blank record structure to add fields to
         * @return a function to assemble the final type from a {@link Container}
         */
        Function<Container, A> build(RecordStructure<A> builder);
    }
}
