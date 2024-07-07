package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public interface Structure<A> {
    <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter);
    default Annotations annotations() {
        return Annotations.empty();
    }

    default <T> Structure<A> annotate(Key<T> key, T value) {
        var outer = this;
        var annotations = annotations().with(key, value);
        return annotatedDelegatingStructure(outer, annotations);
    }

    default Structure<A> annotate(Annotations annotations) {
        var outer = this;
        var combined = annotations().join(annotations);
        return annotatedDelegatingStructure(outer, combined);
    }

    private static <A> Structure<A> annotatedDelegatingStructure(Structure<A> outer, Annotations annotations) {
        final class AnnotatedDelegatingStructure implements Structure<A> {
            final @Nullable AnnotatedDelegatingStructure delegate;

            AnnotatedDelegatingStructure(@Nullable AnnotatedDelegatingStructure delegate) {
                this.delegate = delegate;
            }

            @Override
            public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
                var result = interpretNoAnnotations(interpreter);
                return result.flatMap(r -> interpreter.annotate(r, annotations));
            }

            private <Mu extends K1> DataResult<App<Mu, A>> interpretNoAnnotations(Interpreter<Mu> interpreter) {
                return delegate != null ? delegate.interpretNoAnnotations(interpreter) : outer.interpret(interpreter);
            }

            @Override
            public Annotations annotations() {
                return annotations;
            }
        }

        return new AnnotatedDelegatingStructure(outer instanceof AnnotatedDelegatingStructure annotatedDelegatingStructure ? annotatedDelegatingStructure : null);
    }

    default Structure<List<A>> listOf() {
        var outer = this;
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, List<A>>> interpret(Interpreter<Mu> interpreter) {
                return outer.interpret(interpreter).flatMap(interpreter::list);
            }
        };
    }

    default RecordStructure.Builder<A> fieldOf(String name) {
        return builder -> builder.add(name, this, Function.identity());
    }

    default RecordStructure.Builder<Optional<A>> optionalFieldOf(String name) {
        return builder -> builder.addOptional(name, this, Function.identity());
    }

    default RecordStructure.Builder<A> optionalFieldOf(String name, Supplier<A> defaultValue) {
        return builder -> builder.addOptional(name, this, Function.identity(), defaultValue);
    }

    default <B> Structure<B> flatXmap(Function<A, DataResult<B>> deserializer, Function<B, DataResult<A>> serializer) {
        var outer = this;
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, B>> interpret(Interpreter<Mu> interpreter) {
                return outer.interpret(interpreter).flatMap(app -> interpreter.flatXmap(app, deserializer, serializer));
            }
        };
    }

    default <B> Structure<B> xmap(Function<A, B> deserializer, Function<B, A> serializer) {
        return flatXmap(a -> DataResult.success(deserializer.apply(a)), b -> DataResult.success(serializer.apply(b)));
    }

    static <A> Structure<A> keyed(Key<A> key) {
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
                return interpreter.keyed(key);
            }
        };
    }

    static <A> Structure<A> record(RecordStructure.Builder<A> builder) {
        return RecordStructure.create(builder);
    }

    Structure<Unit> UNIT = keyed(Interpreter.UNIT);
    Structure<Boolean> BOOL = keyed(Interpreter.BOOL);
    Structure<Byte> BYTE = keyed(Interpreter.BYTE);
    Structure<Short> SHORT = keyed(Interpreter.SHORT);
    Structure<Integer> INT = keyed(Interpreter.INT);
    Structure<Long> LONG = keyed(Interpreter.LONG);
    Structure<Float> FLOAT = keyed(Interpreter.FLOAT);
    Structure<Double> DOUBLE = keyed(Interpreter.DOUBLE);
    Structure<String> STRING = keyed(Interpreter.STRING);
}
