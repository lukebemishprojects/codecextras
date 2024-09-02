package dev.lukebemish.codecextras.minecraft.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.Key2;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public final class MinecraftKeys {
    private MinecraftKeys() {
    }

    public static final Key<ResourceLocation> RESOURCE_LOCATION = Key.create("resource_location");
    public static final Key<Integer> ARGB_COLOR = Key.create("argb_color");
    public static final Key<Integer> RGB_COLOR = Key.create("rgb_color");

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

    public static final Key2<RegistryKeyHolder.Mu, ResourceKeyHolder.Mu> RESOURCE_KEY = Key2.create("resource_key");

    public static final Key2<RegistryKeyHolder.Mu, TagKeyHolder.Mu> TAG_KEY = Key2.create("tag_key");

    public static final Key2<RegistryKeyHolder.Mu, TagKeyHolder.Mu> HASHED_TAG_KEY = Key2.create("#tag_key");

    public static final Key2<RegistryKeyHolder.Mu, HolderSetHolder.Mu> HOMOGENOUS_LIST_KEY = Key2.create("homogenous_list");
}
