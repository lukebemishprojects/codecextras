package dev.lukebemish.codecextras.minecraft.structured.config;

import com.mojang.serialization.Codec;
import dev.lukebemish.codecextras.structured.RecordStructure;
import java.util.Optional;

record RecordEntry<T>(String key, ConfigScreenEntry<T> entry, Optional<RecordStructure.Field.MissingBehavior<T>> missingBehavior, Codec<T> codec) {}
