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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A tool for creating a {@link Structure} from a type reflectively. Implementations of this type provide specific
 *  implementations of various {@link CreatorSystem}. Instances of this type are discovered via the service locator. To
 *  create a structure, obtain an {@link Instance}.
 */
public interface ReflectiveStructureCreator {
    /**
     * A system involved in structure creation. Systems provide an intermediary type, that can be baked to a
     * given result type given proper context.
     * @param <T> the final result type of the system
     * @param <R> the intermediary type of the system
     * @param <O> the type of the system
     */
    interface CreatorSystem<T, R, O extends CreatorSystem.Type<T, R, O>> extends App<CreatorSystem.Mu, O> {
        final class Mu implements K1 { private Mu() {} }

        /**
         * {@return the type of the system}
         */
        O type();

        /**
         * A type of {@link CreatorSystem}.
         * @param <T> the final result type of the system
         * @param <R> the intermediary type of the system
         * @param <O> the type of the system; in an implementation, should be the self type
         */
        interface Type<T, R, O extends Type<T, R, O>> {
            /**
             * Merge two intermediary values.
             * @param a the first value
             * @param b the second value
             * @return the merged value
             */
            R merge(R a, R b);

            /**
             * {@return an empty intermediary value}
             */
            R empty();

            /**
             * {@return the key for this type}
             */
            Key<O> key();

            /**
             * Bake an intermediary value given context.
             * @param value the intermediary value
             * @param context the context to bake with
             * @return the final baked value
             */
            T bake(R value, CreationContext context);

            /**
             * {@return whether this type is allowed to be implemented by services} If false, this system may only be
             * provided on instance creation.
             */
            default boolean allowedFromServices() {
                return true;
            }
        }

        /**
         * A type of {@link CreatorSystem} that produces a list of values, where baking involves applying the context to a function.
         * @param <A> the type of the values in the list
         * @param <O> the type of the system; in an implementation, should be the self type
         */
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

        /**
         * A type of {@link CreatorSystem} that produces a map of values, where baking involves applying the context to a function.
         * @param <A> the type of the keys in the map
         * @param <B> the type of the values in the map
         * @param <O> the type of the system; in an implementation, should be the self type
         */
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

        /**
         * A specialized version of {@link MapType} for when the keys may be compared by identity.
         * @param <A> the type of the keys in the map
         * @param <B> the type of the values in the map
         * @param <O> the type of the system; in an implementation, should be the self type
         */
        interface IdentityMapType<A, B, O extends IdentityMapType<A, B, O>> extends MapType<A, B, O> {
            @Override
            default Function<CreationContext, Map<A, B>> empty() {
                return c -> new IdentityHashMap<>();
            }
        }

        /**
         * {@return the created intermediary value}
         */
        R make();
    }

    @SuppressWarnings("unchecked")
    private static <R> R mergeUnchecked(Object existing, Object specific, CreatorSystem.Type<?, R, ?> type) {
        R existingCast = (R) existing;
        R specificCast = (R) specific;
        return type.merge(existingCast, specificCast);
    }

    /**
     * {@return system implementations for this creator implementation}
     */
    default Keys<CreatorSystem.Mu, Object> systems() {
        return Keys.<CreatorSystem.Mu, Object>builder().build();
    }

    /**
     * A structure creator for a specific reified type.
     */
    interface TypedCreator {
        Structure<?> create();
        Type type();
        Class<?> rawType();
    }

    /**
     * Allows creation of structures reflectively.
     */
    final class Instance {
        private final Keys<CreatorSystem.Mu, Object> systems;

        private Instance(Keys<CreatorSystem.Mu, Object> systems) {
            this.systems = systems;
        }

        /**
         * {@return a new builder}
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * A builder for {@link ReflectiveStructureCreator.Instance}.
         */
        public static final class Builder {
            private final Keys.Builder<CreatorSystem.Mu, Object> systems = Keys.builder();

            private Builder() {}

            /**
             * Add a specific system implementation to the instance being built. This will override any implementations
             * of the same system added so far in the builder.
             * @param system the system to add
             * @return this builder
             * @param <T> the final value type of the system
             * @param <R> the intermediary type of the system
             * @param <O> the type of the system
             */
            public <T, R, O extends CreatorSystem.Type<T, R, O>> Builder add(CreatorSystem<T, R, O> system) {
                systems.add(system.type().key(), system);
                return this;
            }

            /**
             * {@return a new instance with the systems added in this builder}
             */
            public Instance build() {
                return new Instance(systems.build());
            }
        }

        private static final LayeredServiceLoader<ReflectiveStructureCreator> SERVICE_LOADER = LayeredServiceLoader.of(ReflectiveStructureCreator.class);

