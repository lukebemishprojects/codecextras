package dev.lukebemish.codecextras.stream.mutable.dev.lukebemish.codecextras.stream;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * Various {@link StreamCodec} utilities that do not have homes other places.
 */
public final class StreamCodecExtras {
	private StreamCodecExtras() {}

	/**
	 * @deprecated use {@link net.minecraft.network.codec.ByteBufCodecs#optional(StreamCodec)}
	 */
	@Deprecated(forRemoval = true)
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
