package dev.lukebemish.codecextras.structured.reflective.systems;

import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.reflective.CreationContext;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import java.util.Map;
import java.util.function.Function;

/**
 * A system that provides parameterized structure creators linked to raw types.
 */
public interface ParameterizedCreators extends ReflectiveStructureCreator.CreatorSystem<Map<Class<?>, ParameterizedCreators.ParameterizedCreator>, Function<CreationContext, Map<Class<?>, ParameterizedCreators.ParameterizedCreator>>, ParameterizedCreators.Type> {
    /**
     * The {@link dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator.CreatorSystem.Type} for this system.
     */
    Type TYPE = new Type();

    @Override
    default Type type() {
        return TYPE;
    }

    /**
     * Creates structures for reified types of a single raw type.
     */
    interface ParameterizedCreator {
        /**
         * {@return a structure for the given parameters}
         * @param parameters the parameters of the reified type to create
         */
        Structure<?> create(ReflectiveStructureCreator.TypedCreator[] parameters);
        default Creators.Creator creator(ReflectiveStructureCreator.TypedCreator[] parameters) {
            return () -> create(parameters);
        }
    }

    final class Type implements ReflectiveStructureCreator.CreatorSystem.MapType<Class<?>, ParameterizedCreator, ParameterizedCreators.Type> {
        private Type() {}
        private static final Key<Type> KEY = Key.create("parameterized_creators");

        @Override
        public Key<Type> key() {
            return KEY;
        }
    }
}
