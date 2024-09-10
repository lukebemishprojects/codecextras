package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.StringRepresentation;
import dev.lukebemish.codecextras.structured.Range;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public final class Widgets {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TRANSPARENT = ResourceLocation.fromNamespaceAndPath("codecextras_minecraft", "widget/transparent");
    private static final int DEFAULT_SPACING = 5;

    private Widgets() {}

    public static <T> LayoutFactory<T> text(Function<String, DataResult<T>> toData, Function<T, DataResult<String>> fromData, Predicate<String> filter, boolean emptyIsMissing) {
        return (parent, width, context, original, update, creationInfo, handleOptional) -> {
            var widget = new EditBox(Minecraft.getInstance().font, width, Button.DEFAULT_HEIGHT, creationInfo.componentInfo().title());
            creationInfo.componentInfo().maybeDescription().ifPresent(description -> {
                var tooltip = Tooltip.create(description);
                widget.setTooltip(tooltip);
            });

            widget.setFilter(filter);

            if (original.isJsonNull()) {
                original = new JsonPrimitive("");
                if (!handleOptional) {
                    update.accept(original);
                }
            }

            var decoded = creationInfo.codec().parse(context.ops(), original);
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
                    var jsonResult = creationInfo.codec().encodeStart(context.ops(), dataResult.getOrThrow());
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

    public static <T> LayoutFactory<T> pickWidget(StringRepresentation<T> representation) {
        return wrapWithOptionalHandling((parent, width, context, original, update, creationInfo, handleOptional) -> {
            String[] stringValue = new String[1];
            if (original.isJsonPrimitive()) {
                if (original.getAsJsonPrimitive().isString()) {
                    stringValue[0] = original.getAsJsonPrimitive().getAsString();
                } else {
                    LOGGER.warn("Failed to decode `{}`: not a string", original);
                }
            } else if (!original.isJsonNull()) {
                LOGGER.warn("Failed to decode `{}`: not a primitive or null", original);
            }
            List<String> values = new ArrayList<>();
            for (var value : representation.values().get()) {
                var valueRepresentation = representation.representation().apply(value);
                values.add(valueRepresentation);
            }
            Supplier<Component> calculateMessage = () -> Component.literal(stringValue[0] == null ? "" : stringValue[0]);
            var holder = new Object() {
                private final Button button = Button.builder(calculateMessage.get(), b -> {
                    Minecraft.getInstance().setScreen(new ChoiceScreen(parent, creationInfo.componentInfo().title(), values, stringValue[0], newKeyValue -> {
                        if (!Objects.equals(newKeyValue, stringValue[0])) {
                            stringValue[0] = newKeyValue;
                            if (newKeyValue == null) {
                                update.accept(JsonNull.INSTANCE);
                            } else {
                                update.accept(new JsonPrimitive(newKeyValue));
                            }
                            this.button.setMessage(calculateMessage.get());
                        }
                    }));
                }).width(width).tooltip(Tooltip.create(creationInfo.componentInfo().description())).build();
            };
            return holder.button;
        });
    }

    public static <T> LayoutFactory<T> wrapWithOptionalHandling(LayoutFactory<T> assumesNonOptional) {
        return (parent, fullWidth, context, original, update, creationInfo, handleOptional) -> {
            if (!handleOptional) {
                return assumesNonOptional.create(parent, fullWidth, context, original, update, creationInfo, false);
            }
            var remainingWidth = fullWidth - Button.DEFAULT_HEIGHT - DEFAULT_SPACING;
            var layout = new EqualSpacingLayout(fullWidth, 0, EqualSpacingLayout.Orientation.HORIZONTAL);
            var object = new Object() {
                private JsonElement value = original;
                private final VisibilityWrapperElement wrapped = VisibilityWrapperElement.ofDirect(assumesNonOptional.create(parent, remainingWidth, context, original, json -> {
                    this.value = json;
                    update.accept(json);
                }, creationInfo, false));
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
                            wrapped.setVisible(false);
                            wrapped.setActive(false);
                            var maxHeight = Math.max(disabled.getHeight(), wrapped.getHeight());
                            disabled.setHeight(maxHeight);
                            disabled.visible = true;
                        } else {
                            update.accept(value);
                            wrapped.setVisible(true);
                            wrapped.setActive(true);
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
                    wrapped.setVisible(!missing);
                    wrapped.setActive(!missing);

                    creationInfo.componentInfo().maybeDescription().ifPresent(description -> {
                        var tooltip = Tooltip.create(description);
                        lock.setTooltip(tooltip);
                        disabled.setTooltip(tooltip);
                    });

                    if (missing) {
                        update.accept(JsonNull.INSTANCE);
                    }
                }
            };
            layout.addChild(object.lock, LayoutSettings.defaults().alignVerticallyMiddle());
            var right = new FrameLayout();
            right.addChild(object.disabled, LayoutSettings.defaults().alignVerticallyMiddle().alignHorizontallyCenter());
            right.addChild(object.wrapped, LayoutSettings.defaults().alignVerticallyMiddle().alignHorizontallyCenter());
            layout.addChild(right, LayoutSettings.defaults().alignVerticallyMiddle());
            return VisibilityWrapperElement.ofDirect(layout);
        };
    }

    public static LayoutFactory<Integer> color(boolean includeAlpha) {
        return wrapWithOptionalHandling((parent, width, context, original, update, creationInfo, handleOptional) -> {
            if (original.isJsonNull()) {
                original = new JsonPrimitive(0);
                if (!handleOptional) {
                    update.accept(original);
                }
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
        return wrapWithOptionalHandling((parent, width, context, original, update, creationInfo, handleOptional) -> {
            if (original.isJsonNull()) {
                original = new JsonPrimitive(range.min());
                if (!handleOptional) {
                    update.accept(original);
                }
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

    public static <L, R> LayoutFactory<Either<L, R>> either(LayoutFactory<L> left, LayoutFactory<R> right) {
        return (parent, fullWidth, context, original, update, creationInfo, handleOptional) -> {
            var remainingWidth = fullWidth - Button.DEFAULT_HEIGHT - DEFAULT_SPACING;
            boolean[] isLeft = new boolean[1];
            boolean[] isMissing = new boolean[1];
            if (!original.isJsonNull()) {
                if (handleOptional) {
                    isMissing[0] = true;
                } else {
                    var result = creationInfo.codec().parse(context.ops(), original);
                    if (result.error().isPresent()) {
                        LOGGER.warn("Failed to decode `{}`: {}", original, result.error().get().message());
                    } else {
                        isLeft[0] = result.getOrThrow().left().isPresent();
                    }
                }
            }
            Codec<L> leftCodec = creationInfo.codec().comapFlatMap(e -> e.left().map(DataResult::success).orElse(DataResult.error(() -> "Expected left value")), Either::left);
            Codec<R> rightCodec = creationInfo.codec().comapFlatMap(e -> e.right().map(DataResult::success).orElse(DataResult.error(() -> "Expected right value")), Either::right);
            var leftElement = VisibilityWrapperElement.ofDirect(left.create(parent, remainingWidth, context, isLeft[0] ? original : JsonNull.INSTANCE, update, creationInfo.withCodec(leftCodec), false));
            var rightElement = VisibilityWrapperElement.ofDirect(right.create(parent, remainingWidth, context, isLeft[0] ? JsonNull.INSTANCE : original, update, creationInfo.withCodec(rightCodec), false));
            var missingElement = handleOptional ? Button.builder(Component.translatable("codecextras.config.missing"), b -> {}).width(remainingWidth).build() : null;
            if (handleOptional) {
                missingElement.active = false;
            }
            update.accept(original);
            var frame = new FrameLayout(remainingWidth, Button.DEFAULT_HEIGHT);
            frame.addChild(leftElement);
            frame.addChild(rightElement);
            Runnable updateVisibility = () -> {
                if (isMissing[0]) {
                    rightElement.setVisible(false);
                    rightElement.setActive(false);
                    leftElement.setVisible(false);
                    leftElement.setActive(false);
                    if (handleOptional) {
                        missingElement.visible = true;
                    }
                } else if (isLeft[0]) {
                    rightElement.setVisible(false);
                    rightElement.setActive(false);
                    leftElement.setVisible(true);
                    leftElement.setActive(true);
                    if (handleOptional) {
                        missingElement.visible = false;
                    }
                } else {
                    leftElement.setVisible(false);
                    leftElement.setActive(false);
                    rightElement.setVisible(true);
                    rightElement.setActive(true);
                    if (handleOptional) {
                        missingElement.visible = false;
                    }
                }
            };
            updateVisibility.run();
            var layout = new EqualSpacingLayout(fullWidth, Button.DEFAULT_HEIGHT, EqualSpacingLayout.Orientation.HORIZONTAL);
            var switchButton = Button.builder(Component.empty(), b -> {
                if (handleOptional) {
                    if (isMissing[0]) {
                        isMissing[0] = false;
                        isLeft[0] = true;
                        update.accept(JsonNull.INSTANCE);
                    } else if (isLeft[0]) {
                        isLeft[0] = false;
                    } else {
                        isMissing[0] = true;
                    }
                } else {
                    isLeft[0] = !isLeft[0];
                }
                updateVisibility.run();
            }).width(Button.DEFAULT_HEIGHT).tooltip(Tooltip.create(Component.translatable("codecextras.config.either.switch"))).build();
            layout.addChild(switchButton, LayoutSettings.defaults().alignVerticallyMiddle());
            layout.addChild(frame, LayoutSettings.defaults().alignVerticallyMiddle());
            return VisibilityWrapperElement.ofDirect(layout);
        };
    }

    public static <T> LayoutFactory<T> unit() {
        return unit(Component.translatable("codecextras.config.unit"));
    }

    public static <T> LayoutFactory<T> unit(Component text) {
        return (parent, width, context, original, update, creationInfo, handleOptional) -> {
            if (original.isJsonNull()) {
                original = new JsonObject();
                if (!handleOptional) {
                    update.accept(original);
                }
            }
            if (handleOptional) {
                var w = Checkbox.builder(Component.empty(), Minecraft.getInstance().font)
                    .maxWidth(width)
                    .onValueChange((checkbox, b) -> {
                        update.accept(b ? new JsonObject() : JsonNull.INSTANCE);
                    })
                    .selected(original.isJsonPrimitive() && original.getAsJsonPrimitive().getAsBoolean())
                    .build();
                creationInfo.componentInfo().maybeDescription().ifPresent(description -> {
                    var tooltip = Tooltip.create(description);
                    w.setTooltip(tooltip);
                });
                w.setMessage(creationInfo.componentInfo().title());
                return w;
            } else {
                var button = Button.builder(text, b -> {
                    })
                    .width(width)
                    .build();
                var tooltip = Tooltip.create(creationInfo.componentInfo().description());
                button.setTooltip(tooltip);
                button.active = false;
                return VisibilityWrapperElement.ofInactive(button);
            }
        };
    }

    public static <T> LayoutFactory<T> bool() {
        LayoutFactory<T> widget = (parent, width, context, original, update, creationInfo, handleOptional) -> {
            if (original.isJsonNull()) {
                original = new JsonPrimitive(false);
                if (!handleOptional) {
                    update.accept(original);
                }
            }
            var w = Checkbox.builder(Component.empty(), Minecraft.getInstance().font)
                .maxWidth(width)
                .onValueChange((checkbox, b) -> {
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
        return (parent, width, context, original, update, entry, handleOptional) -> {
            if (handleOptional) {
                return widget.create(parent, width, context, original, update, entry, true);
            }
            var button = Button.builder(Component.translatable("codecextras.config.unit"), b -> {})
                .width(width)
                .build();
            button.active = false;
            return VisibilityWrapperElement.ofInactive(button);
        };
    }
}
