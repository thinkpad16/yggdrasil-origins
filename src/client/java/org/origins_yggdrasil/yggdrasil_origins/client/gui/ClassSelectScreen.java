package org.origins_yggdrasil.yggdrasil_origins.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;
import org.origins_yggdrasil.yggdrasil_origins.client.ClientClasses;
import org.origins_yggdrasil.yggdrasil_origins.client.ClientVisuals;
import org.origins_yggdrasil.yggdrasil_origins.net.YggPackets;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The class picker: a searchable list on the left, details on the right.
 * Everything is animated by time (panels entering, row highlight, smooth scrolling, details
 * appearing), so the screen never jumps and reads calmly.
 */
public class ClassSelectScreen extends Screen {

    private static final int MARGIN = 14;
    private static final int HEADER = 53;
    private static final int FOOTER = 52;
    private static final int ROW = 27;
    /** Inner panel padding - the same on every side. */
    private static final int PAD = 5;
    private static final int SCROLL_W = 3;
    private static final int SCROLL_GAP = 4;

    static final int RGB_PANEL   = 0x11131C;
    static final int RGB_EDGE    = 0x2E3344;
    static final int RGB_TEXT    = 0xF2F3F7;
    static final int RGB_DIM     = 0x9AA0B4;
    static final int RGB_ACCENT  = 0xFFD479;
    static final int RGB_ACCENT2 = 0x7FD1FF;
    /** A gain, a cost, and everything that is neither. */
    static final int RGB_GAIN    = 0x7BE08A;
    static final int RGB_COST    = 0xF2807E;
    static final int RGB_MIXED   = 0xF2D06B;

