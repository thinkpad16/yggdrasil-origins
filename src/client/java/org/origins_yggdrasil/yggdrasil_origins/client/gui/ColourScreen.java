package org.origins_yggdrasil.yggdrasil_origins.client.gui;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.origins_yggdrasil.yggdrasil_origins.client.ClientClasses;
import org.origins_yggdrasil.yggdrasil_origins.client.ClientVisuals;
import org.origins_yggdrasil.yggdrasil_origins.net.YggPackets;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualPart;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualProfile;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * The step after the origin and the path: the player chooses what colour each of their
 * elements is, and nothing else.
 * <p>
 * Everything else an element could be given - its shape, its size, how it sits, whether it
 * is worn at all - is not in a player's hands at the moment: see {@link VisualStyle}, which
 * drops those settings wherever a look is read. This screen is deliberately the only way
 * into a look, so there is one place where that holds.
 */
public final class ColourScreen extends Screen {

    private static final int RGB_TEXT = 0xF2F3F7;
    private static final int RGB_DIM = 0x9AA0B4;
    private static final int RGB_ACCENT = 0xFFD479;
    private static final int RGB_ACCENT2 = 0x7FD1FF;
    private static final int RGB_PANEL = 0x12141C;
    private static final int RGB_EDGE = 0x262C3C;

    /** Twelve hues over four shades, plus a greyscale row and "leave it alone". */
    private static final int HUES = 12;
    private static final int SHADES = 4;
    private static final int[] PALETTE = buildPalette();

    private static int[] buildPalette() {
        int[] out = new int[1 + HUES * SHADES + HUES];
        int at = 0;
        out[at++] = VisualStyle.NO_TINT;
        for (int shade = 0; shade < SHADES; shade++) {
            float value = 1.0F - shade * 0.22F;
            float sat = shade == 0 ? 0.38F : 0.72F;
            for (int hue = 0; hue < HUES; hue++) {
                out[at++] = Mth.hsvToRgb(hue / (float) HUES, sat, value);
            }
        }
        for (int step = 0; step < HUES; step++) {
            int grey = Math.round(240 - step * (225f / (HUES - 1)));
            out[at++] = (grey << 16) | (grey << 8) | grey;
        }
        return out;
    }

    private final List<VisualPart> parts = new ArrayList<>();
    /** Colours this origin is known by, offered before the general palette. */
    private List<Integer> ownPalette = List.of();
    private VisualStyle style = VisualStyle.EMPTY;
    private int picked;
    private int scroll;
    private boolean relayout;
    private boolean draggingScroll;
    private final long opened = Util.getMillis();

    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int sideX;
    private int sideY;
    private int sideW;
    private int sideH;
    private int viewLeft;
    private int viewRight;
    private int viewTop;
    private int viewBottom;
    private int swatchCols;

    /** The angle the player turned the figure to, and how close they pulled it. */
    private float viewYaw;
    private float viewPitch;
    private float zoom = 52.0F;
    /** Until the figure is grabbed it turns on its own, showing every side of the work. */
    private boolean orbited;

    private static final int ROW = 14;
    private static final int SWATCH = 11;

    public ColourScreen() {
        super(Component.translatable("gui.yggdrasil_origins.customise.title"));
    }

    @Override
    protected void init() {
        parts.clear();
        if (minecraft != null && minecraft.player != null) {
            VisualProfile profile = ClientVisuals.of(minecraft.player.getUUID());
            // an element the origin only offers is not worn, so there is nothing to paint
            for (VisualPart part : profile.parts()) {
                if (!profile.optionalPart(part)) parts.add(part);
            }
            ownPalette = profile.palette();
            VisualStyle worn = ClientVisuals.styleOf(minecraft.player.getUUID());
            style = worn == null ? VisualStyle.EMPTY : worn;
        }
        picked = Mth.clamp(picked, 0, Math.max(0, parts.size() - 1));

        int margin = 14;
        int top = 42;
        int bottom = height - 30;
        listW = Mth.clamp(width / 5, 104, 150);
        sideW = Mth.clamp(width / 4, 128, 152);
        listX = margin;
        listY = top;
        listH = Math.max(60, bottom - top);
        sideX = width - margin - sideW;
        sideY = top;
        sideH = listH;
        viewLeft = listX + listW + 8;
        viewRight = sideX - 8;
        viewTop = top;
        viewBottom = bottom;
        swatchCols = Math.max(8, (sideW - 4) / SWATCH);
        scroll = Mth.clamp(scroll, 0, Math.max(0, parts.size() - visibleRows()));

        buildViewControls();
        buildBottomBar(margin, bottom);
    }

