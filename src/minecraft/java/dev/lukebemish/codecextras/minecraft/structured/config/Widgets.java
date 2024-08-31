package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public final class Widgets {
    private static final Logger LOGGER = LogUtils.getLogger();

    private Widgets() {}

    public static <T> LayoutFactory<T> text(Function<String, DataResult<T>> toData, Function<T, DataResult<String>> fromData, Predicate<String> filter, boolean emptyIsMissing) {
        return (parent, width, ops, original, update, creationInfo, handleOptional) -> {
            var widget = new EditBox(Minecraft.getInstance().font, width, Button.DEFAULT_HEIGHT, creationInfo.componentInfo().title());
            creationInfo.componentInfo().maybeDescription().ifPresent(description -> {
                var tooltip = Tooltip.create(description);
                widget.setTooltip(tooltip);
            });

            widget.setFilter(filter);

            if (!handleOptional && original.isJsonNull()) {
                original = new JsonPrimitive("");
                update.accept(original);
            }

            var decoded = creationInfo.codec().parse(ops, original);
            if (decoded.isError()) {
                LOGGER.warn("Failed to decode `{}`: {}", original, decoded.error().orElseThrow().message());
            } else {
                var decodedValue = decoded.getOrThrow();
                var stringResult = fromData.apply(decodedValue);
                if (stringResult.error().isPresent()) {
                    LOGGER.warn("Failed to encode `{}` as string: {}", decodedValue, stringResult.error().get().message());
                } else {
                    widget.setValue(stringResult.getOrThrow());
                }
            }

            widget.setResponder(string -> {
                if (emptyIsMissing && string.isEmpty()) {
                    update.accept(JsonNull.INSTANCE);
                    return;
                }
                var dataResult = toData.apply(string);
                if (dataResult.error().isPresent()) {
                    LOGGER.warn("Failed to encode `{}` as data: {}", string, dataResult.error().get().message());
                } else {
                    var jsonResult = creationInfo.codec().encodeStart(ops, dataResult.getOrThrow());
                    if (jsonResult.error().isPresent()) {
                        LOGGER.warn("Failed to encode `{}` as json: {}", dataResult.getOrThrow(), jsonResult.error().get().message());
                    } else {
                        update.accept(jsonResult.getOrThrow());
                    }
                }
            });

            return widget;
        };
    }

    public static <T> LayoutFactory<T> text(Function<String, DataResult<T>> toData, Function<T, DataResult<String>> fromData, boolean emptyIsMissing) {
        return text(toData, fromData, s -> true, emptyIsMissing);
    }

    public static <T> LayoutFactory<T> canHandleOptional(LayoutFactory<T> assumesNonOptional) {
        return (parent, fullWidth, ops, original, update, creationInfo, handleOptional) -> {
            if (!handleOptional) {
                return assumesNonOptional.create(parent, fullWidth, ops, original, update, creationInfo, false);
            }
            var remainingWidth = fullWidth - Button.DEFAULT_HEIGHT - Button.DEFAULT_SPACING;
            var layout = new EqualSpacingLayout(Button.DEFAULT_WIDTH, 0, EqualSpacingLayout.Orientation.HORIZONTAL);
            var object = new Object() {
                private final LayoutElement wrapped = assumesNonOptional.create(parent, remainingWidth, ops, original, update, creationInfo, false);
                private final Button disabled = Button.builder(Component.translatable("codecextras.config.missing"), b -> {})
                    .width(remainingWidth)
                    .build();
                boolean missing = original.isJsonNull();
                private final Checkbox lock = Checkbox.builder(Component.empty(), Minecraft.getInstance().font)
                    .maxWidth(Button.DEFAULT_HEIGHT)
                    .onValueChange((checkbox, b) -> {
                        missing = !b;
                        if (missing) {
                            update.accept(JsonNull.INSTANCE);
                            wrapped.visitWidgets(w -> {
                                w.visible = false;
                                w.active = false;
                            });
                            var maxHeight = Math.max(disabled.getHeight(), wrapped.getHeight());
                            disabled.setHeight(maxHeight);
                            disabled.visible = true;
                        } else {
                            wrapped.visitWidgets(w -> {
                                w.visible = true;
                                w.active = true;
                            });
                            var maxHeight = Math.max(disabled.getHeight(), wrapped.getHeight());
                            disabled.setHeight(maxHeight);
                            disabled.visible = false;
                        }
                    })
                    .selected(!missing)
                    .build();

                {
                    var maxHeight = Math.max(disabled.getHeight(), wrapped.getHeight());
                    disabled.setHeight(maxHeight);
                    disabled.active = false;
                    disabled.visible = missing;
                    wrapped.visitWidgets(w -> {
                        w.visible = !missing;
                        w.active = !missing;
                    });

                    creationInfo.componentInfo().maybeDescription().ifPresent(description -> {
                        var tooltip = Tooltip.create(description);
                        lock.setTooltip(tooltip);
                        disabled.setTooltip(tooltip);
                    });
                }
            };
            layout.addChild(object.lock, LayoutSettings.defaults().alignVerticallyMiddle());
            var right = new FrameLayout();
            right.addChild(object.disabled, LayoutSettings.defaults().alignVerticallyMiddle().alignHorizontallyCenter());
            right.addChild(object.wrapped, LayoutSettings.defaults().alignVerticallyMiddle().alignHorizontallyCenter());
            layout.addChild(right, LayoutSettings.defaults().alignVerticallyMiddle());
            return layout;
        };
    }

    public static <T> LayoutFactory<T> bool(boolean falseIfMissing) {
        LayoutFactory<T> widget = (parent, width, ops, original, update, creationInfo, handleOptional) -> {
            if (!handleOptional && original.isJsonNull()) {
                original = new JsonPrimitive(false);
                update.accept(original);
            }
            var w = Checkbox.builder(Component.empty(), Minecraft.getInstance().font)
                .maxWidth(width)
                .onValueChange((checkbox, b) -> {
                    if (falseIfMissing && !b) {
                        update.accept(JsonNull.INSTANCE);
                    }
                    update.accept(new JsonPrimitive(b));
                })
                .selected(original.isJsonPrimitive() && original.getAsJsonPrimitive().getAsBoolean())
                .build();
            creationInfo.componentInfo().maybeDescription().ifPresent(description -> {
                var tooltip = Tooltip.create(description);
                w.setTooltip(tooltip);
            });
            w.setMessage(creationInfo.componentInfo().title());
            return w;
        };
        if (!falseIfMissing) {
            return canHandleOptional(widget);
        }
        return (parent, width, ops, original, update, entry, handleOptional) -> {
            if (handleOptional) {
                return widget.create(parent, width, ops, original, update, entry, true);
            }
            var button = Button.builder(Component.translatable("codecextras.config.unit"), b -> {})
                .width(width)
                .build();
            button.active = false;
            return button;
        };
    }
}
