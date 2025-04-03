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

/**
 * A system that allows discovery of properties in non-standard ways, as a fallback from normal property discovery.
 */
public interface FallbackPropertyDiscoverers extends ReflectiveStructureCreator.CreatorSystem<List<FallbackPropertyDiscoverers.Discoverer>, Function<CreationContext, List<FallbackPropertyDiscoverers.Discoverer>>, FallbackPropertyDiscoverers.Type> {
    /**
     * Discovers fallback properties given context.
     */
    interface Discoverer {
        /**
         * Modifies properties discovered for a class.
         *
         * @param clazz the class to modify properties for
         * @param known the known properties for the class
         * @param parameters type parameters for the reified version of the class
         */
        void modifyProperties(Class<?> clazz, Map<String, java.lang.reflect.Type> known, java.lang.reflect.Type[] parameters);

        /**
         * {@return a method handle to the getter for the given property, or {@code null} if this discoverer cannot find it}
         * @param clazz the class to get the property from
         * @param property the property to get
         * @param exists whether a getter for the property has already been found
         */
        @Nullable MethodHandle getter(Class<?> clazz, String property, boolean exists);
        /**
         * {@return a method handle to the setter for the given property, or {@code null} if this discoverer cannot find it}
         * @param clazz the class to get the property from
         * @param property the property to get
         * @param exists whether a setter for the property has already been found
         */
        @Nullable MethodHandle setter(Class<?> clazz, String property, boolean exists);
        /**
         * {@return a list of elements that are associated with the property}
         * @param clazz the class to get the property from
         * @param property the property to get
         */
        List<AnnotatedElement> context(Class<?> clazz, String property);

        /**
         * {@return the priority of this discoverer, used to determine the order in which they are applied} High priority is applied first.
         */
        default int priority() {
            return 0;
        }
    }

    /**
     * The {@link dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator.CreatorSystem.Type} for this system.
     */
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
