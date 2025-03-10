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

public final class CreationContext {
    private final Map<ReflectiveStructureCreator.CreatorSystem.Type<?, ?, ?>, Object> systems;
    private final Map<ReflectiveStructureCreator.CreatorSystem.Type<?, ?, ?>, Object> bakedSystems = new IdentityHashMap<>();

    CreationContext(Map<ReflectiveStructureCreator.CreatorSystem.Type<?, ?, ?>, Object> systems) {
        this.systems = systems;
    }

    public boolean hasOption(CreationOption option) {
        var options = retrieve(CreationOptions.TYPE);
        return options.contains(option);
    }

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<AnnotationParsers.AnnotationInfo<?>> parseAnnotation(Annotation annotation) {
        var annotationParsers = retrieve(AnnotationParsers.TYPE);
        var function = (Function) annotationParsers.get(annotation.annotationType());
        if (function != null) {
            return (List<AnnotationParsers.AnnotationInfo<?>>) function.apply(annotation);
        }
        return List.of();
    }

    public Function<Structure<?>, Structure<?>> contextualTransform(List<AnnotatedElement> elements) {
        var contextualTransforms = retrieve(ContextualTransforms.TYPE);
        Function<Structure<?>, Structure<?>> function = Function.identity();
        for (var transform : contextualTransforms) {
            function = function.andThen(transform.transform(elements, this));
        }
        return function;
    }
}
