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

public interface StreamDataElementType<B, D, T> extends DataElementType<D, T> {
	StreamCodec<B, T> streamCodec();

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

	@SafeVarargs
	static <B extends FriendlyByteBuf, D> StreamCodec<B, Asymmetry<Consumer<D>, D>> streamCodec(boolean encodeFull, StreamDataElementType<B, D, ?>... elements) {
		List<StreamDataElementType<B, D, ?>> list = List.of(elements);
		return streamCodec(encodeFull, list);
	}

	static <B extends FriendlyByteBuf, D> StreamCodec<B, Asymmetry<Consumer<D>, D>> streamCodec(boolean encodeFull, List<? extends StreamDataElementType<B, D, ?>> elements) {
		return StreamCodec.of((buffer, asymmetry) -> {
			var data = asymmetry.encoding().getOrThrow();
			List<Pair<Integer, Consumer<B>>> toEncode = new ArrayList<>();
			for (int i = 0; i < elements.size(); i++) {
				var type = elements.get(i);
				var dataElement = type.from(data);
				if ((encodeFull && dataElement.includeInFullEncoding()) || dataElement.dirty()) {
					toEncode.add(Pair.of(i, b -> write(b, type, data)));
				}
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

	private static <B, D, T> void write(B buffer, StreamDataElementType<B, D, T> element, D data) {
		element.streamCodec().encode(buffer, element.from(data).get());
	}
}
