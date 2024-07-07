package dev.lukebemish.codecextras.extension;

import com.mojang.datafixers.util.Either;
import dev.lukebemish.autoextension.AutoExtension;
import java.util.function.Function;

@AutoExtension
public final class EitherExtension {
    private EitherExtension() {}

    public static <A> A flatten(Either<A, A> either) {
        return either.map(Function.identity(), Function.identity());
    }
}
