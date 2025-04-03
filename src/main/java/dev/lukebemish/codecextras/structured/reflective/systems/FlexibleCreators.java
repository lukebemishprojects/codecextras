package dev.lukebemish.codecextras.structured.reflective.systems;

import com.google.common.collect.ImmutableList;
import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.reflective.CreationContext;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jetbrains.annotations.ApiStatus;

/**
 * A system that provides flexible structure creators, which can create structures for broad ranges of types.
 */
public interface FlexibleCreators extends ReflectiveStructureCreator.CreatorSystem<List<FlexibleCreators.FlexibleCreator>, Function<CreationContext, List<FlexibleCreators.FlexibleCreator>>, FlexibleCreators.Type> {
    /**
     * The {@link dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator.CreatorSystem.Type} for this system.
     */
    Type TYPE = new Type();

    @Override
    default Type type() {
        return TYPE;
    }

    /**
     * A flexible structure creator that can create structures for a wide range of types.
     */
    interface FlexibleCreator {
        /**
         * {@return a structure for the given class and parameters}
         * @param exact the ra w class to create
         * @param parameters the parameters of the reified type to create
         * @param creator a function to create nested structures with
         */
        Structure<?> create(Class<?> exact, ReflectiveStructureCreator.TypedCreator[] parameters, Function<java.lang.reflect.Type, Structure<?>> creator);

        /**
         * {@return whether this creator supports the given class and parameters}
         * @param exact the raw class to create
         * @param parameters the parameters of the reified type to create
         */
        boolean supports(Class<?> exact, ReflectiveStructureCreator.TypedCreator[] parameters);
        /**
         * {@return the priority of this creator, used to determine the order in which they are checked} High priority is applied first.
         */
        default int priority() {
            return 0;
        }

        @ApiStatus.NonExtendable
        default Creators.Creator creator(Class<?> exact, ReflectiveStructureCreator.TypedCreator[] parameters, Function<java.lang.reflect.Type, Structure<?>> creator) {
            return () -> create(exact, parameters, creator);
        }
    }

    final class Type implements ReflectiveStructureCreator.CreatorSystem.ListType<FlexibleCreator, FlexibleCreators.Type> {
        private Type() {}
        private static final Key<Type> KEY = Key.create("flexible_creators");

        @Override
        public List<FlexibleCreator> bake(Function<CreationContext, List<FlexibleCreator>> value, CreationContext context) {
            var temporary = new ArrayList<>(value.apply(context));
            temporary.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
            return ImmutableList.copyOf(temporary);
        }

        @Override
        public Key<Type> key() {
            return KEY;
        }
    }
}
