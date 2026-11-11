package org.origins_yggdrasil.yggdrasil_origins.visual;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;
import org.origins_yggdrasil.yggdrasil_origins.clazz.ClassManager;
import org.origins_yggdrasil.yggdrasil_origins.clazz.ClassRegistry;
import org.origins_yggdrasil.yggdrasil_origins.net.YggPackets;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Player looks on the server: keeps everyone's profile and sends it out,
 * so that others see the right proportions and parts.
 * <p>
 * The hitbox is computed from the same profile (see {@code PlayerDimensionsMixin}),
 * otherwise the model and the collision box would drift apart.
 */
public final class Visuals {

    private static final Map<UUID, VisualProfile> PROFILES = new HashMap<>();
    private static final Map<UUID, VisualStyle> STYLES = new HashMap<>();

    private Visuals() { }

    public static boolean enabled() {
        return YggdrasilOrigins.config().enableVisuals;
    }

    public static VisualProfile of(UUID player) {
        VisualProfile profile = PROFILES.get(player);
        return profile == null ? VisualProfile.NONE : profile;
    }

    /** Recomputes a player profile from their choice and broadcasts it if it changed. */
    public static void refresh(ServerPlayer player) {
        VisualProfile profile = VisualProfile.NONE;
        if (enabled()) {
            for (String classId : ClassManager.chosen(player).values()) {
                ResourceLocation id = ResourceLocation.tryParse(classId);
                if (id == null) continue;
                VisualProfile part = ClassRegistry.INSTANCE.def(id)
                    .map(ClassRegistry.Def::visual)
                    .orElse(VisualProfile.NONE);
                profile = profile.merge(part);
            }
        }

        STYLES.put(player.getUUID(), StyleManager.read(player));
        VisualProfile previous = PROFILES.put(player.getUUID(), profile);
        VisualSource.apply(player, profile);
        if (previous != null && previous.equals(profile)) return;
        broadcast(player, profile);
    }

    /**
     * The profile is sent to whoever started seeing the player.
     * <p>
     * Here rather than "everyone at once": on a big server there is no point pushing a
     * hundred profiles to people who cannot see those players. The client in turn drops a
     * profile when the player leaves its view and receives it again on the next sighting.
     */
    public static void registerTracking() {
        net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents.START_TRACKING.register(
            (tracked, watcher) -> {
                if (!(tracked instanceof ServerPlayer seen)) return;
                VisualProfile profile = of(seen.getUUID());
                if (profile.isDefault() && profile.tint() == 0xFFFFFF) return;
                sendOne(watcher, seen.getUUID(), profile);
            });
    }

    private static void sendOne(ServerPlayer target, UUID uuid, VisualProfile profile) {
        if (target.connection == null) return;
        if (!ServerPlayNetworking.canSend(target, YggPackets.Visuals.TYPE)) return;
        JsonArray array = new JsonArray();
        array.add(entry(uuid, profile));
        JsonObject root = new JsonObject();
        root.add("players", array);
        ServerPlayNetworking.send(target, new YggPackets.Visuals(root.toString()));
    }

    /** On join a player gets their own profile (they do not "track" themselves). */
    public static void sendSelf(ServerPlayer target) {
        sendOne(target, target.getUUID(), of(target.getUUID()));
    }

    /** Origin changed - the packet goes only to those who see this player, plus the player. */
    private static void broadcast(ServerPlayer player, VisualProfile profile) {
        JsonArray array = new JsonArray();
        array.add(entry(player.getUUID(), profile));
        JsonObject root = new JsonObject();
        root.add("players", array);
        String json = root.toString();

        for (ServerPlayer watcher : net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(player)) {
            if (watcher.connection == null) continue;
            if (!ServerPlayNetworking.canSend(watcher, YggPackets.Visuals.TYPE)) continue;
            ServerPlayNetworking.send(watcher, new YggPackets.Visuals(json));
        }
        if (player.connection != null && ServerPlayNetworking.canSend(player, YggPackets.Visuals.TYPE)) {
            ServerPlayNetworking.send(player, new YggPackets.Visuals(json));
        }
    }

    private static JsonObject entry(UUID uuid, VisualProfile profile) {
        JsonObject entry = profile.toJson();
        entry.addProperty("id", uuid.toString());
        VisualStyle style = STYLES.get(uuid);
        if (style != null && !style.isEmpty()) entry.add("s", style.toJson());
        return entry;
    }

    /** The player's own customisation - colours, sizes and shapes they picked themselves. */
    public static VisualStyle styleOf(UUID player) {
        return STYLES.getOrDefault(player, VisualStyle.EMPTY);
    }

    public static void setStyle(ServerPlayer player, VisualStyle style) {
        STYLES.put(player.getUUID(), style == null ? VisualStyle.EMPTY : style);
        broadcast(player, of(player.getUUID()));
    }

    public static void forget(UUID uuid) {
        PROFILES.remove(uuid);
        STYLES.remove(uuid);
    }

    public static void clear() {
        PROFILES.clear();
        STYLES.clear();
    }
}
