package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.K2;

public interface ParametricKeyedValue<N extends K1, MuP extends K1, MuO extends K1> extends App2<ParametricKeyedValue.Mu<N>, MuP, MuO> {
    final class Mu<N extends K1> implements K2 { private Mu() {} }

    <T> App<N, App<MuO, T>> convert(App<MuP, T> parameter);

    default <M extends K1> ParametricKeyedValue<M, MuP, MuO> map(Converter<N, M> converter) {
        var outer = this;
        return new ParametricKeyedValue<>() {
            @Override
            public <T> App<M, App<MuO, T>> convert(App<MuP, T> parameter) {
                return converter.convert(outer.convert(parameter));
            }
        };
    }

    interface Converter<N extends K1, M extends K1> {
        <T, MuO extends K1> App<M, App<MuO, T>> convert(App<N, App<MuO, T>> app);
    }

    static <N extends K1, MuP extends K1, MuO extends K1> ParametricKeyedValue<N, MuP, MuO> unbox(App2<Mu<N>, MuP, MuO> boxed) {
        return (ParametricKeyedValue<N, MuP, MuO>) boxed;
    }
}
