package dev.lukebemish.codecextras.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import java.io.*;

public class GsonOpsIo implements OpsIo<JsonElement> {
    public static final GsonOpsIo INSTANCE = new GsonOpsIo();

    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    @Override
    public DynamicOps<JsonElement> ops() {
        return JsonOps.INSTANCE;
    }

    @Override
    public JsonElement read(InputStream input) throws IOException {
        try {
            return GSON.fromJson(new InputStreamReader(input), JsonElement.class);
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(JsonElement value, OutputStream output) throws IOException {
        output.write(GSON.toJson(value).getBytes());
    }
}
