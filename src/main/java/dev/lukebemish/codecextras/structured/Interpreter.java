package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import dev.lukebemish.codecextras.StringRepresentation;
import dev.lukebemish.codecextras.types.Identity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Interpreter<Mu extends K1> {
    <A> DataResult<App<Mu, List<A>>> list(App<Mu, A> single);

    <A> DataResult<App<Mu, A>> keyed(Key<A> key);

    <A> DataResult<App<Mu, A>> record(List<RecordStructure.Field<A, ?>> fields, Function<RecordStructure.Container, A> creator);

    <A, B> DataResult<App<Mu, B>> flatXmap(App<Mu, A> input, Function<A, DataResult<B>> deserializer, Function<B, DataResult<A>> serializer);

    <A> DataResult<App<Mu, A>> annotate(Structure<A> original, Keys<Identity.Mu, Object> annotations);

    <E, A> DataResult<App<Mu, E>> dispatch(String key, Structure<A> keyStructure, Function<? super E, ? extends DataResult<A>> function, Supplier<Set<A>> keys, Function<A, DataResult<Structure<? extends E>>> structures);

    default Optional<Key<Mu>> key() {
        return Optional.empty();
    }

    default <A> DataResult<App<Mu,A>> bounded(App<Mu, A> input, Supplier<Set<A>> values) {
        Function<A, DataResult<A>> verifier = a -> {
            if (values.get().contains(a)) {
                return DataResult.success(a);
            }
            return DataResult.error(() -> "Invalid value: " + a);
        };
        return flatXmap(input, verifier, verifier);
    }

    <K, V> DataResult<App<Mu, Map<K,V>>> unboundedMap(App<Mu, K> key, App<Mu, V> value);

    Key<Unit> UNIT = Key.create("UNIT");
    Key<Boolean> BOOL = Key.create("BOOL");
    Key<Byte> BYTE = Key.create("BYTE");
    Key<Short> SHORT = Key.create("SHORT");
    Key<Integer> INT = Key.create("INT");
    Key<Long> LONG = Key.create("LONG");
    Key<Float> FLOAT = Key.create("FLOAT");
    Key<Double> DOUBLE = Key.create("DOUBLE");
    Key<String> STRING = Key.create("STRING");
    Key<Dynamic<?>> PASSTHROUGH = Key.create("PASSTHROUGH");

    <MuO extends K1, MuP extends K1, T> DataResult<App<Mu, App<MuO, T>>> parametricallyKeyed(Key2<MuP,MuO> key, App<MuP, T> parameter);

    Key2<Const.Mu<Range<Integer>>, Const.Mu<Integer>> INT_IN_RANGE = Key2.create("int_in_range");
    Key2<Const.Mu<Range<Byte>>, Const.Mu<Byte>> BYTE_IN_RANGE = Key2.create("byte_in_range");
    Key2<Const.Mu<Range<Short>>, Const.Mu<Short>> SHORT_IN_RANGE = Key2.create("short_in_range");
    Key2<Const.Mu<Range<Long>>, Const.Mu<Long>> LONG_IN_RANGE = Key2.create("long_in_range");
    Key2<Const.Mu<Range<Float>>, Const.Mu<Float>> FLOAT_IN_RANGE = Key2.create("float_in_range");
    Key2<Const.Mu<Range<Double>>, Const.Mu<Double>> DOUBLE_IN_RANGE = Key2.create("double_in_range");
    Key2<StringRepresentation.Mu, Identity.Mu> STRING_REPRESENTABLE = Key2.create("enum");

    <L, R> DataResult<App<Mu, Either<L,R>>> either(App<Mu, L> left, App<Mu, R> right);

    <K, V> DataResult<App<Mu, Map<K, V>>> dispatchedMap(Structure<K> keyStructure, Supplier<Set<K>> keys, Function<K, DataResult<Structure<? extends V>>> valueStructures);
}
