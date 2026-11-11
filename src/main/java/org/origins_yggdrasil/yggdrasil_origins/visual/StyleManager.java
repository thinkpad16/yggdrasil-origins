package org.origins_yggdrasil.yggdrasil_origins.visual;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;

import java.util.Map;

/**
 * Where a player's own customisation lives. Separate from the origin choice on purpose: an
 * admin may rewrite every origin in the pack without wiping what players did to their horns.
 */
public final class StyleManager {

    public static final AttachmentType<Map<String, String>> STYLE = AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "style"),
        builder -> builder
            .persistent(Codec.unboundedMap(Codec.STRING, Codec.STRING))
            .copyOnDeath());

    private StyleManager() { }

    /** Loads the class so the attachment type is registered before the first player joins. */
    public static void touch() {
        // the STYLE field above does the work; this just forces class init at a known moment
    }

    public static VisualStyle read(ServerPlayer player) {
        return VisualStyle.fromStorage(player.getAttachedOrElse(STYLE, Map.of()));
    }

    public static void write(ServerPlayer player, VisualStyle style) {
        if (style == null || style.isEmpty()) player.removeAttached(STYLE);
        else player.setAttached(STYLE, Map.copyOf(style.toStorage()));
        Visuals.setStyle(player, style == null ? VisualStyle.EMPTY : style);
    }

    /** Has this player been through the customisation step at all? */
    public static boolean touched(ServerPlayer player) {
        return player.hasAttached(STYLE);
    }
}
