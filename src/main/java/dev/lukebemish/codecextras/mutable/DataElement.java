package dev.lukebemish.codecextras.mutable;

public interface DataElement<T> {
	void set(T t);
	T get();
	boolean dirty();
	void setDirty(boolean dirty);

	class Simple<T> implements DataElement<T> {
		public Simple(T defaultValue) {
			this.value = defaultValue;
		}

		private volatile T value;
		private volatile boolean dirty;

		@Override
		public synchronized void set(T t) {
			this.value = t;
		}

		@Override
		public T get() {
			return value;
		}

		@Override
		public boolean dirty() {
			return dirty;
		}

		@Override
		public void setDirty(boolean dirty) {
			this.dirty = dirty;
		}
	}
}
