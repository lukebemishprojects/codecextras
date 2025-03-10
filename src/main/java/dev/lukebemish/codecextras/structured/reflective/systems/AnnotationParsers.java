package dev.lukebemish.codecextras.structured.reflective.systems;

import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.reflective.CreationContext;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface AnnotationParsers extends ReflectiveStructureCreator.CreatorSystem<Map<Class<? extends Annotation>, Function<?, List<AnnotationParsers.AnnotationInfo<?>>>>, Function<CreationContext, Map<Class<? extends Annotation>, Function<?, List<AnnotationParsers.AnnotationInfo<?>>>>>, AnnotationParsers.Type> {
    Type TYPE = new Type();

    @Override
    default Type type() {
        return TYPE;
    }

    interface AnnotationInfo<T> {
        Key<T> key();
        T value();
    }

    final class Type implements ReflectiveStructureCreator.CreatorSystem.IdentityMapType<Class<? extends Annotation>, Function<?, List<AnnotationInfo<?>>>, AnnotationParsers.Type> {
        private Type() {}
        private static final Key<Type> KEY = Key.create("annotation_parsers");

        @Override
        public Key<Type> key() {
            return KEY;
        }
    }
}
