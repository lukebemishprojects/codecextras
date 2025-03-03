package dev.lukebemish.codecextras.structured.reflective;

import dev.lukebemish.codecextras.structured.Structure;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
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

    @SuppressWarnings("unchecked")
    static <T> Structure<T> create(Class<T> clazz) {
        List<ReflectiveStructureCreator> services = new ArrayList<>();
        ServiceLoader.load(ReflectiveStructureCreator.class).forEach(services::add);
        if (clazz.getModule().getLayer() == null) {
            ServiceLoader.load(ReflectiveStructureCreator.class, clazz.getClassLoader()).forEach(services::add);
        } else {
            ServiceLoader.load(clazz.getModule().getLayer(), ReflectiveStructureCreator.class).forEach(services::add);
        }
        Map<Class<?>, Creator> creatorsMap = new IdentityHashMap<>();
        Map<Class<?>, ParameterizedCreator> parameterizedCreatorsMap = new IdentityHashMap<>();
        List<FlexibleCreator> flexibleCreators = new ArrayList<>();
        services.forEach(creator -> {
            creatorsMap.putAll(creator.creators());
            parameterizedCreatorsMap.putAll(creator.parameterizedCreators());
            flexibleCreators.addAll(creator.flexibleCreators());
        });
        flexibleCreators.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
        var creator = forType(clazz, creatorsMap, parameterizedCreatorsMap, flexibleCreators);
        return (Structure<T>) creator.create();
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
