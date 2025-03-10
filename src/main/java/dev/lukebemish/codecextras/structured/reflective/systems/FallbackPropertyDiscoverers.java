package dev.lukebemish.codecextras.structured.reflective.systems;

import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.reflective.CreationContext;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public interface FallbackPropertyDiscoverers extends ReflectiveStructureCreator.CreatorSystem<List<FallbackPropertyDiscoverers.Discoverer>, Function<CreationContext, List<FallbackPropertyDiscoverers.Discoverer>>, FallbackPropertyDiscoverers.Type> {
    interface Discoverer {
        void modifyProperties(Class<?> clazz, Map<String, java.lang.reflect.Type> known);
        @Nullable MethodHandle getter(Class<?> clazz, String property, boolean exists);
        @Nullable MethodHandle setter(Class<?> clazz, String property, boolean exists);
        List<AnnotatedElement> context(Class<?> clazz, String property);
        default int priority() {
            return 0;
        }
    }

    Type TYPE = new Type();

    @Override
    default Type type() {
        return TYPE;
    }

    final class Type implements ReflectiveStructureCreator.CreatorSystem.ListType<FallbackPropertyDiscoverers.Discoverer, FallbackPropertyDiscoverers.Type> {
        private Type() {}
        private static final Key<Type> KEY = Key.create("fallback_property_discoverers");

        @Override
        public List<Discoverer> bake(Function<CreationContext, List<Discoverer>> value, CreationContext context) {
            var out = value.apply(context);
            out.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
            return out;
        }

        @Override
        public Key<Type> key() {
            return KEY;
        }
    }
}
