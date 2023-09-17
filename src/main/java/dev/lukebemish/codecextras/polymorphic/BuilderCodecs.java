package dev.lukebemish.codecextras.polymorphic;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class BuilderCodecs {
    private BuilderCodecs() {}

    public static <O, F> RecordCodecBuilder<O, FieldInfo<O, F>> wrap(MapCodec<F> fieldCodec, BiConsumer<O, F> fieldConsumer, Function<O, F> fieldGetter) {
        return fieldCodec.<FieldInfo<O, F>>xmap(f -> FieldInfo.of(o -> fieldConsumer.accept(o, f), () -> f), FieldInfo::get)
            .forGetter(o -> FieldInfo.of(o1 -> fieldConsumer.accept(o, fieldGetter.apply(o)), () -> fieldGetter.apply(o)));
    }

    public static <O> FieldInfoResolver<O> resolver(Supplier<O> initial) {
        return new FieldInfoResolver<>(initial);
    }

    public static <O, P> Codec<O> pair(Codec<O> codec, Codec<P> parent, Function<O, P> parentGetter, BiConsumer<O, P> parentSetter) {
        return Codec.pair(codec, parent).xmap(p -> {
            O builder = p.getFirst();
            parentSetter.accept(builder, p.getSecond());
            return builder;
        }, builder -> Pair.of(builder, parentGetter.apply(builder)));
    }

    public static <O, P> MapCodec<O> mapPair(MapCodec<O> codec, MapCodec<P> parent, Function<O, P> parentGetter, BiConsumer<O, P> parentSetter) {
        return Codec.mapPair(codec, parent).xmap(p -> {
            O builder = p.getFirst();
            parentSetter.accept(builder, p.getSecond());
            return builder;
        }, builder -> Pair.of(builder, parentGetter.apply(builder)));
    }

    public static <O, B extends PolymorphicBuilder<O>> Codec<O> codec(Codec<B> builderCodec, Function<O, B> unbuilder) {
        return builderCodec.flatXmap(PolymorphicBuilder::buildResult, o -> {
            B builder = unbuilder.apply(o);
            return DataResult.success(builder);
        });
    }

    public interface FieldInfo<O, F> extends Consumer<O>, Supplier<F> {
        static <O,F> FieldInfo<O,F> of(Consumer<O> consumer, Supplier<F> supplier) {
            return new FieldInfo<>() {
                @Override
                public void accept(O object) {
                    consumer.accept(object);
                }

                @Override
                public F get() {
                    return supplier.get();
                }
            };
        }
    }

    public static final class FieldInfoResolver<O> {
        private final Supplier<O> initial;

        private FieldInfoResolver(Supplier<O> initial) {
            this.initial = initial;
        }

        @SafeVarargs
        public final O apply(FieldInfo<O, ?>... fieldInfos) {
            O instance = initial.get();
            for (FieldInfo<O, ?> fieldInfo : fieldInfos) {
                fieldInfo.accept(instance);
            }
            return instance;
        }
    }
}
