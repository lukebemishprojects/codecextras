package dev.lukebemish.codecextras.structured.reflective.systems;

import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.reflective.CreationContext;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import java.util.Map;
import java.util.function.Function;

public interface ParameterizedCreators extends ReflectiveStructureCreator.CreatorSystem<Map<Class<?>, ReflectiveStructureCreator.ParameterizedCreator>, Function<CreationContext, Map<Class<?>, ReflectiveStructureCreator.ParameterizedCreator>>, ParameterizedCreators.Type> {
    Type TYPE = new Type();

    @Override
    default Type type() {
        return TYPE;
    }

    final class Type implements ReflectiveStructureCreator.CreatorSystem.MapType<Class<?>, ReflectiveStructureCreator.ParameterizedCreator, ParameterizedCreators.Type> {
        private Type() {}
        private static final Key<Type> KEY = Key.create("parameterized_creators");

        @Override
        public Key<Type> key() {
            return KEY;
        }
    }
}
