package org.origins_yggdrasil.yggdrasil_origins.clazz;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;
import org.origins_yggdrasil.yggdrasil_origins.net.YggPackets;
import org.origins_yggdrasil.yggdrasil_origins.power.PowerEngine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Stores the player choice, grants the powers and keeps pushing the picker until a choice is made. */
public final class ClassManager {

    /** The source all our powers are granted from, so they can be removed in one sweep. */
    public static final ResourceLocation SOURCE =
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "class");

    /** The player choice: layer -> class. Stored in player data and survives death. */
    public static final AttachmentType<Map<String, String>> CHOSEN = AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "chosen"),
        builder -> builder
            .persistent(Codec.unboundedMap(Codec.STRING, Codec.STRING))
            .copyOnDeath());

    private ClassManager() { }

    /** Who was already warned about a missing client-side mod, so the log is not spammed. */
    private static final java.util.Set<java.util.UUID> WARNED = new java.util.HashSet<>();

    /**
     * Whether this player's client can accept our packets.
     * A vanilla client may join a public server; sending it a packet is not allowed,
     * otherwise the server throws an exception right as the player joins.
     */
    private static boolean canTalk(ServerPlayer player, net.minecraft.network.protocol.common.custom
            .CustomPacketPayload.Type<?> type) {
        // a fake player from an automation mod has no connection - skip it silently
        if (player.connection == null) return false;
        if (ServerPlayNetworking.canSend(player, type)) return true;
        if (WARNED.add(player.getUUID())) {
            YggdrasilOrigins.LOGGER.warn("{}: client without the mod - picker screen and bars unavailable",
                player.getGameProfile().getName());
        }
        return false;
    }

    public static Map<String, String> chosen(ServerPlayer player) {
        return new LinkedHashMap<>(player.getAttachedOrElse(CHOSEN, Map.of()));
    }

    /**
     * Whether this player carries a given class, without copying their choices first.
     * <p>
     * Counting a pack asks this of every player in range, several times a second: a copy of
     * the map per question is garbage nobody needs.
     */
    public static boolean wears(ServerPlayer player, String classId) {
        Map<String, String> chosen = player.getAttachedOrElse(CHOSEN, Map.of());
        if (chosen.isEmpty()) return false;
        for (String picked : chosen.values()) {
            if (classId.equals(picked)) return true;
        }
        return false;
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(YggPackets.Sync.TYPE, YggPackets.Sync.CODEC);
        PayloadTypeRegistry.playS2C().register(YggPackets.Open.TYPE, YggPackets.Open.CODEC);
        PayloadTypeRegistry.playS2C().register(YggPackets.Hud.TYPE, YggPackets.Hud.CODEC);
        PayloadTypeRegistry.playS2C().register(YggPackets.Visuals.TYPE, YggPackets.Visuals.CODEC);
        PayloadTypeRegistry.playC2S().register(YggPackets.Choose.TYPE, YggPackets.Choose.CODEC);
        PayloadTypeRegistry.playC2S().register(YggPackets.Back.TYPE, YggPackets.Back.CODEC);
        PayloadTypeRegistry.playC2S().register(YggPackets.Style.TYPE, YggPackets.Style.CODEC);
        PayloadTypeRegistry.playS2C().register(YggPackets.Customise.TYPE, YggPackets.Customise.CODEC);
    }

    private record PacketMark(String layer, long time) { }

    /** The last accepted selection packet, used against client spam. */
    private static final Map<java.util.UUID, PacketMark> LAST_PACKET = new java.util.HashMap<>();

    /**
     * A choice re-grants every power, so spam within one layer is cut off.
     * Moving to another layer (class -> path, "back") passes straight through,
     * otherwise a fast player would lose their second choice.
     */
    private static boolean tooOften(ServerPlayer player, ResourceLocation layerId) {
        long now = player.level().getGameTime();
        PacketMark last = LAST_PACKET.get(player.getUUID());
        boolean sameLayer = last != null && last.layer().equals(layerId.toString());
        if (sameLayer && now - last.time() < 10L) {
            YggdrasilOrigins.LOGGER.debug("{}: selection packet skipped (too frequent)",
                player.getGameProfile().getName());
            return true;
        }
        LAST_PACKET.put(player.getUUID(), new PacketMark(layerId.toString(), now));
        return false;
    }

    public static void registerServer() {

        ServerPlayNetworking.registerGlobalReceiver(YggPackets.Choose.TYPE, (payload, context) ->
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                ResourceLocation layerId = ResourceLocation.tryParse(payload.layer());
                ResourceLocation classId = ResourceLocation.tryParse(payload.clazz());
                if (layerId == null || classId == null) return;
                // the same choice twice - do nothing, not even a re-grant
                if (classId.toString().equals(chosen(player).get(layerId.toString()))) return;
                if (tooOften(player, layerId)) return;
                choose(player, layerId, classId);
            }));

        ServerPlayNetworking.registerGlobalReceiver(YggPackets.Back.TYPE, (payload, context) ->
            context.server().execute(() -> {
                ResourceLocation layerId = ResourceLocation.tryParse(payload.layer());
                if (layerId == null) return;
                if (tooOften(context.player(), layerId)) return;
                back(context.player(), layerId);
            }));

        ServerPlayNetworking.registerGlobalReceiver(YggPackets.Style.TYPE, (payload, context) ->
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                // looks are not in a player's hands: a style packet is simply not read
                if (!org.origins_yggdrasil.yggdrasil_origins.visual.VisualStyle.EDITABLE) return;
                try {
                    var json = com.google.gson.JsonParser.parseString(payload.json());
                    var style = org.origins_yggdrasil.yggdrasil_origins.visual.VisualStyle
                        .fromJson(json.isJsonObject() ? json.getAsJsonObject() : null);
                    org.origins_yggdrasil.yggdrasil_origins.visual.StyleManager.write(player, style);
                } catch (Exception broken) {
                    // a client may send anything; a bad look is simply not applied
                    YggdrasilOrigins.LOGGER.debug("{}: unreadable style packet",
                        player.getGameProfile().getName());
                }
            }));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            server.execute(() -> onJoin(handler.getPlayer())));
    }

    public static void onJoin(ServerPlayer player) {
        migrateLayerName(player);
        applyDefaults(player);
        apply(player);
        sync(player);
        org.origins_yggdrasil.yggdrasil_origins.visual.Visuals.refresh(player);
        org.origins_yggdrasil.yggdrasil_origins.visual.Visuals.sendSelf(player);
        promptIfNeeded(player);
    }

    /** The layer this mod used to call "class" before it was renamed to what it always was. */
    private static final String OLD_CLASS_LAYER = YggdrasilOrigins.MOD_ID + ":class";
    private static final String ORIGIN_LAYER = YggdrasilOrigins.MOD_ID + ":origin";

    /**
     * Carries a choice saved under the old layer name across to the new one, so renaming the
     * layer does not quietly forget what every player had already picked.
     */
    private static void migrateLayerName(ServerPlayer player) {
        Map<String, String> chosen = chosen(player);
        String old = chosen.remove(OLD_CLASS_LAYER);
        if (old == null) return;
        chosen.putIfAbsent(ORIGIN_LAYER, old);
        player.setAttached(CHOSEN, Map.copyOf(chosen));
        YggdrasilOrigins.LOGGER.info("{}: choice moved from the old \"class\" layer to \"origin\"",
            player.getGameProfile().getName());
    }

    /** Layers with a {@code "default"} fill themselves in when the player has chosen nothing. */
    private static void applyDefaults(ServerPlayer player) {
        Map<String, String> chosen = chosen(player);
        boolean changed = false;
        for (ClassRegistry.Layer layer : ClassRegistry.INSTANCE.layers()) {
            if (chosen.containsKey(layer.id().toString())) continue;
            Optional<ResourceLocation> fallback = layer.defaultId();
            if (fallback.isPresent() && ClassRegistry.INSTANCE.def(fallback.get()).isPresent()
                && !layer.required()) {
                chosen.put(layer.id().toString(), fallback.get().toString());
                changed = true;
            }
        }
        if (changed) player.setAttached(CHOSEN, Map.copyOf(chosen));
    }

    /** @return false when the layer is missing or the class is unavailable to this player. */
    public static boolean choose(ServerPlayer player, ResourceLocation layerId, ResourceLocation classId) {

        Optional<ClassRegistry.Layer> layer = ClassRegistry.INSTANCE.layer(layerId);
        if (layer.isEmpty()) {
            YggdrasilOrigins.LOGGER.warn("Layer {} does not exist", layerId);
            return false;
        }

        Map<String, String> chosen = chosen(player);
        boolean allowed = ClassRegistry.INSTANCE.available(layer.get(), chosen).stream()
            .anyMatch(def -> def.id().equals(classId));
        if (!allowed) {
            YggdrasilOrigins.LOGGER.warn("{} tried to pick unavailable class {} in layer {}",
                player.getGameProfile().getName(), classId, layerId);
            return false;
        }

        chosen.put(layerId.toString(), classId.toString());
        // A choice in an earlier layer can invalidate a later one - clear those.
        for (ClassRegistry.Layer other : ClassRegistry.INSTANCE.layers()) {
            String current = chosen.get(other.id().toString());
            if (current == null || other.id().equals(layerId)) continue;
            ResourceLocation currentId = ResourceLocation.tryParse(current);
            List<ClassRegistry.Def> options = ClassRegistry.INSTANCE.available(other, chosen);
            boolean stillAllowed = currentId != null
                && options.stream().anyMatch(def -> def.id().equals(currentId));
            if (!stillAllowed) {
                chosen.remove(other.id().toString());
                continue;
            }
            // The default ("no path") stays valid for every class, so it used to survive a class
            // change and the player was never asked about paths again - they were stuck without
            // one. If a later layer holds nothing but its default and the new class actually
            // offers something, drop it so the picker asks once more.
            boolean holdsDefault = currentId != null
                && other.defaultId().map(id -> id.equals(currentId)).orElse(false);
            if (holdsDefault && other.order() > layer.get().order() && options.size() > 1) {
                chosen.remove(other.id().toString());
            }
        }

        player.setAttached(CHOSEN, Map.copyOf(chosen));
        apply(player);
        sync(player);
        grantGrace(player);
        YggdrasilOrigins.LOGGER.debug("{} chose {} in layer {}",
            player.getGameProfile().getName(), classId, layerId);
        promptIfNeeded(player);
        return true;
    }

    /** "Back": clears this layer and the previous one so the player re-picks the previous. */
    public static void back(ServerPlayer player, ResourceLocation layerId) {
        List<ClassRegistry.Layer> layers = ClassRegistry.INSTANCE.layers();
        int index = -1;
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i).id().equals(layerId)) {
                index = i;
                break;
            }
        }
        if (index <= 0) return;
        Map<String, String> chosen = chosen(player);
        chosen.remove(layerId.toString());
        chosen.remove(layers.get(index - 1).id().toString());
        player.setAttached(CHOSEN, Map.copyOf(chosen));
        apply(player);
        sync(player);
        if (canTalk(player, YggPackets.Open.TYPE)) {
            ServerPlayNetworking.send(player, new YggPackets.Open(layers.get(index - 1).id().toString()));
        }
    }

    /** A random available class in a layer - for /ygg random. */
    public static Optional<ResourceLocation> randomFor(ServerPlayer player, ClassRegistry.Layer layer) {
        List<ClassRegistry.Def> options = ClassRegistry.INSTANCE.available(layer, chosen(player));
        if (options.isEmpty()) return Optional.empty();
        return Optional.of(options.get(player.getRandom().nextInt(options.size())).id());
    }

    /** Which powers the player currently has - for /ygg powers. */
    public static List<ResourceLocation> grantedPowers(ServerPlayer player) {
        List<ResourceLocation> out = new ArrayList<>();
        for (Map.Entry<String, String> picked : chosen(player).entrySet()) {
            ResourceLocation classId = ResourceLocation.tryParse(picked.getValue());
            if (classId == null) continue;
            ClassRegistry.INSTANCE.def(classId).ifPresent(def -> out.addAll(def.powers()));
        }
        return out;
    }

    /** Drops a choice that no longer exists in the data (the class was removed by a reload). */
    public static void dropUnknown(ServerPlayer player) {
        Map<String, String> chosen = chosen(player);
        boolean changed = chosen.entrySet().removeIf(entry -> {
            ResourceLocation classId = ResourceLocation.tryParse(entry.getValue());
            boolean gone = classId == null || ClassRegistry.INSTANCE.def(classId).isEmpty();
            if (gone) {
                YggdrasilOrigins.LOGGER.warn("{}: class {} is gone - choice in layer {} cleared",
                    player.getGameProfile().getName(), entry.getValue(), entry.getKey());
            }
            return gone;
        });
        if (changed) player.setAttached(CHOSEN, Map.copyOf(chosen));
    }

    public static void clear(ServerPlayer player) {
        player.setAttached(CHOSEN, Map.of());
        // the look belonged to the origin that was just given up: its elements do not even
        // exist on the next one, and keeping it would mean the trim screen never opens again
        org.origins_yggdrasil.yggdrasil_origins.visual.StyleManager.write(
            player, org.origins_yggdrasil.yggdrasil_origins.visual.VisualStyle.EMPTY);
        apply(player);
        // the client keeps the last snapshot it was sent; forgetting it here means the empty
        // choice is pushed even if the hash happens to match something it already had
        LAST_SYNC.remove(player.getUUID());
        sync(player);
        promptIfNeeded(player);
    }

    /** Removes all of our powers and grants the ones belonging to the current choice. */
    public static void apply(ServerPlayer player) {

        PowerEngine.INSTANCE.revokeAll(player);

        int granted = 0;
        List<String> unsupported = new ArrayList<>();

        for (Map.Entry<String, String> picked : chosen(player).entrySet()) {
            ResourceLocation classId = ResourceLocation.tryParse(picked.getValue());
            if (classId == null) continue;
            Optional<ClassRegistry.Def> def = ClassRegistry.INSTANCE.def(classId);
            if (def.isEmpty()) continue;
            for (ResourceLocation powerId : def.get().powers()) {
                if (PowerEngine.INSTANCE.supports(powerId)) {
                    PowerEngine.INSTANCE.grant(player, powerId);
                    granted++;
                } else {
                    unsupported.add(powerId.getPath());
                }
            }
        }

        if (YggdrasilOrigins.LOGGER.isDebugEnabled()) {
            YggdrasilOrigins.LOGGER.debug("{}: granted {} powers for choice {} | HP={}, armor={}",
                player.getGameProfile().getName(), granted, chosen(player),
                String.format("%.1f", player.getMaxHealth()),
                String.format("%.1f", player.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)));
        }

        if (!unsupported.isEmpty()) {
            YggdrasilOrigins.LOGGER.warn("  engine does not know the type of these powers: {}", unsupported);
        }
    }

    /** Hash of the last snapshot sent, so the same pile of JSON is not sent twice. */
    private static final Map<java.util.UUID, Integer> LAST_SYNC = new java.util.HashMap<>();

    public static void forgetSync(java.util.UUID uuid) {
        org.origins_yggdrasil.yggdrasil_origins.visual.Visuals.forget(uuid);
        LAST_SYNC.remove(uuid);
        LAST_PACKET.remove(uuid);
        WARNED.remove(uuid);
    }

    /**
     * Pushes the player's attributes to their own client straight away.
     * <p>
     * The server is authoritative, so a reload already changes how much damage a player takes -
     * but their health bar and armour icons would keep showing the old numbers until something
     * else happened to mark the attributes dirty. After an edit that is exactly the moment an
     * admin looks at the screen to check whether it worked.
     */
    /**
     * A short ward right after picking an origin or respawning.
     * <p>
     * A racial curse starts the instant the origin does. Choose undead at noon, or respawn into
     * daylight, and you are on fire before the screen has closed - with no way to have avoided
     * it. The grace is the same ward the gem gives, so the curse is off and the gift that comes
     * with it is off too; the player gets long enough to reach shade, water or a roof.
     */
    public static void grantGrace(ServerPlayer player) {
        int seconds = YggdrasilOrigins.config().graceSeconds;
        if (seconds <= 0) return;
        var ward = org.origins_yggdrasil.yggdrasil_origins.item.WardItems.ward;
        if (ward == null) return;
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            ward, seconds * 20, 0, false, false, true));
    }

    public static void pushAttributes(ServerPlayer player) {
        if (player.connection == null) return;
        var syncable = player.getAttributes().getSyncableAttributes();
        if (!syncable.isEmpty()) {
            player.connection.send(new net.minecraft.network.protocol.game
                .ClientboundUpdateAttributesPacket(player.getId(), syncable));
        }
        player.resetSentInfo();
    }

    public static void sync(ServerPlayer player) {
        Map<String, Boolean> flags = Map.of(
            "walk_on_lava", PowerEngine.INSTANCE.hasFluidWalk(player, true),
            "walk_on_water", PowerEngine.INSTANCE.hasFluidWalk(player, false),
            "glow_wounded", PowerEngine.INSTANCE.glowsWounded(player),
            "lava_swim", PowerEngine.INSTANCE.swimsInLava(player),
            "lava_sight", PowerEngine.INSTANCE.seesInLava(player));
        Map<String, Float> numbers = Map.of(
            "night_vision_strength", PowerEngine.INSTANCE.nightVisionStrength(player),
            "swim_speed", (float) PowerEngine.INSTANCE.swimMultiplier(player));
        org.origins_yggdrasil.yggdrasil_origins.visual.Visuals.refresh(player);
        String snapshot = ClassRegistry.INSTANCE.snapshot(chosen(player), flags, numbers);
        Integer last = LAST_SYNC.get(player.getUUID());
        int hash = snapshot.hashCode();
        if ((last == null || last != hash) && canTalk(player, YggPackets.Sync.TYPE)) {
            LAST_SYNC.put(player.getUUID(), hash);
            ServerPlayNetworking.send(player, new YggPackets.Sync(snapshot));
        }
        org.origins_yggdrasil.yggdrasil_origins.power.HudSync.send(player);
    }

    /** The first required layer without a choice - that is where the player is sent. */
    /**
     * A layer left with a single option that is also the default ("no path") carries no
     * choice - we set it ourselves so the player does not press a button on a one-row screen.
     * This happens for classes without their own paths, the human among them.
     */
    private static void skipEmptyChoices(ServerPlayer player) {
        for (int guard = 0; guard < ClassRegistry.INSTANCE.layers().size(); guard++) {
            Optional<ClassRegistry.Layer> next = pending(player);
            if (next.isEmpty()) return;
            ClassRegistry.Layer layer = next.get();
            List<ClassRegistry.Def> options = ClassRegistry.INSTANCE.available(layer, chosen(player));
            if (options.size() != 1 || layer.defaultId().isEmpty()) return;
            ResourceLocation only = options.get(0).id();
            if (!only.equals(layer.defaultId().get())) return;
            if (!choose(player, layer.id(), only)) return;
        }
    }

    public static Optional<ClassRegistry.Layer> pending(ServerPlayer player) {
        Map<String, String> chosen = chosen(player);
        for (ClassRegistry.Layer layer : ClassRegistry.INSTANCE.layers()) {
            if (!layer.required()) continue;
            if (chosen.containsKey(layer.id().toString())) continue;
            if (ClassRegistry.INSTANCE.available(layer, chosen).isEmpty()) continue;
            return Optional.of(layer);
        }
        return Optional.empty();
    }

    /**
     * The third step used to be the player trimming their own looks. It is switched off:
     * an origin decides how its people look, so nothing is offered and nothing is sent.
     */
    public static void promptCustomise(ServerPlayer player, boolean forced) {
        if (!org.origins_yggdrasil.yggdrasil_origins.visual.VisualStyle.EDITABLE) return;
        if (!canTalk(player, YggPackets.Customise.TYPE)) return;
        // an origin that wears nothing has nothing to trim: opening an empty picker on it
        // would read as a bug rather than as a choice
        if (!forced && org.origins_yggdrasil.yggdrasil_origins.visual.Visuals
                .of(player.getUUID()).parts().isEmpty()) {
            return;
        }
        // the picker reads the looks from the last snapshot, so it gets a fresh one
        LAST_SYNC.remove(player.getUUID());
        sync(player);
        ServerPlayNetworking.send(player, new YggPackets.Customise(forced));
    }

    public static void promptIfNeeded(ServerPlayer player) {
        skipEmptyChoices(player);
        if (pending(player).isEmpty()
                && !org.origins_yggdrasil.yggdrasil_origins.visual.StyleManager.touched(player)) {
            // everything is chosen and they have never been offered the trim - offer it now
            promptCustomise(player, false);
            return;
        }
        pending(player).ifPresent(layer -> {
            if (!canTalk(player, YggPackets.Open.TYPE)) return;
            YggdrasilOrigins.LOGGER.debug("{}: layer {} has no choice - opening the screen",
                player.getGameProfile().getName(), layer.id());
            ServerPlayNetworking.send(player, new YggPackets.Open(layer.id().toString()));
        });
    }

    public static void openFor(ServerPlayer player) {
        // the screen is built from the snapshot the client holds; forgetting the last hash
        // makes sure it has a fresh one, otherwise the command can open nothing at all
        LAST_SYNC.remove(player.getUUID());
        sync(player);
        ClassRegistry.Layer layer = pending(player)
            .orElseGet(() -> ClassRegistry.INSTANCE.layers().isEmpty() ? null
                : ClassRegistry.INSTANCE.layers().get(0));
        if (layer != null && canTalk(player, YggPackets.Open.TYPE)) {
            ServerPlayNetworking.send(player, new YggPackets.Open(layer.id().toString()));
        }
    }
}
