package dev.lukebemish.codecextras;

import com.mojang.datafixers.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class Asymmetry<E, D> {
    private final Either<E, D> either;

    private Asymmetry(Either<E, D> either) {
        this.either = either;
    }

    public DataResult<E> encoding() {
        return either.left().map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Attempted to access encoding value of a decoding asymmetry."));
    }

    public DataResult<D> decoding() {
        return either.right().map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Attempted to access decoding value of an encoding asymmetry."));
    }

    public static <E, D> Asymmetry<E, D> encoding(E encoding) {
        return new Asymmetry<>(Either.left(encoding));
    }

    public static <E, D> Asymmetry<E, D> decoding(D decoding) {
        return new Asymmetry<>(Either.right(decoding));
    }

    public static <E0, D0, E1, D1> Codec<Asymmetry<E1, D1>> map(Codec<Asymmetry<E0, D0>> codec, Function<E1, E0> mapEncoding, Function<D0, D1> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding).map(Asymmetry::decoding), asymmetry -> asymmetry.encoding().map(mapEncoding).map(Asymmetry::encoding));
    }

    public static <E0, D0, E1, D1> Codec<Asymmetry<E1, D1>> flatMap(Codec<Asymmetry<E0, D0>> codec, Function<E1, DataResult<E0>> mapEncoding, Function<D0, DataResult<D1>> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding).map(Asymmetry::decoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding).map(Asymmetry::encoding));
    }

    public static <E0, D, E1> Codec<Asymmetry<E1, D>> mapEncoding(Codec<Asymmetry<E0, D>> codec, Function<E1, E0> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(Asymmetry::decoding), asymmetry -> asymmetry.encoding().map(mapEncoding).map(Asymmetry::encoding));
    }

    public static <E0, D, E1> Codec<Asymmetry<E1, D>> flatMapEncoding(Codec<Asymmetry<E0, D>> codec, Function<E1, DataResult<E0>> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(Asymmetry::decoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding).map(Asymmetry::encoding));
    }

    public static <E, D0, D1> Codec<Asymmetry<E, D1>> mapDecoding(Codec<Asymmetry<E, D0>> codec, Function<D0, D1> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding).map(Asymmetry::decoding), asymmetry -> asymmetry.encoding().map(Asymmetry::encoding));
    }

    public static <E, D0, D1> Codec<Asymmetry<E, D1>> flatMapDecoding(Codec<Asymmetry<E, D0>> codec, Function<D0, DataResult<D1>> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding).map(Asymmetry::decoding), asymmetry -> asymmetry.encoding().map(Asymmetry::encoding));
    }

    public static <E, D, O> Codec<O> join(Codec<Asymmetry<E, D>> codec, Function<O, E> mapEncoding, Function<D, O> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding), object -> DataResult.success(encoding(mapEncoding.apply(object))));
    }

    public static <E, D, O> Codec<O> flatJoin(Codec<Asymmetry<E, D>> codec, Function<O, DataResult<E>> mapEncoding, Function<D, DataResult<O>> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding), object -> mapEncoding.apply(object).map(Asymmetry::encoding));
    }

    public static <E, D, O> Codec<Asymmetry<E, D>> split(Codec<O> codec, Function<E, O> mapEncoding, Function<O, D> mapDecoding) {
        return codec.flatXmap(object -> DataResult.success(decoding(mapDecoding.apply(object))), asymmetry -> asymmetry.encoding().map(mapEncoding));
    }

    public static <E, D, O> Codec<Asymmetry<E, D>> flatSplit(Codec<O> codec, Function<E, DataResult<O>> mapEncoding, Function<O, DataResult<D>> mapDecoding) {
        return codec.flatXmap(object -> mapDecoding.apply(object).map(Asymmetry::decoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding));
    }

    public static <E0, D0, E1, D1> MapCodec<Asymmetry<E1, D1>> map(MapCodec<Asymmetry<E0, D0>> codec, Function<E1, E0> mapEncoding, Function<D0, D1> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding).map(Asymmetry::decoding), asymmetry -> asymmetry.encoding().map(mapEncoding).map(Asymmetry::encoding));
    }

    public static <E0, D0, E1, D1> MapCodec<Asymmetry<E1, D1>> flatMap(MapCodec<Asymmetry<E0, D0>> codec, Function<E1, DataResult<E0>> mapEncoding, Function<D0, DataResult<D1>> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding).map(Asymmetry::decoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding).map(Asymmetry::encoding));
    }

    public static <E0, D, E1> MapCodec<Asymmetry<E1, D>> mapEncoding(MapCodec<Asymmetry<E0, D>> codec, Function<E1, E0> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(Asymmetry::decoding), asymmetry -> asymmetry.encoding().map(mapEncoding).map(Asymmetry::encoding));
    }

    public static <E0, D, E1> MapCodec<Asymmetry<E1, D>> flatMapEncoding(MapCodec<Asymmetry<E0, D>> codec, Function<E1, DataResult<E0>> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(Asymmetry::decoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding).map(Asymmetry::encoding));
    }

    public static <E, D0, D1> MapCodec<Asymmetry<E, D1>> mapDecoding(MapCodec<Asymmetry<E, D0>> codec, Function<D0, D1> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding).map(Asymmetry::decoding), asymmetry -> asymmetry.encoding().map(Asymmetry::encoding));
    }

    public static <E, D0, D1> MapCodec<Asymmetry<E, D1>> flatMapDecoding(MapCodec<Asymmetry<E, D0>> codec, Function<D0, DataResult<D1>> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding).map(Asymmetry::decoding), asymmetry -> asymmetry.encoding().map(Asymmetry::encoding));
    }

    public static <E, D, O> MapCodec<O> join(MapCodec<Asymmetry<E, D>> codec, Function<O, E> mapEncoding, Function<D, O> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding), object -> DataResult.success(encoding(mapEncoding.apply(object))));
    }

    public static <E, D, O> MapCodec<O> flatJoin(MapCodec<Asymmetry<E, D>> codec, Function<O, DataResult<E>> mapEncoding, Function<D, DataResult<O>> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding), object -> mapEncoding.apply(object).map(Asymmetry::encoding));
    }

    public static <E, D, O> MapCodec<Asymmetry<E, D>> split(MapCodec<O> codec, Function<E, O> mapEncoding, Function<O, D> mapDecoding) {
        return codec.flatXmap(object -> DataResult.success(decoding(mapDecoding.apply(object))), asymmetry -> asymmetry.encoding().map(mapEncoding));
    }

    public static <E, D, O> MapCodec<Asymmetry<E, D>> flatSplit(MapCodec<O> codec, Function<E, DataResult<O>> mapEncoding, Function<O, DataResult<D>> mapDecoding) {
        return codec.flatXmap(object -> mapDecoding.apply(object).map(Asymmetry::decoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding));
    }

    public static <E, D, F> Function<DataResult<Asymmetry<E, D>>, DataResult<F>> wrapGetter(Function<E, F> getter) {
        return asymmetryResult -> asymmetryResult.flatMap(asymmetry -> asymmetry.encoding().map(getter));
    }

    public static <F> MapCodec<DataResult<F>> wrapField(MapCodec<F> codec) {
        return codec.flatXmap(o -> DataResult.success(DataResult.success(o)), Function.identity());
    }

    public static <E,D,F1> Function<DataResult<F1>, DataResult<Asymmetry<E, D>>> wrapJoiner(Function<F1, D> function) {
        return f1 -> {
            var d1 = f1.result();
            //noinspection OptionalIsPresent
            if (d1.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2> BiFunction<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(BiFunction<F1,F2,D> function) {
        return (f1, f2) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            if (d1.isPresent() && d2.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2,F3> Function3<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<F3>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(Function3<F1,F2,F3,D> function) {
        return (f1, f2, f3) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            var d3 = f3.result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get(), d3.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2,F3,F4> Function4<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<F3>,
        DataResult<F4>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(Function4<F1,F2,F3,F4,D> function) {
        return (f1, f2, f3, f4) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            var d3 = f3.result();
            var d4 = f4.result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get(), d3.get(), d4.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5> Function5<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<F3>,
        DataResult<F4>,
        DataResult<F5>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(Function5<F1,F2,F3,F4,F5,D> function) {
        return (f1, f2, f3, f4, f5) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            var d3 = f3.result();
            var d4 = f4.result();
            var d5 = f5.result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6> Function6<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<F3>,
        DataResult<F4>,
        DataResult<F5>,
        DataResult<F6>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(Function6<F1,F2,F3,F4,F5,F6,D> function) {
        return (f1, f2, f3, f4, f5, f6) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            var d3 = f3.result();
            var d4 = f4.result();
            var d5 = f5.result();
            var d6 = f6.result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7> Function7<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<F3>,
        DataResult<F4>,
        DataResult<F5>,
        DataResult<F6>,
        DataResult<F7>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(Function7<F1,F2,F3,F4,F5,F6,F7,D> function) {
        return (f1, f2, f3, f4, f5, f6, f7) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            var d3 = f3.result();
            var d4 = f4.result();
            var d5 = f5.result();
            var d6 = f6.result();
            var d7 = f7.result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8> Function8<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<F3>,
        DataResult<F4>,
        DataResult<F5>,
        DataResult<F6>,
        DataResult<F7>,
        DataResult<F8>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(Function8<F1,F2,F3,F4,F5,F6,F7,F8,D> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            var d3 = f3.result();
            var d4 = f4.result();
            var d5 = f5.result();
            var d6 = f6.result();
            var d7 = f7.result();
            var d8 = f8.result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9> Function9<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<F3>,
        DataResult<F4>,
        DataResult<F5>,
        DataResult<F6>,
        DataResult<F7>,
        DataResult<F8>,
        DataResult<F9>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(Function9<F1,F2,F3,F4,F5,F6,F7,F8,F9,D> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            var d3 = f3.result();
            var d4 = f4.result();
            var d5 = f5.result();
            var d6 = f6.result();
            var d7 = f7.result();
            var d8 = f8.result();
            var d9 = f9.result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10> Function10<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<F3>,
        DataResult<F4>,
        DataResult<F5>,
        DataResult<F6>,
        DataResult<F7>,
        DataResult<F8>,
        DataResult<F9>,
        DataResult<F10>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(Function10<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,D> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            var d3 = f3.result();
            var d4 = f4.result();
            var d5 = f5.result();
            var d6 = f6.result();
            var d7 = f7.result();
            var d8 = f8.result();
            var d9 = f9.result();
            var d10 = f10.result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11> Function11<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<F3>,
        DataResult<F4>,
        DataResult<F5>,
        DataResult<F6>,
        DataResult<F7>,
        DataResult<F8>,
        DataResult<F9>,
        DataResult<F10>,
        DataResult<F11>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(Function11<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,D> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            var d3 = f3.result();
            var d4 = f4.result();
            var d5 = f5.result();
            var d6 = f6.result();
            var d7 = f7.result();
            var d8 = f8.result();
            var d9 = f9.result();
            var d10 = f10.result();
            var d11 = f11.result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12> Function12<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<F3>,
        DataResult<F4>,
        DataResult<F5>,
        DataResult<F6>,
        DataResult<F7>,
        DataResult<F8>,
        DataResult<F9>,
        DataResult<F10>,
        DataResult<F11>,
        DataResult<F12>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(Function12<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,D> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            var d3 = f3.result();
            var d4 = f4.result();
            var d5 = f5.result();
            var d6 = f6.result();
            var d7 = f7.result();
            var d8 = f8.result();
            var d9 = f9.result();
            var d10 = f10.result();
            var d11 = f11.result();
            var d12 = f12.result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13> Function13<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<F3>,
        DataResult<F4>,
        DataResult<F5>,
        DataResult<F6>,
        DataResult<F7>,
        DataResult<F8>,
        DataResult<F9>,
        DataResult<F10>,
        DataResult<F11>,
        DataResult<F12>,
        DataResult<F13>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(Function13<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,D> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            var d3 = f3.result();
            var d4 = f4.result();
            var d5 = f5.result();
            var d6 = f6.result();
            var d7 = f7.result();
            var d8 = f8.result();
            var d9 = f9.result();
            var d10 = f10.result();
            var d11 = f11.result();
            var d12 = f12.result();
            var d13 = f13.result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent() && d13.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get(), d13.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14> Function14<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<F3>,
        DataResult<F4>,
        DataResult<F5>,
        DataResult<F6>,
        DataResult<F7>,
        DataResult<F8>,
        DataResult<F9>,
        DataResult<F10>,
        DataResult<F11>,
        DataResult<F12>,
        DataResult<F13>,
        DataResult<F14>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(Function14<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,D> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            var d3 = f3.result();
            var d4 = f4.result();
            var d5 = f5.result();
            var d6 = f6.result();
            var d7 = f7.result();
            var d8 = f8.result();
            var d9 = f9.result();
            var d10 = f10.result();
            var d11 = f11.result();
            var d12 = f12.result();
            var d13 = f13.result();
            var d14 = f14.result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent() && d13.isPresent() && d14.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get(), d13.get(), d14.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,F15> Function15<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<F3>,
        DataResult<F4>,
        DataResult<F5>,
        DataResult<F6>,
        DataResult<F7>,
        DataResult<F8>,
        DataResult<F9>,
        DataResult<F10>,
        DataResult<F11>,
        DataResult<F12>,
        DataResult<F13>,
        DataResult<F14>,
        DataResult<F15>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(Function15<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,F15,D> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            var d3 = f3.result();
            var d4 = f4.result();
            var d5 = f5.result();
            var d6 = f6.result();
            var d7 = f7.result();
            var d8 = f8.result();
            var d9 = f9.result();
            var d10 = f10.result();
            var d11 = f11.result();
            var d12 = f12.result();
            var d13 = f13.result();
            var d14 = f14.result();
            var d15 = f15.result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent() && d13.isPresent() && d14.isPresent() && d15.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get(), d13.get(), d14.get(), d15.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,F15,F16> Function16<
        DataResult<F1>,
        DataResult<F2>,
        DataResult<F3>,
        DataResult<F4>,
        DataResult<F5>,
        DataResult<F6>,
        DataResult<F7>,
        DataResult<F8>,
        DataResult<F9>,
        DataResult<F10>,
        DataResult<F11>,
        DataResult<F12>,
        DataResult<F13>,
        DataResult<F14>,
        DataResult<F15>,
        DataResult<F16>,
        DataResult<Asymmetry<E,D>>> wrapJoiner(Function16<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,F15,F16,D> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16) -> {
            var d1 = f1.result();
            var d2 = f2.result();
            var d3 = f3.result();
            var d4 = f4.result();
            var d5 = f5.result();
            var d6 = f6.result();
            var d7 = f7.result();
            var d8 = f8.result();
            var d9 = f9.result();
            var d10 = f10.result();
            var d11 = f11.result();
            var d12 = f12.result();
            var d13 = f13.result();
            var d14 = f14.result();
            var d15 = f15.result();
            var d16 = f16.result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent() && d13.isPresent() && d14.isPresent() && d15.isPresent() && d16.isPresent()) {
                return DataResult.success(decoding(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get(), d13.get(), d14.get(), d15.get(), d16.get())));
            } else {
                return DataResult.error(() -> "Attempted to use decoding joiner during encoding asymmetry");
            }
        };
    }
}
