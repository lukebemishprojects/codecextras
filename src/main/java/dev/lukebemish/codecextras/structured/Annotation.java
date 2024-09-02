package dev.lukebemish.codecextras.structured;

import dev.lukebemish.codecextras.types.Identity;
import java.util.Optional;

/**
 * Annotations are metadata that can be attached to parts of structures to provide additional information to interpreters.
 * This class contains some annotation keys recognized by built-in interpreters in CodecExtras.
 */
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

    /**
     * Retrieve an annotation value, if present, from a set of annotations.
     * @param keys the annotations to search
     * @param key the key of the annotation to retrieve
     * @return the value of the annotation, if present
     * @param <A> the type of the annotation value
     */
    public static <A> Optional<A> get(Keys<Identity.Mu, Object> keys, Key<A> key) {
        return keys.get(key).map(app -> Identity.unbox(app).value());
    }

    /**
     * {@return an empty annotation set}
     */
    public static Keys<Identity.Mu, Object> empty() {
        return EMPTY;
    }

    private static final Keys<Identity.Mu, Object> EMPTY = Keys.<Identity.Mu, Object>builder().build();

    private Annotation() {}
}