    private static final ResourceLocation[] IMPACT_ICONS = {
        null,
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "textures/gui/impact/low.png"),
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "textures/gui/impact/medium.png"),
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "textures/gui/impact/high.png"),
    };
    /** Row tint by impact: calm -> amber -> crimson. */
    private static final int[] IMPACT_TINTS = { 0x8A93A8, 0x6FBF8F, 0xE6B422, 0xD1603D };

    private static final long IN_MS = 260L;
    private static final long FADE_MS = 170L;

    private final ClientClasses.LayerInfo layer;
    private final List<ClientClasses.ClassInfo> shown = new ArrayList<>();

    private YggSearchBox search;
    private YggButton confirm;
    private YggButton back;
    private YggButton random;
    private final List<YggButton> filters = new ArrayList<>();
    /** -1 - every impact tier; 1..3 - only the selected one. */
    private int impactFilter = -1;

    private int picked = 0;
    private int listScrollTarget = 0;
    private float listScroll = 0f;
    private int detailScrollTarget = 0;
    private float detailScroll = 0f;
    private int hovered = -1;
    private float hoverAlpha = 0f;
    /** Selection frame position in rows: it glides after {@link #picked} instead of jumping. */
    private float selectionGlide = -1f;
    /** A short "breath" of the portrait when the choice changes. */
    private float portraitPop = 0f;
    /** When the list was last rebuilt - drives the short row fade-in. */
    private long listChangedAt = Util.getMillis();
    private long openedAt = Util.getMillis();
    private long selectedAt = Util.getMillis();
    private long lastFrame = Util.getMillis();
    private long lastClickTime = 0L;
    private long scrolledAt = 0L;
    private long closingAt = 0L;
    private YggPackets.Choose closing;
    private static final long CLOSE_MS = 140L;
    private int lastClickIndex = -1;

    private int listX, listY, listW, listH, detailX, detailY, detailW, detailH;
    private int portraitX, portraitY, portraitW, portraitH;
    /** Accumulated portrait rotation angle. */
    private float portraitSpin;
    /** The bare figure the portrait draws, built once from the player's own profile. */
    private net.minecraft.client.player.RemotePlayer stand;

    /** When the choice changed - drives the staggered appearance of description rows. */
    private long rowsChangedAt = Util.getMillis();

    /** Read-only: the "my class" screen picks nothing, it only shows what was chosen. */
    private final boolean readOnly;

    public ClassSelectScreen(ClientClasses.LayerInfo layer) {
        this(layer, layer.title(), false);
    }

    private ClassSelectScreen(ClientClasses.LayerInfo layer, Component title, boolean readOnly) {
        super(title);
        this.layer = layer;
        this.readOnly = readOnly;
    }

    /**
     * A reference for what is already chosen: one class per layer, without picker buttons.
     * {@code null} means the player has chosen nothing yet and there is nothing to show.
     */
    public static ClassSelectScreen mine() {
        List<ClientClasses.ClassInfo> chosen = new ArrayList<>();
        for (ClientClasses.LayerInfo info : ClientClasses.layers()) {
            ClientClasses.chosen(info.id())
                .flatMap(id -> info.classes().stream().filter(entry -> entry.id().equals(id)).findFirst())
                .ifPresent(chosen::add);
        }
        if (chosen.isEmpty()) return null;
        ClientClasses.LayerInfo synthetic = new ClientClasses.LayerInfo(
            ClientClasses.layers().get(0).id(), false, List.copyOf(chosen));
        return new ClassSelectScreen(synthetic,
            Component.translatable("gui.yggdrasil_origins.picker.mine"), true);
    }

    // ---------------------------------------------------------------- data

    private void applyFilter() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        ClientClasses.ClassInfo previous = shown.isEmpty() ? null : shown.get(Mth.clamp(picked, 0, shown.size() - 1));
        shown.clear();
        listChangedAt = Util.getMillis();
        for (ClientClasses.ClassInfo info : layer.classes()) {
            if (impactFilter > 0 && tierOf(info.impact()) != impactFilter) continue;
            if (query.isEmpty()
                || info.name().getString().toLowerCase(Locale.ROOT).contains(query)
                || info.description().getString().toLowerCase(Locale.ROOT).contains(query)) {
                shown.add(info);
            }
        }
        picked = 0;
        if (previous != null) {
            int at = shown.indexOf(previous);
            if (at >= 0) picked = at;
        }
        listScrollTarget = 0;
        detailScrollTarget = 0;
        selectedAt = Util.getMillis();
        updateButtons();
        selectionGlide = picked;
    }

    /** Zero impact counts together with low - it has no icon of its own. */
    private static int tierOf(int impact) {
        return Mth.clamp(impact, 1, 3);
    }

    private ClientClasses.ClassInfo selected() {
        if (shown.isEmpty()) return null;
        return shown.get(Mth.clamp(picked, 0, shown.size() - 1));
    }

    private void updateButtons() {
        if (confirm == null) return;
        ClientClasses.ClassInfo info = selected();
        confirm.active = info != null;
        confirm.setMessage(info == null
            ? Component.translatable("gui.yggdrasil_origins.picker.nothing")
            : Component.translatable("gui.yggdrasil_origins.picker.confirm", info.name()));
    }

    private int layerIndex() {
        return ClientClasses.layers().indexOf(layer);
    }

    // ---------------------------------------------------------------- layout

    @Override
    protected void init() {
        YggdrasilOrigins.LOGGER.debug("Picker screen: layer {}, classes {}", layer.id(), layer.classes().size());

        // the reference has neither search nor buttons - that space goes to the panels
        int bodyTop = MARGIN + (readOnly ? 24 : HEADER);
        int bodyBottom = height - MARGIN - (readOnly ? 20 : FOOTER);
        listX = MARGIN;
        listY = bodyTop;
        listW = Mth.clamp((int) (width * 0.26f), 136, 210);
        int fits = Math.max(1, (bodyBottom - bodyTop - PAD * 2) / ROW);
        listH = fits * ROW + PAD * 2;
        listY = bodyTop + (bodyBottom - bodyTop - listH) / 2;
        detailX = listX + listW + 10;
        detailY = listY;
        detailW = Math.max(140, width - MARGIN - detailX);
        detailH = listH;

        // portrait on the right when the screen is wide enough
        portraitW = width >= 420 ? Mth.clamp((int) (width * 0.20f), 90, 150) : 0;
        if (portraitW > 0) {
            detailW -= portraitW + 10;
            portraitX = detailX + detailW + 10;
            portraitY = detailY;
            portraitH = detailH;
        }

        search = new YggSearchBox(font, listX, MARGIN + 23, listW, 20,
            Component.translatable("gui.yggdrasil_origins.picker.search"));
        search.placeholder(Component.translatable("gui.yggdrasil_origins.picker.search"));
        search.setMaxLength(48);
        search.setResponder(value -> applyFilter());
        if (!readOnly) addRenderableWidget(search);
        setInitialFocus(search);

        filters.clear();
        int filterY = MARGIN + 23;
        int filterX = detailX;
        YggButton all = YggButton.ghost(filterX, filterY, 38, 20,
            Component.translatable("gui.yggdrasil_origins.picker.filter.all"), button -> setImpactFilter(-1));
        all.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.translatable("gui.yggdrasil_origins.picker.filter.all.tip")));
        filters.add(all);
        filterX += 38 + 4;
        for (int tier = 1; tier <= 3; tier++) {
            int value = tier;
            YggButton button = YggButton.ghost(filterX, filterY, 26, 20, Component.empty(),
                b -> setImpactFilter(value));
            button.icon((graphics, x, y, color) ->
                drawImpact(graphics, value, x, y, 12, ((color >>> 24) & 0xFF) / 255f), 12);
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.yggdrasil_origins.picker.impact")
                    .copy().append(" ").append(impactLabel(value))));
            filters.add(button);
            filterX += 26 + 4;
        }
        if (!readOnly) filters.forEach(this::addRenderableWidget);
        updateFilterButtons();

        if (readOnly) {
            applyFilter();
            return;
        }

        int footerY = height - MARGIN - 22;
        int confirmW = Mth.clamp((int) (width * 0.3f), 170, 240);
        confirm = YggButton.primary(width - MARGIN - confirmW, footerY, confirmW, 22,
            Component.empty(), button -> choose());
        addRenderableWidget(confirm);

        int footerX = MARGIN;
        if (layerIndex() > 0) {
            back = YggButton.ghost(footerX, footerY, 96, 22,
                Component.translatable("gui.yggdrasil_origins.picker.back"), button -> goBack());
            addRenderableWidget(back);
            footerX += 96 + 6;
        }
        if (layer.classes().size() > 1) {
            random = YggButton.ghost(footerX, footerY, 114, 22,
                Component.translatable("gui.yggdrasil_origins.picker.random"), button -> pickRandom());
            random.icon(ClassSelectScreen::shuffleIcon, 9);
            addRenderableWidget(random);
        }

        applyFilter();

    }

    private void setImpactFilter(int tier) {
        impactFilter = impactFilter == tier ? -1 : tier;
        updateFilterButtons();
        applyFilter();
    }

    private void updateFilterButtons() {
        if (filters.isEmpty()) return;
        filters.get(0).selected(impactFilter < 0);
        for (int tier = 1; tier <= 3; tier++) {
            filters.get(tier).selected(impactFilter == tier);
        }
    }

    /** How many list rows are fully visible. */
    private int visibleRows() {
        return Math.max(1, (listH - PAD * 2) / ROW);
    }

    private void pickRandom() {
        if (shown.size() < 2) return;
        int next = picked;
        while (next == picked) next = (int) (Math.random() * shown.size());
        select(next);
        int visible = visibleRows();
        listScrollTarget = Mth.clamp(picked - visible / 2, 0, Math.max(0, shown.size() - visible));
    }

    private void choose() {
        ClientClasses.ClassInfo info = selected();
        if (readOnly || info == null || minecraft == null || closingAt > 0L) return;
        // a short fade first, then the packet and the close - no abrupt jump
        closing = new YggPackets.Choose(layer.id().toString(), info.id().toString());
        closingAt = Util.getMillis();
    }

    /** Finishes the fade and sends the choice. */
    private void finishClosing(GuiGraphics graphics) {
        if (closingAt == 0L) return;
        float progress = Mth.clamp((Util.getMillis() - closingAt) / (float) CLOSE_MS, 0f, 1f);
        graphics.fill(0, 0, width, height, withAlpha(0x05060C, progress));
        if (progress < 1f) return;
        if (closing != null && minecraft != null) {
            ClientPlayNetworking.send(closing);
            closing = null;
            minecraft.setScreen(null);
        }
    }

    private void goBack() {
        if (minecraft == null) return;
        ClientPlayNetworking.send(new YggPackets.Back(layer.id().toString()));
        minecraft.setScreen(null);
    }

    // ---------------------------------------------------------------- animation

    /** 0..1 - eased progress of the screen appearing. */
    private float in() {
        float raw = Mth.clamp((Util.getMillis() - openedAt) / (float) IN_MS, 0f, 1f);
        return 1f - (1f - raw) * (1f - raw) * (1f - raw);
    }

    private float detailsFade() {
        return Mth.clamp((Util.getMillis() - selectedAt) / (float) FADE_MS, 0f, 1f);
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Mth.clamp((int) (alpha * 255f), 8, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private void advance() {
        long now = Util.getMillis();
        float dt = Mth.clamp((now - lastFrame) / 1000f, 0f, 0.1f);
        lastFrame = now;
        float speed = Math.min(1f, dt * 14f);
        listScroll = Mth.lerp(speed, listScroll, listScrollTarget * (float) ROW);
        detailScroll = Mth.lerp(speed, detailScroll, detailScrollTarget);
        hoverAlpha = Mth.lerp(Math.min(1f, dt * 10f), hoverAlpha, hovered >= 0 ? 1f : 0f);
        if (selectionGlide < 0f) {
            selectionGlide = picked;
        } else {
            selectionGlide = Mth.lerp(Math.min(1f, dt * 16f), selectionGlide, picked);
            if (Math.abs(selectionGlide - picked) < 0.004f) selectionGlide = picked;
        }
        portraitPop = Math.max(0f, portraitPop - dt * 5f);
        portraitSpin += dt * 22f;              // slow portrait rotation
        if (portraitSpin > 360f) portraitSpin -= 360f;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // A background of exactly one tone. A full-height gradient with a small colour
        // difference breaks into bands (visible on large screens at GUI scale 2),
        // so depth comes from frames and panels rather than from the fill.
        graphics.fill(0, 0, width, height, 0xFB0D0F1A);
    }

    /** A fill with clipped corners - the frame reads as rounded. */
    private void rounded(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0 + 2, y0, x1 - 2, y1, color);
        graphics.fill(x0, y0 + 2, x0 + 2, y1 - 2, color);
        graphics.fill(x1 - 2, y0 + 2, x1, y1 - 2, color);
        graphics.fill(x0 + 1, y0 + 1, x0 + 2, y0 + 2, color);
        graphics.fill(x1 - 2, y0 + 1, x1 - 1, y0 + 2, color);
        graphics.fill(x0 + 1, y1 - 2, x0 + 2, y1 - 1, color);
        graphics.fill(x1 - 2, y1 - 2, x1 - 1, y1 - 1, color);
    }

    private void frame(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0 + 2, y0, x1 - 2, y0 + 1, color);
        graphics.fill(x0 + 2, y1 - 1, x1 - 2, y1, color);
        graphics.fill(x0, y0 + 2, x0 + 1, y1 - 2, color);
        graphics.fill(x1 - 1, y0 + 2, x1, y1 - 2, color);
        graphics.fill(x0 + 1, y0 + 1, x0 + 2, y0 + 2, color);
        graphics.fill(x1 - 2, y0 + 1, x1 - 1, y0 + 2, color);
        graphics.fill(x0 + 1, y1 - 2, x0 + 2, y1 - 1, color);
        graphics.fill(x1 - 2, y1 - 2, x1 - 1, y1 - 1, color);
    }

    private void panel(GuiGraphics graphics, int x, int y, int w, int h, float alpha) {
        int x1 = x + w;
        int y1 = y + h;
        rounded(graphics, x, y, x1, y1, withAlpha(RGB_PANEL, 0.88f * alpha));
        // The gloss is exactly one tone, two pixels tall. A long gradient with low alpha
        // produces visible steps (especially at GUI scale 2 on 2K), so there is none here.
        graphics.fill(x + 2, y + 1, x1 - 2, y + 3, withAlpha(0xFFFFFF, 0.045f * alpha));
        frame(graphics, x, y, x1, y1, withAlpha(RGB_EDGE, alpha));
        int tick = withAlpha(RGB_ACCENT, 0.55f * alpha);
        graphics.fill(x + 2, y + 1, x + 10, y + 2, tick);
        graphics.fill(x + 1, y + 2, x + 2, y + 10, tick);
        graphics.fill(x1 - 10, y1 - 2, x1 - 2, y1 - 1, tick);
        graphics.fill(x1 - 2, y1 - 10, x1 - 1, y1 - 2, tick);
    }

    /** A scrollbar with a track; brighter right after scrolling, then it dims. */
    private void scrollbar(GuiGraphics graphics, int x, int top, int height, int barY, int barH, float alpha) {
        float active = Mth.clamp(1f - (Util.getMillis() - scrolledAt) / 900f, 0f, 1f);
        alpha *= 0.45f + 0.55f * active;
        graphics.fill(x, top, x + SCROLL_W, top + height, withAlpha(0xFFFFFF, 0.06f * alpha));
        graphics.fill(x, barY, x + SCROLL_W, barY + barH, withAlpha(RGB_ACCENT2, 0.55f * alpha));
        graphics.fill(x, barY + 1, x + 1, barY + barH - 1, withAlpha(0xFFFFFF, 0.25f * alpha));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        advance();
        float in = in();
        int slide = (int) ((1f - in) * 14f);
        if (confirm != null) confirm.fade(in);
        if (back != null) back.fade(in);
        if (random != null) random.fade(in);
        for (YggButton button : filters) button.fade(in);

        hovered = -1;
        if (mouseX >= listX + PAD && mouseX <= listX + listW - PAD
            && mouseY >= listY + PAD && mouseY <= listY + listH - PAD) {
            int index = (int) ((mouseY - listY - PAD + listScroll) / ROW);
            if (index >= 0 && index < shown.size()) hovered = index;
        }

        super.render(graphics, mouseX, mouseY, delta);

        // header
        var titlePose = graphics.pose();
        titlePose.pushPose();
        titlePose.translate(MARGIN + 2, MARGIN + 1 - slide / 2f, 0);
        titlePose.scale(1.5f, 1.5f, 1f);
        graphics.drawString(font, title, 1, 1, withAlpha(0x000000, 0.55f * in), false);
        graphics.drawString(font, title, 0, 0, withAlpha(RGB_ACCENT, in), false);
        titlePose.popPose();
        int underline = (int) ((listW + 40) * in);
        graphics.fillGradient(MARGIN + 2, MARGIN + 17, MARGIN + 2 + underline, MARGIN + 18,
            withAlpha(RGB_ACCENT, 0.75f * in), withAlpha(RGB_ACCENT, 0.05f * in));

        List<ClientClasses.LayerInfo> all = ClientClasses.layers();
        if (all.size() > 1 && !readOnly) {
            Component step = Component.translatable("gui.yggdrasil_origins.picker.step", layerIndex() + 1, all.size());
            graphics.drawString(font, step, width - MARGIN - font.width(step), MARGIN + 4,
                withAlpha(RGB_DIM, in), false);
        }
        // panels
        panel(graphics, listX - slide, listY, listW, listH, in);
        renderList(graphics, -slide, in);
        panel(graphics, detailX + slide, detailY, detailW, detailH, in);
        renderDetails(graphics, slide, in);

        if (portraitW > 0) {
            panel(graphics, portraitX + slide, portraitY, portraitW, portraitH, in);
            renderPortrait(graphics, slide, in, mouseX, mouseY);
        }

        int dividerY = height - MARGIN - 30;
        graphics.fillGradient(MARGIN, dividerY, width - MARGIN, dividerY + 1,
            withAlpha(RGB_EDGE, 0.9f * in), withAlpha(RGB_EDGE, 0.1f * in));
        Component hint = Component.translatable(readOnly
            ? "gui.yggdrasil_origins.picker.hint_mine"
            : "gui.yggdrasil_origins.picker.hint");
        graphics.drawString(font, hint, MARGIN + 2, dividerY - 12, withAlpha(RGB_DIM, 0.75f * in), false);

        finishClosing(graphics);
    }

    /**
     * The portrait look: everything already chosen in other layers, plus the current highlight.
     * Otherwise the portrait would lose its race on the path screen.
     */
    private org.origins_yggdrasil.yggdrasil_origins.visual.VisualProfile previewProfile(
            ClientClasses.ClassInfo info) {
        var profile = org.origins_yggdrasil.yggdrasil_origins.visual.VisualProfile.NONE;
        for (ClientClasses.LayerInfo other : ClientClasses.layers()) {
            if (other.id().equals(layer.id())) continue;
            var chosenId = ClientClasses.chosen(other.id());
            if (chosenId.isEmpty()) continue;
            for (ClientClasses.ClassInfo candidate : other.classes()) {
                if (candidate.id().equals(chosenId.get())) {
                    profile = profile.merge(candidate.visual());
                    break;
                }
            }
        }
        return profile.merge(info.visual());
    }

    /**
     * A stand-in for the portrait: the player's own skin and body, but no armour and nothing in
     * the hands. Armour hides the very thing the picker is there to show - the class looks - and
     * whatever the player happens to be carrying has nothing to do with the choice.
     */
    private net.minecraft.world.entity.LivingEntity portraitStand() {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return null;
        if (stand == null || stand.level() != minecraft.level) {
            // the stand shares the player's profile (that is where the skin comes from), so it
            // would also read their game mode off the tab list - and a spectator is drawn as a
            // floating head with no class parts at all. The portrait is never a spectator.
            stand = new net.minecraft.client.player.RemotePlayer(
                    minecraft.level, minecraft.player.getGameProfile()) {
                @Override
                public boolean isSpectator() {
                    return false;
                }

                // a fresh entity shows no outer skin layer (hat, jacket, sleeves) - take the
                // player's own choice, so the portrait wears the skin they actually have
                @Override
                public boolean isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart part) {
                    var self = minecraft.player;
                    return self != null && self.isModelPartShown(part);
                }
            };
        }
        // the portrait rotation was tuned against the real player, so the stand copies its
        // bearing exactly and only the equipment differs
        var self = minecraft.player;
        stand.setPos(self.getX(), self.getY(), self.getZ());
        // head and body face the same way: copying both from the player kept whatever twist
        // they had between them, and the portrait looked over its shoulder
        // 180 is "facing the viewer" in an inventory render, as vanilla's own preview uses
        stand.setYRot(180.0F);
        stand.yRotO = 180.0F;
        stand.yHeadRot = 180.0F;
        stand.yHeadRotO = 180.0F;
        stand.yBodyRot = 180.0F;
        stand.yBodyRotO = 180.0F;
        // it never ticks, so it would count as airborne forever - and spread its wings
        stand.setOnGround(true);
        // always upright: the portrait must not copy a swim, a crawl or a glide
        stand.setPose(net.minecraft.world.entity.Pose.STANDING);
        stand.setXRot(0.0F);
        stand.xRotO = 0.0F;
        stand.walkAnimation.setSpeed(0.0F);
        return stand;
    }

    /** A live portrait: the player wearing the chosen class looks, turning slowly. */
    private void renderPortrait(GuiGraphics graphics, int dx, float in, int mouseX, int mouseY) {
        ClientClasses.ClassInfo info = selected();
        var player = portraitStand();
        if (info == null || player == null) return;

        int x0 = portraitX + dx + PAD;
        int x1 = portraitX + dx + portraitW - PAD;
        int y0 = portraitY + PAD;
        int y1 = portraitY + portraitH - PAD;

        // the figure must fit entirely: measured from the real space above the label,
        // because classes differ in height (a half-giant is a tenth taller than average)
        int centerX = (x0 + x1) / 2;
        int bottomY = y0 + (int) ((y1 - y0) * 0.80f);
        // 3.2 is a measured factor: the figure spans about that many "size" units,
        // accounting for the tallest class and the room under the label
        int size = Math.max(18, Math.min((int) ((x1 - x0) * 0.62f), (int) ((bottomY - y0) / 3.2f)));
        // on a class change the figure briefly "breathes" so the eye catches the swap
        size = Math.max(12, (int) (size * (1f - 0.12f * portraitPop * portraitPop)));

        graphics.enableScissor(x0, y0, x1, y1);
        // a soft backlight so the silhouette reads
        graphics.fill(x0, y0, x1, y1, withAlpha(RGB_ACCENT2, 0.04f * in));

        ClientVisuals.preview(previewProfile(info));
        try {
            float spin = portraitSpin;
            // the camera looks down a little, then the figure spins under it: the tilt goes
            // first in the chain so it stays the viewer's. The other way round, the tilt spun
            // with the body and read as a sideways lean whenever the figure was side-on.
            org.joml.Quaternionf spinQ = new org.joml.Quaternionf()
                .rotateX((float) Math.toRadians(8.0))
                .rotateY((float) Math.toRadians(spin));
            org.joml.Quaternionf tilt = new org.joml.Quaternionf().rotateZ((float) Math.PI);
            tilt.mul(spinQ);
            // the stand is not the camera owner, so the game would hang its name over the
            // portrait; names are off for the length of this one call
            boolean names = minecraft.options.hideGui;
            minecraft.options.hideGui = true;
            try {
                net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(
                    graphics, centerX, bottomY, size,
                    new org.joml.Vector3f(), tilt, null, player);
            } finally {
                minecraft.options.hideGui = names;
            }
        } finally {
            ClientVisuals.preview(null);
        }
        graphics.disableScissor();

        Component label = info.name();
        int labelX = centerX - font.width(label) / 2;
        graphics.drawString(font, label, labelX, y1 - 10, withAlpha(RGB_ACCENT, in), false);
    }

    private void renderList(GuiGraphics graphics, int dx, float in) {
        int visible = visibleRows();
        listScrollTarget = Mth.clamp(listScrollTarget, 0, Math.max(0, shown.size() - visible));
        boolean scrolls = shown.size() > visible;

        int top = listY + PAD;
        int bottom = listY + listH - PAD;
        int x0 = listX + dx + PAD;
        int x1 = listX + dx + listW - PAD - (scrolls ? SCROLL_W + SCROLL_GAP : 0);

        long sinceList = Util.getMillis() - listChangedAt;
        graphics.enableScissor(x0, top, x1, bottom);
        for (int index = 0; index < shown.size(); index++) {
            int rowY = (int) (top + index * ROW - listScroll);
            if (rowY + ROW < top || rowY > bottom) continue;
            // after a search or filter the rows appear top to bottom instead of all at once
            float rowIn = in * Mth.clamp((sinceList - index * 14L) / 110f, 0f, 1f);
            if (rowIn <= 0.01f) continue;

            ClientClasses.ClassInfo info = shown.get(index);
            boolean isPicked = index == picked;
            boolean isHovered = index == hovered;
            int rowBottom = rowY + ROW - 3;
            int tint = IMPACT_TINTS[Mth.clamp(info.impact(), 0, 3)];

            rounded(graphics, x0, rowY, x1, rowBottom,
                withAlpha(tint, (isPicked ? 0.13f : isHovered ? 0.10f : 0.06f) * rowIn));
            if (isHovered && !isPicked) {
                rounded(graphics, x0, rowY, x1, rowBottom, withAlpha(0xFFFFFF, 0.09f * hoverAlpha * rowIn));
            } else if (!isPicked && index > 0) {
                graphics.fill(x0 + 8, rowY - 1, x1 - 8, rowY, withAlpha(RGB_EDGE, 0.45f * rowIn));
            }

            // the item icon slot: 18x18, exactly centred in the row height
            int slotX = x0 + 5;
            int slotY = rowY + 3;
            rounded(graphics, slotX, slotY, slotX + 18, slotY + 18, withAlpha(0x000000, 0.35f * rowIn));
            float hoverLift = isHovered && !isPicked ? hoverAlpha * 1.5f : 0f;
            float scale = isPicked
                ? 0.92f + Mth.sin((Util.getMillis() % 2400L) / 2400f * (float) Math.PI * 2f) * 0.03f
                : 0.8f + hoverLift * 0.04f;
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(slotX + 9f + hoverLift, slotY + 9f, 0f);
            pose.scale(scale, scale, 1f);
            pose.translate(-8f, -8f, 0f);
            graphics.renderItem(info.icon(), 0, 0);
            pose.popPose();

            int iconX = x1 - 4 - 12;
            int nameX = slotX + 18 + 6;
            int nameMax = Math.max(10, iconX - 5 - nameX);
            String name = info.name().getString();
            if (font.width(name) > nameMax) name = font.plainSubstrByWidth(name, nameMax - 6) + "…";
            graphics.drawString(font, name, nameX + (int) hoverLift, rowY + 9,
                withAlpha(isPicked ? RGB_ACCENT : RGB_TEXT, rowIn), false);
            drawImpact(graphics, info.impact(), iconX, rowY + 6, 12, (isPicked ? 1f : 0.8f) * rowIn);
        }
        // the selection frame glides between rows instead of hopping
        if (!shown.isEmpty()) {
            int glideY = (int) (top + selectionGlide * ROW - listScroll);
            int glideBottom = glideY + ROW - 3;
            float pulse = 0.5f + 0.5f * Mth.sin((Util.getMillis() % 2400L) / 2400f * (float) Math.PI * 2f);
            rounded(graphics, x0, glideY, x1, glideBottom,
                withAlpha(RGB_ACCENT, (0.14f + 0.05f * pulse) * in));
            frame(graphics, x0, glideY, x1, glideBottom,
                withAlpha(RGB_ACCENT, (0.35f + 0.2f * pulse) * in));
            graphics.fill(x0 + 1, glideY + 4, x0 + 3, glideBottom - 4, withAlpha(RGB_ACCENT, in));
        }
        graphics.disableScissor();

        if (scrolls) {
            int track = bottom - top;
            int barH = Math.max(16, track * visible / Math.max(1, shown.size()));
            float ratio = listScroll / Math.max(1f, (shown.size() - visible) * (float) ROW);
            int barY = top + (int) ((track - barH) * Mth.clamp(ratio, 0f, 1f));
            scrollbar(graphics, listX + dx + listW - PAD - SCROLL_W, top, track, barY, barH, in);
        }
    }

    /** Two opposing arrows - the icon of the "Random" button. */
    private static void shuffleIcon(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y + 1, x + 7, y + 2, color);
        graphics.fill(x + 5, y, x + 6, y + 1, color);
        graphics.fill(x + 6, y + 1, x + 7, y + 3, color);
        graphics.fill(x + 5, y + 2, x + 6, y + 3, color);

        graphics.fill(x + 2, y + 6, x + 9, y + 7, color);
        graphics.fill(x + 3, y + 5, x + 4, y + 6, color);
        graphics.fill(x + 2, y + 5, x + 3, y + 7, color);
        graphics.fill(x + 3, y + 7, x + 4, y + 8, color);
    }

    /** Impact icon: silver skull -> gold -> crimson. Zero impact has no icon. */
    private void drawImpact(GuiGraphics graphics, int impact, int x, int y, int size, float in) {
        ResourceLocation icon = IMPACT_ICONS[Mth.clamp(impact, 0, 3)];
        float alpha = Mth.clamp(in, 0f, 1f);
        if (icon == null || alpha <= 0.01f) return;
        // without blending enabled the alpha in setColor does nothing: the icon used to be
        // drawn at full brightness and hung in place while the rest of the row faded
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1f, 1f, 1f, alpha);
        graphics.blit(icon, x, y, size, size, 0f, 0f, 16, 16, 16, 16);
        graphics.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private Component impactLabel(int impact) {
        return Component.translatable("gui.yggdrasil_origins.picker.impact." + Mth.clamp(impact, 0, 3));
    }

    private record Row(Component text, FormattedCharSequence seq, boolean impact, int rgb, int height) { }

    static int senseColour(int sense) {
        return sense > 0 ? RGB_GAIN : sense < 0 ? RGB_COST : RGB_MIXED;
    }

    /** A sign in front of the name, so the colour is not the only thing carrying the meaning. */
    static String senseMark(int sense) {
        return sense > 0 ? "+" : sense < 0 ? "-" : "~";
    }

    /** The ability text sits a shade quieter than its heading. */
    private static int dim(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return ((r * 3 / 4) << 16) | ((g * 3 / 4) << 8) | (b * 3 / 4);
    }

    /**
     * A "gives"/"pays" paragraph: its heading, then the text, both in the colour of the verdict.
     * A translation that was left empty is skipped rather than drawn as a bare key.
     */
    private void block(List<Row> rows, Component body, String headingKey, int rgb, int innerW) {
        String text = body.getString();
        if (text.isEmpty() || text.startsWith("origin.")) return;
        rows.add(new Row(null, null, false, 0, 5));
        rows.add(new Row(Component.translatable(headingKey), null, false, rgb, 11));
        for (FormattedCharSequence line : font.split(body, innerW - 6)) {
            rows.add(new Row(null, line, false, dim(rgb), 10));
        }
    }

    private void renderDetails(GuiGraphics graphics, int dx, float in) {
        ClientClasses.ClassInfo info = selected();
        int top = detailY + PAD;
        int bottom = detailY + detailH - PAD;
        int x0 = detailX + dx + PAD;
        // the scrollbar has its own track - the content never runs into it
        int x1 = detailX + dx + detailW - PAD - SCROLL_W - SCROLL_GAP;
        int innerX = x0 + 6;
        int innerW = x1 - innerX;
        float fade = detailsFade() * in;

        if (info == null) {
            graphics.drawString(font, Component.translatable("gui.yggdrasil_origins.picker.empty"),
                innerX, top + 4, withAlpha(RGB_DIM, in), false);
            return;
        }

        List<Row> rows = new ArrayList<>();
        rows.add(new Row(info.name().copy().withStyle(ChatFormatting.BOLD), null, false, RGB_ACCENT, 13));
        rows.add(new Row(Component.translatable("gui.yggdrasil_origins.picker.impact"), null, true, RGB_DIM, 17));
        for (FormattedCharSequence line : font.split(info.description(), innerW)) {
            rows.add(new Row(null, line, false, RGB_TEXT, 10));
        }
        // what the origin hands over and what it takes, each in its own colour
        block(rows, info.gives(), "gui.yggdrasil_origins.picker.gives", RGB_GAIN, innerW);
        block(rows, info.pays(), "gui.yggdrasil_origins.picker.pays", RGB_COST, innerW);
        // "no path" has no abilities - an empty heading would look like an oversight
        if (!info.powers().isEmpty()) {
            rows.add(new Row(null, null, false, 0, 7));
            rows.add(new Row(Component.translatable("gui.yggdrasil_origins.picker.powers"),
                null, false, RGB_ACCENT2, 13));
        }
        // what the origin gives first, then the two-sided ones, then what it costs - a reader
        // should not have to hop between colours to find the upside
        List<ClientClasses.PowerInfo> ordered = new ArrayList<>(info.powers());
        ordered.sort(java.util.Comparator.comparingInt(
            (ClientClasses.PowerInfo power) -> power.sense()).reversed());
        for (ClientClasses.PowerInfo power : ordered) {
            int rgb = senseColour(power.sense());
            rows.add(new Row(Component.literal(senseMark(power.sense()) + " ").append(power.name()),
                null, false, rgb, 11));
            for (FormattedCharSequence line : font.split(power.description(), innerW - 10)) {
                rows.add(new Row(null, line, false, dim(rgb), 10));
            }
            rows.add(new Row(null, null, false, 0, 4));
        }

        int total = 0;
        for (Row row : rows) total += row.height();
        int track = bottom - top;
        int maxScroll = Math.max(0, total - track);
        detailScrollTarget = Mth.clamp(detailScrollTarget, 0, maxScroll);

        graphics.enableScissor(x0, top, x1, bottom);
        int y = (int) (top - detailScroll);
        int index = 0;
        long sinceSelect = Util.getMillis() - selectedAt;
        for (Row row : rows) {
            if (y + row.height() > top && y < bottom) {
                // the name is always visible, the rest appears in order from the top
                float stagger = Mth.clamp((sinceSelect - index * 18L) / 140f, 0f, 1f);
                float alpha = row.rgb() == RGB_ACCENT ? in : fade * stagger;
                if (row.impact()) {
                    graphics.drawString(font, row.text(), innerX, y + 3, withAlpha(row.rgb(), alpha), false);
                    int iconX = innerX + font.width(row.text()) + 5;
                    drawImpact(graphics, info.impact(), iconX, y, 14, alpha);
                    graphics.drawString(font, impactLabel(info.impact()),
                        iconX + (info.impact() > 0 ? 18 : 0), y + 3,
                        withAlpha(IMPACT_TINTS[Mth.clamp(info.impact(), 0, 3)], alpha), false);
                } else if (row.text() != null) {
                    if (row.rgb() == RGB_ACCENT2) {
                        graphics.fill(x0 + 1, y - 1, x0 + 3, y + 9, withAlpha(RGB_ACCENT2, 0.8f * alpha));
                    }
                    graphics.drawString(font, row.text(), innerX, y, withAlpha(row.rgb(), alpha), false);
                } else if (row.seq() != null) {
                    graphics.drawString(font, row.seq(), innerX, y, withAlpha(row.rgb(), alpha), false);
                }
            }
            y += row.height();
            index++;
        }
        if (maxScroll > 0) {
            graphics.fillGradient(x0, bottom - 7, x1, bottom,
                withAlpha(0x000000, 0f), withAlpha(0x05060C, 0.6f * in));
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int barH = Math.max(16, track * track / Math.max(1, total));
            int barY = top + (int) ((track - barH) * (detailScroll / (float) maxScroll));
            scrollbar(graphics, detailX + dx + detailW - PAD - SCROLL_W, top, track, barY, barH, in);
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= listX + PAD && mouseX <= listX + listW - PAD
            && mouseY >= listY + PAD && mouseY <= listY + listH - PAD) {
            int index = (int) ((mouseY - listY - PAD + listScroll) / ROW);
            if (index >= 0 && index < shown.size()) {
                long now = Util.getMillis();
                boolean doubleClick = index == lastClickIndex && now - lastClickTime < 350L;
                select(index);
                lastClickIndex = index;
                lastClickTime = now;
                if (doubleClick) choose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scrolledAt = Util.getMillis();
        if (mouseX >= detailX && mouseX <= detailX + detailW) {
            detailScrollTarget -= (int) (vertical * 18);
            return true;
        }
        listScrollTarget -= (int) Math.signum(vertical);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_DOWN -> { move(1); return true; }
            case GLFW.GLFW_KEY_UP -> { move(-1); return true; }
            case GLFW.GLFW_KEY_PAGE_DOWN -> { move(5); return true; }
            case GLFW.GLFW_KEY_PAGE_UP -> { move(-5); return true; }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { choose(); return true; }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (back != null && (search == null || search.getValue().isEmpty())) {
                    goBack();
                    return true;
                }
            }
            default -> { }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void select(int index) {
        int previous = picked;
        picked = Mth.clamp(index, 0, Math.max(0, shown.size() - 1));
        if (picked != previous) portraitPop = 1f;
        detailScrollTarget = 0;
        detailScroll = 0f;
        selectedAt = Util.getMillis();
        updateButtons();
    }

    private void move(int delta) {
        if (shown.isEmpty()) return;
        select(picked + delta);
        int visible = visibleRows();
        if (picked < listScrollTarget) listScrollTarget = picked;
        if (picked >= listScrollTarget + visible) listScrollTarget = picked - visible + 1;
    }

    /**
     * Escape always closes, even on a layer that has to be chosen eventually.
     * <p>
     * A screen that cannot be dismissed is a trap: someone who logs in to answer a question,
     * or who simply wants to look at the world first, has no way out of it. The choice is
     * not lost - it is only postponed, and the server offers it again on the next respawn,
     * reload or class orb.
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        if (!readOnly && layer.required() && minecraft != null && minecraft.player != null) {
            // left without picking: say how to come back rather than leave them wondering
            minecraft.player.displayClientMessage(
                Component.translatable("gui.yggdrasil_origins.picker.later"), false);
        }
        super.onClose();
    }
}
