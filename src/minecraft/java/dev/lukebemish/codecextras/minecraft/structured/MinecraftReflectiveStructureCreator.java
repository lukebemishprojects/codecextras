package dev.lukebemish.codecextras.minecraft.structured;

import com.google.auto.service.AutoService;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

@AutoService(ReflectiveStructureCreator.class)
public class MinecraftReflectiveStructureCreator implements ReflectiveStructureCreator {
    @Override
    public Map<Class<?>, Creator> creators() {
        return ImmutableMap.<Class<?>, Creator>builder()
            .put(ResourceLocation.class, () -> MinecraftStructures.RESOURCE_LOCATION)
            .put(DataComponentMap.class, () -> MinecraftStructures.DATA_COMPONENT_MAP)
            .put(DataComponentPatch.class, () -> MinecraftStructures.DATA_COMPONENT_PATCH)
            .put(ItemStack.class, () -> MinecraftStructures.ITEM_STACK)
            .build();
    }

    @Override
    public Map<Class<?>, ParameterizedCreator> parameterizedCreators() {
        return ImmutableMap.<Class<?>, ParameterizedCreator>builder()
            .build();
    }

    @Override
    public List<FlexibleCreator> flexibleCreators() {
        return ImmutableList.<FlexibleCreator>builder()
            .add(new FlexibleCreator() {
                @Override
                public Structure<?> create(Class<?> exact, TypedCreator[] parameters, Function<Type, Structure<?>> creator) {
                    Supplier<Object[]> values = Suppliers.memoize(() -> {
                        try {
                            return (Object[]) exact.getMethod("values").invoke(null);
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    return Structure.stringRepresentable(values, t -> ((StringRepresentable)t).getSerializedName());
                }

                @Override
                public int priority() {
                    return 10;
                }

                @Override
                public boolean supports(Class<?> exact, TypedCreator[] parameters) {
                    return Enum.class.isAssignableFrom(exact) && StringRepresentable.class.isAssignableFrom(exact);
                }
            })
            .build();
    }
}
