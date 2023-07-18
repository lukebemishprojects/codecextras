package dev.lukebemish.codecextras.comments;

import com.mojang.serialization.*;

import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CommentMapCodec<A> extends MapCodec<A> {
    private final MapCodec<A> delegate;
    private final Map<String, String> comments;

    private CommentMapCodec(MapCodec<A> delegate, Map<String, String> comments) {
        this.delegate = delegate;
        this.comments = comments;
    }

    public static <A> MapCodec<A> of(MapCodec<A> codec, Map<String, String> comments) {
        return new CommentMapCodec<>(codec, comments);
    }

    public static <A> MapCodec<A> of(MapCodec<A> codec, String comment) {
        Map<String, String> map = codec.keys(JsonOps.INSTANCE)
            .filter(json -> json.isJsonPrimitive() && json.getAsJsonPrimitive().isString())
            .map(json -> json.getAsJsonPrimitive().getAsString())
            .collect(Collectors.toMap(Function.identity(), s -> comment));
        return of(codec, map);
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return delegate.keys(ops);
    }

    @Override
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
        return delegate.decode(ops, input);
    }

    @Override
    public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        final RecordBuilder<T> builder = delegate.encode(input, ops, prefix);

        return new RecordBuilder<>() {
            RecordBuilder<T> mutableBuilder = builder;

            @Override
            public DynamicOps<T> ops() {
                return builder.ops();
            }

            @Override
            public RecordBuilder<T> add(T key, T value) {
                mutableBuilder = mutableBuilder.add(key, value);
                return this;
            }

            @Override
            public RecordBuilder<T> add(T key, DataResult<T> value) {
                mutableBuilder = mutableBuilder.add(key, value);
                return this;
            }

            @Override
            public RecordBuilder<T> add(DataResult<T> key, DataResult<T> value) {
                mutableBuilder = mutableBuilder.add(key, value);
                return this;
            }

            @Override
            public RecordBuilder<T> withErrorsFrom(DataResult<?> result) {
                mutableBuilder = mutableBuilder.withErrorsFrom(result);
                return this;
            }

            @Override
            public RecordBuilder<T> setLifecycle(Lifecycle lifecycle) {
                mutableBuilder = mutableBuilder.setLifecycle(lifecycle);
                return this;
            }

            @Override
            public RecordBuilder<T> mapError(UnaryOperator<String> onError) {
                mutableBuilder = mutableBuilder.mapError(onError);
                return this;
            }

            @Override
            public DataResult<T> build(T prefix) {
                DataResult<T> built = builder.build(prefix);
                if (this.ops() instanceof CommentOps<T> commentOps) {
                    return built.flatMap(t ->
                        commentOps.commentToMap(t, comments.entrySet().stream().collect(Collectors.toMap(e ->
                            ops.createString(e.getKey()), e -> ops.createString(e.getValue()))))
                    );
                }
                return built;
            }
        };
    }

    @Override
    public String toString() {
        return "CommentMapCodec["+delegate+"]";
    }
}
