package dev.lukebemish.codecextras.structured.schema;

import dev.lukebemish.codecextras.structured.Key;

public final class SchemaAnnotations {
    private SchemaAnnotations() {}

    /**
     * The key used with {@code $ref} and {@code $defs} to reuse this schema throughout a root schema.
     * Only one schema in a nested structure should have this key.
     */
    public static final Key<String> REUSE_KEY = Key.create("reuseKey");
}
