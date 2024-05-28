package dev.lukebemish.codecextras.stream.mutable;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import dev.lukebemish.codecextras.Asymmetry;
import dev.lukebemish.codecextras.mutable.DataElement;
import dev.lukebemish.codecextras.mutable.DataElementType;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * A {@link DataElementType} with support for {@link StreamCodec}s.
 * @param <B> the type of buffer used by the stream codec
 * @param <D> the type of the holder object
 * @param <T> the type of data being retrieved
 */
public interface StreamDataElementType<B, D, T> extends DataElementType<D, T> {
	/**
	 * {@return the stream codec to (de)serialize the element}
	 */
	StreamCodec<B, T> streamCodec();

	/**
	 * {@return a new {@link StreamDataElementType} with the provided name, codec, stream codec, and getter}
	 * @param name the name of the data type
	 * @param codec the codec to (de)serialize the data type
	 * @param streamCodec the stream codec to (de)serialize the data type
	 * @param getter a function to retrieve the data element from the object
	 * @param <B> the type of buffer used by the stream codec
	 * @param <D> the type of the holder object
	 * @param <T> the type of the data
	 */
	static <B, D, T> StreamDataElementType<B, D, T> create(String name, Codec<T> codec, StreamCodec<B, T> streamCodec, Function<D, DataElement<T>> getter) {
		return new StreamDataElementType<>() {
			@Override
			public StreamCodec<B, T> streamCodec() {
				return streamCodec;
			}

			@Override
			public DataElement<T> from(D data) {
				return getter.apply(data);
			}

			@Override
			public Codec<T> codec() {
				return codec;
			}

			@Override
			public String name() {
				return name;
			}
		};
	}

	/**
	 * Creates a {@link StreamCodec} for a series of data elements. This codec will encode from an instance of the type that
	 * holds the data elements, and will decode to a {@link Consumer} that can be applied to an instance of that type to
	 * set the data elements' values. The codec can encode either the full state of the data elements, or just the changes
	 * since the last time they were marked as clean. Elements are encoded prefixed by the index of the data type in the
	 * provided collection.
	 * @param encodeFull whether to encode the full state of the data elements
	 * @param elements the data elements to encode
	 * @return a new {@link StreamCodec}
	 * @param <B> the type of buffer used by the stream codec
	 * @param <D> the type of the holder object
	 */
	@SafeVarargs
	static <B extends FriendlyByteBuf, D> StreamCodec<B, Asymmetry<Consumer<D>, D>> streamCodec(boolean encodeFull, StreamDataElementType<B, D, ?>... elements) {
		List<StreamDataElementType<B, D, ?>> list = List.of(elements);
		return streamCodec(encodeFull, list);
	}

	/**
	 * Creates a {@link StreamCodec} for a series of data elements. This codec will encode from an instance of the type that
	 * holds the data elements, and will decode to a {@link Consumer} that can be applied to an instance of that type to
	 * set the data elements' values. The codec can encode either the full state of the data elements, or just the changes
	 * since the last time they were marked as clean. Elements are encoded prefixed by the index of the data type in the
	 * provided collection.
	 * @param encodeFull whether to encode the full state of the data elements
	 * @param elements the data elements to encode
	 * @return a new {@link StreamCodec}
	 * @param <B> the type of buffer used by the stream codec
	 * @param <D> the type of the holder object
	 */
	static <B extends FriendlyByteBuf, D> StreamCodec<B, Asymmetry<Consumer<D>, D>> streamCodec(boolean encodeFull, List<? extends StreamDataElementType<B, D, ?>> elements) {
		return StreamCodec.of((buffer, asymmetry) -> {
			var data = asymmetry.encoding().getOrThrow();
			List<Pair<Integer, Consumer<B>>> toEncode = new ArrayList<>();
			for (int i = 0; i < elements.size(); i++) {
				var type = elements.get(i);
				write(type, data, encodeFull, toEncode, i);
			}
			buffer.writeVarInt(toEncode.size());
			for (var pair : toEncode) {
				buffer.writeVarInt(pair.getFirst());
				pair.getSecond().accept(buffer);
			}
		}, buffer -> {
			record Mutation<D, T>(DataElementType<D, T> element, T value) {
				private static <B, D, T> Mutation<D, T> of(StreamDataElementType<B, D, T> type, B buffer) {
					return new Mutation<>(type, type.streamCodec().decode(buffer));
				}

				public void set(D data) {
					element.from(data).set(value);
				}
			}
			List<Mutation<D, ?>> mutations = new ArrayList<>();

			var listSize = buffer.readVarInt();
			for (int i = 0; i < listSize; i++) {
				var index = buffer.readVarInt();
				if (index < 0 || index >= elements.size()) {
					throw new DecoderException("Invalid index: " + index + " out of bounds");
				}
				mutations.add(Mutation.of(elements.get(index), buffer));
			}

			return Asymmetry.ofDecoding(data -> {
				for (var mutation : mutations) {
					mutation.set(data);
				}
			});
		});
	}

	private static <B, D, T> void write(StreamDataElementType<B, D, T> type, D data, boolean encodeFull, List<Pair<Integer, Consumer<B>>> toEncode, int index) {
		var element = type.from(data);
		element.ifEncoding(encodeFull, value ->
			toEncode.add(Pair.of(index, b -> type.streamCodec().encode(b, value)))
		);
	}
}
