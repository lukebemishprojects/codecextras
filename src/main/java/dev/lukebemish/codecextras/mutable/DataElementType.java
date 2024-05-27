package dev.lukebemish.codecextras.mutable;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.Asymmetry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface DataElementType<D, T> {
	DataElement<T> from(D data);
	Codec<T> codec();
	DataElement<T> create();

	static <D, T> DataElementType<D, T> defaulted(Codec<T> codec, T defaultValue, Function<D, DataElement<T>> getter) {
		return new DataElementType<D, T>() {
			@Override
			public DataElement<T> from(D data) {
				return getter.apply(data);
			}

			@Override
			public Codec<T> codec() {
				return codec;
			}

			@Override
			public DataElement<T> create() {
				return new DataElement<T>() {
					private volatile T value = defaultValue;
					private volatile boolean dirty = false;

					@Override
					public synchronized void set(T t) {
						this.value = t;
					}

					@Override
					public T get() {
						return this.value;
					}

					@Override
					public boolean dirty() {
						return this.dirty;
					}

					@Override
					public synchronized void setDirty(boolean dirty) {
						this.dirty = dirty;
					}
				};
			}
		};
	}

	static <D> Codec<Asymmetry<Consumer<D>, D>> codec(List<? extends DataElementType<D, ?>> elements, boolean encodeAll) {
		MapCodec<Pair<Integer, Dynamic<?>>> partial = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("index").forGetter(Pair::getFirst),
			Codec.PASSTHROUGH.fieldOf("value").forGetter(Pair::getSecond)
		).apply(i, Pair::of));

		return Asymmetry.flatSplit(
			partial.codec().listOf(),
			list -> {
				record Mutation<D, T>(DataElementType<D, T> element, T value) {
					private static <D, T> DataResult<Mutation<D, T>> of(DataElementType<D, T> type, Dynamic<?> dynamic) {
						return type.codec().parse(dynamic).map(value -> new Mutation<>(type, value));
					}

					public void set(D data) {
						element.from(data).set(value);
					}
				}

				List<Mutation<D, ?>> mutations = new ArrayList<>();
				for (var pair : list) {
					var index = pair.getFirst();
					var type = elements.get(index);
					var mutation = Mutation.of(type, pair.getSecond());
					if (mutation.error().isPresent()) {
						return DataResult.error(mutation.error().get().messageSupplier());
					}
					mutations.add(mutation.result().get());
				}

				return DataResult.success((Consumer<D>) data -> {
					for (var mutation : mutations) {
						mutation.set(data);
					}
				});
			},
			data -> {
				List<Pair<Integer, Dynamic<?>>> list = new ArrayList<>();
				for (int i = 0; i < elements.size(); i++) {
					var type = elements.get(i);
					var element = type.from(data);
					if (encodeAll || element.dirty()) {
						var result = forElement(type, data);
						if (result.result().isPresent()) {
							list.add(Pair.of(i, result.result().get()));
						} else {
							return DataResult.error(result.error().get().messageSupplier(), list);
						}
					}
				}
				return DataResult.success(list);
			}
		);
	}

	private static <D, T> DataResult<Dynamic<?>> forElement(DataElementType<D, T> type, D data) {
		var element = type.from(data);
		var result = type.codec().encodeStart(JsonOps.INSTANCE, element.get());
		return result.map(r -> new Dynamic<>(JsonOps.INSTANCE, r));
	}
}
