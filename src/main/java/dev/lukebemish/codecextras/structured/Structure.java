package dev.lukebemish.codecextras.structured;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.types.Flip;
import dev.lukebemish.codecextras.types.Identity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public interface Structure<A> {
    <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter);
    default Keys<Identity.Mu, Object> annotations() {
        return Annotation.empty();
    }

    default <T> Structure<A> annotate(Key<T> key, T value) {
        var outer = this;
        var annotations = annotations().with(key, new Identity<>(value));
        return annotatedDelegatingStructure(outer, annotations);
    }

    default Structure<A> annotate(Keys<Identity.Mu, Object> annotations) {
        var outer = this;
        var combined = annotations().join(annotations);
        return annotatedDelegatingStructure(outer, combined);
    }

    private static <A> Structure<A> annotatedDelegatingStructure(Structure<A> outer, Keys<Identity.Mu, Object> annotations) {
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
            public Keys<Identity.Mu, Object> annotations() {
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

    default <E> Structure<E> dispatch(String key, Function<? super E, ? extends A> function, Supplier<Map<? super A, ? extends Structure<? extends E>>> structures) {
        var structureSupplier = Suppliers.memoize(structures::get);
        var outer = this;
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, E>> interpret(Interpreter<Mu> interpreter) {
                return interpreter.dispatch(key, outer, function, structureSupplier.get());
            }
        };
    }

    static <A> Structure<A> lazy(Supplier<Structure<A>> supplier) {
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
                return supplier.get().interpret(interpreter);
            }
        };
    }

    static <A> Structure<A> keyed(Key<A> key) {
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
                return interpreter.keyed(key);
            }
        };
    }

    static <A> Structure<A> keyed(Key<A> key, Keys<Flip.Mu<A>, K1> keys) {
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
                return interpreter.key().flatMap(k -> keys.get(k).<Flip<Mu, A>>map(Flip::unbox).map(Flip::value))
                    .map(DataResult::success)
                    .orElseGet(() -> interpreter.keyed(key));
            }
        };
    }

    static <MuO extends K1, MuP extends K1, T, A extends App<MuO, T>> Structure<A> parametricallyKeyed(Key2<MuP, MuO> key, App<MuP, T> parameter, Function<App<MuO, T>, A> unboxer) {
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
                return interpreter.parametricallyKeyed(key, parameter).flatMap(app ->
                    interpreter.flatXmap(app, a -> DataResult.success(unboxer.apply(a)), DataResult::success)
                );
            }
        };
    }

    static <MuO extends K1, MuP extends K1, T, A extends App<MuO, T>> Structure<A> parametricallyKeyed(Key2<MuP, MuO> key, App<MuP, T> parameter, Function<App<MuO, T>, A> unboxer, Keys<Flip.Mu<A>, K1> keys) {
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
                return interpreter.key().flatMap(k -> keys.get(k).<Flip<Mu, A>>map(Flip::unbox).map(Flip::value))
                    .map(DataResult::success)
                    .orElseGet(() -> interpreter.parametricallyKeyed(key, parameter).flatMap(app ->
                        interpreter.flatXmap(app, a -> DataResult.success(unboxer.apply(a)), DataResult::success)
                    ));
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
