package org.origins_yggdrasil.yggdrasil_origins;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.world.item.ItemStack;
import org.origins_yggdrasil.yggdrasil_origins.clazz.ClassRegistry.Layer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import org.origins_yggdrasil.yggdrasil_origins.clazz.ClassManager;
import org.origins_yggdrasil.yggdrasil_origins.clazz.ClassRegistry;
import org.origins_yggdrasil.yggdrasil_origins.item.YggItems;
import org.origins_yggdrasil.yggdrasil_origins.power.PowerEngine;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResultHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A standalone class mod.
 * <p>
 * Origins is not required: the choice, its storage, the picker screen and the commands are ours.
 * Powers keep the Apoli JSON shape (RPG Series attributes, conditions, procs): they live in
 * {@code data/yggdrasil_origins/powers/} and this mod grants and runs them itself.
 */
public class YggdrasilOrigins implements ModInitializer {

    /** Which classes suffer because some powers do not work. */
    private static String ownersOf(java.util.Set<ResourceLocation> powers) {
        java.util.LinkedHashSet<String> owners = new java.util.LinkedHashSet<>();
        ClassRegistry.INSTANCE.defs().forEach((id, def) -> {
            for (ResourceLocation power : def.powers()) {
                if (powers.contains(power)) {
                    owners.add(shortId(id.toString()));
                    return;
                }
            }
        });
        return String.join(", ", owners);
    }

    /** How many lines of a diagnostic list are worth printing before it becomes a wall. */
    private static final int DOCTOR_LIST_LIMIT = 10;

    private static int chosenPercent(int count, int total) {
        return total <= 0 ? 0 : (int) Math.round(count * 100.0 / total);
    }

    /** In a report the class matters, not the namespace - lines must stay readable. */
    private static String shortId(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    public static final String MOD_ID = "yggdrasil_origins";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Built-in datapack: silences the default Origins origins when Origins is still installed. */
    public static final ResourceLocation DISABLE_VANILLA_PACK =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "disable_vanilla_origins");

    private static YggdrasilOriginsConfig config = new YggdrasilOriginsConfig();

    public static YggdrasilOriginsConfig config() {
        return config;
    }

