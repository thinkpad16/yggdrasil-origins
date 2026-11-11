package org.origins_yggdrasil.yggdrasil_origins.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.origins_yggdrasil.yggdrasil_origins.client.gui.ClassSelectScreen;

/**
 * The "my class" reference on a key (L by default) and the {@code /yggclass} command.
 * <p>
 * L and not O: in packs with Origins the O key is already their "view origin",
 * and one press would fire two actions.
 * Asks the server for nothing: everything needed is already in the client snapshot,
 * which arrives on join and after every class change.
 */
public final class ClassScreenKey {

    private static KeyMapping key;

    private ClassScreenKey() { }

    public static void register() {

        key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.yggdrasil_origins.my_class",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "key.categories.yggdrasil_origins"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.consumeClick()) open(client);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) ->
            dispatcher.register(ClientCommandManager.literal("yggclass")
                .executes(context -> {
                    open(Minecraft.getInstance());
                    return 1;
                })));
    }

    private static void open(Minecraft client) {
        if (client.player == null || client.screen != null) return;
        ClassSelectScreen screen = ClassSelectScreen.mine();
        if (screen == null) {
            client.player.displayClientMessage(
                Component.translatable("message.yggdrasil_origins.no_class")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        client.setScreen(screen);
    }
}
