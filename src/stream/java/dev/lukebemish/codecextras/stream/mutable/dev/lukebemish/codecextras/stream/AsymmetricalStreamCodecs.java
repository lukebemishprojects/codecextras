package dev.lukebemish.codecextras.stream.mutable.dev.lukebemish.codecextras.stream;

import dev.lukebemish.codecextras.Asymmetry;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.function.Function;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;

/**
 * Utilities for using {@link StreamCodec} with {@link Asymmetry}.
 */
public final class AsymmetricalStreamCodecs {
	private AsymmetricalStreamCodecs() {}

	/**
	 * {@return the value wrapped by the given asymmetry if it is decoding, throwing otherwise}
	 */
	public static <D, E> D getDecoding(Asymmetry<D, E> asymmetry) {
		return asymmetry.decoding().getOrThrow(s -> new DecoderException("Backwards Asymmetry: " + s));
	}

	/**
	 * {@return the value wrapped by the given asymmetry if it is encoding, throwing otherwise}
	 */
	public static <D, E> E getEncoding(Asymmetry<D, E> asymmetry) {
		return asymmetry.encoding().getOrThrow(s -> new EncoderException("Backwards Asymmetry: " + s));
	}

	/**
	 * {@return a codec that can encode and decode asymmetries using the given encoder and decoder}
	 * @param decoder the decoder to use
	 * @param encoder the encoder to use
	 * @param <D> the type of the value when decoding
	 * @param <E> the type of the value when encoding
	 */
	public static <B, D, E> StreamCodec<B, Asymmetry<D, E>> codec(StreamDecoder<B, D> decoder, StreamEncoder<B, E> encoder) {
		return StreamCodec.of(
			(buffer, asymmetry) -> encoder.encode(buffer, getEncoding(asymmetry)),
			(buffer) -> Asymmetry.ofDecoding(decoder.decode(buffer))
		);
	}

	/**
	 * Independently map both the encoding and decoding directions.
	 * @param streamCodec the stream codec to map
	 * @param mapDecoding the function to apply to the decoding value when decoding
	 * @param mapEncoding the function to apply to the encoding value when encoding
	 * @return a stream codec with the given mapping applied
	 * @param <E0> the type of the encoding value before mapping
	 * @param <D0> the type of the decoding value before mapping
	 * @param <E1> the type of the encoding value after mapping
	 * @param <D1> the type of the decoding value after mapping
	 */
	public static <B, E0, D0, E1, D1> StreamCodec<B, Asymmetry<D1, E1>> map(StreamCodec<B, Asymmetry<D0, E0>> streamCodec, Function<D0, D1> mapDecoding, Function<E1, E0> mapEncoding) {
		return streamCodec.map(asymmetry -> Asymmetry.ofDecoding(mapDecoding.apply(getDecoding(asymmetry))), asymmetry -> Asymmetry.ofEncoding(mapEncoding.apply(getEncoding(asymmetry))));
	}

	/**
	 * Map only the encoding direction of the given stream codec.
	 * @param streamCodec the stream codec to map
	 * @param mapEncoding the function to apply to the encoding value when encoding
	 * @return a stream codec with the given mapping applied
	 * @param <E0> the type of the encoding value before mapping
	 * @param <D> the type of the decoding value
	 * @param <E1> the type of the encoding value after mapping
	 */
	public static <B, E0, D, E1> StreamCodec<B, Asymmetry<D, E1>> mapEncoding(StreamCodec<B, Asymmetry<D, E0>> streamCodec, Function<E1, E0> mapEncoding) {
		return streamCodec.map(Asymmetry::asDecoding, asymmetry -> Asymmetry.ofEncoding(mapEncoding.apply(getEncoding(asymmetry))));
	}

	/**
	 * Map the decoding direction of the given stream codec.
	 * @param streamCodec the stream codec to map
	 * @param mapDecoding the function to apply to the decoding value when decoding
	 * @return a stream codec with the given mapping applied
	 * @param <E> the type of the encoding value
	 * @param <D0> the type of the decoding value before mapping
	 * @param <D1> the type of the decoding value after mapping
	 */
	public static <B, E, D0, D1> StreamCodec<B, Asymmetry<D1, E>> mapDecoding(StreamCodec<B, Asymmetry<D0, E>> streamCodec, Function<D0, D1> mapDecoding) {
		return streamCodec.map(asymmetry -> Asymmetry.ofDecoding(mapDecoding.apply(getDecoding(asymmetry))), Asymmetry::asEncoding);
	}

	/**
	 * Join an asymmetrical stream codec into a single type by providing a pair of functions applied on the end of the
	 * stream codec furthest from the data.
	 * @param streamCodec the stream codec to join
	 * @param mapDecoding the function to apply to the decoding value when decoding
	 * @param mapEncoding the function to apply to the encoding value when encoding
	 * @return a stream codec of a single type
	 * @param <E> the type of the encoding value
	 * @param <D> the type of the decoding value
	 * @param <O> the type of the joined codec
	 */
	public static <B, E, D, O> StreamCodec<B, O> join(StreamCodec<B, Asymmetry<D, E>> streamCodec, Function<D, O> mapDecoding, Function<O, E> mapEncoding) {
		return streamCodec.map(asymmetry -> mapDecoding.apply(getDecoding(asymmetry)), object -> Asymmetry.ofEncoding(mapEncoding.apply(object)));
	}

	/**
	 * Split a stream codec into two types by providing a pair of functions applied on the end of the stream codec
	 * closest to the data.
	 * @param codec the stream codec to split
	 * @param mapDecoding the function to apply to the decoding value when decoding
	 * @param mapEncoding the function to apply to the encoding value when encoding
	 * @return an asymmetrical stream codec
	 * @param <E> the type of the encoding value
	 * @param <D> the type of the decoding value
	 * @param <O> the type of the split codec
	 */
	public static <B, E, D, O> StreamCodec<B, Asymmetry<D, E>> split(StreamCodec<B, O> codec, Function<O, D> mapDecoding, Function<E, O> mapEncoding) {
		return codec.map(object -> Asymmetry.ofDecoding(mapDecoding.apply(object)), asymmetry -> mapEncoding.apply(getEncoding(asymmetry)));
	}
}
