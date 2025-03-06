package dev.lukebemish.codecextras.structured.reflective;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public final class CreationOptions {
    private final Set<CreationOption> options;
    private final Map<Class<? extends Annotation>, Function<?, ReflectiveStructureCreator.AnnotationInfo<?>>> annotationParsers;

    CreationOptions(Collection<CreationOption> options, Map<Class<? extends Annotation>, Function<?, ReflectiveStructureCreator.AnnotationInfo<?>>> annotationParsers) {
        this.options = Set.copyOf(options);
        this.annotationParsers = annotationParsers;
    }

    public boolean hasOption(CreationOption option) {
        return options.contains(option);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ReflectiveStructureCreator.@Nullable AnnotationInfo<?> parseAnnotation(Annotation annotation) {
        var function = (Function) annotationParsers.get(annotation.annotationType());
        if (function != null) {
            return (ReflectiveStructureCreator.AnnotationInfo<?>) function.apply(annotation);
        }
        return null;
    }
}
