package dev.lukebemish.codecextras.structured.reflective;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import dev.lukebemish.codecextras.internal.LayeredServiceLoader;
import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.reflective.systems.Creators;
import dev.lukebemish.codecextras.structured.reflective.systems.FlexibleCreators;
import dev.lukebemish.codecextras.structured.reflective.systems.ParameterizedCreators;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ReflectiveStructureCreator {
    interface CreatorSystem<T, R, O extends CreatorSystem.Type<T, R, O>> extends App<CreatorSystem.Mu, O> {
        final class Mu implements K1 { private Mu() {} }

        O type();

        interface Type<T, R, O extends Type<T, R, O>> {
            R merge(R a, R b);
            R empty();
            Key<O> key();
            T bake(R value, CreationContext context);
        }

        interface ListType<A, O extends ListType<A, O>> extends Type<List<A>, Function<CreationContext, List<A>>, O> {
            @Override
            default Function<CreationContext, List<A>> merge(Function<CreationContext, List<A>> a, Function<CreationContext, List<A>> b) {
                return context -> {
                    var out = a.apply(context);
                    out.addAll(b.apply(context));
                    return out;
                };
            }

            @Override
            default Function<CreationContext, List<A>> empty() {
                return c -> new ArrayList<>();
            }

            @Override
            default List<A> bake(Function<CreationContext, List<A>> value, CreationContext context) {
                var out = ImmutableList.<A>builder();
                out.addAll(value.apply(context));
                return out.build();
            }
        }

        interface MapType<A, B, O extends MapType<A, B, O>> extends Type<Map<A, B>, Function<CreationContext, Map<A, B>>, O> {
            @Override
            default Function<CreationContext, Map<A, B>> merge(Function<CreationContext, Map<A, B>> a, Function<CreationContext, Map<A, B>> b) {
                return context -> {
                    var out = a.apply(context);
                    out.putAll(b.apply(context));
                    return out;
                };
            }

            @Override
            default Function<CreationContext, Map<A, B>> empty() {
                return c -> new HashMap<>();
            }

            @Override
            default Map<A, B> bake(Function<CreationContext, Map<A, B>> value, CreationContext context) {
                var out = ImmutableMap.<A, B>builder();
                out.putAll(value.apply(context));
                return out.build();
            }
        }

        interface IdentityMapType<A, B, O extends IdentityMapType<A, B, O>> extends MapType<A, B, O> {
            @Override
            default Function<CreationContext, Map<A, B>> empty() {
                return c -> new IdentityHashMap<>();
            }
        }

        R make();
    }

    @SuppressWarnings("unchecked")
    private static <R> R mergeUnchecked(Object existing, Object specific, CreatorSystem.Type<?, R, ?> type) {
        R existingCast = (R) existing;
        R specificCast = (R) specific;
        return type.merge(existingCast, specificCast);
    }

    default Keys<CreatorSystem.Mu, Object> systems() {
        return Keys.<CreatorSystem.Mu, Object>builder().build();
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
    }

    interface ContextualTransform {
        Function<Structure<?>, Structure<?>> transform(List<AnnotatedElement> elements, CreationContext context);
        default int priority() {
            return 0;
        }
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
        private final Keys<CreatorSystem.Mu, Object> systems;

        private Instance(Keys<CreatorSystem.Mu, Object> systems) {
            this.systems = systems;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final Keys.Builder<CreatorSystem.Mu, Object> systems = Keys.builder();

            private Builder() {}

            public <T, R, O extends CreatorSystem.Type<T, R, O>> Builder add(CreatorSystem<T, R, O> system) {
                systems.add(system.type().key(), system);
                return this;
            }

            public Instance build() {
                return new Instance(systems.build());
            }
        }

        private static final LayeredServiceLoader<ReflectiveStructureCreator> SERVICE_LOADER = LayeredServiceLoader.of(ReflectiveStructureCreator.class);

        private final Map<Type, Structure<?>> cachedCreators = new HashMap<>();

        @SuppressWarnings("unchecked")
        public synchronized <T> Structure<T> create(Class<T> clazz) {
            var caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
            List<ReflectiveStructureCreator> services = LayeredServiceLoader.unique(SERVICE_LOADER.at(ReflectiveStructureCreator.class), SERVICE_LOADER.at(clazz), SERVICE_LOADER.at(caller));

            Map<ReflectiveStructureCreator.CreatorSystem.Type<?, ?, ?>, Object> systemsMap = new IdentityHashMap<>();
            Consumer<Keys<CreatorSystem.Mu, Object>> addSystems = systems -> {
                systems.keys().forEach(key -> {
                    var value = (CreatorSystem<?, ?, ?>) systems.get(key).orElseThrow();
                    var type = value.type();
                    systemsMap.compute(type, (k, existing) ->
                        mergeUnchecked(
                            Objects.requireNonNullElseGet(existing, type::empty),
                            value.make(),
                            type
                        )
                    );
                });
            };
            services.forEach(creator -> addSystems.accept(creator.systems()));
            addSystems.accept(this.systems);

            var context = new CreationContext(systemsMap);

            var recursionCache = new HashMap<Type, Structure<?>>();

            return (Structure<T>) forType(cachedCreators, recursionCache, clazz, context);
        }
    }

    static <T> Structure<T> create(Class<T> clazz) {
        return Instance.builder().build().create(clazz);
    }

    private static Structure<?> forType(Map<Type, Structure<?>> cachedCreators, Map<Type, Structure<?>> recursionCache, Type type, CreationContext context) {
        if (cachedCreators.containsKey(type)) {
            return cachedCreators.get(type);
        }
        if (recursionCache.containsKey(type)) {
            return recursionCache.get(type);
        }

        var creatorsMap = context.retrieve(Creators.TYPE);
        var parameterizedCreatorsMap = context.retrieve(ParameterizedCreators.TYPE);
        var flexibleCreators = context.retrieve(FlexibleCreators.TYPE);

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
                                var structure = forType(cachedCreators, recursionCache, parameters[i], context);
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
                        return flexibleCreator.creator(rawType, parameterCreators, type1 -> forType(cachedCreators, recursionCache, type1, context));
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
