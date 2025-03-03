package dev.lukebemish.codecextras.minecraft.structured;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
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
            .build();
    }
}
