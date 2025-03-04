package dev.lukebemish.codecextras.structured.reflective;

import com.google.common.base.Suppliers;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.utility.LayeredServiceLoader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ReflectiveStructureCreator {
    Map<Class<?>, Creator> creators(CreationOptions options);
    Map<Class<?>, ParameterizedCreator> parameterizedCreators(CreationOptions options);
    List<FlexibleCreator> flexibleCreators(CreationOptions options);

    interface TypedCreator {
        Structure<?> create();
        Type type();
        Class<?> rawType();
    }

    interface Creator {
        Structure<?> create();
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
        private final Map<Class<?>, Creator> creators;
        private final Map<Class<?>, ParameterizedCreator> parameterizedCreators;
        private final List<FlexibleCreator> flexibleCreators;

        private Instance(Map<Class<?>, Creator> creators, Map<Class<?>, ParameterizedCreator> parameterizedCreators, List<FlexibleCreator> flexibleCreators, CreationOptions options) {
            this.creators = creators;
            this.parameterizedCreators = parameterizedCreators;
            this.flexibleCreators = flexibleCreators;
            this.options = options;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final Map<Class<?>, Creator> creators = new IdentityHashMap<>();
            private final Map<Class<?>, ParameterizedCreator> parameterizedCreators = new IdentityHashMap<>();
            private final List<FlexibleCreator> flexibleCreators = new ArrayList<>();
            private final List<CreationOption> options = new ArrayList<>();

            private Builder() {}

            public Builder withCreator(Class<?> clazz, Creator creator) {
                creators.put(clazz, creator);
                return this;
            }

            public Builder withParameterizedCreator(Class<?> clazz, ParameterizedCreator creator) {
                parameterizedCreators.put(clazz, creator);
                return this;
            }

            public Builder withFlexibleCreator(FlexibleCreator creator) {
                flexibleCreators.add(creator);
                return this;
            }

            public Builder withOption(CreationOption option) {
                options.add(option);
                return this;
            }

            public Instance build() {
                return new Instance(creators, parameterizedCreators, flexibleCreators, new CreationOptions(options));
            }
        }

        private static final LayeredServiceLoader<ReflectiveStructureCreator> SERVICE_LOADER = LayeredServiceLoader.of(ReflectiveStructureCreator.class);

        private final Map<Type, Structure<?>> cachedCreators = new HashMap<>();

        private final CreationOptions options;

        @SuppressWarnings("unchecked")
        public synchronized <T> Structure<T> create(Class<T> clazz) {
            var caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
            List<ReflectiveStructureCreator> services = LayeredServiceLoader.unique(SERVICE_LOADER.at(ReflectiveStructureCreator.class), SERVICE_LOADER.at(clazz), SERVICE_LOADER.at(caller));
            Map<Class<?>, Creator> creatorsMap = new IdentityHashMap<>();
            Map<Class<?>, ParameterizedCreator> parameterizedCreatorsMap = new IdentityHashMap<>();
            List<FlexibleCreator> flexibleCreatorsList = new ArrayList<>();
            services.forEach(creator -> {
                creatorsMap.putAll(creator.creators(options));
                parameterizedCreatorsMap.putAll(creator.parameterizedCreators(options));
                flexibleCreatorsList.addAll(creator.flexibleCreators(options));
            });

            creatorsMap.putAll(this.creators);
            parameterizedCreatorsMap.putAll(this.parameterizedCreators);
            flexibleCreatorsList.addAll(this.flexibleCreators);

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
