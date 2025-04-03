package dev.lukebemish.codecextras.structured.reflective.systems;

import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.reflective.CreationContext;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A system that allows interpreting annotations representing structure annotations on properties within a reflective structure creation.
 */
public interface AnnotationParsers extends ReflectiveStructureCreator.CreatorSystem<Map<Class<? extends Annotation>, Function<?, List<AnnotationParsers.AnnotationInfo<?>>>>, Function<CreationContext, Map<Class<? extends Annotation>, Function<?, List<AnnotationParsers.AnnotationInfo<?>>>>>, AnnotationParsers.Type> {
    /**
     * The {@link dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator.CreatorSystem.Type} for this system.
     */
    Type TYPE = new Type();

    @Override
    default Type type() {
        return TYPE;
    }

    /**
     * A result of interpretation
     * @param <T> the value represented
     */
    interface AnnotationInfo<T> {
        /**
         * {@return the key of the extracted structure annotation}
         */
        Key<T> key();
        /**
         * {@return the value of the extracted structure annotation}
         */
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
