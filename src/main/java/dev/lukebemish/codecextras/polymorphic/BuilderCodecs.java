package dev.lukebemish.codecextras.polymorphic;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.Asymmetry;
import dev.lukebemish.codecextras.CodecExtras;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

// TODO: document this class...
public final class BuilderCodecs {
    private BuilderCodecs() {}

    public static <O, F> RecordCodecBuilder<DataResult<Asymmetry<O, DataResult<UnaryOperator<O>>>>, DataResult<Asymmetry<F, UnaryOperator<O>>>> operationWrap(MapCodec<F> fieldCodec, BiFunction<O, F, O> fieldSetter, Function<O, F> fieldGetter) {
        return CodecExtras.raise(Asymmetry.<F, UnaryOperator<O>, F>split(fieldCodec, Function.identity(), f -> o -> fieldSetter.apply(o, f)))
            .forGetter(Asymmetry.wrapGetter(o -> Asymmetry.encoding(fieldGetter.apply(o))));
    }

    public static <O, F> RecordCodecBuilder<DataResult<Asymmetry<DataResult<O>, DataResult<O>>>, DataResult<Asymmetry<F, UnaryOperator<O>>>> wrap(MapCodec<F> fieldCodec, BiFunction<O, F, O> fieldSetter, Function<O, F> fieldGetter) {
        return CodecExtras.raise(Asymmetry.<F, UnaryOperator<O>, F>split(fieldCodec, Function.identity(), f -> o -> fieldSetter.apply(o, f)))
            .forGetter(dr -> dr.flatMap(a -> a.encoding()).flatMap(Function.identity()).map(fieldGetter.andThen(Asymmetry::<F, UnaryOperator<O>>encoding)));
    }

