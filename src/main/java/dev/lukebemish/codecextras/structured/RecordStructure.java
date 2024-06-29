package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class RecordStructure<A> {
	private final List<Field<A, ?>> fields = new ArrayList<>();

	public static final class Container {
		private final Key<?>[] keys;
		private final Object[] array;

		private Container(Key<?>[] keys, Object[] array) {
			this.array = array;
			this.keys = keys;
		}

		@SuppressWarnings("unchecked")
		public <T> T get(Key<T> key) {
			if (key.count >= array.length || key != keys[key.count]) {
				throw new IllegalArgumentException("Key does not belong to the container");
			}
			return (T) array[key.count];
		}

		public static Builder builder() {
			return new Builder();
		}

		public static final class Builder {
			private final List<Object> values = new ArrayList<>();
			private final List<Key<?>> keys = new ArrayList<>();

			private Builder() {}

			public <T> void add(Key<T> key, T value) {
				keys.add(key);
				values.add(value);
			}

			public Container build() {
				return new Container(keys.toArray(new Key<?>[0]), values.toArray());
			}
		}
	}

	public static final class Key<T> {
		private final int count;

		private Key(int i) {
			this.count = i;
		}
	}

	public record Field<A, T>(String name, Structure<T> structure, Function<A, T> getter, Key<T> key) {}

	public <T> Key<T> add(String name, Structure<T> structure, Function<A, T> getter) {
		var key = new Key<T>(fields.size());
		fields.add(new Field<>(name, structure, getter, key));
		return key;
	}

	static <A> Structure<A> create(RecordStructure.Builder<A> builder) {
		RecordStructure<A> instance = new RecordStructure<>();
		var creator = builder.build(instance);
		return new Structure<>() {
			@Override
			public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
				return interpreter.record(instance.fields, creator);
			}
		};
	}

	@FunctionalInterface
	public interface Builder<A> {
		Function<Container, A> build(RecordStructure<A> builder);
	}
}
