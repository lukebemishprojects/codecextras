package dev.lukebemish.codecextras.groovy.structured.reflective.implementation;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableMap;
import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.reflective.CreationContext;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import dev.lukebemish.codecextras.structured.reflective.systems.AnnotationParsers;
import dev.lukebemish.codecextras.structured.reflective.systems.FallbackPropertyDiscoverers;
import groovy.lang.Groovydoc;
import groovy.lang.MetaBeanProperty;
import groovy.lang.MetaClass;
import groovy.lang.MetaProperty;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.codehaus.groovy.reflection.CachedField;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@AutoService(ReflectiveStructureCreator.class)
@ApiStatus.Internal
public class GroovyReflectiveStructureCreator implements ReflectiveStructureCreator {
    private static final MethodHandle META_PROPERTY_GET;
    private static final MethodHandle META_PROPERTY_SET;

    static {
        var lookup = MethodHandles.lookup();
        try {
            META_PROPERTY_GET = lookup.findVirtual(MetaProperty.class, "getProperty", MethodType.methodType(Object.class, Object.class));
            META_PROPERTY_SET = lookup.findVirtual(MetaProperty.class, "setProperty", MethodType.methodType(void.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Keys<CreatorSystem.Mu, Object> systems() {
        var builder = Keys.<CreatorSystem.Mu, Object>builder();
        builder.add(AnnotationParsers.TYPE.key(), new AnnotationParsers() {
            @Override
            public Function<CreationContext, Map<Class<? extends Annotation>, Function<?, List<AnnotationInfo<?>>>>> make() {
                return context -> {
                    var builder = ImmutableMap.<Class<? extends Annotation>, Function<?, List<AnnotationInfo<?>>>>builder();
                    return builder
                        .put(Groovydoc.class, (Groovydoc annotation) -> List.<AnnotationInfo<?>>of(new AnnotationInfo<String>() {
                            @Override
                            public Key<String> key() {
                                return dev.lukebemish.codecextras.structured.Annotation.COMMENT;
                            }

                            @Override
                            public String value() {
                                String groovydoc = annotation.value();
                                if (groovydoc.startsWith("/**@")) {
                                    groovydoc = groovydoc.substring(4, groovydoc.length() - 2);
                                } else if (groovydoc.startsWith("/**")) {
                                    groovydoc = groovydoc.substring(3, groovydoc.length() - 2);
                                }
                                groovydoc = groovydoc.stripTrailing();
                                List<String> lines = new ArrayList<>(groovydoc.lines().toList());
                                while (lines.getFirst().isBlank()) {
                                    lines.removeFirst();
                                }
                                while (lines.getLast().isBlank()) {
                                    lines.removeLast();
                                }
                                lines = lines.stream().map(line -> {
                                    line = line.trim();
                                    if (line.startsWith("*")) {
                                        line = line.substring(1);
                                    }
                                    return line;
                                }).toList();
                                var minSpaceCount = lines.stream().mapToInt(line -> {
                                    int count = 0;
                                    while (count < line.length() && line.charAt(count) == ' ') {
                                        count++;
                                    }
                                    return count;
                                }).min().orElse(0);
                                return lines.stream().map(line ->
                                    line.substring(minSpaceCount)
                                ).collect(Collectors.joining("\n"));
                            }
                        }))
                        .build();
                };
            }
        });
        builder.add(FallbackPropertyDiscoverers.TYPE.key(), new FallbackPropertyDiscoverers() {
            @Override
            public Function<CreationContext, List<Discoverer>> make() {
                return context -> List.of(new Discoverer() {
                    @Override
                    public void modifyProperties(Class<?> clazz, Map<String, java.lang.reflect.Type> known, java.lang.reflect.Type[] parameters) {
                        var metaClass = DefaultGroovyMethods.getMetaClass(clazz);
                        if (Objects.equals(known.get("metaClass"), MetaClass.class)) {
                            known.remove("metaClass");
                        }
                        metaClass.getProperties().forEach(metaProperty -> {
                            if ((metaProperty.getModifiers() & Modifier.TRANSIENT) != 0) {
                                return;
                            }
                            var name = metaProperty.getName();
                            var type = metaProperty.getType();
                            var modifiers = metaProperty.getModifiers();
                            // We can only handle bean properties, due to needing to introspect them
                            if ((modifiers & Modifier.PUBLIC) != 0 && metaProperty instanceof MetaBeanProperty metaBeanProperty) {
                                if (!known.containsKey(name)) {
                                    known.put(name, type);
                                }
                            }
                        });
                    }

                    @Override
                    public int priority() {
                        // Low priority -- only discover this if nothing else is found
                        return -10;
                    }

                    @Override
                    public @Nullable MethodHandle getter(Class<?> clazz, String property, boolean exists) {
                        if (exists) {
                            return null;
                        }
                        var metaClass = DefaultGroovyMethods.getMetaClass(clazz);
                        var metaProperty = metaClass.getMetaProperty(property);
                        if (metaProperty instanceof MetaBeanProperty beanProperty && (beanProperty.getModifiers() & Modifier.PUBLIC) != 0) {
                            var getter = beanProperty.getGetter();
                            if (getter != null) {
                                return META_PROPERTY_GET.bindTo(metaProperty);
                            }
                        }
                        return null;
                    }

                    @Override
                    public @Nullable MethodHandle setter(Class<?> clazz, String property, boolean exists) {
                        if (exists) {
                            return null;
                        }
                        var metaClass = DefaultGroovyMethods.getMetaClass(clazz);
                        var metaProperty = metaClass.getMetaProperty(property);
                        if (metaProperty instanceof MetaBeanProperty beanProperty && (beanProperty.getModifiers() & Modifier.PUBLIC) != 0) {
                            var setter = beanProperty.getSetter();
                            if (setter != null) {
                                return META_PROPERTY_SET.bindTo(metaProperty);
                            }
                        }
                        return null;
                    }

                    @Override
                    public List<AnnotatedElement> context(Class<?> clazz, String property) {
                        var metaProperty = DefaultGroovyMethods.getMetaClass(clazz).getMetaProperty(property);
                        var list = new ArrayList<AnnotatedElement>();
                        if (metaProperty instanceof MetaBeanProperty beanProperty) {
                            if (beanProperty.getField() instanceof CachedField field) {
                                list.add(field.getCachedField());
                            }
                        }
                        // And that's about all we can do...
                        return list;
                    }
                });
            }
        });
        return builder.build();
    }
}
