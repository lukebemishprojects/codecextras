package dev.lukebemish.codecextras.config;

import com.mojang.serialization.DynamicOps;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class SpecializedOpsIo<T> implements OpsIo<T> {
	private final OpsIo<T> delegate;
	private final DynamicOps<T> ops;

	public SpecializedOpsIo(OpsIo<T> opsIo, DynamicOps<T> ops) {
		if (opsIo instanceof SpecializedOpsIo<T> specialized) {
			this.delegate = specialized.delegate;
		} else {
			this.delegate = opsIo;
		}
		this.ops = ops;
	}

	@Override
	public DynamicOps<T> ops() {
		return this.ops;
	}

	@Override
	public T read(InputStream input) throws IOException {
		return delegate.read(input);
	}

	@Override
	public void write(T value, OutputStream output) throws IOException {
		delegate.write(value, output);
	}
}
