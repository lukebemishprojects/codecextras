package dev.lukebemish.codecextras.structured;

import dev.lukebemish.codecextras.types.Identity;
import java.util.Optional;

public class Annotation {
    /**
     * A comment that a field in a structure should be serialized with.
     */
    public static final Key<String> COMMENT = Key.create("comment");
    /**
     * A human-readable title for a part of a structure.
     */
    public static final Key<String> TITLE = Key.create("title");
    /**
     * A human-readable description for a part of a structure; if missing, falls back to {@link #COMMENT}.
     */
    public static final Key<String> DESCRIPTION = Key.create("description");

    public static <A> Optional<A> get(Keys<Identity.Mu, Object> keys, Key<A> key) {
        return keys.get(key).map(app -> Identity.unbox(app).value());
    }

    public static Keys<Identity.Mu, Object> empty() {
        return EMPTY;
    }

    private static final Keys<Identity.Mu, Object> EMPTY = Keys.<Identity.Mu, Object>builder().build();

    private Annotation() {}
}
