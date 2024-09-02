package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.structured.Range;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public final class Widgets {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TRANSPARENT = ResourceLocation.fromNamespaceAndPath("codecextras_minecraft", "widget/transparent");
    private static final int BORDER_COLOR = 0xFFA0A0A0;

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

    public static LayoutFactory<Integer> color(boolean includeAlpha) {
        return canHandleOptional((parent, width, ops, original, update, creationInfo, handleOptional) -> {
            if (!handleOptional && original.isJsonNull()) {
                original = new JsonPrimitive(0);
                update.accept(original);
            }

            int[] value = new int[1];
            if (original.isJsonPrimitive()) {
                try {
                    value[0] = original.getAsInt();
                } catch (NumberFormatException e) {
                    LOGGER.warn("Failed to decode `{}`: {}", original, e.getMessage());
                }
            } else {
                LOGGER.warn("Failed to decode `{}`: not a primitive", original);
            }

            Function<Integer, Component> message = color -> Component.literal("0x"+Integer.toHexString(color)).withColor(color | 0xFF000000);

            return new AbstractButton(0, 0, width, Button.DEFAULT_HEIGHT, Component.empty()) {
                {
                    setTooltip(Tooltip.create(creationInfo.componentInfo().description()));
                }

                @Override
                public void onPress() {
                    var screen = new ColorPickScreen(parent, creationInfo.componentInfo().title(), color -> {
                        update.accept(new JsonPrimitive(color));
                        value[0] = color;
                    }, includeAlpha);
                    screen.setColor(value[0]);
                    Minecraft.getInstance().setScreen(screen);
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    this.defaultButtonNarrationText(narrationElementOutput);
                }

                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                    super.renderWidget(guiGraphics, i, j, f);
                    int rectangleHeight = 12;
                    int startY = getY() + getHeight()/2 - rectangleHeight/2;
                    int startX = getX() + (startY - getY());
                    int endY = getY() + getHeight()/2 + rectangleHeight/2;
                    int endX = getX() + getWidth() - (startX - getX());
                    if (includeAlpha) {
                        guiGraphics.blitSprite(TRANSPARENT, startX, startY, endX - startX, endY - startY);
                    }
                    guiGraphics.fill(startX, startY, endX, endY, includeAlpha ? value[0] : value[0] | 0xFF000000);
                }
            };
        });
    }

    public static <T, N extends Number & Comparable<N>> LayoutFactory<T> slider(Range<N> range, Function<N, DataResult<JsonElement>> toJson, Function<JsonElement, DataResult<N>> fromJson, boolean isDoubleLike) {
        return canHandleOptional((parent, width, ops, original, update, creationInfo, handleOptional) -> {
            if (!handleOptional && original.isJsonNull()) {
                original = new JsonPrimitive(range.min());
                update.accept(original);
            }

            var valueResult = fromJson.apply(original);
            N value;
            if (valueResult.error().isPresent()) {
                LOGGER.warn("Failed to decode `{}`: {}", original, valueResult.error().get().message());
                value = range.min();
            } else {
                value = valueResult.getOrThrow();
            }

            AbstractSliderButton widget = new AbstractSliderButton(0, 0, width, Button.DEFAULT_HEIGHT, Component.empty(), valueInRange(range, value)) {
                {
                    this.updateMessage();
                }

                @Override
                protected void updateMessage() {
                    N value = calculateValue();
                    this.setMessage(Component.literal(isDoubleLike ? String.format("%.2f", value.doubleValue()) : String.valueOf(value.intValue())));
                }

                private N calculateValue() {
                    JsonElement valueElement;
                    var realValue = this.value * (range.max().doubleValue() - range.min().doubleValue()) + range.min().doubleValue();
                    if (isDoubleLike) {
                        valueElement = new JsonPrimitive(realValue);
                    } else {
                        valueElement = new JsonPrimitive(Math.round(realValue));
                    }
                    var valueResult = fromJson.apply(valueElement);
                    N value;
                    if (valueResult.error().isPresent()) {
                        LOGGER.warn("Failed to decode `{}`: {}", valueElement, valueResult.error().get().message());
                        value = range.min();
                    } else {
                        value = valueResult.getOrThrow();
                    }
                    return value;
                }

                @Override
                protected void applyValue() {
                    N value = calculateValue();
                    var jsonResult = toJson.apply(value);
                    if (jsonResult.error().isPresent()) {
                        LOGGER.warn("Failed to encode `{}`: {}", value, jsonResult.error().get().message());
                    } else {
                        update.accept(jsonResult.getOrThrow());
                    }
                    this.value = valueInRange(range, value);
                }
            };
            widget.setTooltip(Tooltip.create(creationInfo.componentInfo().description()));
            return widget;
        });
    }

    private static <N extends Number & Comparable<N>> double valueInRange(Range<N> range, N value) {
        return (value.doubleValue() - range.min().doubleValue()) / (range.max().doubleValue() - range.min().doubleValue());
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
