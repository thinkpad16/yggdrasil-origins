package org.origins_yggdrasil.yggdrasil_origins.power;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.origins_yggdrasil.yggdrasil_origins.net.YggPackets;

/**
 * Sends the bar state to the client: how much cooldown is left, how full a resource is, and
 * which conditional powers are in force at this moment.
 * Sent only when the state changed (class choice, cooldown start, resource change, a
 * condition turning on or off); the client fills in the progress itself from world time.
 */
public final class HudSync {

    private static final java.util.Map<java.util.UUID, Integer> LAST = new java.util.HashMap<>();
    /** Players whose state moved this tick; the packet goes out once, at the end of it. */
    private static final java.util.Set<java.util.UUID> DIRTY = new java.util.HashSet<>();

    private HudSync() { }

    public static void forget(java.util.UUID uuid) {
        LAST.remove(uuid);
        DIRTY.remove(uuid);
    }

    /**
     * Notes that this player's bars may have moved. Several powers can turn on in the same
     * tick - three tiers of one path do it together - and building the packet for each of
     * them would be three strings nobody reads.
     */
    public static void mark(ServerPlayer player) {
        DIRTY.add(player.getUUID());
    }

    /** Sends what the tick changed, one packet per player at most. */
    public static void flush(net.minecraft.server.MinecraftServer server) {
        if (DIRTY.isEmpty()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (DIRTY.contains(player.getUUID())) send(player);
        }
        DIRTY.clear();
    }

    public static void send(ServerPlayer player) {
        java.util.List<ResourceLocation> powers = PowerEngine.INSTANCE.hudPowers(player);
        java.util.List<ResourceLocation> active = PowerEngine.INSTANCE.activeConditions(player);
        if (powers.isEmpty() && active.isEmpty() && !LAST.containsKey(player.getUUID())) return;
        JsonArray bars = new JsonArray();
        for (ResourceLocation power : powers) {
            JsonObject bar = new JsonObject();
            bar.addProperty("id", power.toString());
            int length = PowerEngine.INSTANCE.cooldownLength(power);
            if (length > 0) {
                bar.addProperty("kind", "cooldown");
                bar.addProperty("length", length);
                bar.addProperty("until", PowerState.cooldownUntil(player, power));
            } else {
                bar.addProperty("kind", "resource");
                bar.addProperty("value", PowerState.resource(player, power));
                bar.addProperty("min", PowerEngine.INSTANCE.resourceMin(power));
                bar.addProperty("max", PowerEngine.INSTANCE.resourceMax(power));
            }
            bars.add(bar);
        }
        JsonArray marks = new JsonArray();
        for (ResourceLocation power : active) {
            marks.add(power.toString());
        }
        JsonObject root = new JsonObject();
        root.add("bars", bars);
        root.add("marks", marks);
        String json = root.toString();
        Integer last = LAST.get(player.getUUID());
        int hash = json.hashCode();
        // nothing changed - the packet would add nothing
        if (last != null && last == hash) return;
        LAST.put(player.getUUID(), hash);
        // a vanilla client does not accept our packets, and fake players (automation mods)
        // have no connection at all - canSend throws an NPE on those
        if (player.connection == null) return;
        if (!ServerPlayNetworking.canSend(player, YggPackets.Hud.TYPE)) return;
        ServerPlayNetworking.send(player, new YggPackets.Hud(json));
    }
}
