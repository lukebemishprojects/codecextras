package dev.lukebemish.codecextras;

import com.mojang.serialization.*;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.stream.Stream;

public abstract sealed class ExtendedRecordCodecBuilder<A, F, B extends ExtendedRecordCodecBuilder.AppFunction> {

    public static <O, F> ExtendedRecordCodecBuilder<O, F, FinalAppFunction<O, F>> start(MapCodec<F> codec, Function<O, F> getter) {
        return new Endpoint<>(codec, getter);
    }

    public <N> ExtendedRecordCodecBuilder<A, N, FromAppFunction<N, B>> field(MapCodec<N> codec, Function<A, N> getter) {
        return new Delegating<>(codec, getter, this);
    }

    public abstract Codec<A> build(B b);

    public non-sealed interface FinalAppFunction<A, B> extends AppFunction {
        A create(B b);
    }

    public non-sealed interface FromAppFunction<B, C extends AppFunction> extends AppFunction {
        C create(B b);
    }

    public sealed interface AppFunction {}

    protected final MapCodec<F> codec;
    protected final Function<A, F> getter;

    private ExtendedRecordCodecBuilder(MapCodec<F> codec, Function<A, F> getter) {
        this.codec = codec;
        this.getter = getter;
    }

    @NotNull protected abstract <T> RecordBuilder<T> encodeChildren(A input, DynamicOps<T> ops, RecordBuilder<T> prefix);
    @NotNull protected abstract <T> DataResult<A> decodePartial(DynamicOps<T> ops, MapLike<T> input, B b);
    @NotNull protected abstract <T> Stream<T> keysPartial(DynamicOps<T> ops);

    private static final class Endpoint<A, F, B extends ExtendedRecordCodecBuilder.FinalAppFunction<A, F>> extends ExtendedRecordCodecBuilder<A, F, B> {
        private Endpoint(MapCodec<F> codec, Function<A, F> getter) {
            super(codec, getter);
        }

        @Override
        protected @NotNull <T> RecordBuilder<T> encodeChildren(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            F field = getter.apply(input);
            prefix = codec.encode(field, ops, prefix);
            return prefix;
        }

        @Override
        @NotNull
        protected <T> DataResult<A> decodePartial(DynamicOps<T> ops, MapLike<T> input, B b) {
            DataResult<F> field = codec.decode(ops, input);
            return field.map(b::create);
        }

        @Override
        @NotNull
        protected <T> Stream<T> keysPartial(DynamicOps<T> ops) {
            return codec.keys(ops);
        }

        public Codec<A> build(B b) {
            return new MapCodec<A>() {

                @Override
                public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                    return encodeChildren(input, ops, prefix);
                }

                @Override
                public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
                    return decodePartial(ops, input, b);
                }

                @Override
                public <T> Stream<T> keys(DynamicOps<T> ops) {
                    return keysPartial(ops);
                }
            }.codec();
        }
    }

    private static final class Delegating<A, F, D extends ExtendedRecordCodecBuilder.AppFunction, B extends ExtendedRecordCodecBuilder.FromAppFunction<F, D>> extends ExtendedRecordCodecBuilder<A, F, B> {
        private final ExtendedRecordCodecBuilder<A, ?, D> delegate;
        private Delegating(MapCodec<F> codec, Function<A, F> getter, ExtendedRecordCodecBuilder<A, ?, D> delegate) {
            super(codec, getter);
            this.delegate = delegate;
        }

        @Override
        protected @NotNull <T> RecordBuilder<T> encodeChildren(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            F field = getter.apply(input);
            prefix = codec.encode(field, ops, prefix);
            return delegate.encodeChildren(input, ops, prefix);
        }

        @Override
        @NotNull
        protected <T> DataResult<A> decodePartial(DynamicOps<T> ops, MapLike<T> input, B b) {
            DataResult<F> field = codec.decode(ops, input);
            return field.flatMap(f -> {
                D d = b.create(f);
                return delegate.decodePartial(ops, input, d);
            });
        }

        @SuppressWarnings("InfiniteRecursion")
        @Override
        @NotNull
        protected <T> Stream<T> keysPartial(DynamicOps<T> ops) {
            return Stream.concat(keysPartial(ops), delegate.keysPartial(ops));
        }

        @Override
        public Codec<A> build(B b) {
            return new MapCodec<A>() {

                @Override
                public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                    return encodeChildren(input, ops, prefix);
                }

                @Override
                public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
                    return decodePartial(ops, input, b);
                }

                @Override
                public <T> Stream<T> keys(DynamicOps<T> ops) {
                    return keysPartial(ops);
                }
            }.codec();
        }
    }
}
