package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import org.slf4j.Logger;

public class VerifyingEditBox<T> extends EditBox {
    private static final Logger LOGGER = LogUtils.getLogger();

    private VerifyingEditBox(int width, EntryCreationInfo<T> creationInfo, DynamicOps<JsonElement> ops, Consumer<JsonElement> update, JsonElement original, Function<String, DataResult<T>> toData, Function<T, DataResult<String>> fromData, Predicate<String> filter, boolean emptyIsMissing) {
        super(Minecraft.getInstance().font, width, Button.DEFAULT_HEIGHT, creationInfo.componentInfo().title());

        if (!original.isJsonPrimitive()) {
            if (!original.isJsonNull()) {
                LOGGER.warn("`{}` is not primitive, as required for an edit box", original);
            }
        } else {
            var decoded = creationInfo.codec().parse(ops, original);
            if (decoded.isError()) {
                LOGGER.warn("Failed to decode `{}`: {}", original, decoded.error().orElseThrow().message());
            } else {
                var decodedValue = decoded.getOrThrow();
                var stringResult = fromData.apply(decodedValue);
                if (stringResult.error().isPresent()) {
                    LOGGER.warn("Failed to encode `{}` as string: {}", decodedValue, stringResult.error().get().message());
                } else {
                    this.setValue(stringResult.getOrThrow());
                }
            }
        }

        this.setFilter(filter);

        this.setResponder(string -> {
            if (emptyIsMissing && string.isEmpty()) {
                update.accept(JsonNull.INSTANCE);
                return;
            }
            var dataResult = toData.apply(string);
            if (dataResult.error().isPresent()) {
                LOGGER.warn("Failed to encode `{}` as data: {}", string, dataResult.error().get().message());
            } else {
                var jsonResult = creationInfo.codec().encode(dataResult.getOrThrow(), ops, original);
                if (jsonResult.error().isPresent()) {
                    LOGGER.warn("Failed to encode `{}` as json: {}", dataResult.getOrThrow(), jsonResult.error().get().message());
                } else {
                    update.accept(jsonResult.getOrThrow());
                }
            }
        });
    }

    public static <T> WidgetFactory<T> of(Function<String, DataResult<T>> toData, Function<T, DataResult<String>> fromData, Predicate<String> filter, boolean emptyIsMissing) {
        return (parent, width, ops, original, update, creationInfo) -> new VerifyingEditBox<>(width, creationInfo, ops, update, original, toData, fromData, filter, emptyIsMissing);
    }
}
