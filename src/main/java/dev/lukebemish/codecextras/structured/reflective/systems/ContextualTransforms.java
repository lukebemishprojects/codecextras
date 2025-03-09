package dev.lukebemish.codecextras.structured.reflective.systems;

import com.google.common.collect.ImmutableList;
import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.reflective.CreationContext;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public interface ContextualTransforms extends ReflectiveStructureCreator.CreatorSystem<List<ReflectiveStructureCreator.ContextualTransform>, Supplier<List<ReflectiveStructureCreator.ContextualTransform>>, ContextualTransforms.Type> {
    Type TYPE = new Type();

    @Override
    default Type type() {
        return TYPE;
    }

    final class Type implements ReflectiveStructureCreator.CreatorSystem.Type<List<ReflectiveStructureCreator.ContextualTransform>, Supplier<List<ReflectiveStructureCreator.ContextualTransform>>, Type> {
        private Type() {}
        private static final Key<Type> KEY = Key.create("contextual_transforms");

        @Override
        public Supplier<List<ReflectiveStructureCreator.ContextualTransform>> merge(Supplier<List<ReflectiveStructureCreator.ContextualTransform>> a, Supplier<List<ReflectiveStructureCreator.ContextualTransform>> b) {
            return () -> {
                var out = a.get();
                out.addAll(b.get());
                return out;
            };
        }

        @Override
        public Supplier<List<ReflectiveStructureCreator.ContextualTransform>> empty() {
            return ArrayList::new;
        }

        @Override
        public Key<Type> key() {
            return KEY;
        }

        @Override
        public List<ReflectiveStructureCreator.ContextualTransform> bake(Supplier<List<ReflectiveStructureCreator.ContextualTransform>> value, CreationContext context) {
            var temporary = new ArrayList<>(value.get());
            temporary.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
            return ImmutableList.copyOf(temporary);
        }
    }
}
