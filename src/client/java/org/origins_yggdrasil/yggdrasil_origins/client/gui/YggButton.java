package org.origins_yggdrasil.yggdrasil_origins.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.Util;

/**
 * A button in the picker style: no vanilla texture, its own frame,
 * a smooth hover, a press and a fade when disabled.
 */
public class YggButton extends AbstractButton {

    /** Button look. PRIMARY is the main action, GHOST a secondary one. */
    public enum Variant { PRIMARY, GHOST }

    public interface OnPress {
        void onPress(YggButton button);
    }

    /** Draws the button icon: the colour already carries the needed alpha. */
    public interface IconPainter {
        void paint(GuiGraphics graphics, int x, int y, int color);
    }

    private final Variant variant;
    private final OnPress action;

    private float hover = 0f;
    private float press = 0f;
    private float enabled = 1f;
    private long lastFrame = Util.getMillis();

    /** Alpha of the whole button - the screen overrides it during the entry animation. */
    private float fade = 1f;

    private IconPainter icon;
    private int iconSize = 9;
    private boolean selected;

    public YggButton(int x, int y, int width, int height, Component message, Variant variant, OnPress action) {
        super(x, y, width, height, message);
        this.variant = variant;
        this.action = action;
    }

    public static YggButton primary(int x, int y, int w, int h, Component message, OnPress action) {
        return new YggButton(x, y, w, h, message, Variant.PRIMARY, action);
    }

    public static YggButton ghost(int x, int y, int w, int h, Component message, OnPress action) {
        return new YggButton(x, y, w, h, message, Variant.GHOST, action);
    }

    public YggButton icon(IconPainter painter, int size) {
        this.icon = painter;
        this.iconSize = size;
        return this;
    }

    /** Toggle "on" state - the button stays highlighted without a hover. */
    public YggButton selected(boolean value) {
        this.selected = value;
        return this;
    }

    public boolean isSelected() {
        return selected;
    }

    public YggButton fade(float value) {
        this.fade = Mth.clamp(value, 0f, 1f);
        return this;
    }

    @Override
    public void onPress() {
        press = 1f;
        action.onPress(this);
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    @Override
    public void playDownSound(SoundManager sounds) {
        sounds.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
            variant == Variant.PRIMARY ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.UI_BUTTON_CLICK.value(),
            variant == Variant.PRIMARY ? 1.2f : 1.0f, 0.5f));
    }

    private void advance() {
        long now = Util.getMillis();
        float dt = Mth.clamp((now - lastFrame) / 1000f, 0f, 0.1f);
        lastFrame = now;
        hover = Mth.lerp(Math.min(1f, dt * 12f), hover, isHovered() && active ? 1f : 0f);
        press = Mth.lerp(Math.min(1f, dt * 9f), press, 0f);
        enabled = Mth.lerp(Math.min(1f, dt * 8f), enabled, active ? 1f : 0f);
    }

    private int alpha(int rgb, float a) {
        int value = Mth.clamp((int) (a * fade * 255f), 0, 255);
        return (value << 24) | (rgb & 0xFFFFFF);
    }

    /** A fill with clipped corners - reads as a rounded rectangle. */
    private void rounded(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0 + 1, y0, x1 - 1, y1, color);
        graphics.fill(x0, y0 + 1, x0 + 1, y1 - 1, color);
        graphics.fill(x1 - 1, y0 + 1, x1, y1 - 1, color);
    }

    private void frame(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0 + 1, y0, x1 - 1, y0 + 1, color);
        graphics.fill(x0 + 1, y1 - 1, x1 - 1, y1, color);
        graphics.fill(x0, y0 + 1, x0 + 1, y1 - 1, color);
        graphics.fill(x1 - 1, y0 + 1, x1, y1 - 1, color);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        advance();
        if (fade <= 0.01f) return;

        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + width;
        int y1 = y0 + height;

        // a press pushes the button in slightly
        int sink = press > 0.08f ? 1 : 0;
        y0 += sink;
        y1 += sink;

        float lit = Mth.clamp(Math.max(hover, selected ? 0.75f : 0f) * enabled, 0f, 1f);
        float pulse = 0.5f + 0.5f * Mth.sin((Util.getMillis() % 2600L) / 2600f * (float) Math.PI * 2f);

        if (variant == Variant.PRIMARY) {
            float base = 0.16f + 0.10f * lit + (active ? 0.04f * pulse : 0f);
            rounded(graphics, x0, y0, x1, y1, alpha(ClassSelectScreen.RGB_ACCENT, base * Math.max(0.35f, enabled)));
            // a light inner shadow at the bottom gives the button volume
            graphics.fillGradient(x0 + 1, y0 + 1, x1 - 1, y1 - 1,
                alpha(0xFFFFFF, 0.05f * enabled), alpha(0x000000, 0.18f));
            frame(graphics, x0, y0, x1, y1,
                alpha(ClassSelectScreen.RGB_ACCENT, (0.45f + 0.45f * lit) * Math.max(0.25f, enabled)));
            if (lit > 0.02f) {
                // top edge highlight on hover
                graphics.fill(x0 + 2, y0 + 1, x1 - 2, y0 + 2, alpha(ClassSelectScreen.RGB_ACCENT, 0.35f * lit));
            }
        } else {
            rounded(graphics, x0, y0, x1, y1, alpha(ClassSelectScreen.RGB_PANEL, 0.72f + 0.16f * lit));
            frame(graphics, x0, y0, x1, y1,
                alpha(lit > 0.02f ? ClassSelectScreen.RGB_ACCENT2 : ClassSelectScreen.RGB_EDGE,
                    0.55f + 0.40f * lit));
        }

        if (isFocused()) {
            frame(graphics, x0 - 1, y0 - 1, x1 + 1, y1 + 1, alpha(ClassSelectScreen.RGB_ACCENT2, 0.75f));
        }

        if (selected && variant == Variant.GHOST) {
            frame(graphics, x0, y0, x1, y1, alpha(ClassSelectScreen.RGB_ACCENT, 0.85f));
        }

        int textRgb = variant == Variant.PRIMARY
            ? (active ? ClassSelectScreen.RGB_ACCENT : ClassSelectScreen.RGB_DIM)
            : (selected ? ClassSelectScreen.RGB_ACCENT : lit > 0.02f ? ClassSelectScreen.RGB_TEXT : ClassSelectScreen.RGB_DIM);
        float textAlpha = active ? 1f : 0.55f;

        var font = Minecraft.getInstance().font;
        String text = getMessage().getString();
        int gap = icon != null && !text.isEmpty() ? 4 : 0;
        int max = width - 10 - (icon != null ? iconSize + gap : 0);
        if (!text.isEmpty() && font.width(text) > max) text = font.plainSubstrByWidth(text, Math.max(6, max - 6)) + "…";

        int textW = text.isEmpty() ? 0 : font.width(text);
        int block = (icon != null ? iconSize + gap : 0) + textW;
        int cursor = x0 + (width - block) / 2;
        if (icon != null) {
            icon.paint(graphics, cursor, y0 + (height - iconSize) / 2,
                alpha(selected || lit > 0.02f ? 0xFFFFFF : ClassSelectScreen.RGB_DIM, textAlpha));
            cursor += iconSize + gap;
        }
        if (!text.isEmpty()) {
            graphics.drawString(font, text, cursor, y0 + (height - 8) / 2, alpha(textRgb, textAlpha), false);
        }
    }
}
