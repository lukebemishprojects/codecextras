package dev.lukebemish.codecextras.structured.reflective;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import dev.lukebemish.codecextras.internal.LayeredServiceLoader;
import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.Structure;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ReflectiveStructureCreator {
    default Map<Class<?>, Creator> creators(CreationContext options) {
        return Map.of();
    }
    default Map<Class<?>, ParameterizedCreator> parameterizedCreators(CreationContext options) {
        return Map.of();
    }
    default List<FlexibleCreator> flexibleCreators(CreationContext options) {
        return List.of();
    }
    default Map<Class<? extends Annotation>, Function<?, List<AnnotationInfo<?>>>> annotationParsers(Set<CreationOption> options) {
        return Map.of();
    }
    default List<ContextualTransform> structureContextualTransforms(Set<CreationOption> options) {
        return List.of();
    }

    interface AnnotationInfo<T> {
        Key<T> key();
        T value();
    }

    interface TypedCreator {
        Structure<?> create();
        Type type();
        Class<?> rawType();
    }

    interface Creator {
        Structure<?> create();
        default Creator andThen(Function<Structure<?>, Structure<?>> function) {
            return () -> function.apply(create());
        }
    }

    interface ContextualTransform {
        Function<Structure<?>, Structure<?>> transform(List<AnnotatedElement> elements, CreationContext context);
    }

    interface FlexibleCreator {
        Structure<?> create(Class<?> exact, TypedCreator[] parameters, Function<Type, Structure<?>> creator);
        boolean supports(Class<?> exact, TypedCreator[] parameters);
        default int priority() {
            return 0;
        }
        default Creator creator(Class<?> exact, TypedCreator[] parameters, Function<Type, Structure<?>> creator) {
            return () -> create(exact, parameters, creator);
        }
    }

    interface ParameterizedCreator {
        Structure<?> create(TypedCreator[] parameters);
        default Creator creator(TypedCreator[] parameters) {
            return () -> create(parameters);
        }
    }

    final class Instance {
        private final Map<Class<?>, Function<CreationContext, Creator>> creators;
        private final Map<Class<?>, Function<CreationContext, ParameterizedCreator>> parameterizedCreators;
        private final List<Function<CreationContext, FlexibleCreator>> flexibleCreators;
        private final Map<Class<? extends Annotation>, Function<Set<CreationOption>, Function<?, List<AnnotationInfo<?>>>>> annotationParsers;
        private final List<Function<Set<CreationOption>, ContextualTransform>> contextualTransforms;
        private final List<CreationOption> options;

        private Instance(Map<Class<?>, Function<CreationContext, Creator>> creators, Map<Class<?>, Function<CreationContext, ParameterizedCreator>> parameterizedCreators, List<Function<CreationContext, FlexibleCreator>> flexibleCreators, Map<Class<? extends Annotation>, Function<Set<CreationOption>, Function<?, List<AnnotationInfo<?>>>>> annotationParsers, List<Function<Set<CreationOption>, ContextualTransform>> contextualTransforms, List<CreationOption> options) {
            this.creators = creators;
            this.parameterizedCreators = parameterizedCreators;
            this.flexibleCreators = flexibleCreators;
            this.annotationParsers = annotationParsers;
            this.contextualTransforms = contextualTransforms;
            this.options = options;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final Map<Class<?>, Function<CreationContext, Creator>> creators = new IdentityHashMap<>();
            private final Map<Class<?>, Function<CreationContext, ParameterizedCreator>> parameterizedCreators = new IdentityHashMap<>();
            private final List<Function<CreationContext, FlexibleCreator>> flexibleCreators = new ArrayList<>();
            private final List<CreationOption> options = new ArrayList<>();
            private final Map<Class<? extends Annotation>, Function<Set<CreationOption>, Function<?, List<AnnotationInfo<?>>>>> annotationParsers = new IdentityHashMap<>();
            private final List<Function<Set<CreationOption>, ContextualTransform>> contextualTransforms = new ArrayList<>();

            private Builder() {}

            public Builder withCreator(Class<?> clazz, Creator creator) {
                creators.put(clazz, ignored -> creator);
                return this;
            }

            public Builder withParameterizedCreator(Class<?> clazz, ParameterizedCreator creator) {
                parameterizedCreators.put(clazz, ignored -> creator);
                return this;
            }

            public Builder withFlexibleCreator(FlexibleCreator creator) {
                flexibleCreators.add(ignored -> creator);
                return this;
            }

            public Builder withOption(CreationOption option) {
                options.add(option);
                return this;
            }

            public <T extends Annotation> Builder withAnnotationParser(Class<T> annotation, Function<T, List<AnnotationInfo<?>>> discoverer) {
                annotationParsers.put(annotation, ignored -> discoverer);
                return this;
            }

            public Builder withContextualTransform(Function<Set<CreationOption>, ContextualTransform> transform) {
                contextualTransforms.add(transform);
                return this;
            }

            public Instance build() {
                return new Instance(creators, parameterizedCreators, flexibleCreators, annotationParsers, contextualTransforms, options);
            }
        }

        private static final LayeredServiceLoader<ReflectiveStructureCreator> SERVICE_LOADER = LayeredServiceLoader.of(ReflectiveStructureCreator.class);

        private final Map<Type, Structure<?>> cachedCreators = new HashMap<>();

        @SuppressWarnings("unchecked")
        public synchronized <T> Structure<T> create(Class<T> clazz) {
            var caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
            List<ReflectiveStructureCreator> services = LayeredServiceLoader.unique(SERVICE_LOADER.at(ReflectiveStructureCreator.class), SERVICE_LOADER.at(clazz), SERVICE_LOADER.at(caller));

            var creationOptions = ImmutableSet.copyOf(this.options);

            Map<Class<? extends Annotation>, Function<?, List<AnnotationInfo<?>>>> annotationParsersMap = new IdentityHashMap<>();
            services.forEach(creator -> annotationParsersMap.putAll(creator.annotationParsers(creationOptions)));
            this.annotationParsers.forEach((key, function) -> {
                annotationParsersMap.put(key, function.apply(creationOptions));
            });

            List<ContextualTransform> contextualTransformList = new ArrayList<>();
            services.forEach(creator -> contextualTransformList.addAll(creator.structureContextualTransforms(creationOptions)));
            this.contextualTransforms.forEach(function -> {
                contextualTransformList.add(function.apply(creationOptions));
            });

            var options = new CreationContext(this.options, annotationParsersMap, contextualTransformList);

            Map<Class<?>, Creator> creatorsMap = new IdentityHashMap<>();
            Map<Class<?>, ParameterizedCreator> parameterizedCreatorsMap = new IdentityHashMap<>();
            List<FlexibleCreator> flexibleCreatorsList = new ArrayList<>();
            services.forEach(creator -> {
                creatorsMap.putAll(creator.creators(options));
                parameterizedCreatorsMap.putAll(creator.parameterizedCreators(options));
                flexibleCreatorsList.addAll(creator.flexibleCreators(options));
            });

            this.creators.forEach((key, function) -> {
                creatorsMap.put(key, function.apply(options));
            });
            this.parameterizedCreators.forEach((key, function) -> {
                parameterizedCreatorsMap.put(key, function.apply(options));
            });
            this.flexibleCreators.forEach(function -> {
                flexibleCreatorsList.add(function.apply(options));
            });

            flexibleCreatorsList.sort((a, b) -> Integer.compare(b.priority(), a.priority()));

            var recursionCache = new HashMap<Type, Structure<?>>();

            return (Structure<T>) forType(cachedCreators, recursionCache, clazz, creatorsMap, parameterizedCreatorsMap, flexibleCreatorsList);
        }
    }

    static <T> Structure<T> create(Class<T> clazz) {
        return Instance.builder().build().create(clazz);
    }

    private static Structure<?> forType(Map<Type, Structure<?>> cachedCreators, Map<Type, Structure<?>> recursionCache, Type type, Map<Class<?>, Creator> creatorsMap, Map<Class<?>, ParameterizedCreator> parameterizedCreatorsMap, List<FlexibleCreator> flexibleCreators) {
        if (cachedCreators.containsKey(type)) {
            return cachedCreators.get(type);
        }
        if (recursionCache.containsKey(type)) {
            return recursionCache.get(type);
        }
        @SuppressWarnings({"rawtypes", "unchecked"}) Supplier<Structure<?>> full = Suppliers.memoize(() -> Structure.recursive((Function) (Function<Structure, Structure>) (Structure itself) -> {
            recursionCache.put(type, itself);

            Supplier<Creator> creatorSupplier = () -> {
                Class<?> rawType = null;
                TypedCreator[] parameterCreators = null;
                if (type instanceof ParameterizedType parameterizedType) {
                    if (parameterizedType.getRawType() instanceof Class<?> clazz) {
                        rawType = clazz;
                        var parameters = parameterizedType.getActualTypeArguments();
                        parameterCreators = new TypedCreator[parameters.length];
                        if (parameterizedCreatorsMap.containsKey(clazz)) {
                            for (int i = 0; i < parameters.length; i++) {
                                var structure = forType(cachedCreators, recursionCache, parameters[i], creatorsMap, parameterizedCreatorsMap, flexibleCreators);
                                var parameterType = parameters[i];
                                parameterCreators[i] = new TypedCreator() {
                                    @Override
                                    public Structure<?> create() {
                                        return structure;
                                    }

                                    @Override
                                    public Type type() {
                                        return parameterType;
                                    }

                                    @Override
                                    public Class<?> rawType() {
                                        if (parameterType instanceof Class<?> clazz) {
                                            return clazz;
                                        } else if (parameterType instanceof ParameterizedType parameterizedType) {
                                            return (Class<?>) parameterizedType.getRawType();
                                        } else {
                                            throw new IllegalArgumentException("Unknown type: " + type);
                                        }
                                    }
                                };
                            }
                            return parameterizedCreatorsMap.get(clazz).creator(parameterCreators);
                        }
                    }
                } else if (type instanceof Class<?> clazz) {
                    rawType = clazz;
                    parameterCreators = new TypedCreator[0];
                    var foundCreator = creatorsMap.get(clazz);
                    if (foundCreator != null) {
                        return foundCreator;
                    }
                } else {
                    throw new IllegalArgumentException("Unknown type: " + type);
                }

                for (var flexibleCreator : flexibleCreators) {
                    if (flexibleCreator.supports(Objects.requireNonNull(rawType), parameterCreators)) {
                        return flexibleCreator.creator(rawType, parameterCreators, type1 -> forType(cachedCreators, recursionCache, type1, creatorsMap, parameterizedCreatorsMap, flexibleCreators));
                    }
                }
                throw new IllegalArgumentException("No creator found for type: " + type);
            };
            var creator = creatorSupplier.get();
            var structure = creator.create();
            recursionCache.remove(type);
            return structure;
        }));
        cachedCreators.put(type, full.get());
        return full.get();
    }
}
