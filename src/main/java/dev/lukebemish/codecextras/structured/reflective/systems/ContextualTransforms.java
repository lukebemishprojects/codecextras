package dev.lukebemish.codecextras.structured.reflective.systems;

import com.google.common.collect.ImmutableList;
import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.reflective.CreationContext;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ContextualTransforms extends ReflectiveStructureCreator.CreatorSystem<List<ContextualTransforms.ContextualTransform>, Supplier<List<ContextualTransforms.ContextualTransform>>, ContextualTransforms.Type> {
    Type TYPE = new Type();

    @Override
    default Type type() {
        return TYPE;
    }

    interface ContextualTransform {
        Function<Structure<?>, Structure<?>> transform(List<AnnotatedElement> elements, CreationContext context);
        default int priority() {
            return 0;
        }
    }

    final class Type implements ReflectiveStructureCreator.CreatorSystem.Type<List<ContextualTransform>, Supplier<List<ContextualTransform>>, Type> {
        private Type() {}
        private static final Key<Type> KEY = Key.create("contextual_transforms");

        @Override
        public Supplier<List<ContextualTransform>> merge(Supplier<List<ContextualTransform>> a, Supplier<List<ContextualTransform>> b) {
            return () -> {
                var out = a.get();
                out.addAll(b.get());
                return out;
            };
        }

        @Override
        public Supplier<List<ContextualTransform>> empty() {
            return ArrayList::new;
        }

        @Override
        public Key<Type> key() {
            return KEY;
        }

        @Override
        public List<ContextualTransform> bake(Supplier<List<ContextualTransform>> value, CreationContext context) {
            var temporary = new ArrayList<>(value.get());
            temporary.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
            return ImmutableList.copyOf(temporary);
        }
    }
}
