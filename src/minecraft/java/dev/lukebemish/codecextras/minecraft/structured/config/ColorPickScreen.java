package dev.lukebemish.codecextras.minecraft.structured.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

class ColorPickScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Screen backgroundScreen;
    private final LinearLayout layout = LinearLayout.vertical();
    private final Consumer<Integer> consumer;
    private final boolean hasAlpha;
    int color;
    private @Nullable EditBox textField;
    private @Nullable ColorPickWidget pick;

    protected ColorPickScreen(Screen backgroundScreen, Component component, Consumer<Integer> consumer, boolean hasAlpha) {
        super(component);
        this.backgroundScreen = backgroundScreen;
        this.consumer = consumer;
        this.hasAlpha = hasAlpha;
    }

    @Override
    public void added() {
        super.added();
        this.backgroundScreen.clearFocus();
    }

    protected void init() {
        this.backgroundScreen.init(this.minecraft, this.width, this.height);
        this.layout.spacing(12).defaultCellSetting().alignHorizontallyCenter();
        this.pick = new ColorPickWidget(0, 0, Component.empty(), this::setColor, hasAlpha);
        this.layout.addChild(pick);
        var bottomLayout = new EqualSpacingLayout(pick.getWidth(), Button.DEFAULT_HEIGHT, EqualSpacingLayout.Orientation.HORIZONTAL);
        this.textField = new EditBox(this.font, 0, 0, 80, Button.DEFAULT_HEIGHT, Component.empty());
        textField.setResponder(string -> {
            try {
                if (hasAlpha && string.length() != 8) {
                    return;
                } else if (!hasAlpha && string.length() != 6) {
                    return;
                }
                var color = Integer.parseUnsignedInt(string, 16);
                setColor(color);
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid hex number: {}", string, e);
            }
        });
        textField.setFilter(string -> string.matches("^[0-9a-fA-F]{0,"+(hasAlpha ? 8 : 6)+"}$"));
        var button = Button.builder(CommonComponents.GUI_DONE, b -> this.onClose()).width(pick.getWidth() - 80 - 8).build();
        bottomLayout.addChild(textField);
        bottomLayout.addChild(button);
        this.layout.addChild(bottomLayout);
        this.layout.visitWidgets(this::addRenderableWidget);
        this.updateColor(color);
        this.repositionElements();
    }

    public void onClose() {
        this.consumer.accept(this.color);
        this.minecraft.setScreen(this.backgroundScreen);
    }

    @Override
    protected void repositionElements() {
        this.backgroundScreen.resize(this.minecraft, this.width, this.height);
        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
    }

    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        this.backgroundScreen.render(guiGraphics, -1, -1, f);
        guiGraphics.flush();
        RenderSystem.clear(256);
        this.renderTransparentBackground(guiGraphics);
    }

    public void setColor(int color) {
        if (color != this.color) {
            updateColor(color);
        }
    }

    private void updateColor(int color) {
        this.color = color;
        var string = Integer.toHexString(hasAlpha ? color : color & 0xFFFFFF);
        if (hasAlpha) {
            string = "00000000".substring(string.length()) + string;
        } else {
            string = "000000".substring(string.length()) + string;
        }
        if (this.textField != null) {
            if (!this.textField.getValue().equalsIgnoreCase(string)) {
                this.textField.setValue(string.toUpperCase(Locale.ROOT));
            }
        }
        if (this.pick != null) {
        this.pick.setColor(color);
        }
    }
}
