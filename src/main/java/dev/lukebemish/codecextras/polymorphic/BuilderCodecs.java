package dev.lukebemish.codecextras.polymorphic;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.Asymmetry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

// TODO: document this class...
public final class BuilderCodecs {
	private BuilderCodecs() {}

	public static <O, F> RecordCodecBuilder<Asymmetry<UnaryOperator<O>, O>, Asymmetry<UnaryOperator<O>, F>> operationWrap(MapCodec<F> fieldCodec, BiFunction<O, F, O> fieldSetter, Function<O, F> fieldGetter) {
		return Asymmetry.<F, UnaryOperator<O>, F>split(fieldCodec, f -> o -> fieldSetter.apply(o, f), Function.identity())
			.forGetter(Asymmetry.wrapGetter(fieldGetter));
	}

	public static <O, F> RecordCodecBuilder<Asymmetry<O, O>, Asymmetry<UnaryOperator<O>, F>> wrap(MapCodec<F> fieldCodec, BiFunction<O, F, O> fieldSetter, Function<O, F> fieldGetter) {
		return Asymmetry.<F, UnaryOperator<O>, F>split(fieldCodec, f -> o -> fieldSetter.apply(o, f), Function.identity())
			.forGetter(Asymmetry.wrapGetter(fieldGetter));
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

	public static <O, B> Codec<O> operationCodec(Codec<Asymmetry<UnaryOperator<B>, B>> builderCodec, Function<O, B> deBuilder, Supplier<B> initial, Function<B, DataBuilder<O>> buildFunction) {
		return Asymmetry.join(builderCodec, op -> op.apply(initial.get()), Function.identity())
			.flatXmap(b -> buildFunction.apply(b).buildResult(), o -> {
				B builder = deBuilder.apply(o);
				return DataResult.success(builder);
			});
	}

	public static <O, B> MapCodec<O> operationMapCodec(MapCodec<Asymmetry<UnaryOperator<B>, B>> builderCodec, BiFunction<B, O, B> deBuilder, Supplier<B> initial, Function<B, DataBuilder<O>> buildFunction) {
		return Asymmetry.join(builderCodec, op -> op.apply(initial.get()), Function.identity())
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
		public final DataResult<O> apply(UnaryOperator<O>... operators) {
			O instance = initial.get();
			for (UnaryOperator<O> operator : operators) {
				instance = operator.apply(instance);
			}
			return DataResult.success(instance);
		}

		public DataResult<O> apply1(
				UnaryOperator<O> a1) {
			return apply(a1);
		}

		public DataResult<O> apply2(
				UnaryOperator<O> a1,
				UnaryOperator<O> a2) {
			return apply(a1, a2);
		}

		public DataResult<O> apply3(
				UnaryOperator<O> a1,
				UnaryOperator<O> a2,
				UnaryOperator<O> a3) {
			return apply(a1, a2, a3);
		}

		public DataResult<O> apply4(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4) {
			return apply(a1, a2, a3, a4);
		}

		public DataResult<O> apply5(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4,
			UnaryOperator<O> a5) {
			return apply(a1, a2, a3, a4, a5);
		}

		public DataResult<O> apply6(
				UnaryOperator<O> a1,
				UnaryOperator<O> a2,
				UnaryOperator<O> a3,
				UnaryOperator<O> a4,
				UnaryOperator<O> a5,
				UnaryOperator<O> a6) {
			return apply(a1, a2, a3, a4, a5, a6);
		}

		public DataResult<O> apply7(
				UnaryOperator<O> a1,
				UnaryOperator<O> a2,
				UnaryOperator<O> a3,
				UnaryOperator<O> a4,
				UnaryOperator<O> a5,
				UnaryOperator<O> a6,
				UnaryOperator<O> a7) {
			return apply(a1, a2, a3, a4, a5, a6, a7);
		}

		public DataResult<O> apply8(
				UnaryOperator<O> a1,
				UnaryOperator<O> a2,
				UnaryOperator<O> a3,
				UnaryOperator<O> a4,
				UnaryOperator<O> a5,
				UnaryOperator<O> a6,
				UnaryOperator<O> a7,
				UnaryOperator<O> a8) {
			return apply(a1, a2, a3, a4, a5, a6, a7, a8);
		}

		public DataResult<O> apply9(
				UnaryOperator<O> a1,
				UnaryOperator<O> a2,
				UnaryOperator<O> a3,
				UnaryOperator<O> a4,
				UnaryOperator<O> a5,
				UnaryOperator<O> a6,
				UnaryOperator<O> a7,
				UnaryOperator<O> a8,
				UnaryOperator<O> a9) {
			return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9);
		}

		public DataResult<O> apply10(
				UnaryOperator<O> a1,
				UnaryOperator<O> a2,
				UnaryOperator<O> a3,
				UnaryOperator<O> a4,
				UnaryOperator<O> a5,
				UnaryOperator<O> a6,
				UnaryOperator<O> a7,
				UnaryOperator<O> a8,
				UnaryOperator<O> a9,
				UnaryOperator<O> a10) {
			return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
		}

		public DataResult<O> apply11(
				UnaryOperator<O> a1,
				UnaryOperator<O> a2,
				UnaryOperator<O> a3,
				UnaryOperator<O> a4,
				UnaryOperator<O> a5,
				UnaryOperator<O> a6,
				UnaryOperator<O> a7,
				UnaryOperator<O> a8,
				UnaryOperator<O> a9,
				UnaryOperator<O> a10,
				UnaryOperator<O> a11) {
			return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
		}

		public DataResult<O> apply12(
				UnaryOperator<O> a1,
				UnaryOperator<O> a2,
				UnaryOperator<O> a3,
				UnaryOperator<O> a4,
				UnaryOperator<O> a5,
				UnaryOperator<O> a6,
				UnaryOperator<O> a7,
				UnaryOperator<O> a8,
				UnaryOperator<O> a9,
				UnaryOperator<O> a10,
				UnaryOperator<O> a11,
				UnaryOperator<O> a12) {
			return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);
		}