        private final Map<Type, Structure<?>> cachedCreators = new HashMap<>();

        /**
         * Create a structure reflectively for the given class.
         * @param clazz the class to create a structure for
         * @return the structure created
         * @param <T> the type of the class
         */
        @SuppressWarnings("unchecked")
        public synchronized <T> Structure<T> create(Class<T> clazz) {
            var caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
            List<ReflectiveStructureCreator> services = LayeredServiceLoader.unique(SERVICE_LOADER.at(ReflectiveStructureCreator.class), SERVICE_LOADER.at(clazz), SERVICE_LOADER.at(caller));

            Map<ReflectiveStructureCreator.CreatorSystem.Type<?, ?, ?>, Object> systemsMap = new IdentityHashMap<>();
            BiConsumer<Boolean, Keys<CreatorSystem.Mu, Object>> addSystems = (isService, systems) -> {
                systems.keys().forEach(key -> {
                    var value = (CreatorSystem<?, ?, ?>) systems.get(key).orElseThrow();
                    var type = value.type();
                    if (isService && !type.allowedFromServices()) {
                        throw new IllegalStateException("CreatorSystem " + value.type().key() + " is not allowed to be implemented by services; it may only be used by building a ReflectiveStructureCreator.Instance");
                    }
                    systemsMap.compute(type, (k, existing) ->
                        mergeUnchecked(
                            Objects.requireNonNullElseGet(existing, type::empty),
                            value.make(),
                            type
                        )
                    );
                });
            };
            services.forEach(creator -> addSystems.accept(true, creator.systems()));
            addSystems.accept(false, this.systems);

            var context = new CreationContext(systemsMap);

            var recursionCache = new HashMap<Type, Structure<?>>();

            return (Structure<T>) forType(cachedCreators, recursionCache, clazz, context);
        }
    }

    /**
     * Create a structure reflectively for the given class, using an empty {@link Instance}.
     * @param clazz the class to create a structure for
     * @return the structure created
     * @param <T> the type of the class
     */
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

            Supplier<Creators.Creator> creatorSupplier = () -> {
                Class<?> rawType = null;
                TypedCreator[] parameterCreators = null;

                switch (type) {
                    case ParameterizedType parameterizedType -> {
                        if (parameterizedType.getRawType() instanceof Class<?> clazz) {
                            rawType = clazz;
                            var parameters = parameterizedType.getActualTypeArguments();
                            parameterCreators = new TypedCreator[parameters.length];
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
                            if (parameterizedCreatorsMap.containsKey(clazz)) {
                                return parameterizedCreatorsMap.get(clazz).creator(parameterCreators);
                            }
                        }
                    }
                    case Class<?> clazz -> {
                        rawType = clazz;
                        parameterCreators = new TypedCreator[0];
                        var foundCreator = creatorsMap.get(clazz);
                        if (foundCreator != null) {
                            return foundCreator;
                        }
                    }
                    case GenericArrayType genericArrayType -> {
                        var results = handleGenericArrayType(cachedCreators, recursionCache, type, context, genericArrayType);
                        rawType = results.rawType().arrayType();
                        parameterCreators = results.parameterCreators();
                    }
                    default -> throw new IllegalArgumentException("Unknown type: " + type);
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

    private static ParameterizedTypeResults handleGenericArrayType(Map<Type, Structure<?>> cachedCreators, Map<Type, Structure<?>> recursionCache, Type type, CreationContext context, GenericArrayType genericArrayType) {
        TypedCreator[] parameterCreators;
        Class<?> rawType;
        var componentType = genericArrayType.getGenericComponentType();
        switch (componentType) {
            case Class<?> clazz -> {
                rawType = clazz;
                parameterCreators = new TypedCreator[0];
            }
            case ParameterizedType parameterizedType when parameterizedType.getRawType() instanceof Class<?> clazz -> {
                rawType = clazz;
                parameterCreators = new TypedCreator[parameterizedType.getActualTypeArguments().length];
                for (int i = 0; i < parameterizedType.getActualTypeArguments().length; i++) {
                    var structure = forType(cachedCreators, recursionCache, parameterizedType.getActualTypeArguments()[i], context);
                    var parameterType = parameterizedType.getActualTypeArguments()[i];
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
            }
            case GenericArrayType genericArrayComponentType -> {
                var results = handleGenericArrayType(cachedCreators, recursionCache, type, context, genericArrayComponentType);
                rawType = results.rawType().arrayType();
                parameterCreators = results.parameterCreators();
            }
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        }
        return new ParameterizedTypeResults(rawType, parameterCreators);
    }
}
