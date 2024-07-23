package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.types.Identity;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface Interpreter<Mu extends K1> {
    <A> DataResult<App<Mu, List<A>>> list(App<Mu, A> single);

    <A> DataResult<App<Mu, A>> keyed(Key<A> key);

    <A> DataResult<App<Mu, A>> record(List<RecordStructure.Field<A, ?>> fields, Function<RecordStructure.Container, A> creator);

    <A, B> DataResult<App<Mu, B>> flatXmap(App<Mu, A> input, Function<A, DataResult<B>> deserializer, Function<B, DataResult<A>> serializer);

    <A> DataResult<App<Mu, A>> annotate(App<Mu, A> input, Keys<Identity.Mu, Object> annotations);

    default Optional<Key<Mu>> key() {
        return Optional.empty();
    }

    Key<Unit> UNIT = Key.create("UNIT");
    Key<Boolean> BOOL = Key.create("BOOL");
    Key<Byte> BYTE = Key.create("BYTE");
    Key<Short> SHORT = Key.create("SHORT");
    Key<Integer> INT = Key.create("INT");
    Key<Long> LONG = Key.create("LONG");
    Key<Float> FLOAT = Key.create("FLOAT");
    Key<Double> DOUBLE = Key.create("DOUBLE");
    Key<String> STRING = Key.create("STRING");

    <MuO extends K1, MuP extends K1, T> DataResult<App<Mu, App<MuO, T>>> parametricallyKeyed(Key2<MuP,MuO> key, App<MuP, T> parameter);
}
