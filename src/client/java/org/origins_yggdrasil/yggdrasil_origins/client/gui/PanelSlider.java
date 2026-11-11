package org.origins_yggdrasil.yggdrasil_origins.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * A slider in the picker's own style: a dark track, a filled part up to the handle and the
 * value written across it.
 * <p>
 * It exists because the arrows it replaces needed twenty clicks to cross their range. The
 * value is kept in the caller's own units - the slider only converts to and from the bar.
 */
public final class PanelSlider extends AbstractWidget {

    private static final int TRACK = 0x161A26;
    private static final int TRACK_EDGE = 0x2E3547;
    private static final int TEXT = 0xF2F3F7;

    private final float min;
    private final float max;
    private final float step;
    private final int accent;
    private final java.util.function.Consumer<Float> onChange;
    private final java.util.function.Function<Float, Component> label;
    private float value;

    public PanelSlider(int x, int y, int width, int height, float min, float max, float step,
                       float value, int accent,
                       java.util.function.Function<Float, Component> label,
                       java.util.function.Consumer<Float> onChange) {
        super(x, y, width, height, Component.empty());
        this.min = min;
        this.max = max;
        this.step = step;
        this.accent = accent;
        this.label = label;
        this.onChange = onChange;
        this.value = Mth.clamp(value, min, max);
        setMessage(label.apply(this.value));
    }

    private void set(float raw, boolean notify) {
        float snapped = min + Math.round((Mth.clamp(raw, min, max) - min) / step) * step;
        snapped = Mth.clamp(snapped, min, max);
        if (snapped == value) return;
        value = snapped;
        setMessage(label.apply(value));
        if (notify) onChange.accept(value);
    }

    private void fromMouse(double mouseX) {
        set(min + (float) ((mouseX - getX() - 2) / Math.max(1, width - 4)) * (max - min), true);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        fromMouse(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        fromMouse(mouseX);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT) {
            set(value - step, true);
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT) {
            set(value + step, true);
            return true;
        }
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x, y, right, bottom, 0xFF000000 | TRACK);
        int edge = isHoveredOrFocused() ? accent : TRACK_EDGE;
        graphics.fill(x, y, right, y + 1, 0xFF000000 | edge);
        graphics.fill(x, bottom - 1, right, bottom, 0xFF000000 | edge);
        graphics.fill(x, y, x + 1, bottom, 0xFF000000 | edge);
        graphics.fill(right - 1, y, right, bottom, 0xFF000000 | edge);

        float along = max > min ? (value - min) / (max - min) : 0.0F;
        int filled = Math.round((width - 4) * along);
        if (filled > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + filled, bottom - 2,
                PanelButton.blend(TRACK, accent, 0.28F) | 0xFF000000);
        }
        // the handle: a bright bar rather than a knob, so it reads at any gui scale
        int handle = Mth.clamp(x + 2 + filled, x + 2, right - 4);
        graphics.fill(handle, y + 1, handle + 2, bottom - 1, 0xFF000000 | accent);

        var font = Minecraft.getInstance().font;
        graphics.drawString(font, getMessage(),
            x + (width - font.width(getMessage())) / 2, y + (height - 8) / 2,
            0xFF000000 | TEXT, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
    }
}
