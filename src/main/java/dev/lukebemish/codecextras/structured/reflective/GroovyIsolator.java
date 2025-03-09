package dev.lukebemish.codecextras.structured.reflective;

import com.google.common.collect.ImmutableMap;
import dev.lukebemish.codecextras.structured.Key;
import groovy.lang.Groovydoc;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

interface GroovyIsolator {
    void collectAnnotationParsers(ImmutableMap.Builder<Class<? extends Annotation>, Function<?, List<ReflectiveStructureCreator.AnnotationInfo<?>>>> builder);

    static @Nullable GroovyIsolator getInstance() {
        try {
            var groovyObject = Class.forName("groovy.lang.GroovyObject");
        } catch (ClassNotFoundException e) {
            return null;
        }

        return new Impl();
    }

    final class Impl implements GroovyIsolator {
        @Override
        public void collectAnnotationParsers(ImmutableMap.Builder<Class<? extends Annotation>, Function<?, List<ReflectiveStructureCreator.AnnotationInfo<?>>>> builder) {
            builder
                .put(Groovydoc.class, (Groovydoc annotation) -> List.<ReflectiveStructureCreator.AnnotationInfo<?>>of(new ReflectiveStructureCreator.AnnotationInfo<String>() {
                    @Override
                    public Key<String> key() {
                        return dev.lukebemish.codecextras.structured.Annotation.COMMENT;
                    }

                    @Override
                    public String value() {
                        return annotation.value();
                    }
                }));
        }
    }
}
