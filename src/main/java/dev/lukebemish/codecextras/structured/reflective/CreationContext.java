package dev.lukebemish.codecextras.structured.reflective;

import dev.lukebemish.codecextras.structured.Structure;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class CreationContext {
    private final Set<CreationOption> options;
    private final Map<Class<? extends Annotation>, Function<?, List<ReflectiveStructureCreator.AnnotationInfo<?>>>> annotationParsers;
    private final List<ReflectiveStructureCreator.ContextualTransform> contextualTransforms;

    CreationContext(Collection<CreationOption> options, Map<Class<? extends Annotation>, Function<?, List<ReflectiveStructureCreator.AnnotationInfo<?>>>> annotationParsers, List<ReflectiveStructureCreator.ContextualTransform> contextualTransforms) {
        this.options = Set.copyOf(options);
        this.annotationParsers = annotationParsers;
        this.contextualTransforms = contextualTransforms;
    }

    public boolean hasOption(CreationOption option) {
        return options.contains(option);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<ReflectiveStructureCreator.AnnotationInfo<?>> parseAnnotation(Annotation annotation) {
        var function = (Function) annotationParsers.get(annotation.annotationType());
        if (function != null) {
            return (List<ReflectiveStructureCreator.AnnotationInfo<?>>) function.apply(annotation);
        }
        return List.of();
    }

    public Function<Structure<?>, Structure<?>> contextualTransform(List<AnnotatedElement> elements) {
        Function<Structure<?>, Structure<?>> function = Function.identity();
        for (var transform : contextualTransforms) {
            function = function.andThen(transform.transform(elements, this));
        }
        return function;
    }
}
