package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.types.Identity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Attempts to recover a default value from a structure by evaluating missing behaviours as if the value is missing.
 */
public class IdentityInterpreter implements Interpreter<Identity.Mu> {
    /**
     * The singleton instance of this interpreter.
     */
    public static final IdentityInterpreter INSTANCE = new IdentityInterpreter();

    /**
     * The key for this interpreter.
     */
    public static final Key<Identity.Mu> KEY = Key.create("IdentityInterpreter");

    @Override
    public Optional<Key<Identity.Mu>> key() {
        return Optional.of(KEY);
    }

    @Override
    public <A> DataResult<App<Identity.Mu, List<A>>> list(App<Identity.Mu, A> single) {
        return DataResult.error(() -> "No default value available for a list");
    }

    @Override
    public <A> DataResult<App<Identity.Mu, A>> keyed(Key<A> key) {
        return DataResult.error(() -> "No default value available for a key");
    }

    @Override
    public <A> DataResult<App<Identity.Mu, A>> record(List<RecordStructure.Field<A, ?>> fields, Function<RecordStructure.Container, A> creator) {
        var builder = RecordStructure.Container.builder();
        for (var field : fields) {
            DataResult<App<Identity.Mu, A>> result = forField(field, builder);
            if (result != null) return result;
        }
        return DataResult.success(new Identity<>(creator.apply(builder.build())));
    }

    private <A, F> @Nullable DataResult<App<Identity.Mu, A>> forField(RecordStructure.Field<A, F> field, RecordStructure.Container.Builder builder) {
        var missingBehavior = field.missingBehavior();
        if (missingBehavior.isPresent()) {
            builder.add(field.key(), missingBehavior.get().missing().get());
        } else {
            var result = field.structure().interpret(this).map(i -> Identity.unbox(i).value());
            if (result.error().isPresent()) {
                return DataResult.error(() -> "No default value available for field " + field.name() + ": " + result.error().orElseThrow().message());
            }
            builder.add(field.key(), result.result().orElseThrow());
        }
        return null;
    }

    @Override
    public <A, B> DataResult<App<Identity.Mu, B>> flatXmap(App<Identity.Mu, A> input, Function<A, DataResult<B>> deserializer, Function<B, DataResult<A>> serializer) {
        var value = Identity.unbox(input).value();
        return deserializer.apply(value).map(Identity::new);
    }

    @Override
    public <A> DataResult<App<Identity.Mu, A>> annotate(Structure<A> original, Keys<Identity.Mu, Object> annotations) {
        return original.interpret(this);
    }

    @Override
    public <E, A> DataResult<App<Identity.Mu, E>> dispatch(String key, Structure<A> keyStructure, Function<? super E, ? extends DataResult<A>> function, Set<A> keys, Function<A, Structure<? extends E>> structures) {
        return DataResult.error(() -> "No default value available for a dispatch");
    }

    @Override
    public <MuO extends K1, MuP extends K1, T> DataResult<App<Identity.Mu, App<MuO, T>>> parametricallyKeyed(Key2<MuP, MuO> key, App<MuP, T> parameter) {
        return DataResult.error(() -> "No default value available for a parametric key");
    }

    public <A> DataResult<A> interpret(Structure<A> structure) {
        return structure.interpret(this).map(i -> Identity.unbox(i).value());
    }
}
