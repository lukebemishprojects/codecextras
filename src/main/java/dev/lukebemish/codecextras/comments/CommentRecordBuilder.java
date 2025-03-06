package dev.lukebemish.codecextras.comments;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
import dev.lukebemish.codecextras.companion.AccompaniedOps;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface CommentRecordBuilder<T> extends RecordBuilder<T> {
    CommentRecordBuilder<T> comment(T key, T value);

    final class MapBuilder<T> extends AbstractUniversalBuilder<T, ImmutableMap.Builder<T, T>> implements CommentRecordBuilder<T> {
        private final ImmutableMap.Builder<T, T> commentsBuilder = ImmutableMap.builder();

        public MapBuilder(final DynamicOps<T> ops) {
            super(ops);
        }

        @Override
        public CommentRecordBuilder<T> comment(T key, T value) {
            commentsBuilder.put(key, value);
            return this;
        }

        @Override
        protected ImmutableMap.Builder<T, T> initBuilder() {
            return ImmutableMap.builder();
        }

        @Override
        protected ImmutableMap.Builder<T, T> append(final T key, final T value, final ImmutableMap.Builder<T, T> builder) {
            return builder.put(key, value);
        }

        @Override
        protected DataResult<T> build(final ImmutableMap.Builder<T, T> builder, final T prefix) {
            var built = ops().mergeToMap(prefix, builder.buildKeepingLast());
            var comments = commentsBuilder.build();
            return AccompaniedOps.find(this.ops()).map(accompaniedOps -> {
                Optional<CommentOps<T>> commentOps = accompaniedOps.getCompanion(CommentOps.TOKEN);
                if (commentOps.isPresent()) {
                    return built.flatMap(t ->
                        commentOps.get().commentToMap(t, comments.entrySet().stream().collect(
                            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                        ))
                    );
                }
                return built;
            }).orElse(built);
        }
    }
}
