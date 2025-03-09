package dev.lukebemish.codecextras.groovy.structured.reflective.implementation;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableMap;
import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.reflective.CreationContext;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import dev.lukebemish.codecextras.structured.reflective.systems.AnnotationParsers;
import groovy.lang.Groovydoc;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.ApiStatus;

@AutoService(ReflectiveStructureCreator.class)
@ApiStatus.Internal
public class GroovyReflectiveStructureCreator implements ReflectiveStructureCreator {
    @Override
    public Keys<CreatorSystem.Mu, Object> systems() {
        var builder = Keys.<CreatorSystem.Mu, Object>builder();
        builder.add(AnnotationParsers.TYPE.key(), new AnnotationParsers() {
            @Override
            public Function<CreationContext, Map<Class<? extends Annotation>, Function<?, List<AnnotationInfo<?>>>>> make() {
                return context -> {
                    var builder = ImmutableMap.<Class<? extends Annotation>, Function<?, List<AnnotationInfo<?>>>>builder();
                    return builder
                        .put(Groovydoc.class, (Groovydoc annotation) -> List.<ReflectiveStructureCreator.AnnotationInfo<?>>of(new ReflectiveStructureCreator.AnnotationInfo<String>() {
                            @Override
                            public Key<String> key() {
                                return dev.lukebemish.codecextras.structured.Annotation.COMMENT;
                            }

                            @Override
                            public String value() {
                                return annotation.value();
                            }
                        }))
                        .build();
                };
            }
        });
        return builder.build();
    }
}
