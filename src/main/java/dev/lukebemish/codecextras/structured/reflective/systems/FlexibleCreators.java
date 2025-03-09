package dev.lukebemish.codecextras.structured.reflective.systems;

import com.google.common.collect.ImmutableList;
import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.reflective.CreationContext;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public interface FlexibleCreators extends ReflectiveStructureCreator.CreatorSystem<List<ReflectiveStructureCreator.FlexibleCreator>, Function<CreationContext, List<ReflectiveStructureCreator.FlexibleCreator>>, FlexibleCreators.Type> {
    Type TYPE = new Type();

    @Override
    default Type type() {
        return TYPE;
    }

    final class Type implements ReflectiveStructureCreator.CreatorSystem.ListType<ReflectiveStructureCreator.FlexibleCreator, FlexibleCreators.Type> {
        private Type() {}
        private static final Key<Type> KEY = Key.create("flexible_creators");

        @Override
        public List<ReflectiveStructureCreator.FlexibleCreator> bake(Function<CreationContext, List<ReflectiveStructureCreator.FlexibleCreator>> value, CreationContext context) {
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
