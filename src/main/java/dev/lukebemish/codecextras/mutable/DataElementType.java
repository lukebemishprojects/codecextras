package dev.lukebemish.codecextras.mutable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.Asymmetry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public interface DataElementType<D, T> {
	DataElement<T> from(D data);
	Codec<T> codec();
	String name();

	static <D, T> DataElementType<D, T> create(String name, Codec<T> codec, Function<D, DataElement<T>> getter) {
		return new DataElementType<>() {
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
	static <D> Consumer<D> cleaner(DataElementType<D, ?>... types) {
		List<DataElementType<D, ?>> list = List.of(types);
		return cleaner(list);
	}

	static <D> Consumer<D> cleaner(List<? extends DataElementType<D, ?>> types) {
		return data -> {
			for (var type : types) {
				type.from(data).setDirty(false);
			}
		};
	}

	@SafeVarargs
	static <D> Codec<Asymmetry<Consumer<D>, D>> codec(boolean encodeFull, DataElementType<D, ?>... elements) {
		List<DataElementType<D, ?>> list = List.of(elements);
		return codec(encodeFull, list);
	}

	static <D> Codec<Asymmetry<Consumer<D>, D>> codec(boolean encodeFull, List<? extends DataElementType<D, ?>> elements) {
		Map<String, DataElementType<D, ?>> elementTypeMap = new HashMap<>();
		for (var element : elements) {
			elementTypeMap.put(element.name(), element);
		}

		Codec<Map<String, Dynamic<?>>> partial = Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH);

		return Asymmetry.flatSplit(
			partial,
			map -> {
				record Mutation<D, T>(DataElementType<D, T> element, T value) {
					private static <D, T> DataResult<Mutation<D, T>> of(DataElementType<D, T> type, Dynamic<?> dynamic) {
						return type.codec().parse(dynamic).map(value -> new Mutation<>(type, value));
					}

					public void set(D data) {
						element.from(data).set(value);
					}
				}

				List<Mutation<D, ?>> mutations = new ArrayList<>();
				for (var pair : map.entrySet()) {
					String name = pair.getKey();
					var type = elementTypeMap.get(name);
					if (type == null) {
						return DataResult.error(() -> "Invalid name for DataElementType: " + name);
					}
					var mutation = Mutation.of(type, pair.getValue());
					if (mutation.error().isPresent()) {
						return DataResult.error(mutation.error().get().messageSupplier());
					}
					mutations.add(mutation.result().get());
				}

				return DataResult.success(data -> {
					for (var mutation : mutations) {
						mutation.set(data);
					}
				});
			},
			data -> {
				Map<String, Dynamic<?>> map = new HashMap<>();
				for (DataElementType<D, ?> type : elements) {
					var element = type.from(data);
					if ((encodeFull && element.includeInFullEncoding()) || element.dirty()) {
						var result = forElement(type, data);
						if (result.result().isPresent()) {
							map.put(type.name(), result.result().get());
						} else {
							return DataResult.error(result.error().get().messageSupplier(), map);
						}
					}
				}
				return DataResult.success(map);
			}
		);
	}

	private static <D, T> DataResult<Dynamic<?>> forElement(DataElementType<D, T> type, D data) {
		var element = type.from(data);
		var result = type.codec().encodeStart(JsonOps.INSTANCE, element.get());
		return result.map(r -> new Dynamic<>(JsonOps.INSTANCE, r));
	}
}
