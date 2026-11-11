package org.origins_yggdrasil.yggdrasil_origins.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.Util;

/**
 * A search field in the screen style: its own frame, a lens icon and a focus highlight
 * instead of the vanilla grey border.
 */
public class YggSearchBox extends EditBox {

    private float focus = 0f;
    private long lastFrame = Util.getMillis();

    private Component placeholder;

    private final int frameX;
    private final int frameY;
    private final int frameW;
    private final int frameH;

    /** The coordinates are the frame; the text field sits inside, leaving room for the lens. */
    @SuppressWarnings("this-escape")
    public YggSearchBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x + 16, y + (height - 8) / 2, width - 22, 8, message);
        this.frameX = x;
        this.frameY = y;
        this.frameW = width;
        this.frameH = height;
        // the field draws its own frame; EditBox is final enough here that the call is safe,
        // but the compiler cannot know that, hence the explicit suppression on the constructor
        this.setBordered(false);
    }

    /** Placeholder for the empty field - drawn by hand, because the vanilla one hides on focus. */
    public void placeholder(Component text) {
        this.placeholder = text;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= frameX && mouseX <= frameX + frameW && mouseY >= frameY && mouseY <= frameY + frameH) {
            return super.mouseClicked(
                Mth.clamp(mouseX, getX(), getX() + width - 1),
                Mth.clamp(mouseY, getY(), getY() + height - 1), button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int alpha(int rgb, float a) {
        int value = Mth.clamp((int) (a * 255f), 0, 255);
        return (value << 24) | (rgb & 0xFFFFFF);
    }

    private void frame(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0 + 1, y0, x1 - 1, y0 + 1, color);
        graphics.fill(x0 + 1, y1 - 1, x1 - 1, y1, color);
        graphics.fill(x0, y0 + 1, x0 + 1, y1 - 1, color);
        graphics.fill(x1 - 1, y0 + 1, x1, y1 - 1, color);
    }

    /** The lens is a few fills, so no texture is needed. */
    private void lens(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 1, y, x + 4, y + 1, color);
        graphics.fill(x + 1, y + 4, x + 4, y + 5, color);
        graphics.fill(x, y + 1, x + 1, y + 4, color);
        graphics.fill(x + 4, y + 1, x + 5, y + 4, color);
        graphics.fill(x + 5, y + 5, x + 7, y + 7, color);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        long now = Util.getMillis();
        float dt = Mth.clamp((now - lastFrame) / 1000f, 0f, 0.1f);
        lastFrame = now;
        focus = Mth.lerp(Math.min(1f, dt * 12f), focus, isFocused() ? 1f : 0f);

        int x0 = frameX;
        int y0 = frameY;
        int x1 = frameX + frameW;
        int y1 = frameY + frameH;

        graphics.fill(x0 + 1, y0, x1 - 1, y1, alpha(0x0B0D15, 0.72f));
        graphics.fill(x0, y0 + 1, x0 + 1, y1 - 1, alpha(0x0B0D15, 0.72f));
        graphics.fill(x1 - 1, y0 + 1, x1, y1 - 1, alpha(0x0B0D15, 0.72f));
        frame(graphics, x0, y0, x1, y1,
            alpha(focus > 0.02f ? ClassSelectScreen.RGB_ACCENT2 : ClassSelectScreen.RGB_EDGE, 0.5f + 0.45f * focus));
        lens(graphics, x0 + 5, y0 + (y1 - y0 - 7) / 2,
            alpha(focus > 0.02f ? ClassSelectScreen.RGB_ACCENT2 : ClassSelectScreen.RGB_DIM, 0.6f + 0.4f * focus));

        if (placeholder != null && getValue().isEmpty()) {
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, placeholder,
                getX(), getY(), alpha(ClassSelectScreen.RGB_DIM, 0.45f + 0.15f * focus), false);
        }

        super.renderWidget(graphics, mouseX, mouseY, delta);
    }
}
