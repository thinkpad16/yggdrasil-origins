package org.origins_yggdrasil.yggdrasil_origins.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Player looks on the client: sent by the server, used by both the renderer and the hitbox. */
public final class ClientVisuals {

    private static final Gson GSON = new Gson();
    private static final Map<UUID, VisualProfile> PROFILES = new HashMap<>();
    private static final Map<UUID, org.origins_yggdrasil.yggdrasil_origins.visual.VisualStyle>
        STYLES = new HashMap<>();
    /** Set while the picker previews a look that is not on anybody yet. */
    private static org.origins_yggdrasil.yggdrasil_origins.visual.VisualStyle previewStyle;

    private ClientVisuals() { }

    /**
     * Profile for **drawing**: honours the player's personal switch.
     */
    /** Profile temporarily substituted for the player to draw the picker portrait. */
    private static VisualProfile preview;

    public static void preview(VisualProfile profile) {
        preview = profile;
    }

    /** The customisation on this player - colours, sizes, shapes they chose. */
    public static org.origins_yggdrasil.yggdrasil_origins.visual.VisualStyle styleOf(UUID player) {
        if (previewStyle != null) return previewStyle;
        return STYLES.getOrDefault(player,
            org.origins_yggdrasil.yggdrasil_origins.visual.VisualStyle.EMPTY);
    }

    public static void previewStyle(org.origins_yggdrasil.yggdrasil_origins.visual.VisualStyle style) {
        previewStyle = style;
    }

    public static VisualProfile of(UUID player) {
        if (preview != null) return preview;
        if (!YggdrasilOrigins.config().showVisuals) return VisualProfile.NONE;
        return raw(player);
    }

    /**
     * The profile as-is, ignoring the personal switch - used for the hitbox.
     * If the switch applied here too, the client would size the player differently from
     * the server and movement would start rubber-banding.
     */
    public static VisualProfile raw(UUID player) {
        VisualProfile profile = PROFILES.get(player);
        return profile == null ? VisualProfile.NONE : profile;
    }

    public static void accept(String json) {
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null || !root.has("players")) return;
            for (var element : root.getAsJsonArray("players")) {
                JsonObject entry = element.getAsJsonObject();
                UUID id = UUID.fromString(entry.get("id").getAsString());
                VisualProfile profile = VisualProfile.fromJson(entry);
                PROFILES.put(id, profile);
                STYLES.put(id, entry.has("s")
                    ? org.origins_yggdrasil.yggdrasil_origins.visual.VisualStyle
                        .fromJson(entry.getAsJsonObject("s"))
                    : org.origins_yggdrasil.yggdrasil_origins.visual.VisualStyle.EMPTY);
                applyToEntity(id, profile);
            }
        } catch (Exception e) {
            YggdrasilOrigins.LOGGER.error("Broken visuals packet: {}", e.toString());
        }
    }

    /** The player is already in the world - refresh dimensions so the hitbox matches the server. */
    private static void applyToEntity(UUID id, VisualProfile profile) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return;
        var player = level.getPlayerByUUID(id);
        if (player != null) {
            org.origins_yggdrasil.yggdrasil_origins.visual.VisualSource.apply(player, profile);
        }
    }

    /** The player left the world - the profile is no longer needed. */
    public static void forget(UUID id) {
        PROFILES.remove(id);
        STYLES.remove(id);
    }

    public static void clear() {
        PROFILES.clear();
        STYLES.clear();
        preview = null;
        previewStyle = null;
    }
}
