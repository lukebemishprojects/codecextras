package dev.lukebemish.codecextras.types;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.K2;

public record Second<N extends K2, A, B>(App2<N, A, B> value) implements App<Second.Mu<N, A>, B> {
    public static final class Mu<N extends K2, A> implements K1 { private Mu() {} }

    public static <N extends K2, A, B> Second<N, A, B> unbox(App<Second.Mu<N, A>, B> box) {
        return (Second<N, A, B>) box;
    }
}