		public DataResult<O> apply13(
				UnaryOperator<O> a1,
				UnaryOperator<O> a2,
				UnaryOperator<O> a3,
				UnaryOperator<O> a4,
				UnaryOperator<O> a5,
				UnaryOperator<O> a6,
				UnaryOperator<O> a7,
				UnaryOperator<O> a8,
				UnaryOperator<O> a9,
				UnaryOperator<O> a10,
				UnaryOperator<O> a11,
				UnaryOperator<O> a12,
				UnaryOperator<O> a13) {
			return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);
		}

		public DataResult<O> apply14(
				UnaryOperator<O> a1,
				UnaryOperator<O> a2,
				UnaryOperator<O> a3,
				UnaryOperator<O> a4,
				UnaryOperator<O> a5,
				UnaryOperator<O> a6,
				UnaryOperator<O> a7,
				UnaryOperator<O> a8,
				UnaryOperator<O> a9,
				UnaryOperator<O> a10,
				UnaryOperator<O> a11,
				UnaryOperator<O> a12,
				UnaryOperator<O> a13,
				UnaryOperator<O> a14) {
			return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);
		}

		public DataResult<O> apply15(
				UnaryOperator<O> a1,
				UnaryOperator<O> a2,
				UnaryOperator<O> a3,
				UnaryOperator<O> a4,
				UnaryOperator<O> a5,
				UnaryOperator<O> a6,
				UnaryOperator<O> a7,
				UnaryOperator<O> a8,
				UnaryOperator<O> a9,
				UnaryOperator<O> a10,
				UnaryOperator<O> a11,
				UnaryOperator<O> a12,
				UnaryOperator<O> a13,
				UnaryOperator<O> a14,
				UnaryOperator<O> a15) {
			return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);
		}

		public DataResult<O> apply16(
				UnaryOperator<O> a1,
				UnaryOperator<O> a2,
				UnaryOperator<O> a3,
				UnaryOperator<O> a4,
				UnaryOperator<O> a5,
				UnaryOperator<O> a6,
				UnaryOperator<O> a7,
				UnaryOperator<O> a8,
				UnaryOperator<O> a9,
				UnaryOperator<O> a10,
				UnaryOperator<O> a11,
				UnaryOperator<O> a12,
				UnaryOperator<O> a13,
				UnaryOperator<O> a14,
				UnaryOperator<O> a15,
				UnaryOperator<O> a16) {
			return apply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16);
		}

		@SafeVarargs
		public static <O> DataResult<UnaryOperator<O>> operationApply(UnaryOperator<O>... asymmetries) {
			return DataResult.success(o -> {
				for (UnaryOperator<O> operator : asymmetries) {
					o = operator.apply(o);
				}
				return o;
			});
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply1(UnaryOperator<O> a1) {
			return operationApply(a1);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply2(UnaryOperator<O> a1, UnaryOperator<O> a2) {
			return operationApply(a1, a2);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply3(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3) {
			return operationApply(a1, a2, a3);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply4(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4) {
			return operationApply(a1, a2, a3, a4);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply5(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4,
			UnaryOperator<O> a5) {
			return operationApply(a1, a2, a3, a4, a5);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply6(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4,
			UnaryOperator<O> a5,
			UnaryOperator<O> a6) {
			return operationApply(a1, a2, a3, a4, a5, a6);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply7(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4,
			UnaryOperator<O> a5,
			UnaryOperator<O> a6,
			UnaryOperator<O> a7) {
			return operationApply(a1, a2, a3, a4, a5, a6, a7);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply8(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4,
			UnaryOperator<O> a5,
			UnaryOperator<O> a6,
			UnaryOperator<O> a7,
			UnaryOperator<O> a8) {
			return operationApply(a1, a2, a3, a4, a5, a6, a7, a8);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply9(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4,
			UnaryOperator<O> a5,
			UnaryOperator<O> a6,
			UnaryOperator<O> a7,
			UnaryOperator<O> a8,
			UnaryOperator<O> a9) {
			return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply10(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4,
			UnaryOperator<O> a5,
			UnaryOperator<O> a6,
			UnaryOperator<O> a7,
			UnaryOperator<O> a8,
			UnaryOperator<O> a9,
			UnaryOperator<O> a10) {
			return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply11(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4,
			UnaryOperator<O> a5,
			UnaryOperator<O> a6,
			UnaryOperator<O> a7,
			UnaryOperator<O> a8,
			UnaryOperator<O> a9,
			UnaryOperator<O> a10,
			UnaryOperator<O> a11) {
			return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply12(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4,
			UnaryOperator<O> a5,
			UnaryOperator<O> a6,
			UnaryOperator<O> a7,
			UnaryOperator<O> a8,
			UnaryOperator<O> a9,
			UnaryOperator<O> a10,
			UnaryOperator<O> a11,
			UnaryOperator<O> a12) {
			return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply13(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4,
			UnaryOperator<O> a5,
			UnaryOperator<O> a6,
			UnaryOperator<O> a7,
			UnaryOperator<O> a8,
			UnaryOperator<O> a9,
			UnaryOperator<O> a10,
			UnaryOperator<O> a11,
			UnaryOperator<O> a12,
			UnaryOperator<O> a13) {
			return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply14(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4,
			UnaryOperator<O> a5,
			UnaryOperator<O> a6,
			UnaryOperator<O> a7,
			UnaryOperator<O> a8,
			UnaryOperator<O> a9,
			UnaryOperator<O> a10,
			UnaryOperator<O> a11,
			UnaryOperator<O> a12,
			UnaryOperator<O> a13,
			UnaryOperator<O> a14) {
			return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply15(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4,
			UnaryOperator<O> a5,
			UnaryOperator<O> a6,
			UnaryOperator<O> a7,
			UnaryOperator<O> a8,
			UnaryOperator<O> a9,
			UnaryOperator<O> a10,
			UnaryOperator<O> a11,
			UnaryOperator<O> a12,
			UnaryOperator<O> a13,
			UnaryOperator<O> a14,
			UnaryOperator<O> a15) {
			return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);
		}

		public static <O> DataResult<UnaryOperator<O>> operationApply16(
			UnaryOperator<O> a1,
			UnaryOperator<O> a2,
			UnaryOperator<O> a3,
			UnaryOperator<O> a4,
			UnaryOperator<O> a5,
			UnaryOperator<O> a6,
			UnaryOperator<O> a7,
			UnaryOperator<O> a8,
			UnaryOperator<O> a9,
			UnaryOperator<O> a10,
			UnaryOperator<O> a11,
			UnaryOperator<O> a12,
			UnaryOperator<O> a13,
			UnaryOperator<O> a14,
			UnaryOperator<O> a15,
			UnaryOperator<O> a16) {
			return operationApply(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16);
		}
	}
}
