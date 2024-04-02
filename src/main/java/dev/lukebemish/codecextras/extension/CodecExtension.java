package dev.lukebemish.codecextras.extension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.lukebemish.autoextension.AutoExtension;
import dev.lukebemish.codecextras.CodecExtras;

@AutoExtension
public final class CodecExtension {
	private CodecExtension() {}

	public static <O> Codec<O> flatten(Codec<DataResult<O>> codec) {
		return CodecExtras.flatten(codec);
	}

	public static <O> Codec<DataResult<O>> raise(Codec<O> codec) {
		return CodecExtras.raise(codec);
	}
}