    /** Turning the figure by hand, for players who would never think to drag it. */
    private void buildViewControls() {
        if (viewRight - viewLeft < 96) return;
        int y = viewBottom - 18;
        int middle = (viewLeft + viewRight) / 2;
        addRenderableWidget(new PanelButton(middle - 42, y, 24, 16,
            Component.literal("◄"), RGB_ACCENT2, () -> turn(-24.0F)));
        addRenderableWidget(new PanelButton(middle - 14, y, 28, 16,
            Component.literal("↻"), RGB_ACCENT2, this::resetView));
        addRenderableWidget(new PanelButton(middle + 18, y, 24, 16,
            Component.literal("►"), RGB_ACCENT2, () -> turn(24.0F)));
    }

    private void buildBottomBar(int margin, int bottom) {
        int y = bottom + 6;
        int rightEdge = width - margin - Math.min(sideW, 152) - 8;
        int leftSpan = Mth.clamp(Math.min(viewRight - listX, 240), 120,
            Math.max(120, rightEdge - listX));
        PanelButton.row(this, this::addRenderableWidget, listX, y, leftSpan, 20, 5, RGB_ACCENT2,
            new PanelButton.Entry(Component.translatable("gui.yggdrasil_origins.customise.colour_all"),
                this::colourAll, null, () -> parts.size() > 1),
            new PanelButton.Entry(Component.translatable("gui.yggdrasil_origins.customise.reset_all"),
                this::resetAll, null, () -> !style.isEmpty()));
        int rightW = Math.min(sideW, 152);
        int before = children().size();
        PanelButton.row(this, this::addRenderableWidget, width - margin - rightW, y, rightW, 20, 4,
            RGB_ACCENT,
            new PanelButton.Entry(Component.translatable("gui.yggdrasil_origins.customise.reset"),
                this::resetPart, null, () -> !trim().isDefault()),
            new PanelButton.Entry(Component.translatable("gui.yggdrasil_origins.customise.done"),
                this::confirm));
        String[] hints = {
            "gui.yggdrasil_origins.colour.reset_hint",
            "gui.yggdrasil_origins.customise.done_hint",
        };
        int at = 0;
        for (int i = before; i < children().size() && at < hints.length; i++) {
            if (children().get(i) instanceof PanelButton button) {
                button.setTooltip(Tooltip.create(Component.translatable(hints[at++])));
            }
        }
    }

    private void turn(float by) {
        if (!orbited) {
            viewYaw = idleYaw();
            orbited = true;
        }
        viewYaw += by;
    }

    private void resetView() {
        viewPitch = 0.0F;
        zoom = 52.0F;
        orbited = false;
    }

    private VisualPart current() {
        return parts.isEmpty() ? null : parts.get(Mth.clamp(picked, 0, parts.size() - 1));
    }

    private VisualStyle.Trim trim() {
        VisualPart part = current();
        return part == null ? VisualStyle.Trim.DEFAULT : style.of(part);
    }

    /** The one thing this screen writes: the colour of the element in hand. */
    private void paint(int tint) {
        VisualPart part = current();
        if (part == null) return;
        apply(style.with(part, trim().withTint(tint).clamped(part)));
    }

    private void apply(VisualStyle next) {
        if (next.equals(style)) return;
        style = next;
        ClientVisuals.previewStyle(style);
    }

    /** One colour over everything that can take one: the quick way to look like a clan. */
    private void colourAll() {
        int tint = trim().tint();
        VisualStyle painted = style;
        for (VisualPart part : parts) {
            if (!part.recolourable()) continue;
            painted = painted.with(part, painted.of(part).withTint(tint).clamped(part));
        }
        apply(painted);
    }

