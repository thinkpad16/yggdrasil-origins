package org.origins_yggdrasil.yggdrasil_origins.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * A flat button in the look picker's own style: a dark slab with a thin border that lights
 * up along its left edge when the pointer is over it.
 * <p>
 * The vanilla button texture is built for the inventory and reads as a different program
 * next to this screen, so these are drawn by hand. Everything else about them is a normal
 * widget: they take keyboard focus and narrate like any other button.
 */
public final class PanelButton extends AbstractWidget {

    private static final int FILL = 0x171B27;
    private static final int FILL_OVER = 0x222838;
    private static final int EDGE = 0x2E3547;
    private static final int TEXT = 0xF2F3F7;
    private static final int TEXT_OFF = 0x6E7488;

    private final Runnable action;
    private final int accent;
    /** Grows towards one while the pointer rests on the button, so nothing snaps. */
    private float warmth;
    /** Asked every frame whether this button is the one currently in force. */
    private java.util.function.BooleanSupplier chosen = () -> false;
    /** Asked every frame whether the button has anything to do at all. */
    private java.util.function.BooleanSupplier usable = () -> true;
    /** Counts down while the button is held, so a click looks like a press. */
    private int pressed;

    public PanelButton marking(java.util.function.BooleanSupplier test) {
        this.chosen = test;
        return this;
    }

    public PanelButton usableWhen(java.util.function.BooleanSupplier test) {
        this.usable = test;
        return this;
    }

    public PanelButton(int x, int y, int width, int height, Component label, int accent,
                       Runnable action) {
        super(x, y, width, height, label);
        this.action = action;
        this.accent = accent;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        pressed = 3;
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        active = usable.getAsBoolean();
        boolean marked = chosen.getAsBoolean();
        boolean lit = (isHoveredOrFocused() && active) || marked;
        warmth = Mth.clamp(warmth + (lit ? 0.22F : -0.22F), 0.0F, 1.0F);

        int x = getX();
        int y = getY();
        int right = x + width;
        int bottom = y + height;

        graphics.fill(x, y, right, bottom, 0xFF000000 | blend(FILL, FILL_OVER, warmth));
        int edge = blend(EDGE, accent, warmth);
        graphics.fill(x, y, right, y + 1, 0xFF000000 | edge);
        graphics.fill(x, bottom - 1, right, bottom, 0xFF000000 | edge);
        graphics.fill(x, y, x + 1, bottom, 0xFF000000 | edge);
        graphics.fill(right - 1, y, right, bottom, 0xFF000000 | edge);
        // the lit edge: a bar that grows down the left side rather than a flashing border
        if (warmth > 0.0F) {
            int bar = Math.max(1, Math.round((height - 2) * warmth));
            graphics.fill(x, y + 1 + (height - 2 - bar) / 2, x + 2,
                y + 1 + (height - 2 - bar) / 2 + bar, 0xFF000000 | accent);
        }

        // a bar under the label marks the one in force, the way a tab is marked
        if (marked) {
            graphics.fill(x + 3, bottom - 2, right - 3, bottom - 1, 0xFF000000 | accent);
        }
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int colour = active ? blend(TEXT, accent, warmth) : TEXT_OFF;
        int sink = pressed > 0 ? 1 : 0;
        if (pressed > 0) pressed--;
        graphics.drawString(font, getMessage(),
            x + (width - font.width(getMessage())) / 2, y + (height - 8) / 2 + sink,
            0xFF000000 | colour, false);
    }

    /** Mixes two packed colours; {@code amount} of zero keeps the first. */
    static int blend(int from, int to, float amount) {
        float t = Mth.clamp(amount, 0.0F, 1.0F);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    /** Lays a row of buttons out across a width, with an even gap between them. */
    static void row(Screen screen, java.util.function.Consumer<PanelButton> add, int x, int y,
                    int width, int height, int gap, int accent, Entry... entries) {
        if (entries.length == 0) return;
        int each = (width - gap * (entries.length - 1)) / entries.length;
        for (int i = 0; i < entries.length; i++) {
            int left = x + i * (each + gap);
            int span = i == entries.length - 1 ? x + width - left : each;
            PanelButton button = new PanelButton(left, y, span, height, entries[i].label(),
                accent, entries[i].action());
            if (entries[i].chosen() != null) button.marking(entries[i].chosen());
            if (entries[i].usable() != null) button.usableWhen(entries[i].usable());
            add.accept(button);
        }
    }

    record Entry(Component label, Runnable action, java.util.function.BooleanSupplier chosen,
                 java.util.function.BooleanSupplier usable) {

        Entry(Component label, Runnable action) {
            this(label, action, null, null);
        }

        Entry(Component label, Runnable action, java.util.function.BooleanSupplier chosen) {
            this(label, action, chosen, null);
        }
    }
}