    @Override
    public void onInitialize() {

        config = YggdrasilOriginsConfig.load();

        org.origins_yggdrasil.yggdrasil_origins.visual.StyleManager.touch();
        YggItems.register();
        org.origins_yggdrasil.yggdrasil_origins.item.WardItems.register();
        registerDisableVanillaPack();

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(ClassRegistry.INSTANCE);

        ClassManager.registerPayloads();
        ClassManager.registerServer();
        org.origins_yggdrasil.yggdrasil_origins.visual.Visuals.registerTracking();

        ServerTickEvents.END_SERVER_TICK.register(PowerEngine.INSTANCE::tick);

        // fire immunity: cancel the fire damage itself
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) return true;
            return !(source.is(DamageTypeTags.IS_FIRE) && PowerEngine.INSTANCE.fireImmune(player));
        });

        // hit reactions: we were hit / we hit someone
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseAmount, taken, blocked) -> {
            if (entity instanceof ServerPlayer victim) {
                PowerEngine.INSTANCE.onDamaged(victim, source, taken);
            }
            if (source.getEntity() instanceof ServerPlayer attacker && attacker != entity && taken > 0.0F) {
                PowerEngine.INSTANCE.onDealt(attacker, entity);
                boolean projectile = source.getDirectEntity() != null && source.getDirectEntity() != attacker;
                PowerEngine.INSTANCE.onDamageLanded(attacker, entity, source, projectile);
            }
        });

        // powers that save from death
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
            !(entity instanceof ServerPlayer player) || !PowerEngine.INSTANCE.preventDeath(player, source));

        // actions on breaking a block (e.g. the forge palm of the blazing class)
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register(
            (world, player, pos, state, entity) -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    PowerEngine.INSTANCE.onBlockBroken(serverPlayer, state);
                }
            });

        // item bans (e.g. a bow for the werewolf)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayer serverPlayer
                && PowerEngine.INSTANCE.preventsUse(serverPlayer, player.getItemInHand(hand))) {
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            java.util.UUID uuid = handler.getPlayer().getUUID();
            PowerEngine.INSTANCE.forget(uuid);
            ClassManager.forgetSync(uuid);
            org.origins_yggdrasil.yggdrasil_origins.power.HudSync.forget(uuid);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            ClassManager.apply(newPlayer);
            ClassManager.sync(newPlayer);
            // a respawn point is not a choice: an undead must not land in daylight already burning
            ClassManager.grantGrace(newPlayer);
            // the player could die with the picker open - bring them back to it
            ClassManager.promptIfNeeded(newPlayer);
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            config = YggdrasilOriginsConfig.load();
            if (!success) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                // a reload may have removed the class the player had - then they must pick again
                ClassManager.dropUnknown(player);
                ClassManager.apply(player);
                ClassManager.sync(player);
                ClassManager.pushAttributes(player);
                ClassManager.promptIfNeeded(player);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
            dispatcher.register(buildCommand()));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildCommand() {

        SuggestionProvider<CommandSourceStack> layerIds = (context, builder) -> SharedSuggestionProvider.suggest(
            ClassRegistry.INSTANCE.layers().stream().map(layer -> layer.id().toString()).toList(), builder);
        SuggestionProvider<CommandSourceStack> classIds = (context, builder) -> SharedSuggestionProvider.suggest(
            ClassRegistry.INSTANCE.defs().keySet().stream().map(ResourceLocation::toString).toList(), builder);

        return Commands.literal("ygg")
            .then(Commands.literal("gui")
                .executes(context -> {
                    ClassManager.openFor(context.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.argument("player", EntityArgument.players())
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        var targets = EntityArgument.getPlayers(context, "player");
                        targets.forEach(ClassManager::openFor);
                        return targets.size();
                    })))
            .then(Commands.literal("set")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.players())
                    .then(Commands.argument("layer", ResourceLocationArgument.id()).suggests(layerIds)
                        .then(Commands.argument("class", ResourceLocationArgument.id()).suggests(classIds)
                            .executes(context -> {
                                var targets = EntityArgument.getPlayers(context, "player");
                                ResourceLocation layer = ResourceLocationArgument.getId(context, "layer");
                                ResourceLocation clazz = ResourceLocationArgument.getId(context, "class");
                                int done = 0;
                                for (ServerPlayer player : targets) {
                                    if (ClassManager.choose(player, layer, clazz)) done++;
                                }
                                if (done == 0) {
                                    context.getSource().sendFailure(Component.translatable(
                                        "command.yggdrasil_origins.set.unavailable",
                                        clazz.toString(), layer.toString()));
                                    return 0;
                                }
                                int count = done;
                                context.getSource().sendSuccess(() -> Component.translatable(
                                    "command.yggdrasil_origins.set.done",
                                    clazz.toString(), layer.toString(), count), true);
                                return count;
                            })))))
            .then(Commands.literal("clear")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.players())
                    .executes(context -> {
                        var targets = EntityArgument.getPlayers(context, "player");
                        targets.forEach(ClassManager::clear);
                        context.getSource().sendSuccess(() -> Component.translatable(
                            "command.yggdrasil_origins.clear.done", targets.size()), true);
                        return targets.size();
                    })))
            .then(Commands.literal("get")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.players())
                    .executes(context -> {
                        var targets = EntityArgument.getPlayers(context, "player");
                        for (ServerPlayer player : targets) {
                            var chosen = ClassManager.chosen(player);
                            String name = player.getGameProfile().getName();
                            if (chosen.isEmpty()) {
                                context.getSource().sendSuccess(() -> Component.translatable(
                                    "command.yggdrasil_origins.get.none", name), false);
                            } else {
                                context.getSource().sendSuccess(() -> Component.literal(name + ":"), false);
                                chosen.forEach((layer, clazz) -> context.getSource().sendSuccess(() ->
                                    Component.literal("  " + layer + " -> " + clazz), false));
                            }
                        }
                        return targets.size();
                    })))
            .then(Commands.literal("has")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.players())
                    .then(Commands.argument("class", ResourceLocationArgument.id()).suggests(classIds)
                        .executes(context -> {
                            var targets = EntityArgument.getPlayers(context, "player");
                            String wanted = ResourceLocationArgument.getId(context, "class").toString();
                            int matches = 0;
                            for (ServerPlayer player : targets) {
                                boolean has = ClassManager.chosen(player).containsValue(wanted);
                                if (has) matches++;
                                String name = player.getGameProfile().getName();
                                context.getSource().sendSuccess(() -> Component.translatable(
                                    has ? "command.yggdrasil_origins.has.yes"
                                        : "command.yggdrasil_origins.has.no",
                                    name, wanted.toString()), false);
                            }
                            // number of matches: handy for execute store / if
                            return matches;
                        }))))
            .then(Commands.literal("random")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.players())
                    .executes(context -> randomFor(context, null))
                    .then(Commands.argument("layer", ResourceLocationArgument.id()).suggests(layerIds)
                        .executes(context -> randomFor(context,
                            ResourceLocationArgument.getId(context, "layer"))))))
            .then(Commands.literal("powers")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.players())
                    .executes(context -> {
                        var targets = EntityArgument.getPlayers(context, "player");
                        int total = 0;
                        for (ServerPlayer player : targets) {
                            var powers = ClassManager.grantedPowers(player);
                            total += powers.size();
                            String name = player.getGameProfile().getName();
                            context.getSource().sendSuccess(() -> Component.translatable(
                                "command.yggdrasil_origins.powers.count", name, powers.size()), false);
                            powers.forEach(id -> context.getSource().sendSuccess(() ->
                                Component.literal("  " + id), false));
                        }
                        return total;
                    })))
            .then(Commands.literal("list")
                .executes(context -> {
                    for (Layer layer : ClassRegistry.INSTANCE.layers()) {
                        context.getSource().sendSuccess(() -> Component.literal(
                            layer.id() + " (" + layer.entries().size() + "):"), false);
                        layer.entries().forEach(entry -> context.getSource().sendSuccess(() -> Component.literal(
                            "  " + entry.id() + entry.requires()
                                .map(req -> " <- " + req).orElse("")), false));
                    }
                    return 1;
                }))
            .then(Commands.literal("give")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.players())
                    .executes(context -> {
                        var targets = EntityArgument.getPlayers(context, "player");
                        targets.forEach(player ->
                            player.getInventory().placeItemBackInInventory(new ItemStack(YggItems.CLASS_ORB)));
                        context.getSource().sendSuccess(() -> Component.translatable(
                            "command.yggdrasil_origins.give.done", targets.size()), true);
                        return targets.size();
                    })))
            .then(Commands.literal("debug")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.players())
                    .executes(context -> {
                        for (ServerPlayer player : EntityArgument.getPlayers(context, "player")) {
                        var level = player.serverLevel();
                        StringBuilder out = new StringBuilder("ENGINE for ")
                            .append(player.getGameProfile().getName()).append(": ");
                        for (String type : new String[]{"generic", "magic", "arrow", "on_fire"}) {
                            var source = level.damageSources().source(net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                                ResourceLocation.withDefaultNamespace(type)));
                            float modified = PowerEngine.INSTANCE.modifyIncoming(player, source, 10.0F);
                            out.append(type).append("=").append(String.format("%.2f", modified)).append("  ");
                        }
                        out.append("| swim=").append(
                            String.format("%.2f", PowerEngine.INSTANCE.swimMultiplier(player)));
                        out.append(" | fireImmune=").append(PowerEngine.INSTANCE.fireImmune(player));
                        out.append(" | enginePowers=").append(PowerEngine.INSTANCE.grantedCount(player));
                        context.getSource().sendSuccess(() -> Component.literal(out.toString()), true);
                        }
                        return 1;
                    })))
            .then(Commands.literal("cooldowns")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.players())
                    .then(Commands.literal("clear")
                        .executes(context -> {
                            var targets = EntityArgument.getPlayers(context, "player");
                            int cleared = 0;
                            for (ServerPlayer player : targets) {
                                cleared += org.origins_yggdrasil.yggdrasil_origins.power.PowerState.reset(player);
                            }
                            int total = cleared;
                            context.getSource().sendSuccess(() -> Component.translatable(
                                "command.yggdrasil_origins.cooldowns.done",
                                total, targets.size()), true);
                            return total;
                        }))))
            .then(Commands.literal("look")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ClassManager.promptCustomise(player, true);
                    return 1;
                })
                .then(Commands.argument("targets", EntityArgument.players())
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        var targets = EntityArgument.getPlayers(context, "targets");
                        targets.forEach(player -> ClassManager.promptCustomise(player, true));
                        return targets.size();
                    })))
            .then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    // a full datapack reload, which the END_DATA_PACK_RELOAD hook follows by
                    // re-granting every online player their powers - edit a file, run this,
                    // and the people wearing that origin change with it
                    CommandSourceStack source = context.getSource();
                    MinecraftServer server = source.getServer();
                    int online = server.getPlayerList().getPlayers().size();
                    source.sendSuccess(() -> Component.translatable(
                        "command.yggdrasil_origins.reload.start"), true);
                    server.reloadResources(server.getPackRepository().getSelectedIds())
                        .thenRun(() -> server.execute(() -> source.sendSuccess(() ->
                            Component.translatable("command.yggdrasil_origins.reload.done",
                                ClassRegistry.INSTANCE.defs().size(),
                                PowerEngine.INSTANCE.supportedCount(),
                                online), true)))
                        .exceptionally(error -> {
                            server.execute(() -> source.sendFailure(Component.translatable(
                                "command.yggdrasil_origins.reload.failed", error.getMessage())));
                            return null;
                        });
                    return online;
                }))
            .then(Commands.literal("stats")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    MinecraftServer server = context.getSource().getServer();
                    List<ServerPlayer> online = server.getPlayerList().getPlayers();
                    MutableComponent report = Component.literal(
                        "Origin spread \u2014 players online: " + online.size());
                    int layers = 0;
                    for (ClassRegistry.Layer layer : ClassRegistry.INSTANCE.layers()) {
                        Map<String, Integer> counts = new LinkedHashMap<>();
                        int chosen = 0;
                        for (ServerPlayer player : online) {
                            String id = ClassManager.chosen(player).get(layer.id().toString());
                            if (id == null) continue;
                            chosen++;
                            counts.merge(id, 1, Integer::sum);
                        }
                        layers++;
                        report.append(Component.literal("\n§6" + layer.id().getPath()
                            + "§r \u2014 chosen by " + chosen + " of " + online.size()));
                        if (counts.isEmpty()) {
                            report.append(Component.literal("\n  (nobody)"));
                            continue;
                        }
                        counts.entrySet().stream()
                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                            .forEach(entry -> {
                                int count = entry.getValue();
                                int percent = chosenPercent(count, online.size());
                                report.append(Component.literal("\n  " + shortId(entry.getKey())
                                    + ": " + count + " (" + percent + "%)"));
                            });
                    }
                    if (layers == 0) {
                        context.getSource().sendFailure(Component.literal("No layers loaded"));
                        return 0;
                    }
                    context.getSource().sendSuccess(() -> report, false);
                    return online.size();
                }))
            .then(Commands.literal("doctor")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    MutableComponent report = Component.literal("Mod health in this pack:");
                    int problems = 0;

                    var missing = PowerEngine.INSTANCE.missingAttributeUsers();
                    if (missing.isEmpty()) {
                        report.append(Component.literal("\n§aAll attributes present§r"));
                    } else {
                        problems += missing.size();
                        report.append(Component.literal("\n§eMissing attributes: " + missing.size()
                            + "§r \u2014 the mods providing them are not installed, so these"
                            + " classes silently lose what their text promises:"));
                        // a wall of 60 lines helps nobody: show the worst offenders, count the rest
                        int shown = 0;
                        for (var entry : missing.entrySet()) {
                            if (shown++ >= DOCTOR_LIST_LIMIT) break;
                            String owners = ownersOf(entry.getValue());
                            report.append(Component.literal("\n  " + entry.getKey() + " \u2014 powers: "
                                + entry.getValue().size()
                                + (owners.isEmpty() ? "" : " (" + owners + ")")));
                        }
                        if (missing.size() > DOCTOR_LIST_LIMIT) {
                            report.append(Component.literal("\n  \u2026 and "
                                + (missing.size() - DOCTOR_LIST_LIMIT) + " more"));
                        }
                        report.append(Component.literal(
                            "\n  §7Install the mod that owns the namespace, or accept that those"
                                + " modifiers are skipped. Everything else keeps working.§r"));
                    }

                    var starved = PowerEngine.INSTANCE.attributeStarvedPowers();
                    if (!starved.isEmpty()) {
                        report.append(Component.literal("\n§eAbilities switched off: " + starved.size()
                            + "§r \u2014 every attribute they touch comes from a mod that is absent."
                            + " This is expected without the RPG stack, not a broken install."));
                    }

                    var broken = PowerEngine.INSTANCE.unsupportedPowers();
                    if (!broken.isEmpty()) {
                        problems += broken.size();
                        report.append(Component.literal("\n§cUnknown ability types: "
                            + broken.size() + "§r \u2014 the engine cannot run these at all:"));
                        broken.entrySet().stream().limit(DOCTOR_LIST_LIMIT).forEach(entry ->
                            report.append(Component.literal("\n  " + shortId(entry.getKey().toString())
                                + " \u2014 type " + entry.getValue())));
                        if (broken.size() > DOCTOR_LIST_LIMIT) {
                            report.append(Component.literal("\n  \u2026 and "
                                + (broken.size() - DOCTOR_LIST_LIMIT) + " more"));
                        }
                    }

                    List<String> unknown = new ArrayList<>();
                    for (String id : config().disabledOrigins) {
                        ResourceLocation parsed = ResourceLocation.tryParse(id);
                        if (parsed == null || ClassRegistry.INSTANCE.def(parsed).isEmpty()) {
                            unknown.add(id);
                        }
                    }
                    if (!unknown.isEmpty()) {
                        problems += unknown.size();
                        report.append(Component.literal("\n§cUnknown ids in disabled_origins: "
                            + String.join(", ", unknown) + "§r"));
                    }

                    if (FabricLoader.getInstance().isModLoaded("origins")) {
                        report.append(Component.literal(
                            "\n§eOrigins is installed§r \u2014 keep the datapack"
                                + " yggdrasil_origins:disable_vanilla_origins enabled, or players are asked twice"));
                    }

                    report.append(Component.literal("\n§7Origins: "
                        + ClassRegistry.INSTANCE.defs().size()
                        + ", working powers: " + PowerEngine.INSTANCE.supportedCount()
                        + ", players online: "
                        + context.getSource().getServer().getPlayerList().getPlayerCount() + "§r"));
                    if (problems == 0) {
                        report.append(Component.literal("\n§aNo problems found§r"));
                    }
                    context.getSource().sendSuccess(() -> report, false);
                    return problems;
                }))
            .then(Commands.literal("perf")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    String line = PowerEngine.INSTANCE.performance(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> Component.literal(line), true);
                    return 1;
                })
                .then(Commands.literal("reset")
                    .executes(context -> {
                        PowerEngine.INSTANCE.resetPerformance();
                        context.getSource().sendSuccess(() -> Component.translatable(
                            "command.yggdrasil_origins.perf.reset"), false);
                        return 1;
                    })))
            .then(Commands.literal("info")
                .executes(context -> {
                    int classes = ClassRegistry.INSTANCE.defs().size();
                    int layers = ClassRegistry.INSTANCE.layers().size();
                    context.getSource().sendSuccess(() -> Component.literal(
                        "Origins: " + classes + ", selection layers: " + layers), false);
                    return 1;
                }));
    }

    private static int randomFor(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                                ResourceLocation layerId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        java.util.List<Layer> layers = layerId == null
            ? ClassRegistry.INSTANCE.layers()
            : ClassRegistry.INSTANCE.layer(layerId).map(java.util.List::of).orElse(java.util.List.of());
        int done = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(context, "player")) {
            for (Layer layer : layers) {
                var picked = ClassManager.randomFor(player, layer);
                if (picked.isEmpty()) continue;
                ClassManager.choose(player, layer.id(), picked.get());
                ResourceLocation chosenId = picked.get();
                context.getSource().sendSuccess(() -> Component.literal(
                    player.getGameProfile().getName() + ": " + layer.id() + " -> " + chosenId), true);
                done++;
            }
        }
        return done;
    }

    private void registerDisableVanillaPack() {

        ModContainer container = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .orElseThrow(() -> new IllegalStateException("Own ModContainer not found: " + MOD_ID));

        ResourcePackActivationType activationType = config.removeVanillaOrigins
            ? ResourcePackActivationType.DEFAULT_ENABLED
            : ResourcePackActivationType.NORMAL;

        boolean registered = ResourceManagerHelper.registerBuiltinResourcePack(
            DISABLE_VANILLA_PACK,
            container,
            Component.translatable("pack." + MOD_ID + ".disable_vanilla_origins"),
            activationType
        );

        if (registered) {
            LOGGER.debug("Datapack \"{}\" registered ({})", DISABLE_VANILLA_PACK, activationType);
        } else {
            LOGGER.error("Failed to register built-in datapack \"{}\"", DISABLE_VANILLA_PACK);
        }
    }
}