    public static <O> Resolver<O> resolver(Supplier<O> initial) {
        return new Resolver<>(initial);
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

    public static <O, B> Codec<O> codec(Codec<B> builderCodec, Function<O, B> deBuilder, Function<B, DataBuilder<O>> buildFunction) {
        return builderCodec.flatXmap(b -> buildFunction.apply(b).buildResult(), o -> {
            B builder = deBuilder.apply(o);
            return DataResult.success(builder);
        });
    }

    public static <O, B> MapCodec<O> mapCodec(MapCodec<B> builderCodec, Function<O, B> deBuilder, Function<B, DataBuilder<O>> buildFunction) {
        return builderCodec.flatXmap(b -> buildFunction.apply(b).buildResult(), o -> {
            B builder = deBuilder.apply(o);
            return DataResult.success(builder);
        });
    }

    public static <O, B> Codec<O> operationCodec(Codec<Asymmetry<B, UnaryOperator<B>>> builderCodec, Function<O, B> deBuilder, Supplier<B> initial, Function<B, DataBuilder<O>> buildFunction) {
        return Asymmetry.join(builderCodec, Function.identity(), op -> op.apply(initial.get()))
            .flatXmap(b -> buildFunction.apply(b).buildResult(), o -> {
                B builder = deBuilder.apply(o);
                return DataResult.success(builder);
            });
    }

    public static <O, B> MapCodec<O> operationMapCodec(MapCodec<Asymmetry<B, UnaryOperator<B>>> builderCodec, BiFunction<B, O, B> deBuilder, Supplier<B> initial, Function<B, DataBuilder<O>> buildFunction) {
        return Asymmetry.join(builderCodec, Function.identity(), op -> op.apply(initial.get()))
            .flatXmap(b -> buildFunction.apply(b).buildResult(), o -> {
                B builder = deBuilder.apply(initial.get(), o);
                return DataResult.success(builder);
            });
    }

    public static final class Resolver<O> {
        private final Supplier<O> initial;

        private Resolver(Supplier<O> initial) {
            this.initial = initial;
        }

        @SafeVarargs
        public final DataResult<O> apply(Asymmetry<?, UnaryOperator<O>>... asymmetries) {
            O instance = initial.get();
            for (Asymmetry<?, UnaryOperator<O>> operator : asymmetries) {
                if (operator.decoding().result().isPresent()) {
                    instance = operator.decoding().result().get().apply(instance);
                } else {
                    return DataResult.error(() -> "Encoding asymmetry during decoding context");
                }
            }
            return DataResult.success(instance);
        }

        public DataResult<O> apply1(
                Asymmetry<?, UnaryOperator<O>> a1) {
            return apply(a1);
        }

        public DataResult<O> apply2(
                Asymmetry<?, UnaryOperator<O>> a1,
                Asymmetry<?, UnaryOperator<O>> a2) {
            return apply(a1, a2);
        }

        public DataResult<O> apply3(
                Asymmetry<?, UnaryOperator<O>> a1,
                Asymmetry<?, UnaryOperator<O>> a2,
                Asymmetry<?, UnaryOperator<O>> a3) {
            return apply(a1, a2, a3);
        }

        public DataResult<O> apply4(
            Asymmetry<?, UnaryOperator<O>> a1,
            Asymmetry<?, UnaryOperator<O>> a2,
            Asymmetry<?, UnaryOperator<O>> a3,
            Asymmetry<?, UnaryOperator<O>> a4) {
            return apply(a1, a2, a3, a4);
        }

        public DataResult<O> apply5(
            Asymmetry<?, UnaryOperator<O>> a1,
            Asymmetry<?, UnaryOperator<O>> a2,
            Asymmetry<?, UnaryOperator<O>> a3,
            Asymmetry<?, UnaryOperator<O>> a4,
            Asymmetry<?, UnaryOperator<O>> a5) {
            return apply(a1, a2, a3, a4, a5);
        }

        public DataResult<O> apply6(
                Asymmetry<?, UnaryOperator<O>> a1,
                Asymmetry<?, UnaryOperator<O>> a2,
                Asymmetry<?, UnaryOperator<O>> a3,
                Asymmetry<?, UnaryOperator<O>> a4,
                Asymmetry<?, UnaryOperator<O>> a5,
                Asymmetry<?, UnaryOperator<O>> a6) {
            return apply(a1, a2, a3, a4, a5, a6);
        }

        public DataResult<O> apply7(
                Asymmetry<?, UnaryOperator<O>> a1,
                Asymmetry<?, UnaryOperator<O>> a2,
                Asymmetry<?, UnaryOperator<O>> a3,
                Asymmetry<?, UnaryOperator<O>> a4,
                Asymmetry<?, UnaryOperator<O>> a5,
                Asymmetry<?, UnaryOperator<O>> a6,
                Asymmetry<?, UnaryOperator<O>> a7) {
            return apply(a1, a2, a3, a4, a5, a6, a7);
        }

        public DataResult<O> apply8(
                Asymmetry<?, UnaryOperator<O>> a1,
                Asymmetry<?, UnaryOperator<O>> a2,
                Asymmetry<?, UnaryOperator<O>> a3,
                Asymmetry<?, UnaryOperator<O>> a4,
                Asymmetry<?, UnaryOperator<O>> a5,
                Asymmetry<?, UnaryOperator<O>> a6,
                Asymmetry<?, UnaryOperator<O>> a7,
                Asymmetry<?, UnaryOperator<O>> a8) {
            return apply(a1, a2, a3, a4, a5, a6, a7, a8);
        }

        public DataResult<O> apply9(
                Asymmetry<?, UnaryOperator<O>> a1,
                Asymmetry<?, UnaryOperator<O>> a2,
                Asymmetry<?, UnaryOperator<O>> a3,
                Asymmetry<?, UnaryOperator<O>> a4,
                Asymmetry<?, UnaryOperator<O>> a5,
                Asymmetry<?, UnaryOperator<O>> a6,
                Asymmetry<?, UnaryOperator<O>> a7,
                Asymmetry<?, UnaryOperator<O>> a8,
                Asymmetry<?, UnaryOperator<O>> a9) {
            return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9);
        }

        public DataResult<O> apply10(
                Asymmetry<?, UnaryOperator<O>> a1,
                Asymmetry<?, UnaryOperator<O>> a2,
                Asymmetry<?, UnaryOperator<O>> a3,
                Asymmetry<?, UnaryOperator<O>> a4,
                Asymmetry<?, UnaryOperator<O>> a5,
                Asymmetry<?, UnaryOperator<O>> a6,
                Asymmetry<?, UnaryOperator<O>> a7,
                Asymmetry<?, UnaryOperator<O>> a8,
                Asymmetry<?, UnaryOperator<O>> a9,
                Asymmetry<?, UnaryOperator<O>> a10) {
            return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
        }

        public DataResult<O> apply11(
                Asymmetry<?, UnaryOperator<O>> a1,
                Asymmetry<?, UnaryOperator<O>> a2,
                Asymmetry<?, UnaryOperator<O>> a3,
                Asymmetry<?, UnaryOperator<O>> a4,
                Asymmetry<?, UnaryOperator<O>> a5,
                Asymmetry<?, UnaryOperator<O>> a6,
                Asymmetry<?, UnaryOperator<O>> a7,
                Asymmetry<?, UnaryOperator<O>> a8,
                Asymmetry<?, UnaryOperator<O>> a9,
                Asymmetry<?, UnaryOperator<O>> a10,
                Asymmetry<?, UnaryOperator<O>> a11) {
            return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
        }

        public DataResult<O> apply12(
                Asymmetry<?, UnaryOperator<O>> a1,
                Asymmetry<?, UnaryOperator<O>> a2,
                Asymmetry<?, UnaryOperator<O>> a3,
                Asymmetry<?, UnaryOperator<O>> a4,
                Asymmetry<?, UnaryOperator<O>> a5,
                Asymmetry<?, UnaryOperator<O>> a6,
                Asymmetry<?, UnaryOperator<O>> a7,
                Asymmetry<?, UnaryOperator<O>> a8,
                Asymmetry<?, UnaryOperator<O>> a9,
                Asymmetry<?, UnaryOperator<O>> a10,
                Asymmetry<?, UnaryOperator<O>> a11,
                Asymmetry<?, UnaryOperator<O>> a12) {
            return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);
        }

