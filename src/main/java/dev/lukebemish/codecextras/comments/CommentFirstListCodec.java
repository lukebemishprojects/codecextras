package dev.lukebemish.codecextras.comments;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import dev.lukebemish.codecextras.companion.AccompaniedOps;

import java.util.List;

public final class CommentFirstListCodec<A> implements Codec<List<A>> {
    private final Codec<A> elementCodec;
    private final Codec<List<A>> decodeDelegate;

    private CommentFirstListCodec(Codec<A> elementCodec) {
        this.elementCodec = elementCodec;
        this.decodeDelegate = elementCodec.listOf();
    }

    public static <A> Codec<List<A>> of(Codec<A> codec) {
        return new CommentFirstListCodec<>(codec);
    }

    @Override
    public String toString() {
        return "CommentFirstListCodec["+elementCodec+"]";
    }

    @Override
    public <T> DataResult<Pair<List<A>, T>> decode(DynamicOps<T> ops, T input) {
        return decodeDelegate.decode(ops, input);
    }

    @Override
    public <T> DataResult<T> encode(List<A> input, DynamicOps<T> ops, T prefix) {
        final ListBuilder<T> builder = ops.listBuilder();
        DynamicOps<T> rest;
        if (ops instanceof AccompaniedOps<T> accompaniedOps) {
            CommentOps<T> commentOps = accompaniedOps.getCompanion(CommentOps.TOKEN);
            if (commentOps != null) {
                rest = commentOps.parentOps();
            } else {
                rest = ops;
            }
        } else {
            rest = ops;
        }
        boolean isFirst = true;
        for (A a : input) {
            if (isFirst) {
                builder.add(elementCodec.encodeStart(ops, a));
                isFirst = false;
                continue;
            }
            builder.add(elementCodec.encodeStart(rest, a));
        }
        return builder.build(prefix);
    }
}
