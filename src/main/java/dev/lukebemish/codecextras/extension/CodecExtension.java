package dev.lukebemish.codecextras.extension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.lukebemish.autoextension.AutoExtension;
import dev.lukebemish.codecextras.Asymmetry;
import dev.lukebemish.codecextras.CodecExtras;

import java.util.function.Function;

@AutoExtension
public final class CodecExtension {
    private CodecExtension() {}

    public static <O> Codec<O> flatten(Codec<DataResult<O>> codec) {
        return CodecExtras.flatten(codec);
    }

    public static <O> Codec<DataResult<O>> raise(Codec<O> codec) {
        return CodecExtras.raise(codec);
    }

    public static <E0, D0, E1, D1> Codec<Asymmetry<D1, E1>> mapAsymmetry(Codec<Asymmetry<D0, E0>> codec, Function<D0, D1> mapDecoding, Function<E1, E0> mapEncoding) {
        return Asymmetry.map(codec, mapDecoding, mapEncoding);
    }

    public static <E0, D0, E1, D1> Codec<Asymmetry<D1, E1>> flatMapAsymmetry(Codec<Asymmetry<D0, E0>> codec, Function<D0, DataResult<D1>> mapDecoding, Function<E1, DataResult<E0>> mapEncoding) {
        return Asymmetry.flatMap(codec, mapDecoding, mapEncoding);
    }

    public static <E0, D, E1> Codec<Asymmetry<D, E1>> mapEncodingAsymmetry(Codec<Asymmetry<D, E0>> codec, Function<E1, E0> mapEncoding) {
        return Asymmetry.mapEncoding(codec, mapEncoding);
    }

    public static <E0, D, E1> Codec<Asymmetry<D, E1>> flatMapEncodingAsymmetry(Codec<Asymmetry<D, E0>> codec, Function<E1, DataResult<E0>> mapEncoding) {
        return Asymmetry.flatMapEncoding(codec, mapEncoding);
    }

    public static <E, D0, D1> Codec<Asymmetry<D1, E>> mapDecodingAsymmetry(Codec<Asymmetry<D0, E>> codec, Function<D0, D1> mapDecoding) {
        return Asymmetry.mapDecoding(codec, mapDecoding);
    }

    public static <E, D0, D1> Codec<Asymmetry<D1, E>> flatMapDecodingAsymmetry(Codec<Asymmetry<D0, E>> codec, Function<D0, DataResult<D1>> mapDecoding) {
        return Asymmetry.flatMapDecoding(codec, mapDecoding);
    }

    public static <E, D, O> Codec<O> joinAsymmetry(Codec<Asymmetry<D, E>> codec, Function<D, O> mapDecoding, Function<O, E> mapEncoding) {
        return Asymmetry.join(codec, mapDecoding, mapEncoding);
    }

    public static <E, D, O> Codec<O> flatJoinAsymmetry(Codec<Asymmetry<D, E>> codec, Function<D, DataResult<O>> mapDecoding, Function<O, DataResult<E>> mapEncoding) {
        return Asymmetry.flatJoin(codec, mapDecoding, mapEncoding);
    }

    public static <E, D, O> Codec<Asymmetry<D, E>> splitAsymmetry(Codec<O> codec, Function<O, D> mapDecoding, Function<E, O> mapEncoding) {
        return Asymmetry.split(codec, mapDecoding, mapEncoding);
    }

    public static <E, D, O> Codec<Asymmetry<D, E>> flatSplitAsymmetry(Codec<O> codec, Function<O, DataResult<D>> mapDecoding, Function<E, DataResult<O>> mapEncoding) {
        return Asymmetry.flatSplit(codec, mapDecoding, mapEncoding);
    }
}
