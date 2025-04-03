package dev.lukebemish.codecextras.structured.reflective;

import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.reflective.systems.AnnotationParsers;
import dev.lukebemish.codecextras.structured.reflective.systems.ContextualTransforms;
import dev.lukebemish.codecextras.structured.reflective.systems.CreationOptions;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Context provided to systems in reflective structure creation
 */
public final class CreationContext {
    private final Map<ReflectiveStructureCreator.CreatorSystem.Type<?, ?, ?>, Object> systems;
    private final Map<ReflectiveStructureCreator.CreatorSystem.Type<?, ?, ?>, Object> bakedSystems = new IdentityHashMap<>();

    CreationContext(Map<ReflectiveStructureCreator.CreatorSystem.Type<?, ?, ?>, Object> systems) {
        this.systems = systems;
    }

    /**
     * {@return whether the given creation option is present}
     */
    public boolean hasOption(CreationOption option) {
        var options = retrieve(CreationOptions.TYPE);
        return options.contains(option);
    }

    /**
     * Retrieves a system of the given type, baking it as necessary.
     * @param type the type of the system to retrieve
     * @return the system's results
     * @param <R> the intermediary type of the system
     * @param <T> the result type of the system
     */
    @SuppressWarnings("unchecked")
    public synchronized <R, T> T retrieve(ReflectiveStructureCreator.CreatorSystem.Type<T, R, ?> type) {
        var existingBaked = bakedSystems.get(type);
        if (existingBaked != null) {
            return (T) existingBaked;
        }
        var existing = systems.get(type);
        if (existing != null) {
            var baked = type.bake((R) existing, this);
            bakedSystems.put(type, baked);
            return baked;
        }
        return (T) bakedSystems.computeIfAbsent(type, t -> type.bake(type.empty(), this));
    }

    /**
     * Parses the given annotation using the {@link AnnotationParsers} system.
     * @param annotation the annotation to parse
     * @return the parsed annotation information
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<AnnotationParsers.AnnotationInfo<?>> parseAnnotation(Annotation annotation) {
        var annotationParsers = retrieve(AnnotationParsers.TYPE);
        var function = (Function) annotationParsers.get(annotation.annotationType());
        if (function != null) {
            return (List<AnnotationParsers.AnnotationInfo<?>>) function.apply(annotation);
        }
        return List.of();
    }

    /**
     * Find a contextual transform using the {@link ContextualTransforms} system.
     * @param elements the elements associated with the target property
     * @return a function to transform the property's structure
     */
    public Function<Structure<?>, Structure<?>> contextualTransform(List<AnnotatedElement> elements) {
        var contextualTransforms = retrieve(ContextualTransforms.TYPE);
        Function<Structure<?>, Structure<?>> function = Function.identity();
        for (var transform : contextualTransforms) {
            function = function.andThen(transform.transform(elements, this));
        }
        return function;
    }
}
