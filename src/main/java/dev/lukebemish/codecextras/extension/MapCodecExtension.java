package dev.lukebemish.codecextras.extension;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.lukebemish.autoextension.AutoExtension;
import dev.lukebemish.codecextras.Asymmetry;
import dev.lukebemish.codecextras.CodecExtras;
import dev.lukebemish.codecextras.comments.CommentMapCodec;

import java.util.Map;
import java.util.function.Function;

@AutoExtension
public final class MapCodecExtension {
    private MapCodecExtension() {}
    public static <O> MapCodec<O> flatten(MapCodec<DataResult<O>> codec) {
        return CodecExtras.flatten(codec);
    }

    public static <O> MapCodec<DataResult<O>> raise(MapCodec<O> codec) {
        return CodecExtras.raise(codec);
    }

    public static <O> MapCodec<O> withComment(MapCodec<O> codec, Map<String, String> comments) {
        return CommentMapCodec.of(codec, comments);
    }

    public static <A> MapCodec<A> withComment(MapCodec<A> codec, String comment) {
        return CommentMapCodec.of(codec, comment);
    }

    public static <E0, D0, E1, D1> MapCodec<Asymmetry<D1, E1>> mapAsymmetry(MapCodec<Asymmetry<D0, E0>> mapCodec, Function<D0, D1> mapDecoding, Function<E1, E0> mapEncoding) {
        return Asymmetry.map(mapCodec, mapDecoding, mapEncoding);
    }

    public static <E0, D0, E1, D1> MapCodec<Asymmetry<D1, E1>> flatMapAsymmetry(MapCodec<Asymmetry<D0, E0>> mapCodec, Function<D0, DataResult<D1>> mapDecoding, Function<E1, DataResult<E0>> mapEncoding) {
        return Asymmetry.flatMap(mapCodec, mapDecoding, mapEncoding);
    }

    public static <E0, D, E1> MapCodec<Asymmetry<D, E1>> mapEncodingAsymmetry(MapCodec<Asymmetry<D, E0>> mapCodec, Function<E1, E0> mapEncoding) {
        return Asymmetry.mapEncoding(mapCodec, mapEncoding);
    }

    public static <E0, D, E1> MapCodec<Asymmetry<D, E1>> flatMapEncodingAsymmetry(MapCodec<Asymmetry<D, E0>> mapCodec, Function<E1, DataResult<E0>> mapEncoding) {
        return Asymmetry.flatMapEncoding(mapCodec, mapEncoding);
    }

    public static <E, D0, D1> MapCodec<Asymmetry<D1, E>> mapDecodingAsymmetry(MapCodec<Asymmetry<D0, E>> mapCodec, Function<D0, D1> mapDecoding) {
        return Asymmetry.mapDecoding(mapCodec, mapDecoding);
    }

    public static <E, D0, D1> MapCodec<Asymmetry<D1, E>> flatMapDecodingAsymmetry(MapCodec<Asymmetry<D0, E>> mapCodec, Function<D0, DataResult<D1>> mapDecoding) {
        return Asymmetry.flatMapDecoding(mapCodec, mapDecoding);
    }

    public static <E, D, O> MapCodec<O> joinAsymmetry(MapCodec<Asymmetry<D, E>> mapCodec, Function<D, O> mapDecoding, Function<O, E> mapEncoding) {
        return Asymmetry.join(mapCodec, mapDecoding, mapEncoding);
    }

    public static <E, D, O> MapCodec<O> flatJoinAsymmetry(MapCodec<Asymmetry<D, E>> mapCodec, Function<D, DataResult<O>> mapDecoding, Function<O, DataResult<E>> mapEncoding) {
        return Asymmetry.flatJoin(mapCodec, mapDecoding, mapEncoding);
    }

    public static <E, D, O> MapCodec<Asymmetry<D, E>> splitAsymmetry(MapCodec<O> mapCodec, Function<O, D> mapDecoding, Function<E, O> mapEncoding) {
        return Asymmetry.split(mapCodec, mapDecoding, mapEncoding);
    }

    public static <E, D, O> MapCodec<Asymmetry<D, E>> flatSplitAsymmetry(MapCodec<O> mapCodec, Function<O, DataResult<D>> mapDecoding, Function<E, DataResult<O>> mapEncoding) {
        return Asymmetry.flatSplit(mapCodec, mapDecoding, mapEncoding);
    }
}
