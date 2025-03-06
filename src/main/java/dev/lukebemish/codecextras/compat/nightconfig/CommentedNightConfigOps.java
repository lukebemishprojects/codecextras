package dev.lukebemish.codecextras.compat.nightconfig;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.mojang.serialization.RecordBuilder;
import dev.lukebemish.codecextras.comments.CommentRecordBuilder;
import dev.lukebemish.codecextras.companion.AccompaniedOps;

public abstract class CommentedNightConfigOps<T extends CommentedConfig> extends NightConfigOps<T> implements AccompaniedOps<Object> {
    @Override
    public T copyConfig(Config config) {
        T out = super.copyConfig(config);
        if (config instanceof CommentedConfig commentedConfig) {
            out.putAllComments(commentedConfig.getComments());
        }
        return out;
    }

    @Override
    public RecordBuilder<Object> mapBuilder() {
        return new CommentRecordBuilder.MapBuilder<>(this);
    }
}
