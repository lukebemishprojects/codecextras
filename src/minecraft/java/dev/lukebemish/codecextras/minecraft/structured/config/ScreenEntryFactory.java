package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import java.util.function.Consumer;

public interface ScreenEntryFactory<T> {
    ScreenEntryProvider open(EntryCreationContext context, JsonElement original, Consumer<JsonElement> onClose, EntryCreationInfo<T> entry);

    default ScreenEntryProvider openChecked(EntryCreationContext context, JsonElement original, Consumer<JsonElement> onClose, EntryCreationInfo<T> entry) {
        EntryCreationContext.ProblemMarker[] problems = {null};
        return open(context, original, jsonElement -> {
            var codec = entry.codec();
            var result = codec.parse(context.ops(), jsonElement);
            if (result.isError()) {
                problems[0] = context.problem(problems[0], result.error().orElseThrow().message());
            }
            onClose.accept(jsonElement);
        }, entry);
    }
}
