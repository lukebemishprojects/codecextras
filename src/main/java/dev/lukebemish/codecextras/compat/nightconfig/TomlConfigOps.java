package dev.lukebemish.codecextras.compat.nightconfig;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.lukebemish.codecextras.comments.CommentOps;
import dev.lukebemish.codecextras.companion.Companion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TomlConfigOps extends CommentedNightConfigOps<CommentedConfig> {
    public static final TomlConfigOps INSTANCE = new TomlConfigOps();
    public static final TomlConfigOps COMMENTED = new TomlConfigOps() {
        @SuppressWarnings("unchecked")
        @Override
        public <O extends Companion.CompanionToken, C extends Companion<Object, O>> @Nullable C getCompanion(O token) {
            if (token == CommentOps.TOKEN) {
                return (C) TomlConfigCommentOps.INSTANCE;
            }
            return super.getCompanion(token);
        }
    };

    @Override
    protected CommentedConfig newConfig() {
        return TomlFormat.newConfig();
    }

    private static final class TomlConfigCommentOps extends NightConfigCommentOps<CommentedConfig, TomlConfigOps> {
        private static final TomlConfigCommentOps INSTANCE = new TomlConfigCommentOps();

        @Override
        public @NotNull TomlConfigOps parentOps() {
            return TomlConfigOps.INSTANCE;
        }
    }
}
