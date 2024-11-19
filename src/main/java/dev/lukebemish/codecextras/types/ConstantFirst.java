package dev.lukebemish.codecextras.types;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.K2;

public record ConstantFirst<N extends K1, A, B>(App<N, A> value) implements App2<ConstantFirst.Mu<N>, A, B> {
    public static final class Mu<N extends K1> implements K2 { private Mu() {} }

    public static <N extends K1, A, B> ConstantFirst<N, A, B> unbox(App2<ConstantFirst.Mu<N>, A, B> box) {
        return (ConstantFirst<N, A, B>) box;
    }
}
