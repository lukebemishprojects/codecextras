package dev.lukebemish.codecextras.minecraft.structured.config;

import com.mojang.serialization.Codec;
import java.util.function.Predicate;

record RecordEntry<T>(String key, ConfigScreenEntry<T> entry, Predicate<T> shouldEncode, Codec<T> codec) {}
