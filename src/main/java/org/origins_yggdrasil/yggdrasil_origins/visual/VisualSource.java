package org.origins_yggdrasil.yggdrasil_origins.visual;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.function.Function;

/**
 * The single place where both server and client read a player's looks.
 * <p>
 * The value is cached on the player ({@link VisualHolder}): {@code getDimensions} runs
 * dozens of times per tick per player, and a map lookup there is wasted work.
 */
public final class VisualSource {

    private static Function<UUID, VisualProfile> clientLookup;

    private VisualSource() { }

    public static void bindClient(Function<UUID, VisualProfile> lookup) {
        clientLookup = lookup;
    }

    public static VisualProfile get(Player player) {
        if (player instanceof VisualHolder holder) {
            VisualProfile cached = holder.yggdrasil$visual();
            if (cached != null) return cached;
        }
        VisualProfile profile = lookup(player);
        if (player instanceof VisualHolder holder) holder.yggdrasil$visual(profile);
        return profile;
    }

    /** The profile changed - store it on the player and refresh their dimensions. */
    public static void apply(Player player, VisualProfile profile) {
        if (player instanceof VisualHolder holder) holder.yggdrasil$visual(profile);
        player.refreshDimensions();
    }

    private static VisualProfile lookup(Player player) {
        if (player instanceof ServerPlayer) return Visuals.of(player.getUUID());
        return clientLookup == null ? VisualProfile.NONE : clientLookup.apply(player.getUUID());
    }
}
