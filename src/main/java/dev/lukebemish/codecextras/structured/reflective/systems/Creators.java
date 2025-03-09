package dev.lukebemish.codecextras.structured.reflective.systems;

import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.reflective.CreationContext;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import java.util.Map;
import java.util.function.Function;

public interface Creators extends ReflectiveStructureCreator.CreatorSystem<Map<Class<?>, ReflectiveStructureCreator.Creator>, Function<CreationContext, Map<Class<?>, ReflectiveStructureCreator.Creator>>, Creators.Type> {
    Type TYPE = new Type();

    @Override
    default Type type() {
        return TYPE;
    }

    final class Type implements ReflectiveStructureCreator.CreatorSystem.IdentityMapType<Class<?>, ReflectiveStructureCreator.Creator, Creators.Type> {
        private Type() {}
        private static final Key<Type> KEY = Key.create("creators");

        @Override
        public Key<Type> key() {
            return KEY;
        }
    }
}
