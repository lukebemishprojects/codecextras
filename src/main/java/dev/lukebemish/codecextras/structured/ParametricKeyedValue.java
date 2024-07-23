package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.K2;

public record ParametricKeyedValue<N extends K1, MuP extends K1, MuO extends K1>(Converter<N, MuP, MuO> converter) implements App2<ParametricKeyedValue.Mu<N>, MuP, MuO> {
    public static class Mu<N extends K1> implements K2 {}

    public interface Converter<N extends K1, MuP extends K1, MuO extends K1> {
        <T> App<N, App<MuO, T>> convert(App<MuP, T> parameter);
    }

    public static <N extends K1, MuP extends K1, MuO extends K1> ParametricKeyedValue<N, MuP, MuO> unbox(App2<Mu<N>, MuP, MuO> boxed) {
        return (ParametricKeyedValue<N, MuP, MuO>) boxed;
    }
}
