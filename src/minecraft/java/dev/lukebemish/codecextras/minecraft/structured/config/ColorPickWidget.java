package dev.lukebemish.codecextras.minecraft.structured.config;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

class ColorPickWidget extends AbstractWidget {
    private static final ResourceLocation HUE = ResourceLocation.fromNamespaceAndPath("codecextras_minecraft", "widget/hue");
    private static final ResourceLocation TRANSPARENT = ResourceLocation.fromNamespaceAndPath("codecextras_minecraft", "widget/transparent");
    private static final int BORDER_COLOR = 0xFFA0A0A0;

    private final Consumer<Integer> consumeClick;
    private final boolean hasAlpha;

    private int color;
    private double alpha;
    private double hue;
    private double saturation;
    private double value;

    private int fullySaturated;

    ColorPickWidget(int i, int j, Component component, Consumer<Integer> consumer, boolean hasAlpha) {
        super(i, j, calculateWidth(hasAlpha), 128 + 2, component);
        this.consumeClick = consumer;
        this.hasAlpha = hasAlpha;
    }

    private static int calculateWidth(boolean alpha) {
        return 128 + 8*(alpha ? 4 : 2) + 2*(alpha ? 3 : 2);
    }

    public void setColor(int argbColor) {
        this.color = argbColor;

        double r = (argbColor >> 16 & 255) / 255.0F;
        double g = (argbColor >> 8 & 255) / 255.0F;
        double b = (argbColor & 255) / 255.0F;

        this.alpha = (argbColor >> 24 & 255) / 255.0F;
        this.value = value(r, g, b);
        var saturation = saturation(r, g, b);

        if (toRgb(this.hue, saturation, this.value) != argbColor) {
            this.hue = hue(r, g, b);
        }

        if (toRgb(this.hue, this.saturation, this.value) != argbColor) {
            this.saturation = saturation;
        }

        this.fullySaturated = 0xFF000000 | toRgb(hue, 1.0, 1.0);
    }

    private static int toRgb(double hue, double saturation, double value) {
        double prime = hue / (1d/6d);
        double c = value * saturation;
        double x = c * (1 - Math.abs(prime % 2 - 1));
        double r = 0;
        double g = 0;
        double b = 0;
        if (prime < 1) {
            r = c;
            g = x;
        } else if (prime < 2) {
            r = x;
            g = c;
        } else if (prime < 3) {
            g = c;
            b = x;
        } else if (prime < 4) {
            g = x;
            b = c;
        } else if (prime < 5) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }

        r += value - c;
        g += value - c;
        b += value - c;

        int rI = (int) Math.round(r * 255);
        int gI = (int) Math.round(g * 255);
        int bI = (int) Math.round(b * 255);
        return (rI & 0xFF) << 16 | (gI & 0xFF) << 8 | (bI & 0xFF);
    }

    private static double value(double r, double g, double b) {
        return Math.max(r, Math.max(g, b));
    }

    private static double saturation(double r, double g, double b) {
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double diff = max - min;
        return max == 0 ? 0 : diff / max;
    }

    private static double hue(double r, double g, double b) {
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double diff = max - min;
        double h;

        if (diff == 0) {
            h = 0;
        } else if (max == r) {
            h = (1d/6d * ((g - b) / diff) + 1.0) % 1.0;
        } else if (max == g) {
            h = (1d/6d * ((b - r) / diff) + 1d/3d) % 1.0;
        } else {
            h = (1d/6d * ((r - g) / diff) + 2d/3d) % 1.0;
        }
        return h;
    }

    private int invert(int rgb) {
        int r = 255 - (rgb >> 16 & 255);
        int g = 255 - (rgb >> 8 & 255);
        int b = 255 - (rgb & 255);
        return r << 16 | g << 8 | b;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        if (!this.visible) {
            return;
        }
        int x1 = getX()+1;
        int y1 = getY()+1;
        int x2 = x1 + 128;
        int y2 = y1 + 128;
        guiGraphics.renderOutline(getX(), getY(), 128+2, 128+2, BORDER_COLOR);
        guiGraphics.renderOutline(getX()+128+8+2, getY(), 8+2, 128+2, BORDER_COLOR);
        if (hasAlpha) {
            guiGraphics.renderOutline(getX() + 128 + 8 * 3 + 2 * 2, getY(), 8 + 2, 128 + 2, BORDER_COLOR);
        }
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        VertexConsumer vertexConsumer = guiGraphics.bufferSource().getBuffer(RenderType.gui());
        vertexConsumer.addVertex(matrix4f, x1, y1, 0).setColor(0xFFFFFFFF);
        vertexConsumer.addVertex(matrix4f, x1, y2, 0).setColor(0xFFFFFFFF);
        vertexConsumer.addVertex(matrix4f, x2, y2, 0).setColor(fullySaturated);
        vertexConsumer.addVertex(matrix4f, x2, y1, 0).setColor(fullySaturated);
        guiGraphics.flush();
        guiGraphics.fillGradient(x1, y1, x2, y2, 0, 0xFF000000);

        guiGraphics.enableScissor(x1, y1, x2, y2);
        int xCenter = (int) (x1 + saturation * 127);
        int yCenter = (int) (y1 + (1 - value) * 127);
        guiGraphics.fill(xCenter-2, yCenter, xCenter+3, yCenter+1, invert(color) | 0xFF000000);
        guiGraphics.fill(xCenter, yCenter-2, xCenter+1, yCenter+3, invert(color) | 0xFF000000);
        guiGraphics.disableScissor();

        guiGraphics.blitSprite(HUE, x1+128+8+2, y1, 8, 128);

        guiGraphics.fill(x1+128+8+2, y1+(int)(hue*127), x1+128+8*2+2, y1+(int)(hue*127)+1, 0xFF000000);

        var ax1 = x1 + 128 + 8 * 3 + 2 * 2;
        var ax2 = ax1 + 8;
        if (this.hasAlpha) {
            guiGraphics.blitSprite(TRANSPARENT, ax1, y1, 8, 128);

            guiGraphics.fillGradient(ax1, y1, ax2, y2, 0x00000000, color | 0xFF000000);

            guiGraphics.fill(ax1, y1+(int)(alpha*127), ax2, y1+(int)(alpha*127)+1, 0xFF000000);
        }
    }

    @Override
    protected void onDrag(double x, double y, double oldX, double oldY) {
        this.fromPosition(x, y);
        super.onDrag(x, y, oldX, oldY);
    }

    @Override
    public void onClick(double x, double y) {
        this.fromPosition(x, y);
        super.onClick(x, y);
    }

    private void fromPosition(double x, double y) {
        x = x - getX();
        y = y - getY();
        if (x > 1 && x < 128+1 && y > 1 && y < 129) {
            saturation = Math.max(0, Math.min(1, (x - 1) / 128));
            value = Math.max(0, Math.min(1, 1 - (y - 1) / 128));
            updateColor();
        } else if (x > 128+2+8 && x < 128+2+8*2+2 && y > 1 && y < 129) {
            hue = Math.max(0, Math.min(1, (y - 1) / 128));
            updateColor();
        } else if (hasAlpha && x > 128+2*2+8*3 && x < 128+2*2+8*4+2 && y > 1 && y < 129) {
            alpha = Math.max(0, Math.min(1, (y - 1) / 128));
            updateColor();
        }
    }

    private void updateColor() {
        int color = toRgb(hue, saturation, value);
        if (hasAlpha) {
            color |= (int) (alpha * 255) << 24;
        }
        consumeClick.accept(color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
