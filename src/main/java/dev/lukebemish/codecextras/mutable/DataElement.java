package dev.lukebemish.codecextras.mutable;

public interface DataElement<T> {
	void set(T t);
	T get();
	boolean dirty();
	void setDirty(boolean dirty);
}
