package dev.lukebemish.codecextras.minecraft.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.Key2;
import dev.lukebemish.codecextras.types.Identity;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public final class MinecraftKeys {
    private static final Map<ResourceKey<DataComponentType<?>>, Key<?>> DATA_COMPONENT_TYPE_KEYS = new ConcurrentHashMap<>();
    public static Key<Map<DataComponentType<?>, Object>> VALUE_MAP = Key.create("value_map");
    public static Key<DataComponentMap> DATA_COMPONENT_MAP = Key.create("data_component_map");
    public static Key<DataComponentPatch> DATA_COMPONENT_PATCH = Key.create("data_component_patch");
    public static Key2<DataComponentTypeHolder.Mu, Identity.Mu> FALLBACK_DATA_COMPONENT_TYPE = Key2.create("data_component_type");

    private MinecraftKeys() {
    }

    public static final Key<ResourceLocation> RESOURCE_LOCATION = Key.create("resource_location");
    public static final Key<Integer> ARGB_COLOR = Key.create("argb_color");
    public static final Key<Integer> RGB_COLOR = Key.create("rgb_color");

    public record DataComponentTypeHolder<T>(DataComponentType<T> value) implements App<DataComponentTypeHolder.Mu, T> {
        public static final class Mu implements K1 {
            private Mu() {
            }
        }

        public static <T> DataComponentTypeHolder<T> unbox(App<DataComponentTypeHolder.Mu, T> box) {
            return (DataComponentTypeHolder<T>) box;
        }
    }

    public record ResourceKeyHolder<T>(ResourceKey<T> value) implements App<ResourceKeyHolder.Mu, T> {
        public static final class Mu implements K1 {
            private Mu() {
            }
        }

        public static <T> ResourceKeyHolder<T> unbox(App<ResourceKeyHolder.Mu, T> box) {
            return (ResourceKeyHolder<T>) box;
        }
    }

    public record TagKeyHolder<T>(TagKey<T> value) implements App<TagKeyHolder.Mu, T> {
        public static final class Mu implements K1 {
            private Mu() {
            }
        }

        public static <T> TagKeyHolder<T> unbox(App<TagKeyHolder.Mu, T> box) {
            return (TagKeyHolder<T>) box;
        }
    }

    public record HolderSetHolder<T>(HolderSet<T> value) implements App<HolderSetHolder.Mu, T> {
        public static final class Mu implements K1 {
            private Mu() {
            }
        }

        public static <T> HolderSetHolder<T> unbox(App<HolderSetHolder.Mu, T> box) {
            return (HolderSetHolder<T>) box;
        }
    }

    public record RegistryKeyHolder<T>(
        ResourceKey<? extends Registry<T>> value) implements App<RegistryKeyHolder.Mu, T> {
        public static final class Mu implements K1 {
            private Mu() {
            }
        }

        public static <T> RegistryKeyHolder<T> unbox(App<RegistryKeyHolder.Mu, T> box) {
            return (RegistryKeyHolder<T>) box;
        }
    }

    public static final Key<DataComponentPatchKey<?>> DATA_COMPONENT_PATCH_KEY = Key.create("data_component_patch_key");

    public record DataComponentPatchKey<T>(DataComponentType<T> type, boolean removes) {}

    public static final Key2<RegistryKeyHolder.Mu, ResourceKeyHolder.Mu> RESOURCE_KEY = Key2.create("resource_key");

    public static final Key2<RegistryKeyHolder.Mu, TagKeyHolder.Mu> TAG_KEY = Key2.create("tag_key");

    public static final Key2<RegistryKeyHolder.Mu, TagKeyHolder.Mu> HASHED_TAG_KEY = Key2.create("#tag_key");

    public static final Key2<RegistryKeyHolder.Mu, HolderSetHolder.Mu> HOMOGENOUS_LIST = Key2.create("homogenous_list");
}
