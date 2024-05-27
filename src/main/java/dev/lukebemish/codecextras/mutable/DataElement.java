package dev.lukebemish.codecextras.mutable;

import java.util.Objects;

public interface DataElement<T> {
	void set(T t);
	T get();
	boolean dirty();
	void setDirty(boolean dirty);
	default boolean includeInFullEncoding() {
		return true;
	}

	class Simple<T> implements DataElement<T> {
		private volatile T value;
		private volatile boolean dirty = false;
		private final T defaultValue;

		public Simple(T value) {
			this.value = value;
			this.defaultValue = value;
		}

		@Override
		public synchronized void set(T t) {
			this.value = t;
			setDirty(true);
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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Simple<?> simple)) return false;
			return Objects.equals(value, simple.value);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(value);
		}

		@Override
		public boolean includeInFullEncoding() {
			return !value.equals(defaultValue);
		}
	}
}
