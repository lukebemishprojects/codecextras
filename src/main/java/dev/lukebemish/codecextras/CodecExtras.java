package dev.lukebemish.codecextras;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import java.util.function.Function;

public class CodecExtras {
    public static <O> Codec<O> flatten(Codec<DataResult<O>> codec) {
        return codec.flatXmap(Function.identity(), o -> DataResult.success(DataResult.success(o)));
    }

    public static <O> MapCodec<O> flatten(MapCodec<DataResult<O>> codec) {
        return codec.flatXmap(Function.identity(), o -> DataResult.success(DataResult.success(o)));
    }

    public static <O> Codec<DataResult<O>> raise(Codec<O> codec) {
        return codec.flatXmap(o -> DataResult.success(DataResult.success(o)), Function.identity());
    }

    public static <O> MapCodec<DataResult<O>> raise(MapCodec<O> codec) {
        return codec.flatXmap(o -> DataResult.success(DataResult.success(o)), Function.identity());
    }
}
