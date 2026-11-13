package org.origins_yggdrasil.yggdrasil_origins.power;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Player power state: cooldowns and resources (the bars).
 * <p>
 * Stored in the player save data rather than in server memory: otherwise relogging would
 * reset the cooldown and a long power ("once every few minutes") would become spam. The
 * cooldown is kept as the world time it expires at, and that keeps running across restarts.
 */
public final class PowerState {

    /*
     * Keyed by the power id itself rather than its string: every check used to build a fresh
     * string just to look the power up. The saved form does not change - map keys are written
     * as strings either way - so existing player data reads back as before.
     */
    private static final AttachmentType<Map<ResourceLocation, Long>> COOLDOWNS = AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "cooldowns"),
        builder -> builder.persistent(Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)).copyOnDeath());

    private static final AttachmentType<Map<ResourceLocation, Integer>> RESOURCES = AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "resources"),
        builder -> builder.persistent(Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)).copyOnDeath());

    private PowerState() { }

    public static void triggerCooldown(ServerPlayer player, ResourceLocation power) {
        int length = PowerEngine.INSTANCE.cooldownLength(power);
        if (length <= 0) return;
        Map<ResourceLocation, Long> map = new LinkedHashMap<>(player.getAttachedOrElse(COOLDOWNS, Map.of()));
        map.put(power, player.level().getGameTime() + length);
        player.setAttached(COOLDOWNS, map);
        // sent at the end of the tick, together with anything else that moved in it
        HudSync.mark(player);
    }

    /** World time when the cooldown ends (0 - never started). */
    public static long cooldownUntil(ServerPlayer player, ResourceLocation power) {
        Long until = player.getAttachedOrElse(COOLDOWNS, Map.<ResourceLocation, Long>of()).get(power);
        return until == null ? 0L : until;
    }

    /** true - the cooldown is ready (in Apoli terms: the power is "active"). */
    public static boolean ready(ServerPlayer player, ResourceLocation power) {
        Long until = player.getAttachedOrElse(COOLDOWNS, Map.<ResourceLocation, Long>of()).get(power);
        return until == null || player.level().getGameTime() >= until;
    }

    public static float cooldownProgress(ServerPlayer player, ResourceLocation power) {
        int length = PowerEngine.INSTANCE.cooldownLength(power);
        Long until = player.getAttachedOrElse(COOLDOWNS, Map.<ResourceLocation, Long>of()).get(power);
        if (length <= 0 || until == null) return 1f;
        long left = until - player.level().getGameTime();
        return left <= 0 ? 1f : 1f - (left / (float) length);
    }

    public static int resource(ServerPlayer player, ResourceLocation power) {
        Integer value = player.getAttachedOrElse(RESOURCES, Map.<ResourceLocation, Integer>of()).get(power);
        return value != null ? value : PowerEngine.INSTANCE.resourceStart(power);
    }

    public static void setResource(ServerPlayer player, ResourceLocation power, int value) {
        int clamped = PowerEngine.INSTANCE.clampResource(power, value);
        Map<ResourceLocation, Integer> current = player.getAttachedOrElse(RESOURCES, Map.of());
        Integer previous = current.get(power);
        // value unchanged - no write, no packet
        if (previous != null && previous == clamped) return;
        Map<ResourceLocation, Integer> map = new LinkedHashMap<>(current);
        map.put(power, clamped);
        player.setAttached(RESOURCES, map);
        HudSync.mark(player);
    }

    public static void changeResource(ServerPlayer player, ResourceLocation power, int change) {
        setResource(player, power, resource(player, power) + change);
    }

    /** Resets all cooldowns and resources of a player (admin command). */
    public static int reset(ServerPlayer player) {
        int count = player.getAttachedOrElse(COOLDOWNS, Map.<ResourceLocation, Long>of()).size()
            + player.getAttachedOrElse(RESOURCES, Map.<ResourceLocation, Integer>of()).size();
        player.setAttached(COOLDOWNS, Map.of());
        player.setAttached(RESOURCES, Map.of());
        HudSync.mark(player);
        return count;
    }

    /** Nothing is cleared on quit: the state now persists with the player. */
    public static void forget(UUID player) {
    }
}
