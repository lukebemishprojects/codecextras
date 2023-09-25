package dev.lukebemish.codecextras.config;

import com.mojang.serialization.DynamicOps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface OpsIo<T> {
    DynamicOps<T> ops();

    T read(InputStream input) throws IOException;

    void write(T value, OutputStream output) throws IOException;
}