        public DataResult<O> apply13(
                Asymmetry<?, UnaryOperator<O>> a1,
                Asymmetry<?, UnaryOperator<O>> a2,
                Asymmetry<?, UnaryOperator<O>> a3,
                Asymmetry<?, UnaryOperator<O>> a4,
                Asymmetry<?, UnaryOperator<O>> a5,
                Asymmetry<?, UnaryOperator<O>> a6,
                Asymmetry<?, UnaryOperator<O>> a7,
                Asymmetry<?, UnaryOperator<O>> a8,
                Asymmetry<?, UnaryOperator<O>> a9,
                Asymmetry<?, UnaryOperator<O>> a10,
                Asymmetry<?, UnaryOperator<O>> a11,
                Asymmetry<?, UnaryOperator<O>> a12,
                Asymmetry<?, UnaryOperator<O>> a13) {
            return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);
        }

        public DataResult<O> apply14(
                Asymmetry<?, UnaryOperator<O>> a1,
                Asymmetry<?, UnaryOperator<O>> a2,
                Asymmetry<?, UnaryOperator<O>> a3,
                Asymmetry<?, UnaryOperator<O>> a4,
                Asymmetry<?, UnaryOperator<O>> a5,
                Asymmetry<?, UnaryOperator<O>> a6,
                Asymmetry<?, UnaryOperator<O>> a7,
                Asymmetry<?, UnaryOperator<O>> a8,
                Asymmetry<?, UnaryOperator<O>> a9,
                Asymmetry<?, UnaryOperator<O>> a10,
                Asymmetry<?, UnaryOperator<O>> a11,
                Asymmetry<?, UnaryOperator<O>> a12,
                Asymmetry<?, UnaryOperator<O>> a13,
                Asymmetry<?, UnaryOperator<O>> a14) {
            return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);
        }

        public DataResult<O> apply15(
                Asymmetry<?, UnaryOperator<O>> a1,
                Asymmetry<?, UnaryOperator<O>> a2,
                Asymmetry<?, UnaryOperator<O>> a3,
                Asymmetry<?, UnaryOperator<O>> a4,
                Asymmetry<?, UnaryOperator<O>> a5,
                Asymmetry<?, UnaryOperator<O>> a6,
                Asymmetry<?, UnaryOperator<O>> a7,
                Asymmetry<?, UnaryOperator<O>> a8,
                Asymmetry<?, UnaryOperator<O>> a9,
                Asymmetry<?, UnaryOperator<O>> a10,
                Asymmetry<?, UnaryOperator<O>> a11,
                Asymmetry<?, UnaryOperator<O>> a12,
                Asymmetry<?, UnaryOperator<O>> a13,
                Asymmetry<?, UnaryOperator<O>> a14,
                Asymmetry<?, UnaryOperator<O>> a15) {
            return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);
        }

        public DataResult<O> apply16(
                Asymmetry<?, UnaryOperator<O>> a1,
                Asymmetry<?, UnaryOperator<O>> a2,
                Asymmetry<?, UnaryOperator<O>> a3,
                Asymmetry<?, UnaryOperator<O>> a4,
                Asymmetry<?, UnaryOperator<O>> a5,
                Asymmetry<?, UnaryOperator<O>> a6,
                Asymmetry<?, UnaryOperator<O>> a7,
                Asymmetry<?, UnaryOperator<O>> a8,
                Asymmetry<?, UnaryOperator<O>> a9,
                Asymmetry<?, UnaryOperator<O>> a10,
                Asymmetry<?, UnaryOperator<O>> a11,
                Asymmetry<?, UnaryOperator<O>> a12,
                Asymmetry<?, UnaryOperator<O>> a13,
                Asymmetry<?, UnaryOperator<O>> a14,
                Asymmetry<?, UnaryOperator<O>> a15,
                Asymmetry<?, UnaryOperator<O>> a16) {
            return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16);
        }

        @SafeVarargs
        public static <O> DataResult<UnaryOperator<O>> operationApply(Asymmetry<?, UnaryOperator<O>>... asymmetries) {
            List<UnaryOperator<O>> operators = new ArrayList<>();
            for (Asymmetry<?, UnaryOperator<O>> operator : asymmetries) {
                if (operator.decoding().result().isPresent()) {
                    operators.add(operator.decoding().result().get());
                } else {
                    return DataResult.error(() -> "Encoding asymmetry during decoding context");
                }
            }
            return DataResult.success(instance -> {
                for (UnaryOperator<O> operator : operators) {
                    instance = operator.apply(instance);
                }
                return instance;
            });
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply1(Asymmetry<?,UnaryOperator<O>> a1) {
            return operationApply(a1);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply2(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2) {
            return operationApply(a1, a2);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply3(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2,
            Asymmetry<?,UnaryOperator<O>> a3) {
            return operationApply(a1, a2, a3);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply4(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2,
            Asymmetry<?,UnaryOperator<O>> a3,
            Asymmetry<?,UnaryOperator<O>> a4) {
            return operationApply(a1, a2, a3, a4);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply5(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2,
            Asymmetry<?,UnaryOperator<O>> a3,
            Asymmetry<?,UnaryOperator<O>> a4,
            Asymmetry<?,UnaryOperator<O>> a5) {
            return operationApply(a1, a2, a3, a4, a5);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply6(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2,
            Asymmetry<?,UnaryOperator<O>> a3,
            Asymmetry<?,UnaryOperator<O>> a4,
            Asymmetry<?,UnaryOperator<O>> a5,
            Asymmetry<?,UnaryOperator<O>> a6) {
            return operationApply(a1, a2, a3, a4, a5, a6);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply7(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2,
            Asymmetry<?,UnaryOperator<O>> a3,
            Asymmetry<?,UnaryOperator<O>> a4,
            Asymmetry<?,UnaryOperator<O>> a5,
            Asymmetry<?,UnaryOperator<O>> a6,
            Asymmetry<?,UnaryOperator<O>> a7) {
            return operationApply(a1, a2, a3, a4, a5, a6, a7);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply8(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2,
            Asymmetry<?,UnaryOperator<O>> a3,
            Asymmetry<?,UnaryOperator<O>> a4,
            Asymmetry<?,UnaryOperator<O>> a5,
            Asymmetry<?,UnaryOperator<O>> a6,
            Asymmetry<?,UnaryOperator<O>> a7,
            Asymmetry<?,UnaryOperator<O>> a8) {
            return operationApply(a1, a2, a3, a4, a5, a6, a7, a8);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply9(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2,
            Asymmetry<?,UnaryOperator<O>> a3,
            Asymmetry<?,UnaryOperator<O>> a4,
            Asymmetry<?,UnaryOperator<O>> a5,
            Asymmetry<?,UnaryOperator<O>> a6,
            Asymmetry<?,UnaryOperator<O>> a7,
            Asymmetry<?,UnaryOperator<O>> a8,
            Asymmetry<?,UnaryOperator<O>> a9) {
            return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply10(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2,
            Asymmetry<?,UnaryOperator<O>> a3,
            Asymmetry<?,UnaryOperator<O>> a4,
            Asymmetry<?,UnaryOperator<O>> a5,
            Asymmetry<?,UnaryOperator<O>> a6,
            Asymmetry<?,UnaryOperator<O>> a7,
            Asymmetry<?,UnaryOperator<O>> a8,
            Asymmetry<?,UnaryOperator<O>> a9,
            Asymmetry<?,UnaryOperator<O>> a10) {
            return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply11(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2,
            Asymmetry<?,UnaryOperator<O>> a3,
            Asymmetry<?,UnaryOperator<O>> a4,
            Asymmetry<?,UnaryOperator<O>> a5,
            Asymmetry<?,UnaryOperator<O>> a6,
            Asymmetry<?,UnaryOperator<O>> a7,
            Asymmetry<?,UnaryOperator<O>> a8,
            Asymmetry<?,UnaryOperator<O>> a9,
            Asymmetry<?,UnaryOperator<O>> a10,
            Asymmetry<?,UnaryOperator<O>> a11) {
            return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply12(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2,
            Asymmetry<?,UnaryOperator<O>> a3,
            Asymmetry<?,UnaryOperator<O>> a4,
            Asymmetry<?,UnaryOperator<O>> a5,
            Asymmetry<?,UnaryOperator<O>> a6,
            Asymmetry<?,UnaryOperator<O>> a7,
            Asymmetry<?,UnaryOperator<O>> a8,
            Asymmetry<?,UnaryOperator<O>> a9,
            Asymmetry<?,UnaryOperator<O>> a10,
            Asymmetry<?,UnaryOperator<O>> a11,
            Asymmetry<?,UnaryOperator<O>> a12) {
            return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply13(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2,
            Asymmetry<?,UnaryOperator<O>> a3,
            Asymmetry<?,UnaryOperator<O>> a4,
            Asymmetry<?,UnaryOperator<O>> a5,
            Asymmetry<?,UnaryOperator<O>> a6,
            Asymmetry<?,UnaryOperator<O>> a7,
            Asymmetry<?,UnaryOperator<O>> a8,
            Asymmetry<?,UnaryOperator<O>> a9,
            Asymmetry<?,UnaryOperator<O>> a10,
            Asymmetry<?,UnaryOperator<O>> a11,
            Asymmetry<?,UnaryOperator<O>> a12,
            Asymmetry<?,UnaryOperator<O>> a13) {
            return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply14(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2,
            Asymmetry<?,UnaryOperator<O>> a3,
            Asymmetry<?,UnaryOperator<O>> a4,
            Asymmetry<?,UnaryOperator<O>> a5,
            Asymmetry<?,UnaryOperator<O>> a6,
            Asymmetry<?,UnaryOperator<O>> a7,
            Asymmetry<?,UnaryOperator<O>> a8,
            Asymmetry<?,UnaryOperator<O>> a9,
            Asymmetry<?,UnaryOperator<O>> a10,
            Asymmetry<?,UnaryOperator<O>> a11,
            Asymmetry<?,UnaryOperator<O>> a12,
            Asymmetry<?,UnaryOperator<O>> a13,
            Asymmetry<?,UnaryOperator<O>> a14) {
            return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply15(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2,
            Asymmetry<?,UnaryOperator<O>> a3,
            Asymmetry<?,UnaryOperator<O>> a4,
            Asymmetry<?,UnaryOperator<O>> a5,
            Asymmetry<?,UnaryOperator<O>> a6,
            Asymmetry<?,UnaryOperator<O>> a7,
            Asymmetry<?,UnaryOperator<O>> a8,
            Asymmetry<?,UnaryOperator<O>> a9,
            Asymmetry<?,UnaryOperator<O>> a10,
            Asymmetry<?,UnaryOperator<O>> a11,
            Asymmetry<?,UnaryOperator<O>> a12,
            Asymmetry<?,UnaryOperator<O>> a13,
            Asymmetry<?,UnaryOperator<O>> a14,
            Asymmetry<?,UnaryOperator<O>> a15) {
            return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);
        }

        public static <O> DataResult<UnaryOperator<O>> operationApply16(
            Asymmetry<?,UnaryOperator<O>> a1,
            Asymmetry<?,UnaryOperator<O>> a2,
            Asymmetry<?,UnaryOperator<O>> a3,
            Asymmetry<?,UnaryOperator<O>> a4,
            Asymmetry<?,UnaryOperator<O>> a5,
            Asymmetry<?,UnaryOperator<O>> a6,
            Asymmetry<?,UnaryOperator<O>> a7,
            Asymmetry<?,UnaryOperator<O>> a8,
            Asymmetry<?,UnaryOperator<O>> a9,
            Asymmetry<?,UnaryOperator<O>> a10,
            Asymmetry<?,UnaryOperator<O>> a11,
            Asymmetry<?,UnaryOperator<O>> a12,
            Asymmetry<?,UnaryOperator<O>> a13,
            Asymmetry<?,UnaryOperator<O>> a14,
            Asymmetry<?,UnaryOperator<O>> a15,
            Asymmetry<?,UnaryOperator<O>> a16) {
            return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16);
        }
    }
}
