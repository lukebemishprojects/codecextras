package dev.lukebemish.codecextras.extension;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.lukebemish.autoextension.AutoExtension;
import dev.lukebemish.codecextras.CodecExtras;
import dev.lukebemish.codecextras.comments.CommentMapCodec;
import java.util.Map;

@AutoExtension
public final class MapCodecExtension {
	private MapCodecExtension() {}
	public static <O> MapCodec<O> flatten(MapCodec<DataResult<O>> codec) {
		return CodecExtras.flatten(codec);
	}

	public static <O> MapCodec<DataResult<O>> raise(MapCodec<O> codec) {
		return CodecExtras.raise(codec);
	}

	public static <O> MapCodec<O> withComment(MapCodec<O> codec, Map<String, String> comments) {
		return CommentMapCodec.of(codec, comments);
	}

	public static <A> MapCodec<A> withComment(MapCodec<A> codec, String comment) {
		return CommentMapCodec.of(codec, comment);
	}
}
