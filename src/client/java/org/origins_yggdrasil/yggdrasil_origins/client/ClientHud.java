package org.origins_yggdrasil.yggdrasil_origins.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Ability bars above the hotbar: cooldowns and resources (stacks).
 * The server sends only changes; progress is filled in from world time.
 */
public final class ClientHud {

    private static final Gson GSON = new Gson();

    /** kind: cooldown - until/length in world ticks; resource - value within min..max. */
    public record Bar(ResourceLocation id, String kind, long until, int length,
                      int value, int min, int max) {

        public Component label() {
            return Component.translatable("power." + id.getNamespace() + "." + id.getPath() + ".name");
        }
    }

    private static final List<Bar> BARS = new ArrayList<>();
    /** Conditional powers the server says are in force right now. */
    private static final List<ResourceLocation> MARKS = new ArrayList<>();

    private ClientHud() { }

    public static void accept(String json) {
        try {
            parse(json);
        } catch (Exception e) {
            org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins.LOGGER.error(
                "Broken packet from server, keeping previous data: {}", e.toString());
        }
    }

    private static void parse(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        if (root == null || !root.has("bars")) return;
        List<Bar> parsed = new ArrayList<>();
        for (var element : root.getAsJsonArray("bars")) {
            JsonObject bar = element.getAsJsonObject();
            parsed.add(new Bar(
                ResourceLocation.parse(bar.get("id").getAsString()),
                bar.get("kind").getAsString(),
                bar.has("until") ? bar.get("until").getAsLong() : 0L,
                bar.has("length") ? bar.get("length").getAsInt() : 0,
                bar.has("value") ? bar.get("value").getAsInt() : 0,
                bar.has("min") ? bar.get("min").getAsInt() : 0,
                bar.has("max") ? bar.get("max").getAsInt() : 1));
        }
        BARS.clear();
        BARS.addAll(parsed);
        MARKS.clear();
        if (root.has("marks")) {
            for (var element : root.getAsJsonArray("marks")) {
                MARKS.add(ResourceLocation.parse(element.getAsString()));
            }
        }
    }

    public static void clear() {
        BARS.clear();
        MARKS.clear();
        ANIMS.clear();
        MARK_ANIMS.clear();
    }

