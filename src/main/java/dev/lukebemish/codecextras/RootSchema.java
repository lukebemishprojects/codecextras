package dev.lukebemish.codecextras;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A schema that does not register any block entities or entities by default. Useful as a base schema for a
 * {@link com.mojang.datafixers.DataFixerBuilder}.
 */
public abstract class RootSchema extends Schema {
    public RootSchema(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    @Override
    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        return Map.of();
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        return Map.of();
    }
}
