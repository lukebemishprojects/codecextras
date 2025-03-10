package dev.lukebemish.codecextras.structured.reflective.systems;

import com.google.common.collect.ImmutableList;
import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.reflective.CreationContext;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public interface FlexibleCreators extends ReflectiveStructureCreator.CreatorSystem<List<FlexibleCreators.FlexibleCreator>, Function<CreationContext, List<FlexibleCreators.FlexibleCreator>>, FlexibleCreators.Type> {
    Type TYPE = new Type();

    @Override
    default Type type() {
        return TYPE;
    }

    interface FlexibleCreator {
        Structure<?> create(Class<?> exact, ReflectiveStructureCreator.TypedCreator[] parameters, Function<java.lang.reflect.Type, Structure<?>> creator);
        boolean supports(Class<?> exact, ReflectiveStructureCreator.TypedCreator[] parameters);
        default int priority() {
            return 0;
        }
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
