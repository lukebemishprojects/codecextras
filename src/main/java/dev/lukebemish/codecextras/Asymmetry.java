package dev.lukebemish.codecextras;

import com.mojang.datafixers.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract sealed class Asymmetry<D, E> {

    public abstract DataResult<E> encoding();

    public abstract DataResult<D> decoding();

    private static final class Decoding<D, E> extends Asymmetry<D, E> {
        private final D decoding;

        private Decoding(D decoding) {
            this.decoding = decoding;
        }

        @Override
        public DataResult<E> encoding() {
            return DataResult.error(() -> "Attempted to access encoding value of a decoding asymmetry.");
        }

        @Override
        public DataResult<D> decoding() {
            return DataResult.success(decoding);
        }
    }

    private static final class Encoding<D, E> extends Asymmetry<D, E> {
        private final E encoding;

        private Encoding(E encoding) {
            this.encoding = encoding;
        }

        @Override
        public DataResult<E> encoding() {
            return DataResult.success(encoding);
        }

        @Override
        public DataResult<D> decoding() {
            return DataResult.error(() -> "Attempted to access decoding value of an encoding asymmetry.");
        }
    }

    private static final class Discontinuity<D, E> extends Asymmetry<D, E> {
        private static final Discontinuity<?, ?> INSTANCE = new Discontinuity<>();

        private Discontinuity() {}

        @Override
        public DataResult<E> encoding() {
            return DataResult.error(() -> "Attempted to access encoding value of a discontinuous asymmetry.");
        }

        @Override
        public DataResult<D> decoding() {
            return DataResult.error(() -> "Attempted to access decoding value of a discontinuous asymmetry.");
        }
    }

    public static <D, E> Asymmetry<D, E> ofEncoding(E encoding) {
        return new Asymmetry.Encoding<>(encoding);
    }

    public static <D, E> Asymmetry<D, E> ofDecoding(D decoding) {
        return new Asymmetry.Decoding<>(decoding);
    }

    @SuppressWarnings("unchecked")
    public static <D, E> Asymmetry<D, E> discontinuous() {
        return (Asymmetry<D, E>) Discontinuity.INSTANCE;
    }

    public static <D, E> Asymmetry<D, E> ofEncodingResult(DataResult<E> encoding) {
        return encoding.result().map(Asymmetry::<D,E>ofEncoding).orElseGet(Asymmetry::discontinuous);
    }

    public static <D, E> Asymmetry<D, E> ofDecodingResult(DataResult<D> decoding) {
        return decoding.result().map(Asymmetry::<D,E>ofDecoding).orElseGet(Asymmetry::discontinuous);
    }

    public static <E0, D0, E1, D1> Codec<Asymmetry<D1, E1>> map(Codec<Asymmetry<D0, E0>> codec, Function<D0, D1> mapDecoding, Function<E1, E0> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(mapEncoding).map(Asymmetry::ofEncoding));
    }

    public static <E0, D0, E1, D1> Codec<Asymmetry<D1, E1>> flatMap(Codec<Asymmetry<D0, E0>> codec, Function<D0, DataResult<D1>> mapDecoding, Function<E1, DataResult<E0>> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding).map(Asymmetry::ofEncoding));
    }

    public static <E0, D, E1> Codec<Asymmetry<D, E1>> mapEncoding(Codec<Asymmetry<D, E0>> codec, Function<E1, E0> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(mapEncoding).map(Asymmetry::ofEncoding));
    }

    public static <E0, D, E1> Codec<Asymmetry<D, E1>> flatMapEncoding(Codec<Asymmetry<D, E0>> codec, Function<E1, DataResult<E0>> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding).map(Asymmetry::ofEncoding));
    }

    public static <E, D0, D1> Codec<Asymmetry<D1, E>> mapDecoding(Codec<Asymmetry<D0, E>> codec, Function<D0, D1> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(Asymmetry::ofEncoding));
    }

    public static <E, D0, D1> Codec<Asymmetry<D1, E>> flatMapDecoding(Codec<Asymmetry<D0, E>> codec, Function<D0, DataResult<D1>> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(Asymmetry::ofEncoding));
    }

    public static <E, D, O> Codec<O> join(Codec<Asymmetry<D, E>> codec, Function<D, O> mapDecoding, Function<O, E> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding), object -> DataResult.success(ofEncoding(mapEncoding.apply(object))));
    }

    public static <E, D, O> Codec<O> flatJoin(Codec<Asymmetry<D, E>> codec, Function<D, DataResult<O>> mapDecoding, Function<O, DataResult<E>> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding), object -> mapEncoding.apply(object).map(Asymmetry::ofEncoding));
    }

    public static <E, D, O> Codec<Asymmetry<D, E>> split(Codec<O> codec, Function<O, D> mapDecoding, Function<E, O> mapEncoding) {
        return codec.flatXmap(object -> DataResult.success(ofDecoding(mapDecoding.apply(object))), asymmetry -> asymmetry.encoding().map(mapEncoding));
    }

    public static <E, D, O> Codec<Asymmetry<D, E>> flatSplit(Codec<O> codec, Function<O, DataResult<D>> mapDecoding, Function<E, DataResult<O>> mapEncoding) {
        return codec.flatXmap(object -> mapDecoding.apply(object).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding));
    }

    public static <E0, D0, E1, D1> MapCodec<Asymmetry<D1, E1>> map(MapCodec<Asymmetry<D0, E0>> codec, Function<D0, D1> mapDecoding, Function<E1, E0> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(mapEncoding).map(Asymmetry::ofEncoding));
    }

    public static <E0, D0, E1, D1> MapCodec<Asymmetry<D1, E1>> flatMap(MapCodec<Asymmetry<D0, E0>> codec, Function<D0, DataResult<D1>> mapDecoding, Function<E1, DataResult<E0>> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding).map(Asymmetry::ofEncoding));
    }

    public static <E0, D, E1> MapCodec<Asymmetry<D, E1>> mapEncoding(MapCodec<Asymmetry<D, E0>> codec, Function<E1, E0> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(mapEncoding).map(Asymmetry::ofEncoding));
    }

    public static <E0, D, E1> MapCodec<Asymmetry<D, E1>> flatMapEncoding(MapCodec<Asymmetry<D, E0>> codec, Function<E1, DataResult<E0>> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding).map(Asymmetry::ofEncoding));
    }

    public static <E, D0, D1> MapCodec<Asymmetry<D1, E>> mapDecoding(MapCodec<Asymmetry<D0, E>> codec, Function<D0, D1> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(Asymmetry::ofEncoding));
    }

    public static <E, D0, D1> MapCodec<Asymmetry<D1, E>> flatMapDecoding(MapCodec<Asymmetry<D0, E>> codec, Function<D0, DataResult<D1>> mapDecoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().map(Asymmetry::ofEncoding));
    }

    public static <E, D, O> MapCodec<O> join(MapCodec<Asymmetry<D, E>> codec, Function<D, O> mapDecoding, Function<O, E> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().map(mapDecoding), object -> DataResult.success(ofEncoding(mapEncoding.apply(object))));
    }

    public static <E, D, O> MapCodec<O> flatJoin(MapCodec<Asymmetry<D, E>> codec, Function<D, DataResult<O>> mapDecoding, Function<O, DataResult<E>> mapEncoding) {
        return codec.flatXmap(asymmetry -> asymmetry.decoding().flatMap(mapDecoding), object -> mapEncoding.apply(object).map(Asymmetry::ofEncoding));
    }

    public static <E, D, O> MapCodec<Asymmetry<D, E>> split(MapCodec<O> codec, Function<O, D> mapDecoding, Function<E, O> mapEncoding) {
        return codec.flatXmap(object -> DataResult.success(ofDecoding(mapDecoding.apply(object))), asymmetry -> asymmetry.encoding().map(mapEncoding));
    }

    public static <E, D, O> MapCodec<Asymmetry<D, E>> flatSplit(MapCodec<O> codec, Function<O, DataResult<D>> mapDecoding, Function<E, DataResult<O>> mapEncoding) {
        return codec.flatXmap(object -> mapDecoding.apply(object).map(Asymmetry::ofDecoding), asymmetry -> asymmetry.encoding().flatMap(mapEncoding));
    }

    public static <D, E, FD, FE> Function<Asymmetry<D, E>, Asymmetry<FD, FE>> wrapGetter(Function<E, FE> getter) {
        return asymmetryResult -> ofEncodingResult(asymmetryResult.encoding().map(getter));
    }

    public static <E,D,F1,FE1> Function<Asymmetry<F1,FE1>, Asymmetry<D, E>> wrapJoiner(Function<F1, DataResult<D>> function) {
        return f1 -> {
            var d1 = f1.decoding().result();
            //noinspection OptionalIsPresent
            if (d1.isPresent()) {
                return ofDecodingResult(function.apply(d1.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,FE1,FE2> BiFunction<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<D, E>> wrapJoiner(BiFunction<F1,F2,DataResult<D>> function) {
        return (f1, f2) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            if (d1.isPresent() && d2.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,F3,FE1,FE2,FE3> Function3<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<F3,FE3>,
        Asymmetry<D, E>> wrapJoiner(Function3<F1,F2,F3,DataResult<D>> function) {
        return (f1, f2, f3) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            var d3 = f3.decoding().result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,FE1,FE2,FE3,FE4> Function4<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<F3,FE3>,
        Asymmetry<F4,FE4>,
        Asymmetry<D, E>> wrapJoiner(Function4<F1,F2,F3,F4,DataResult<D>> function) {
        return (f1, f2, f3, f4) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            var d3 = f3.decoding().result();
            var d4 = f4.decoding().result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,FE1,FE2,FE3,FE4,FE5> Function5<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<F3,FE3>,
        Asymmetry<F4,FE4>,
        Asymmetry<F5,FE5>,
        Asymmetry<D, E>> wrapJoiner(Function5<F1,F2,F3,F4,F5,DataResult<D>> function) {
        return (f1, f2, f3, f4, f5) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            var d3 = f3.decoding().result();
            var d4 = f4.decoding().result();
            var d5 = f5.decoding().result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,FE1,FE2,FE3,FE4,FE5,FE6> Function6<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<F3,FE3>,
        Asymmetry<F4,FE4>,
        Asymmetry<F5,FE5>,
        Asymmetry<F6,FE6>,
        Asymmetry<D, E>> wrapJoiner(Function6<F1,F2,F3,F4,F5,F6,DataResult<D>> function) {
        return (f1, f2, f3, f4, f5, f6) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            var d3 = f3.decoding().result();
            var d4 = f4.decoding().result();
            var d5 = f5.decoding().result();
            var d6 = f6.decoding().result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,FE1,FE2,FE3,FE4,FE5,FE6,FE7> Function7<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<F3,FE3>,
        Asymmetry<F4,FE4>,
        Asymmetry<F5,FE5>,
        Asymmetry<F6,FE6>,
        Asymmetry<F7,FE7>,
        Asymmetry<D, E>> wrapJoiner(Function7<F1,F2,F3,F4,F5,F6,F7,DataResult<D>> function) {
        return (f1, f2, f3, f4, f5, f6, f7) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            var d3 = f3.decoding().result();
            var d4 = f4.decoding().result();
            var d5 = f5.decoding().result();
            var d6 = f6.decoding().result();
            var d7 = f7.decoding().result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8> Function8<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<F3,FE3>,
        Asymmetry<F4,FE4>,
        Asymmetry<F5,FE5>,
        Asymmetry<F6,FE6>,
        Asymmetry<F7,FE7>,
        Asymmetry<F8,FE8>,
        Asymmetry<D, E>> wrapJoiner(Function8<F1,F2,F3,F4,F5,F6,F7,F8,DataResult<D>> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            var d3 = f3.decoding().result();
            var d4 = f4.decoding().result();
            var d5 = f5.decoding().result();
            var d6 = f6.decoding().result();
            var d7 = f7.decoding().result();
            var d8 = f8.decoding().result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9> Function9<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<F3,FE3>,
        Asymmetry<F4,FE4>,
        Asymmetry<F5,FE5>,
        Asymmetry<F6,FE6>,
        Asymmetry<F7,FE7>,
        Asymmetry<F8,FE8>,
        Asymmetry<F9,FE9>,
        Asymmetry<D, E>> wrapJoiner(Function9<F1,F2,F3,F4,F5,F6,F7,F8,F9,DataResult<D>> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            var d3 = f3.decoding().result();
            var d4 = f4.decoding().result();
            var d5 = f5.decoding().result();
            var d6 = f6.decoding().result();
            var d7 = f7.decoding().result();
            var d8 = f8.decoding().result();
            var d9 = f9.decoding().result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9,FE10> Function10<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<F3,FE3>,
        Asymmetry<F4,FE4>,
        Asymmetry<F5,FE5>,
        Asymmetry<F6,FE6>,
        Asymmetry<F7,FE7>,
        Asymmetry<F8,FE8>,
        Asymmetry<F9,FE9>,
        Asymmetry<F10,FE10>,
        Asymmetry<D, E>> wrapJoiner(Function10<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,DataResult<D>> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            var d3 = f3.decoding().result();
            var d4 = f4.decoding().result();
            var d5 = f5.decoding().result();
            var d6 = f6.decoding().result();
            var d7 = f7.decoding().result();
            var d8 = f8.decoding().result();
            var d9 = f9.decoding().result();
            var d10 = f10.decoding().result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9,FE10,FE11> Function11<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<F3,FE3>,
        Asymmetry<F4,FE4>,
        Asymmetry<F5,FE5>,
        Asymmetry<F6,FE6>,
        Asymmetry<F7,FE7>,
        Asymmetry<F8,FE8>,
        Asymmetry<F9,FE9>,
        Asymmetry<F10,FE10>,
        Asymmetry<F11,FE11>,
        Asymmetry<D, E>> wrapJoiner(Function11<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,DataResult<D>> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            var d3 = f3.decoding().result();
            var d4 = f4.decoding().result();
            var d5 = f5.decoding().result();
            var d6 = f6.decoding().result();
            var d7 = f7.decoding().result();
            var d8 = f8.decoding().result();
            var d9 = f9.decoding().result();
            var d10 = f10.decoding().result();
            var d11 = f11.decoding().result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9,FE10,FE11,FE12> Function12<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<F3,FE3>,
        Asymmetry<F4,FE4>,
        Asymmetry<F5,FE5>,
        Asymmetry<F6,FE6>,
        Asymmetry<F7,FE7>,
        Asymmetry<F8,FE8>,
        Asymmetry<F9,FE9>,
        Asymmetry<F10,FE10>,
        Asymmetry<F11,FE11>,
        Asymmetry<F12,FE12>,
        Asymmetry<D, E>> wrapJoiner(Function12<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,DataResult<D>> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            var d3 = f3.decoding().result();
            var d4 = f4.decoding().result();
            var d5 = f5.decoding().result();
            var d6 = f6.decoding().result();
            var d7 = f7.decoding().result();
            var d8 = f8.decoding().result();
            var d9 = f9.decoding().result();
            var d10 = f10.decoding().result();
            var d11 = f11.decoding().result();
            var d12 = f12.decoding().result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9,FE10,FE11,FE12,FE13> Function13<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<F3,FE3>,
        Asymmetry<F4,FE4>,
        Asymmetry<F5,FE5>,
        Asymmetry<F6,FE6>,
        Asymmetry<F7,FE7>,
        Asymmetry<F8,FE8>,
        Asymmetry<F9,FE9>,
        Asymmetry<F10,FE10>,
        Asymmetry<F11,FE11>,
        Asymmetry<F12,FE12>,
        Asymmetry<F13,FE13>,
        Asymmetry<D, E>> wrapJoiner(Function13<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,DataResult<D>> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            var d3 = f3.decoding().result();
            var d4 = f4.decoding().result();
            var d5 = f5.decoding().result();
            var d6 = f6.decoding().result();
            var d7 = f7.decoding().result();
            var d8 = f8.decoding().result();
            var d9 = f9.decoding().result();
            var d10 = f10.decoding().result();
            var d11 = f11.decoding().result();
            var d12 = f12.decoding().result();
            var d13 = f13.decoding().result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent() && d13.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get(), d13.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9,FE10,FE11,FE12,FE13,FE14> Function14<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<F3,FE3>,
        Asymmetry<F4,FE4>,
        Asymmetry<F5,FE5>,
        Asymmetry<F6,FE6>,
        Asymmetry<F7,FE7>,
        Asymmetry<F8,FE8>,
        Asymmetry<F9,FE9>,
        Asymmetry<F10,FE10>,
        Asymmetry<F11,FE11>,
        Asymmetry<F12,FE12>,
        Asymmetry<F13,FE13>,
        Asymmetry<F14,FE14>,
        Asymmetry<D, E>> wrapJoiner(Function14<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,DataResult<D>> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            var d3 = f3.decoding().result();
            var d4 = f4.decoding().result();
            var d5 = f5.decoding().result();
            var d6 = f6.decoding().result();
            var d7 = f7.decoding().result();
            var d8 = f8.decoding().result();
            var d9 = f9.decoding().result();
            var d10 = f10.decoding().result();
            var d11 = f11.decoding().result();
            var d12 = f12.decoding().result();
            var d13 = f13.decoding().result();
            var d14 = f14.decoding().result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent() && d13.isPresent() && d14.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get(), d13.get(), d14.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,F15,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9,FE10,FE11,FE12,FE13,FE14,FE15> Function15<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<F3,FE3>,
        Asymmetry<F4,FE4>,
        Asymmetry<F5,FE5>,
        Asymmetry<F6,FE6>,
        Asymmetry<F7,FE7>,
        Asymmetry<F8,FE8>,
        Asymmetry<F9,FE9>,
        Asymmetry<F10,FE10>,
        Asymmetry<F11,FE11>,
        Asymmetry<F12,FE12>,
        Asymmetry<F13,FE13>,
        Asymmetry<F14,FE14>,
        Asymmetry<F15,FE15>,
        Asymmetry<D, E>> wrapJoiner(Function15<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,F15,DataResult<D>> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            var d3 = f3.decoding().result();
            var d4 = f4.decoding().result();
            var d5 = f5.decoding().result();
            var d6 = f6.decoding().result();
            var d7 = f7.decoding().result();
            var d8 = f8.decoding().result();
            var d9 = f9.decoding().result();
            var d10 = f10.decoding().result();
            var d11 = f11.decoding().result();
            var d12 = f12.decoding().result();
            var d13 = f13.decoding().result();
            var d14 = f14.decoding().result();
            var d15 = f15.decoding().result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent() && d13.isPresent() && d14.isPresent() && d15.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get(), d13.get(), d14.get(), d15.get()));
            } else {
                return discontinuous();
            }
        };
    }

    public static <E,D,F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,F15,F16,FE1,FE2,FE3,FE4,FE5,FE6,FE7,FE8,FE9,FE10,FE11,FE12,FE13,FE14,FE15,FE16> Function16<
        Asymmetry<F1,FE1>,
        Asymmetry<F2,FE2>,
        Asymmetry<F3,FE3>,
        Asymmetry<F4,FE4>,
        Asymmetry<F5,FE5>,
        Asymmetry<F6,FE6>,
        Asymmetry<F7,FE7>,
        Asymmetry<F8,FE8>,
        Asymmetry<F9,FE9>,
        Asymmetry<F10,FE10>,
        Asymmetry<F11,FE11>,
        Asymmetry<F12,FE12>,
        Asymmetry<F13,FE13>,
        Asymmetry<F14,FE14>,
        Asymmetry<F15,FE15>,
        Asymmetry<F16,FE16>,
        Asymmetry<D, E>> wrapJoiner(Function16<F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,F15,F16,DataResult<D>> function) {
        return (f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16) -> {
            var d1 = f1.decoding().result();
            var d2 = f2.decoding().result();
            var d3 = f3.decoding().result();
            var d4 = f4.decoding().result();
            var d5 = f5.decoding().result();
            var d6 = f6.decoding().result();
            var d7 = f7.decoding().result();
            var d8 = f8.decoding().result();
            var d9 = f9.decoding().result();
            var d10 = f10.decoding().result();
            var d11 = f11.decoding().result();
            var d12 = f12.decoding().result();
            var d13 = f13.decoding().result();
            var d14 = f14.decoding().result();
            var d15 = f15.decoding().result();
            var d16 = f16.decoding().result();
            if (d1.isPresent() && d2.isPresent() && d3.isPresent() && d4.isPresent() && d5.isPresent() && d6.isPresent() && d7.isPresent() && d8.isPresent() && d9.isPresent() && d10.isPresent() && d11.isPresent() && d12.isPresent() && d13.isPresent() && d14.isPresent() && d15.isPresent() && d16.isPresent()) {
                return ofDecodingResult(function.apply(d1.get(), d2.get(), d3.get(), d4.get(), d5.get(), d6.get(), d7.get(), d8.get(), d9.get(), d10.get(), d11.get(), d12.get(), d13.get(), d14.get(), d15.get(), d16.get()));
            } else {
                return discontinuous();
            }
        };
    }
}
