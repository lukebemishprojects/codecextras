package dev.lukebemish.codecextras.compat.jankson;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.api.SyntaxError;
import com.mojang.serialization.DynamicOps;
import dev.lukebemish.codecextras.config.OpsIo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class JanksonOpsIo implements OpsIo<JsonElement> {
    public static final JanksonOpsIo INSTANCE = new JanksonOpsIo();

    private static final JsonGrammar OUTPUT = JsonGrammar.JSON5;
    private static final Jankson JANKSON = Jankson.builder().allowBareRootObject().build();

    @Override
    public DynamicOps<JsonElement> ops() {
        return JanksonOps.COMMENTED;
    }

    @Override
    public JsonElement read(InputStream input) throws IOException {
        try {
            return JANKSON.loadElement(input);
        } catch (SyntaxError e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(JsonElement value, OutputStream output) throws IOException {
        value.toJson(new OutputStreamWriter(output), OUTPUT, 0);
    }
}
