package dev.lukebemish.codecextras.stream.mutable.dev.lukebemish.codecextras.stream;

import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public final class StreamCodecExtras {
	private StreamCodecExtras() {}

	public static <B extends FriendlyByteBuf, O> StreamCodec<B, Optional<O>> optional(StreamCodec<B, O> streamCodec) {
		return new StreamCodec<>() {
			@Override
			public Optional<O> decode(B buffer) {
				boolean present = buffer.readBoolean();
				if (present) {
					return Optional.of(streamCodec.decode(buffer));
				}
				return Optional.empty();
			}

			@Override
			public void encode(B buffer, Optional<O> optional) {
				optional.ifPresentOrElse(o -> {
					buffer.writeBoolean(true);
					streamCodec.encode(buffer, o);
				}, () -> buffer.writeBoolean(false));
			}
		};
	}
}
