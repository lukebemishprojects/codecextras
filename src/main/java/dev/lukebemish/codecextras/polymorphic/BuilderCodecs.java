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

    public static <O, F> RecordCodecBuilder<BuilderResolver<O>, FieldInfo<O, F>> wrap(MapCodec<F> fieldCodec, BiConsumer<O, F> fieldConsumer, Function<O, F> fieldGetter) {
        return fieldCodec.<FieldInfo<O, F>>xmap(f -> FieldInfo.of(o -> fieldConsumer.accept(o, f), () -> f), FieldInfo::get)
            .forGetter(o -> FieldInfo.of(o1 -> fieldConsumer.accept(o.instance(), fieldGetter.apply(o.instance())), () -> fieldGetter.apply(o.instance())));
    }

    public static <O> FieldInfoResolver<O> resolver(Supplier<O> initial) {
        return new FieldInfoResolver<>(initial);
    }

    public static <O, P> Codec<BuilderResolver<O>> pair(Codec<BuilderResolver<O>> codec, Codec<BuilderResolver<P>> parent, Function<O, P> parentGetter) {
        return Codec.pair(codec, parent).xmap(p -> BuilderResolver.of(p.getFirst().instance(), builder -> {
            p.getFirst().apply(builder);
            p.getSecond().apply(parentGetter.apply(builder));
        }), both -> Pair.of(
            BuilderResolver.of(both.instance(), both::apply), BuilderResolver.of(parentGetter.apply(both.instance()), b -> {})
        ));
    }

    public static <O, P> MapCodec<BuilderResolver<O>> mapPair(MapCodec<BuilderResolver<O>> codec, MapCodec<BuilderResolver<P>> parent, Function<O, P> parentGetter) {
        return Codec.mapPair(codec, parent).xmap(p -> BuilderResolver.of(p.getFirst().instance(), builder -> {
            p.getFirst().apply(builder);
            p.getSecond().apply(parentGetter.apply(builder));
        }), both -> Pair.of(
            BuilderResolver.of(both.instance(), both::apply), BuilderResolver.of(parentGetter.apply(both.instance()), b -> {})
        ));
    }

    public static <O, B extends PolymorphicBuilder<O>> Codec<O> codec(Codec<BuilderResolver<B>> builderCodec, Function<O, B> unbuilder) {
        return builderCodec.flatXmap(br -> {
            B builder = br.instance();
            br.apply(builder);
            return builder.buildResult();
        }, o -> {
            B builder = unbuilder.apply(o);
            return DataResult.success(BuilderResolver.of(builder, b -> {}));
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
        public final BuilderResolver<O> apply(FieldInfo<O, ?>... fieldInfos) {
            O instance = initial.get();
            return new BuilderResolver<>() {
                @Override
                public void apply(O builder) {
                    for (FieldInfo<O, ?> fieldInfo : fieldInfos) {
                        fieldInfo.accept(builder);
                    }
                }

                @Override
                public O instance() {
                    return instance;
                }
            };
        }
    }

    public interface BuilderResolver<O> {
        void apply(O builder);
        O instance();

        static <O> BuilderResolver<O> of(O initial, Consumer<O> consumer) {
            return new BuilderResolver<>() {
                @Override
                public void apply(O builder) {
                    consumer.accept(builder);
                }

                @Override
                public O instance() {
                    return initial;
                }
            };
        }
    }
}
