package dev.lukebemish.codecextras.polymorphic;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

// TODO: document this class...
public final class BuilderCodecs {
    private BuilderCodecs() {}

    public static <O, F> RecordCodecBuilder<O, FieldInfo<O, F>> wrap(MapCodec<F> fieldCodec, BiFunction<O, F, O> fieldSetter, Function<O, F> fieldGetter) {
        return fieldCodec.<FieldInfo<O, F>>xmap(f -> FieldInfo.of(o -> fieldSetter.apply(o, f), () -> f), FieldInfo::get)
            .forGetter(o -> FieldInfo.of(o1 -> fieldSetter.apply(o, fieldGetter.apply(o)), () -> fieldGetter.apply(o)));
    }

    public static <O, F> RecordCodecBuilder<Either<O, UnaryOperator<O>>, OptionalFieldInfo<O, F>> operationWrap(MapCodec<F> fieldCodec, BiFunction<O, F, O> fieldSetter, Function<O, F> fieldGetter) {
        return fieldCodec.<OptionalFieldInfo<O, F>>flatXmap(f -> DataResult.success(OptionalFieldInfo.of(o -> fieldSetter.apply(o, f), () -> Optional.of(f))), info -> info.get().map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Field not present")))
            .forGetter(either -> either.map(o -> OptionalFieldInfo.of(o1 -> fieldSetter.apply(o, fieldGetter.apply(o)), () -> Optional.of(fieldGetter.apply(o))), unaryOp -> OptionalFieldInfo.of(o -> o, Optional::empty)));
    }

    public static <O> FieldInfoResolver<O> resolver(Supplier<O> initial) {
        return new FieldInfoResolver<>(initial);
    }

    @SuppressWarnings("unchecked")
    public static <O> FieldInfoOperationResolver<O> operationResolver() {
        return (FieldInfoOperationResolver<O>) FieldInfoOperationResolver.INSTANCE;
    }

    public static <O, P> Codec<O> pair(Codec<O> codec, Codec<P> parent, Function<O, P> parentGetter, BiFunction<O, P, O> parentSetter) {
        return Codec.pair(codec, parent).xmap(p -> {
            O builder = p.getFirst();
            return parentSetter.apply(builder, p.getSecond());
        }, builder -> Pair.of(builder, parentGetter.apply(builder)));
    }

    public static <O, P> MapCodec<O> mapPair(MapCodec<O> codec, MapCodec<P> parent, Function<O, P> parentGetter, BiFunction<O, P, O> parentSetter) {
        return Codec.mapPair(codec, parent).xmap(p -> {
            O builder = p.getFirst();
            return parentSetter.apply(builder, p.getSecond());
        }, builder -> Pair.of(builder, parentGetter.apply(builder)));
    }

    public static <O, P> Codec<O> flatPair(Codec<O> codec, Codec<P> parent, Function<O, DataResult<P>> parentGetter, BiFunction<O, P, DataResult<O>> parentSetter) {
        return Codec.pair(codec, parent).flatXmap(p -> {
            O builder = p.getFirst();
            return parentSetter.apply(builder, p.getSecond());
        }, builder -> parentGetter.apply(builder).map(p->Pair.of(builder, p)));
    }

    public static <O, P> MapCodec<O> flatMapPair(MapCodec<O> codec, MapCodec<P> parent, Function<O, DataResult<P>> parentGetter, BiFunction<O, P, DataResult<O>> parentSetter) {
        return Codec.mapPair(codec, parent).flatXmap(p -> {
            O builder = p.getFirst();
            return parentSetter.apply(builder, p.getSecond());
        }, builder -> parentGetter.apply(builder).map(p->Pair.of(builder, p)));
    }

    public static <O, B extends DataBuilder<O>> Codec<O> codec(Codec<B> builderCodec, Function<O, B> deBuilder) {
        return builderCodec.flatXmap(DataBuilder::buildResult, o -> {
            B builder = deBuilder.apply(o);
            return DataResult.success(builder);
        });
    }

    public static <O, B extends DataBuilder<O>> MapCodec<O> mapCodec(MapCodec<B> builderCodec, Function<O, B> deBuilder) {
        return builderCodec.flatXmap(DataBuilder::buildResult, o -> {
            B builder = deBuilder.apply(o);
            return DataResult.success(builder);
        });
    }

    public static <O, B> Codec<O> operationCodec(Codec<Either<B, UnaryOperator<B>>> builderCodec, Function<O, B> deBuilder, Supplier<B> initial, Function<B, DataBuilder<O>> buildFunction) {
        return builderCodec
            .xmap(either -> either.map(b -> b, op -> op.apply(initial.get())), Either::left)
            .flatXmap(b -> buildFunction.apply(b).buildResult(), o -> {
                B builder = deBuilder.apply(o);
                return DataResult.success(builder);
            });
    }

    public static <O, B> MapCodec<O> operationMapCodec(MapCodec<Either<B, UnaryOperator<B>>> builderCodec, BiFunction<B, O, B> deBuilder, Supplier<B> initial, Function<B, DataBuilder<O>> buildFunction) {
        return builderCodec
            .xmap(either -> either.map(b -> b, op -> op.apply(initial.get())), Either::left)
            .flatXmap(b -> buildFunction.apply(b).buildResult(), o -> {
                B builder = deBuilder.apply(initial.get(), o);
                return DataResult.success(builder);
            });
    }

    public interface FieldInfo<O, F> extends UnaryOperator<O>, Supplier<F> {
        static <O,F> FieldInfo<O,F> of(UnaryOperator<O> consumer, Supplier<F> supplier) {
            return new FieldInfo<>() {
                @Override
                public O apply(O object) {
                    return consumer.apply(object);
                }

                @Override
                public F get() {
                    return supplier.get();
                }
            };
        }
    }

    public interface OptionalFieldInfo<O, F> extends UnaryOperator<O>, Supplier<Optional<F>> {
        static <O,F> OptionalFieldInfo<O,F> of(UnaryOperator<O> consumer, Supplier<Optional<F>> supplier) {
            return new OptionalFieldInfo<>() {
                @Override
                public O apply(O object) {
                    return consumer.apply(object);
                }

                @Override
                public Optional<F> get() {
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
                instance = fieldInfo.apply(instance);
            }
            return instance;
        }
    }

    public static final class FieldInfoOperationResolver<O> {
        private static final FieldInfoOperationResolver<?> INSTANCE = new FieldInfoOperationResolver<>();

        private FieldInfoOperationResolver() {}

        @SafeVarargs
        public final Either<O, UnaryOperator<O>> apply(OptionalFieldInfo<O, ?>... fieldInfos) {
            return Either.right(instance -> {
                for (OptionalFieldInfo<O, ?> fieldInfo : fieldInfos) {
                    instance = fieldInfo.apply(instance);
                }
                return instance;
            });
        }
    }
}