    private void resetPart() {
        VisualPart part = current();
        if (part == null) return;
        apply(style.with(part, VisualStyle.Trim.DEFAULT));
        relayout = true;
    }

    private void resetAll() {
        apply(VisualStyle.EMPTY);
        relayout = true;
    }

    private void confirm() {
        JsonObject json = style.toJson();
        ClientPlayNetworking.send(new YggPackets.Style(json.toString()));
        ClientVisuals.previewStyle(null);
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void onClose() {
        // leaving without confirming keeps whatever the server already had
        ClientVisuals.previewStyle(null);
        super.onClose();
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (super.keyPressed(key, scan, modifiers)) return true;
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        if (parts.isEmpty()) return false;
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_UP || key == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN) {
            pick(picked + (key == org.lwjgl.glfw.GLFW.GLFW_KEY_UP ? -1 : 1));
            return true;
        }
        return false;
    }

    private void pick(int row) {
        int next = Mth.clamp(row, 0, Math.max(0, parts.size() - 1));
        if (next == picked) return;
        picked = next;
        int rows = visibleRows();
        scroll = Mth.clamp(scroll, Math.max(0, picked - rows + 1),
            Math.max(0, Math.min(picked, Math.max(0, parts.size() - rows))));
        relayout = true;
    }

    private int visibleRows() {
        return Math.max(1, (listH - 16) / ROW);
    }