    private static int alpha(int rgb, float a) {
        return (Mth.clamp((int) (a * 255f), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** Animation state of a bar: client-only, disappears together with the bar. */
    private static final class Anim {
        private float alpha;
        private float fill = -1f;
        private float flash;
        private boolean seen;
    }

    private static final java.util.Map<ResourceLocation, Anim> ANIMS = new java.util.HashMap<>();
    private static final java.util.Map<ResourceLocation, Anim> MARK_ANIMS = new java.util.HashMap<>();
    private static final float FADE_PER_TICK = 0.14f;

    public static void render(GuiGraphics graphics, float delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        if (!org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins.config().enableHud
            || minecraft.options.hideGui || minecraft.player.isSpectator()) {
            ANIMS.clear();
            return;
        }

        long now = minecraft.level.getGameTime();
        List<Bar> visible = new ArrayList<>();
        for (Bar bar : BARS) {
            // a ready cooldown and an empty resource do not clutter the screen
            if (bar.kind().equals("cooldown") && now >= bar.until()) continue;
            if (bar.kind().equals("resource") && bar.value() <= bar.min()) continue;
            visible.add(bar);
        }

        // the ones that vanished keep fading for a moment instead of popping out
        for (Anim anim : ANIMS.values()) anim.seen = false;
        for (Bar bar : visible) ANIMS.computeIfAbsent(bar.id(), id -> new Anim()).seen = true;
        List<Bar> drawn = new ArrayList<>(visible);
        for (var entry : ANIMS.entrySet()) {
            Anim anim = entry.getValue();
            if (anim.seen) continue;
            anim.alpha = Math.max(0f, anim.alpha - FADE_PER_TICK * delta);
            if (anim.alpha > 0.01f) {
                Bar fading = fading(entry.getKey());
                if (fading != null) drawn.add(fading);
            }
        }
        ANIMS.entrySet().removeIf(entry -> !entry.getValue().seen && entry.getValue().alpha <= 0.01f);
        if (drawn.isEmpty()) return;

        int width = 76;
        int height = 5;
        int row = height + 12;
        int x = graphics.guiWidth() / 2 - 91 - width - 8;
        if (x < 4) x = 4;
        int y = graphics.guiHeight() - 36 - (drawn.size() - 1) * row;

        for (Bar bar : drawn) {
            Anim anim = ANIMS.get(bar.id());
            if (anim == null) continue;
            if (anim.seen) anim.alpha = Math.min(1f, anim.alpha + FADE_PER_TICK * delta);
            float in = easeOut(anim.alpha);

            float target;
            int color;
            if (bar.kind().equals("cooldown")) {
                long left = bar.until() - now;
                target = bar.length() <= 0 ? 1f : 1f - Mth.clamp(left / (float) bar.length(), 0f, 1f);
                color = 0x7FD1FF;
            } else {
                int span = Math.max(1, bar.max() - bar.min());
                target = Mth.clamp((bar.value() - bar.min()) / (float) span, 0f, 1f);
                color = 0xFFD479;
            }
            // a cooldown crawls on its own while a resource jumps, so only the resource is smoothed
            if (anim.fill < 0f) {
                anim.fill = target;
            } else if (bar.kind().equals("resource")) {
                anim.fill += (target - anim.fill) * Math.min(1f, 0.25f * delta);
                if (Math.abs(target - anim.fill) < 0.002f) anim.fill = target;
            } else {
                anim.fill = target;
            }
            float fill = anim.fill;
            // a nearly ready cooldown flashes so it is caught by the corner of the eye
            if (bar.kind().equals("cooldown") && target > 0.985f) anim.flash = 1f;
            anim.flash = Math.max(0f, anim.flash - 0.06f * delta);

            String label = bar.label().getString();
            String right = bar.kind().equals("cooldown")
                ? Component.translatable("hud.yggdrasil_origins.seconds",
                    Math.max(1, (int) Math.ceil((bar.until() - now) / 20.0))).getString()
                : bar.value() + "/" + bar.max();
            int rightWidth = minecraft.font.width(right);
            int labelMax = width - rightWidth - 4;
            if (minecraft.font.width(label) > labelMax) {
                label = minecraft.font.plainSubstrByWidth(label, Math.max(6, labelMax - 4)) + "…";
            }
            int textAlpha = (int) (0.92f * in * 255f);
            graphics.drawString(minecraft.font, label, x, y - 10, (textAlpha << 24) | 0xC8CEDE, true);
            graphics.drawString(minecraft.font, right, x + width - rightWidth, y - 10,
                (textAlpha << 24) | (bar.kind().equals("cooldown") ? 0x7FD1FF : 0xFFD479), true);

            graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, alpha(0x05060C, 0.65f * in));
            graphics.fill(x, y, x + width, y + height, alpha(0x2E3344, 0.9f * in));
            int filled = (int) (width * fill);
            graphics.fill(x, y, x + filled, y + height, alpha(color, 0.95f * in));
            // a lighter top edge on the fill stops the bar from being a flat blob
            if (filled > 0) {
                graphics.fill(x, y, x + filled, y + 1, alpha(lighten(color), 0.85f * in));
            }
            if (anim.flash > 0.01f) {
                graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1,
                    alpha(0xFFFFFF, 0.45f * anim.flash * in));
            }

            if (bar.kind().equals("resource")) {
                int span = Math.max(1, bar.max() - bar.min());
                // ticks only make sense while the eye can still count them
                if (span <= 12) {
                    for (int i = 1; i < span; i++) {
                        int tick = x + width * i / span;
                        graphics.fill(tick, y, tick + 1, y + height, alpha(0x05060C, 0.8f * in));
                    }
                }
            }
            y += row;
        }
    }

    /** A bar no longer present in the data but still fading out. */
    private static Bar fading(ResourceLocation id) {
        for (Bar bar : BARS) {
            if (bar.id().equals(id)) {
                return bar.kind().equals("cooldown")
                    ? new Bar(id, "cooldown", bar.until(), bar.length(), 0, 0, 1)
                    : new Bar(id, "resource", 0L, 0, bar.min(), bar.min(), bar.max());
            }
        }
        return null;
    }

    private static float easeOut(float t) {
        float clamped = Mth.clamp(t, 0f, 1f);
        return 1f - (1f - clamped) * (1f - clamped);
    }

    private static int lighten(int rgb) {
        int r = Math.min(255, ((rgb >> 16) & 0xFF) + 60);
        int g = Math.min(255, ((rgb >> 8) & 0xFF) + 60);
        int b = Math.min(255, (rgb & 0xFF) + 60);
        return (r << 16) | (g << 8) | b;
    }
}
