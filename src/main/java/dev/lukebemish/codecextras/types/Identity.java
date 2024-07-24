package dev.lukebemish.codecextras.types;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.K1;
import java.util.function.Function;

public record Identity<T>(T value) implements App<Identity.Mu, T> {
    public static final class Mu implements K1 { private Mu() {} }

    public static <T> Identity<T> unbox(App<Mu, T> input) {
        return (Identity<T>) input;
    }

    enum Instance implements Applicative<Identity.Mu, Identity.Instance.Mu> {
        INSTANCE;

        public static final class Mu implements Applicative.Mu { private Mu() {} }

        @Override
        public <A> App<Identity.Mu, A> point(A a) {
            return new Identity<>(a);
        }

        @Override
        public <A, R> Function<App<Identity.Mu, A>, App<Identity.Mu, R>> lift1(App<Identity.Mu, Function<A, R>> function) {
            var f = unbox(function).value();
            return app -> new Identity<>(f.apply(unbox(app).value()));
        }

        @Override
        public <T, R> App<Identity.Mu, R> map(Function<? super T, ? extends R> func, App<Identity.Mu, T> ts) {
            return new Identity<>(func.apply(unbox(ts).value()));
        }
    }
}