    private boolean inView(double mouseX, double mouseY) {
        return mouseX >= viewLeft && mouseX <= viewRight
            && mouseY >= viewTop && mouseY <= viewBottom - 20;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScroll) {
            scrollTo(mouseY);
            return true;
        }
        if (button == 0 && inView(mouseX, mouseY)) {
            if (!orbited) {
                // start from wherever the idle spin had got to, so the figure does not jump
                viewYaw = idleYaw();
                orbited = true;
            }
            viewYaw += (float) dragX;
            viewPitch = Mth.clamp(viewPitch + (float) dragY, -28.0F, 28.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (inView(mouseX, mouseY)) {
            zoom = Mth.clamp(zoom + (float) deltaY * 5.0F, 28.0F, 110.0F);
            return true;
        }
        if (mouseX >= listX && mouseX <= listX + listW) {
            scroll = Mth.clamp(scroll - (int) Math.signum(deltaY), 0,
                Math.max(0, parts.size() - visibleRows()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Puts the list where the pointer is along the scrollbar. */
    private void scrollTo(double mouseY) {
        int rows = visibleRows();
        if (parts.size() <= rows) return;
        int top = listY + 13;
        int span = listY + listH - 2 - top;
        double along = (mouseY - top) / Math.max(1, span);
        scroll = Mth.clamp((int) Math.round(along * (parts.size() - rows)), 0,
            parts.size() - rows);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && inView(mouseX, mouseY)) {
            resetView();
            return true;
        }
        if (button == 0 && parts.size() > visibleRows()
                && mouseX >= listX + listW - 5 && mouseX <= listX + listW
                && mouseY >= listY + 13 && mouseY <= listY + listH - 2) {
            draggingScroll = true;
            scrollTo(mouseY);
            return true;
        }
        int row = (int) ((mouseY - listY - 12) / ROW) + scroll;
        if (mouseX >= listX && mouseX <= listX + listW - 6 && mouseY >= listY + 12
                && row >= 0 && row < parts.size()) {
            pick(row);
            return true;
        }
        if (clickedSwatch(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** The colour panel: the origin's own row first, then everything else. */
    private boolean clickedSwatch(double mouseX, double mouseY) {
        VisualPart part = current();
        if (part == null || !part.recolourable()) return false;
        int x = sideX + 2;
        int y = colourTop();
        if (!ownPalette.isEmpty()) {
            for (int i = 0; i < ownPalette.size() && i < swatchCols; i++) {
                if (hit(mouseX, mouseY, x + i * SWATCH, y, SWATCH - 2, SWATCH - 2)) {
                    paint(ownPalette.get(i));
                    return true;
                }
            }
            y += SWATCH + 12;
        }
        for (int i = 0; i < PALETTE.length; i++) {
            int sx = x + (i % swatchCols) * SWATCH;
            int sy = y + (i / swatchCols) * SWATCH;
            if (hit(mouseX, mouseY, sx, sy, SWATCH - 2, SWATCH - 2)) {
                paint(PALETTE[i]);
                return true;
            }
        }
        return false;
    }

    /** Where the colour rows begin, below the heading over them. */
    private int colourTop() {
        return sideY + 26 + (ownPalette.isEmpty() ? 0 : 11);
    }

    private static boolean hit(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    /**
     * Everything behind the widgets, painted by hand. The vanilla background blurs the
     * world and then paints over whatever a screen has already drawn - which left the
     * picker behind a pane of frosted glass.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float in = Mth.clamp((Util.getMillis() - opened) / 180f, 0f, 1f);
        graphics.fill(0, 0, width, height, 0xFB0D0F1A);

        graphics.drawString(font, title, listX, 16, withAlpha(RGB_ACCENT, in), false);
        Component origin = originName();
        if (origin != null) {
            graphics.drawString(font, origin, listX + font.width(title) + 6, 16,
                withAlpha(RGB_ACCENT2, 0.85f * in), false);
        }
        graphics.drawString(font, font.plainSubstrByWidth(
                Component.translatable("gui.yggdrasil_origins.colour.hint").getString(),
                width - listX * 2),
            listX, 27, withAlpha(RGB_DIM, 0.8f * in), false);
        graphics.fill(listX, 38, width - listX, 39, withAlpha(RGB_EDGE, 0.8f * in));

        renderList(graphics, in, mouseX, mouseY);
        renderView(graphics, in);
        renderSide(graphics, in, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refresh();
        if (relayout) {
            relayout = false;
            rebuildWidgets();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Picks up a look that arrived after the screen opened, which a server can do. */
    private void refresh() {
        if (minecraft == null || minecraft.player == null) return;
        VisualProfile profile = ClientVisuals.of(minecraft.player.getUUID());
        List<VisualPart> worn = new ArrayList<>();
        for (VisualPart part : profile.parts()) {
            if (!profile.optionalPart(part)) worn.add(part);
        }
        if (worn.equals(parts)) return;
        picked = Mth.clamp(picked, 0, Math.max(0, worn.size() - 1));
        scroll = 0;
        relayout = true;
    }

    /** The name of the origin whose marks are being coloured, when it can be worked out. */
    private Component originName() {
        for (var layer : ClientClasses.layers()) {
            var chosen = ClientClasses.chosen(layer.id());
            if (chosen.isEmpty()) continue;
            for (var info : layer.classes()) {
                if (info.id().equals(chosen.get()) && !info.visual().parts().isEmpty()) {
                    return info.name();
                }
            }
        }
        return null;
    }

    /**
     * A dark slab with a hairline border and its corners trimmed off. The trim is what
     * keeps the panels from reading as plain rectangles without changing the style.
     */
    private void frame(GuiGraphics graphics, int x, int y, int right, int bottom, float in) {
        int body = withAlpha(RGB_PANEL, in);
        // painted as three bands so the corner pixels are never filled at all
        graphics.fill(x + 2, y, right - 2, y + 2, body);
        graphics.fill(x, y + 2, right, bottom - 2, body);
        graphics.fill(x + 2, bottom - 2, right - 2, bottom, body);

        int edge = withAlpha(RGB_EDGE, in);
        graphics.fill(x + 2, y, right - 2, y + 1, edge);
        graphics.fill(x + 2, bottom - 1, right - 2, bottom, edge);
        graphics.fill(x, y + 2, x + 1, bottom - 2, edge);
        graphics.fill(right - 1, y + 2, right, bottom - 2, edge);
        // the single pixel that turns each corner
        graphics.fill(x + 1, y + 1, x + 2, y + 2, edge);
        graphics.fill(right - 2, y + 1, right - 1, y + 2, edge);
        graphics.fill(x + 1, bottom - 2, x + 2, bottom - 1, edge);
        graphics.fill(right - 2, bottom - 2, right - 1, bottom - 1, edge);
    }

    /** The strip a panel is titled with: a lighter band with the heading on it. */
    private void panelHeader(GuiGraphics graphics, int x, int y, int right, String key, float in) {
        graphics.fill(x, y, right, y + 11, withAlpha(0x1A1F2B, in));
        graphics.fill(x, y + 11, right, y + 12, withAlpha(RGB_EDGE, in));
        graphics.drawString(font, Component.translatable(key), x + 4, y + 2,
            withAlpha(RGB_DIM, 0.85f * in), false);
    }

    /** A small heading over a group of swatches, with a rule running out to the edge. */
    private void heading(GuiGraphics graphics, int x, int y, int to, String key, float in) {
        Component label = Component.translatable(key);
        graphics.drawString(font, label, x, y, withAlpha(RGB_DIM, 0.75f * in), false);
        int from = x + font.width(label) + 4;
        if (to > from) graphics.fill(from, y + 3, to, y + 4, withAlpha(RGB_EDGE, in));
    }

    private void renderList(GuiGraphics graphics, float in, int mouseX, int mouseY) {
        frame(graphics, listX - 3, listY - 3, listX + listW + 3, listY + listH + 3, in);
        panelHeader(graphics, listX - 2, listY - 2, listX + listW + 2,
            "gui.yggdrasil_origins.customise.group_parts", in);
        if (parts.isEmpty()) {
            graphics.drawString(font,
                Component.translatable("gui.yggdrasil_origins.colour.nothing"),
                listX + 4, listY + 16, withAlpha(RGB_DIM, in), false);
            return;
        }
        int rows = visibleRows();
        for (int i = 0; i < rows && i + scroll < parts.size(); i++) {
            VisualPart part = parts.get(i + scroll);
            int y = listY + 14 + i * ROW;
            boolean active = i + scroll == picked;
            boolean over = hit(mouseX, mouseY, listX, y - 2, listW, ROW - 1);
            if (active || over) {
                graphics.fill(listX, y - 2, listX + listW, y + ROW - 3,
                    withAlpha(active ? RGB_ACCENT : RGB_TEXT, (active ? 0.16f : 0.07f) * in));
            }
            if (active) {
                graphics.fill(listX, y - 2, listX + 2, y + ROW - 3, withAlpha(RGB_ACCENT, in));
            }
            // a chip showing what colour that element is wearing right now
            VisualStyle.Trim worn = style.of(part);
            int chip = worn.tint() == VisualStyle.NO_TINT ? 0x4A5062 : worn.tint();
            graphics.fill(listX + 5, y + 1, listX + 10, y + 6, withAlpha(chip, in));
            graphics.drawString(font, partName(part), listX + 14, y,
                withAlpha(active ? RGB_ACCENT : RGB_TEXT, in), false);
        }
        renderScrollbar(graphics, in, rows);
    }

    /** A thin bar on the edge of the list: where you are, and how much there is. */
    private void renderScrollbar(GuiGraphics graphics, float in, int rows) {
        if (parts.size() <= rows) return;
        int top = listY + 13;
        int bottom = listY + listH - 2;
        int x = listX + listW - 3;
        graphics.fill(x, top, x + 2, bottom, withAlpha(0x1E2432, in));
        int span = bottom - top;
        int thumb = Math.max(10, span * rows / parts.size());
        int at = top + (span - thumb) * scroll / Math.max(1, parts.size() - rows);
        graphics.fill(x, at, x + 2, at + thumb, withAlpha(RGB_ACCENT, 0.75f * in));
    }

    /** The angle of the idle spin at this moment, in degrees. */
    private float idleYaw() {
        return (Util.getMillis() - opened) * 0.03F;
    }

    private void renderView(GuiGraphics graphics, float in) {
        if (minecraft == null || minecraft.player == null) return;
        frame(graphics, viewLeft, viewTop - 3, viewRight, viewBottom + 3, in);
        int x = (viewLeft + viewRight) / 2;
        int y = (viewTop + viewBottom) / 2 + Math.round(zoom * 0.62F);
        // a pool of light behind the figure and a shadow under its feet
        for (int ring = 5; ring >= 1; ring--) {
            int half = Math.round(zoom * (0.22F + ring * 0.16F));
            graphics.fill(Math.max(viewLeft + 1, x - half), Math.max(viewTop + 1, y - half * 2),
                Math.min(viewRight - 1, x + half), Math.min(viewBottom - 1, y + 2),
                withAlpha(0x1A2030, 0.16f * in));
        }
        for (int step = 0; step < 3; step++) {
            int half = Math.round(zoom * (0.34F - step * 0.09F));
            graphics.fill(x - half, y - 1 + step, x + half, y + step,
                withAlpha(0x05070C, (0.30f - step * 0.07f) * in));
        }
        org.joml.Quaternionf spin = new org.joml.Quaternionf()
            .rotateY((float) Math.toRadians(orbited ? viewYaw : idleYaw()))
            .rotateX((float) Math.toRadians(viewPitch));
        org.joml.Quaternionf tilt = new org.joml.Quaternionf().rotateZ((float) Math.PI);
        tilt.mul(spin);
        boolean names = minecraft.options.hideGui;
        minecraft.options.hideGui = true;
        try {
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(
                graphics, x, y, Math.round(zoom), new org.joml.Vector3f(), tilt, null,
                minecraft.player);
        } finally {
            minecraft.options.hideGui = names;
        }
        String turn = font.plainSubstrByWidth(
            Component.translatable("gui.yggdrasil_origins.customise.turn").getString(),
            viewRight - viewLeft - 8);
        graphics.drawString(font, turn, viewLeft + 4, viewTop + 2,
            withAlpha(RGB_DIM, 0.55f * in), false);
    }

    private void renderSide(GuiGraphics graphics, float in, int mouseX, int mouseY) {
        frame(graphics, sideX - 3, sideY - 3, sideX + sideW + 3, sideY + sideH + 3, in);
        VisualPart part = current();
        if (part == null) return;
        graphics.drawString(font, partName(part).copy().withStyle(ChatFormatting.BOLD),
            sideX + 2, sideY + 1, withAlpha(RGB_ACCENT, in), false);
        int x = sideX + 2;
        int w = sideW - 4;
        if (!part.recolourable()) {
            graphics.drawString(font,
                Component.translatable("gui.yggdrasil_origins.customise.skin_colour"),
                x, sideY + 26, withAlpha(RGB_DIM, 0.7f * in), false);
            return;
        }
        int y = colourTop();
        if (!ownPalette.isEmpty()) {
            heading(graphics, x, y - 11, x + w, "gui.yggdrasil_origins.customise.group_own", in);
            for (int i = 0; i < ownPalette.size() && i < swatchCols; i++) {
                swatch(graphics, x + i * SWATCH, y, ownPalette.get(i), in, mouseX, mouseY);
            }
            y += SWATCH + 12;
            heading(graphics, x, y - 11, x + w, "gui.yggdrasil_origins.customise.group_colour", in);
        }
        for (int i = 0; i < PALETTE.length; i++) {
            swatch(graphics, x + (i % swatchCols) * SWATCH, y + (i / swatchCols) * SWATCH,
                PALETTE[i], in, mouseX, mouseY);
        }
    }

    private void swatch(GuiGraphics graphics, int x, int y, int colour, float in,
                        int mouseX, int mouseY) {
        boolean chosen = trim().tint() == colour;
        boolean over = hit(mouseX, mouseY, x, y, SWATCH - 2, SWATCH - 2);
        if (colour == VisualStyle.NO_TINT) {
            graphics.fill(x, y, x + SWATCH - 1, y + SWATCH - 1,
                withAlpha(chosen ? RGB_ACCENT : 0x2A2E3C, in));
            graphics.drawString(font, "×", x + 2, y + 1,
                withAlpha(chosen ? RGB_PANEL : RGB_TEXT, in), false);
        } else {
            graphics.fill(x, y, x + SWATCH - 1, y + SWATCH - 1, withAlpha(colour, in));
        }
        if (chosen) {
            graphics.renderOutline(x - 1, y - 1, SWATCH + 1, SWATCH + 1, withAlpha(RGB_ACCENT, in));
        } else if (over) {
            graphics.renderOutline(x - 1, y - 1, SWATCH + 1, SWATCH + 1,
                withAlpha(RGB_TEXT, 0.7f * in));
        }
    }

    private static Component partName(VisualPart part) {
        return Component.translatable("part.yggdrasil_origins." + part.key());
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Mth.clamp((int) (alpha * 255f), 0, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
