package dev.lukebemish.codecextras.structured.reflective;

import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.utility.LayeredServiceLoader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface ReflectiveStructureCreator {
    Map<Class<?>, Creator> creators();
    Map<Class<?>, ParameterizedCreator> parameterizedCreators();
    List<FlexibleCreator> flexibleCreators();

    interface Creator {
        Structure<?> create();
    }

    interface FlexibleCreator {
        Structure<?> create(Class<?> exact, Function<Type, Structure<?>> creator);
        boolean supports(Class<?> exact);
        default int priority() {
            return 0;
        }
        default Creator creator(Class<?> exact, Function<Type, Structure<?>> creator) {
            return () -> create(exact, creator);
        }
    }

    interface ParameterizedCreator {
        Structure<?> create(Creator[] parameters);
        default Creator creator(Creator[] parameters) {
            return () -> create(parameters);
        }
    }

    final class Instance {
        private final Map<Class<?>, Creator> creators;
        private final Map<Class<?>, ParameterizedCreator> parameterizedCreators;
        private final List<FlexibleCreator> flexibleCreators;

        private Instance(Map<Class<?>, Creator> creators, Map<Class<?>, ParameterizedCreator> parameterizedCreators, List<FlexibleCreator> flexibleCreators) {
            this.creators = creators;
            this.parameterizedCreators = parameterizedCreators;
            this.flexibleCreators = flexibleCreators;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final Map<Class<?>, Creator> creators = new IdentityHashMap<>();
            private final Map<Class<?>, ParameterizedCreator> parameterizedCreators = new IdentityHashMap<>();
            private final List<FlexibleCreator> flexibleCreators = new ArrayList<>();

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

            public Instance build() {
                return new Instance(creators, parameterizedCreators, flexibleCreators);
            }
        }

        private static final LayeredServiceLoader<ReflectiveStructureCreator> SERVICE_LOADER = LayeredServiceLoader.of(ReflectiveStructureCreator.class);

        @SuppressWarnings("unchecked")
        public <T> Structure<T> create(Class<T> clazz) {
            var caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
            List<ReflectiveStructureCreator> services = LayeredServiceLoader.unique(SERVICE_LOADER.at(ReflectiveStructureCreator.class), SERVICE_LOADER.at(clazz), SERVICE_LOADER.at(caller));
            Map<Class<?>, Creator> creatorsMap = new IdentityHashMap<>();
            Map<Class<?>, ParameterizedCreator> parameterizedCreatorsMap = new IdentityHashMap<>();
            List<FlexibleCreator> flexibleCreatorsList = new ArrayList<>();
            services.forEach(creator -> {
                creatorsMap.putAll(creator.creators());
                parameterizedCreatorsMap.putAll(creator.parameterizedCreators());
                flexibleCreatorsList.addAll(creator.flexibleCreators());
            });

            creatorsMap.putAll(this.creators);
            parameterizedCreatorsMap.putAll(this.parameterizedCreators);
            flexibleCreatorsList.addAll(this.flexibleCreators);

            flexibleCreatorsList.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
            var creator = forType(clazz, creatorsMap, parameterizedCreatorsMap, flexibleCreatorsList);
            return (Structure<T>) creator.create();
        }
    }

    static <T> Structure<T> create(Class<T> clazz) {
        return Instance.builder().build().create(clazz);
    }

    private static Creator forType(Type type, Map<Class<?>, Creator> creatorsMap, Map<Class<?>, ParameterizedCreator> parameterizedCreatorsMap, List<FlexibleCreator> flexibleCreators) {
        if (type instanceof ParameterizedType parameterizedType) {
            var rawType = parameterizedType.getRawType();
            if (rawType instanceof Class<?> clazz && parameterizedCreatorsMap.containsKey(clazz)) {
                var parameters = parameterizedType.getActualTypeArguments();
                Creator[] parameterCreators = new Creator[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    parameterCreators[i] = forType(parameters[i], creatorsMap, parameterizedCreatorsMap, flexibleCreators);
                }
                return parameterizedCreatorsMap.get(clazz).creator(parameterCreators);
            }
        }
        if (type instanceof Class<?> clazz) {
            var foundCreator = creatorsMap.get(clazz);
            if (foundCreator != null) {
                return foundCreator;
            }
            for (var flexibleCreator : flexibleCreators) {
                if (flexibleCreator.supports(clazz)) {
                    return flexibleCreator.creator(clazz, type1 -> forType(type1, creatorsMap, parameterizedCreatorsMap, flexibleCreators).create());
                }
            }
        }
        throw new IllegalArgumentException("No creator found for type: " + type);
    }
}
