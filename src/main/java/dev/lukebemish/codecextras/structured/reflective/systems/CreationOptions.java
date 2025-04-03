package dev.lukebemish.codecextras.structured.reflective.systems;

import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.reflective.CreationContext;
import dev.lukebemish.codecextras.structured.reflective.CreationOption;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A system that allows specifying specific keyed options for the reflective creation of structures.
 */
public interface CreationOptions extends ReflectiveStructureCreator.CreatorSystem<Set<CreationOption>, List<CreationOption>, CreationOptions.Type> {
    /**
     * The {@link dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator.CreatorSystem.Type} for this system.
     */
    Type TYPE = new Type();

    @Override
    default Type type() {
        return TYPE;
    }

    final class Type implements ReflectiveStructureCreator.CreatorSystem.Type<Set<CreationOption>, List<CreationOption>, Type> {
        private Type() {}
        private static final Key<Type> KEY = Key.create("creation_options");

        @Override
        public List<CreationOption> merge(List<CreationOption> a, List<CreationOption> b) {
            a.addAll(b);
            return a;
        }

        @Override
        public List<CreationOption> empty() {
            return new ArrayList<>();
        }

        @Override
        public Key<Type> key() {
            return KEY;
        }

        @Override
        public Set<CreationOption> bake(List<CreationOption> value, CreationContext context) {
            return Set.copyOf(value);
        }

        @Override
        public boolean allowedFromServices() {
            return false;
        }
    }
}
