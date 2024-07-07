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

public class RecordStructure<A> {
    private final List<Field<A, ?>> fields = new ArrayList<>();
    private final Set<String> fieldNames = new HashSet<>();
    private int count = 0;

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

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final List<Object> values = new ArrayList<>();
            private final List<Key<?>> keys = new ArrayList<>();

            private Builder() {}

            public <T> void add(Key<T> key, T value) {
                keys.add(key);
                values.add(value);
            }

            public Container build() {
                return new Container(keys.toArray(new Key<?>[0]), values.toArray());
            }
        }
    }

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

    public interface Field<A, T> {
        String name();

        Structure<T> structure();

        Function<A, T> getter();

        Optional<MissingBehavior<T>> missingBehavior();

        Key<T> key();

        interface MissingBehavior<T> {
            Supplier<T> missing();
            Predicate<T> predicate();
        }
    }

    private record MissingBehaviorImpl<T>(Supplier<T> missing, Predicate<T> predicate) implements Field.MissingBehavior<T> {}

    private record FieldImpl<A, T>(String name, Structure<T> structure, Function<A, T> getter, Optional<MissingBehavior<T>> missingBehavior, Key<T> key) implements Field<A, T> {}

    public <T> Key<T> add(String name, Structure<T> structure, Function<A, T> getter) {
        var key = new Key<T>(count);
        count++;
        fields.add(new FieldImpl<>(name, structure, getter, Optional.empty(), key));
        fieldNames.add(name);
        return key;
    }

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

    public <T> Key<T> addOptional(String name, Structure<T> structure, Function<A, T> getter, Supplier<T> defaultValue) {
        var key = new Key<T>(count);
        count++;
        fields.add(new FieldImpl<>(name, structure, getter, Optional.of(new MissingBehaviorImpl<>(defaultValue, t -> !t.equals(defaultValue))), key));
        fieldNames.add(name);
        return key;
    }

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

    @FunctionalInterface
    public interface Builder<A> {
        Function<Container, A> build(RecordStructure<A> builder);
    }
}
