package org.origins_yggdrasil.yggdrasil_origins.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;

/**
 * Client-side switch for class looks: a key in the controls settings
 * and the {@code /yggvisuals} command. The state goes into the player config; the server
 * never learns about it - it is purely "how I prefer to look at the game".
 */
public final class VisualToggle {

    private static KeyMapping key;

    private VisualToggle() { }

    public static void register() {

        key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.yggdrasil_origins.toggle_visuals",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.yggdrasil_origins"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.consumeClick()) {
                toggle(client, !YggdrasilOrigins.config().showVisuals);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) ->
            dispatcher.register(ClientCommandManager.literal("yggvisuals")
                .executes(context -> {
                    toggle(Minecraft.getInstance(), !YggdrasilOrigins.config().showVisuals);
                    return 1;
                })
                .then(ClientCommandManager.literal("on").executes(context -> {
                    toggle(Minecraft.getInstance(), true);
                    return 1;
                }))
                .then(ClientCommandManager.literal("off").executes(context -> {
                    toggle(Minecraft.getInstance(), false);
                    return 1;
                }))));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) ->
            dispatcher.register(ClientCommandManager.literal("ygghud")
                .executes(context -> hud(!YggdrasilOrigins.config().enableHud))
                .then(ClientCommandManager.literal("on").executes(context -> hud(true)))
                .then(ClientCommandManager.literal("off").executes(context -> hud(false)))));
    }

    /** Ability bars: in the way when another mod draws its own in the same corner. */
    private static int hud(boolean on) {
        YggdrasilOrigins.config().enableHud = on;
        YggdrasilOrigins.config().save();
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(
                Component.translatable(on
                        ? "message.yggdrasil_origins.hud_on"
                        : "message.yggdrasil_origins.hud_off")
                    .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
        }
        return 1;
    }

    private static void toggle(Minecraft client, boolean on) {
        if (YggdrasilOrigins.config().showVisuals == on) {
            message(client, on);
            return;
        }
        YggdrasilOrigins.config().showVisuals = on;
        YggdrasilOrigins.config().save();
        // models and hitboxes re-read the profile - refreshing the players is enough
        if (client.level != null) {
            client.level.players().forEach(player -> player.refreshDimensions());
        }
        message(client, on);
    }

    private static void message(Minecraft client, boolean on) {
        if (client.player == null) return;
        client.player.displayClientMessage(
            Component.translatable(on
                    ? "message.yggdrasil_origins.visuals_on"
                    : "message.yggdrasil_origins.visuals_off")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
    }
}
