package dev.lukebemish.codecextras.config;

import com.mojang.serialization.DynamicOps;
import dev.lukebemish.codecextras.companion.Companion;
import dev.lukebemish.codecextras.companion.DelegatingOps;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface OpsIo<T> {
	DynamicOps<T> ops();

	T read(InputStream input) throws IOException;

	void write(T value, OutputStream output) throws IOException;

	default <Q extends Companion.CompanionToken> OpsIo<T> accompanied(Q token, Companion<T, Q> companion) {
		return new SpecializedOpsIo(this, DelegatingOps.of(token, companion, ops()));
	}
}
