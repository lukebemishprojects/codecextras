package dev.lukebemish.codecextras.extension;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import dev.lukebemish.autoextension.AutoExtension;
import dev.lukebemish.codecextras.CodecExtras;

@AutoExtension
public final class PairExtension {
    private PairExtension() {}

    public static <L, F> DataResult<Pair<L, F>> flattenLeft(Pair<DataResult<L>, F> pair) {
        return CodecExtras.flattenLeft(pair);
    }

    public static <L, F> DataResult<Pair<L, F>> flattenRight(Pair<L, DataResult<F>> pair) {
        return CodecExtras.flattenRight(pair);
    }

    public static <L, F> DataResult<Pair<L, F>> flatten(Pair<DataResult<L>, DataResult<F>> pair) {
        return CodecExtras.flatten(pair);
    }
}
