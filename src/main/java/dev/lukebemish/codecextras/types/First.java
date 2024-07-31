package dev.lukebemish.codecextras.types;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.K2;

public record First<N extends K2, A, B>(App2<N, A, B> value) implements App<First.Mu<N, B>, A> {
    public static final class Mu<N extends K2, A> implements K1 { private Mu() {} }

    public static <N extends K2, A, B> First<N, A, B> unbox(App<First.Mu<N, B>, A> box) {
        return (First<N, A, B>) box;
    }
}
