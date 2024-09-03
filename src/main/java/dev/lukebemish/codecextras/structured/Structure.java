package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.StringRepresentation;
import dev.lukebemish.codecextras.types.Flip;
import dev.lukebemish.codecextras.types.Identity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Represents the structure of a data type in a generic form. This structure can then be interpreted into any number of
 * specific representations, such as a {@link com.mojang.serialization.Codec}, by using the appropriate {@link Interpreter}.
 * @param <A> the type of data this structure represents
 */
public interface Structure<A> {
    /**
     * Use the structure to create a representation of the data type.
     * @param interpreter contains the logic to create a specific type of representation from a structure
     * @return the specific representation of the data type, boxed as an {@link App}, or an error if one could not be created
     * @param <Mu> the type function of the sort of specific representation
     */
    <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter);

    /**
     * {@return the annotations attached to this structure}
     * Annotations are pieces of metadata attached to a structure which an interpreter may optionally use to mark up the
     * result it produces; examples would include comments to attach to a codec that would show up in supported serialized
     * data formats, or the like.
     * @see Annotation
     */
    default Keys<Identity.Mu, Object> annotations() {
        return Annotation.empty();
    }

    /**
     * {@return a new structure with the single provided annotation added}
     * @param key the annotation key
     * @param value the annotation value
     * @param <T> the type of the annotation
     * @see Annotation
     */
    default <T> Structure<A> annotate(Key<T> key, T value) {
        var outer = this;
        var annotations = annotations().with(key, new Identity<>(value));
        return annotatedDelegatingStructure(outer, annotations);
    }

    /**
     * {@return a new structure with the provided annotations added}
     * @param annotations the annotations to add
     * @see Annotation
     */
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
                return interpreter.annotate(original(), annotations);
            }

            private Structure<A> original() {
                return delegate != null ? delegate.original() : outer;
            }

            @Override
            public Keys<Identity.Mu, Object> annotations() {
                return annotations;
            }
        }

        return new AnnotatedDelegatingStructure(outer instanceof AnnotatedDelegatingStructure annotatedDelegatingStructure ? annotatedDelegatingStructure : null);
    }

    /**
     * {@return a new structure representing a list of the current structure}
     */
    default Structure<List<A>> listOf() {
        var outer = this;
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, List<A>>> interpret(Interpreter<Mu> interpreter) {
                return outer.interpret(interpreter).flatMap(interpreter::list);
            }
        };
    }

    static <K, V> Structure<Map<K, V>> unboundedMap(Structure<K> key, Structure<V> value) {
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, Map<K, V>>> interpret(Interpreter<Mu> interpreter) {
                return key.interpret(interpreter).flatMap(k -> value.interpret(interpreter).flatMap(v -> interpreter.unboundedMap(k, v)));
            }
        };
    }

    static <L, R> Structure<Either<L, R>> either(Structure<L> left, Structure<R> right) {
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, Either<L, R>>> interpret(Interpreter<Mu> interpreter) {
                var leftResult = left.interpret(interpreter);
                var rightResult = right.interpret(interpreter);
                return leftResult
                    .mapError(s -> rightResult.error().map(e -> s + "; " + e.message()).orElse(s))
                    .flatMap(leftApp -> rightResult.flatMap(rightApp -> interpreter.either(leftApp, rightApp)));
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

    /**
     * Like codecs, the type a structure represents can be changed without changing the actual underlying data structure,
     * by providing conversion functions to and from the new type.
     * @param deserializer converts the old type to the new type, if possible
     * @param serializer converts the new type to the old type, if possible
     * @return a new structure representing the new type
     * @param <B> the new type to represent
     */
    default <B> Structure<B> flatXmap(Function<A, DataResult<B>> deserializer, Function<B, DataResult<A>> serializer) {
        var outer = this;
        return annotatedDelegatingStructure(new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, B>> interpret(Interpreter<Mu> interpreter) {
                return outer.interpret(interpreter).flatMap(app -> interpreter.flatXmap(app, deserializer, serializer));
            }
        }, outer.annotations());
    }

    /**
     * Similar to {@link #flatXmap(Function, Function)} (Function, Function)}, except that the conversion functions are not allowed to fail.
     * @param deserializer converts the old type to the new type
     * @param serializer converts the new type to the old type
     * @return a new structure representing the new type
     * @param <B> the new type to represent
     */
    default <B> Structure<B> xmap(Function<A, B> deserializer, Function<B, A> serializer) {
        return flatXmap(a -> DataResult.success(deserializer.apply(a)), b -> DataResult.success(serializer.apply(b)));
    }

    default <E> Structure<E> dispatch(String key, Function<? super E, DataResult<A>> function, Supplier<Set<A>> keys, Function<A, DataResult<Structure<? extends E>>> structures) {
        var outer = this;
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, E>> interpret(Interpreter<Mu> interpreter) {
                return interpreter.dispatch(key, outer, function, keys, structures);
            }
        };
    }

    default <V> Structure<Map<A, V>> dispatchMap(Supplier<Set<A>> keys, Function<A, DataResult<Structure<? extends V>>> structures) {
        var outer = this;
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, Map<A, V>>> interpret(Interpreter<Mu> interpreter) {
                return interpreter.dispatchMap(outer, keys, structures);
            }
        };
    }

    /**
     * It might be necessary to lazily-initialize a structure to avoid circular static field dependencies.
     * @param supplier the structure to lazily initialize
     * @return a new structure that will initialize the wrapped structure when interpreted
     * @param <A> the type of data the structure represents
     */
    static <A> Structure<A> lazyInitialized(Supplier<Structure<A>> supplier) {
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
                return supplier.get().interpret(interpreter);
            }
        };
    }

    /**
     * Keys provide a way of representing the smallest building blocks of a structure. Interpreters are responsible for
     * finding a matching specific representation given a key when interpreting a structure.
     * @param key the key which will be matched to a specific representation
     * @return a new structure
     * @param <A> the type of data the structure represents
     * @see Key
     */
    static <A> Structure<A> keyed(Key<A> key) {
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
                return interpreter.keyed(key);
            }
        };
    }

    /**
     * Similar to {@link #keyed(Key)}, except a fallback structure is provided in case the interpreter cannot resolve the key.
     * @param key the key which will be matched to a specific representation
     * @param fallback the structure to interpret if the key cannot be resolved
     * @return a new structure
     * @param <A> the type of data the structure represents
     */
    static <A> Structure<A> keyed(Key<A> key, Structure<A> fallback) {
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
                var result = interpreter.keyed(key);
                if (result.error().isPresent()) {
                    return fallback.interpret(interpreter).mapError(s -> "Could not interpret keyed structure: "+s+"; "+result.error().orElseThrow().message());
                }
                return result;
            }
        };
    }

    /**
     * Similar to {@link #keyed(Key)}, except that specific representations may also be stored on the structure, by
     * resolved interpreter-specific keys. The key set used is {@link Flip}ed so that the type parameterizing the key can
     * be the type function of the interpreter. Interpreter keys are matched against the provided key set, and if missing
     * the provided key is resolved by the interpreter.
     * @param key the key which will be matched to a specific representation
     * @param keys the set of specific representations to match against
     * @return a new structure
     * @param <A> the type of data the structure represents
     * @see Interpreter#key()
     */
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


    static <A> Structure<A> keyed(Key<A> key, Keys<Flip.Mu<A>, K1> keys, Structure<A> fallback) {
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
                var result = interpreter.key().flatMap(k -> keys.get(k).<Flip<Mu, A>>map(Flip::unbox).map(Flip::value))
                    .map(DataResult::success)
                    .orElseGet(() -> interpreter.keyed(key));
                if (result.error().isPresent()) {
                    return fallback.interpret(interpreter).mapError(s -> "Could not interpret keyed structure: "+s+"; "+result.error().orElseThrow().message());
                }
                return result;
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

    static <MuO extends K1, MuP extends K1, T, A extends App<MuO, T>> Structure<A> parametricallyKeyed(Key2<MuP, MuO> key, App<MuP, T> parameter, Function<App<MuO, T>, A> unboxer, Structure<A> fallback) {
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
                var result = interpreter.parametricallyKeyed(key, parameter).flatMap(app ->
                        interpreter.flatXmap(app, a -> DataResult.success(unboxer.apply(a)), DataResult::success)
                );
                if (result.error().isPresent()) {
                    return fallback.interpret(interpreter).mapError(s -> "Could not interpret parametrically keyed structure: "+s+"; "+result.error().orElseThrow().message());
                }
                return result;
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

    static <MuO extends K1, MuP extends K1, T, A extends App<MuO, T>> Structure<A> parametricallyKeyed(Key2<MuP, MuO> key, App<MuP, T> parameter, Function<App<MuO, T>, A> unboxer, Keys<Flip.Mu<A>, K1> keys, Structure<A> fallback) {
        return new Structure<>() {
            @Override
            public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
                var result = interpreter.key().flatMap(k -> keys.get(k).<Flip<Mu, A>>map(Flip::unbox).map(Flip::value))
                    .map(DataResult::success)
                    .orElseGet(() -> interpreter.parametricallyKeyed(key, parameter).flatMap(app ->
                        interpreter.flatXmap(app, a -> DataResult.success(unboxer.apply(a)), DataResult::success)
                    ));
                if (result.error().isPresent()) {
                    return fallback.interpret(interpreter).mapError(s -> "Could not interpret parametrically keyed structure: "+s+"; "+result.error().orElseThrow().message());
                }
                return result;
            }
        };
    }

    static <A> Structure<A> record(RecordStructure.Builder<A> builder) {
        return RecordStructure.create(builder);
    }

    /**
     * Represents a {@link Unit} value.
     */
    Structure<Unit> UNIT = keyed(Interpreter.UNIT);
    /**
     * Represents a {@code boolean} value.
     */
    Structure<Boolean> BOOL = keyed(Interpreter.BOOL);
    /**
     * Represents a {@code byte} value.
     */
    Structure<Byte> BYTE = keyed(Interpreter.BYTE);
    /**
     * Represents a {@code short} value.
     */
    Structure<Short> SHORT = keyed(Interpreter.SHORT);
    /**
     * Represents an {@code int} value.
     */
    Structure<Integer> INT = keyed(Interpreter.INT);
    /**
     * Represents a {@code long} value.
     */
    Structure<Long> LONG = keyed(Interpreter.LONG);
    /**
     * Represents a {@code float} value.
     */
    Structure<Float> FLOAT = keyed(Interpreter.FLOAT);
    /**
     * Represents a {@code double} value.
     */
    Structure<Double> DOUBLE = keyed(Interpreter.DOUBLE);
    /**
     * Represents a {@link String} value.
     */
    Structure<String> STRING = keyed(Interpreter.STRING);

    /**
     * {@return a structure representing integer values within a range}
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     */
    static Structure<Integer> intInRange(int min, int max) {
        return Structure.parametricallyKeyed(Interpreter.INT_IN_RANGE, Const.create(new Range<>(min, max)), app -> (Const<Integer, Object>) app)
                .xmap(Const::unbox, Const::create);
    }

    /**
     * {@return a structure representing byte values within a range}
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     */
    static Structure<Byte> byteInRange(byte min, byte max) {
        return Structure.parametricallyKeyed(Interpreter.BYTE_IN_RANGE, Const.create(new Range<>(min, max)), app -> (Const<Byte, Object>) app)
                .xmap(Const::unbox, Const::create);
    }

    /**
     * {@return a structure representing short values within a range}
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     */
    static Structure<Short> shortInRange(short min, short max) {
        return Structure.parametricallyKeyed(Interpreter.SHORT_IN_RANGE, Const.create(new Range<>(min, max)), app -> (Const<Short, Object>) app)
                .xmap(Const::unbox, Const::create);
    }

    /**
     * {@return a structure representing long values within a range}
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     */
    static Structure<Long> longInRange(long min, long max) {
        return Structure.parametricallyKeyed(Interpreter.LONG_IN_RANGE, Const.create(new Range<>(min, max)), app -> (Const<Long, Object>) app)
                .xmap(Const::unbox, Const::create);
    }

    /**
     * {@return a structure representing float values within a range}
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     */
    static Structure<Float> floatInRange(float min, float max) {
        return Structure.parametricallyKeyed(Interpreter.FLOAT_IN_RANGE, Const.create(new Range<>(min, max)), app -> (Const<Float, Object>) app)
                .xmap(Const::unbox, Const::create);
    }

    /**
     * {@return a structure representing double values within a range}
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     */
    static Structure<Double> doubleInRange(double min, double max) {
        return Structure.parametricallyKeyed(Interpreter.DOUBLE_IN_RANGE, Const.create(new Range<>(min, max)), app -> (Const<Double, Object>) app)
                .xmap(Const::unbox, Const::create);
    }

    /**
     * {@return a structure representing a type with finite possible values, each of which can be represented as a string}
     * @param values provides the possible (ordered) values of the type
     * @param representation converts a value to a string
     * @param <T> the type to represent
     */
    static <T> Structure<T> stringRepresentable(Supplier<T[]> values, Function<T, String> representation) {
        return Structure.parametricallyKeyed(Interpreter.STRING_REPRESENTABLE, new StringRepresentation<>(values, representation), app -> (Identity<T>) app)
                .xmap(i -> Identity.unbox(i).value(), Identity::new);
    }
}
