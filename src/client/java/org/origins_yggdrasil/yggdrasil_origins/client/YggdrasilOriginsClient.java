package org.origins_yggdrasil.yggdrasil_origins.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import org.origins_yggdrasil.yggdrasil_origins.client.gui.ClassSelectScreen;
import org.origins_yggdrasil.yggdrasil_origins.client.render.VisualParts;
import org.origins_yggdrasil.yggdrasil_origins.client.render.VisualPartsLayer;

import org.origins_yggdrasil.yggdrasil_origins.net.YggPackets;

public class YggdrasilOriginsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        ClientPlayNetworking.registerGlobalReceiver(YggPackets.Sync.TYPE, (payload, context) ->
            context.client().execute(() -> ClientClasses.accept(payload.json())));

        ClientPlayNetworking.registerGlobalReceiver(YggPackets.Open.TYPE, (payload, context) ->
            context.client().execute(() -> {
                ResourceLocation layerId = ResourceLocation.tryParse(payload.layer());
                if (layerId == null) return;
                ClientClasses.layer(layerId)
                    .filter(layer -> !layer.classes().isEmpty())
                    .ifPresent(layer -> context.client().setScreen(new ClassSelectScreen(layer)));
            }));

        ClientPlayNetworking.registerGlobalReceiver(YggPackets.Hud.TYPE, (payload, context) ->
            context.client().execute(() -> ClientHud.accept(payload.json())));

        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
            (graphics, tickDelta) -> ClientHud.render(graphics, tickDelta.getGameTimeDeltaTicks()));

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
            (handler, client) -> {
                ClientHud.clear();
                ClientVisuals.clear();
            });

        // The look pickers are parked (see VisualStyle#EDITABLE), so this opens nothing. The
        // receiver stays registered: a server that still sends the packet is answered with
        // silence rather than with a warning in the log.
        ClientPlayNetworking.registerGlobalReceiver(YggPackets.Customise.TYPE,
            (payload, context) -> { });

        ClientPlayNetworking.registerGlobalReceiver(YggPackets.Visuals.TYPE, (payload, context) ->
            context.client().execute(() -> ClientVisuals.accept(payload.json())));
        org.origins_yggdrasil.yggdrasil_origins.visual.VisualSource.bindClient(ClientVisuals::raw);
        VisualToggle.register();
        ClassScreenKey.register();

        // the player left our view - drop the profile so the map does not grow forever
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_UNLOAD.register(
            (entity, world) -> {
                if (entity instanceof net.minecraft.world.entity.player.Player player) {
                    ClientVisuals.forget(player.getUUID());
                }
            });

        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
            VisualParts.SKIN_LAYER, VisualParts::skinMesh);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
            VisualParts.PROP_LAYER, VisualParts::propMesh);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
            VisualParts.BODY_LAYER, VisualParts::bodyMesh);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
            VisualParts.DETAIL_LAYER, VisualParts::detailMesh);

        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT
            .register((entityType, renderer, helper, context) -> {
                if (!(renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer)) return;
                // the callback hands us a raw RenderLayerParent; the instanceof above is what
                // guarantees the player type, so the unchecked cast is the only way through
                @SuppressWarnings("unchecked")
                net.minecraft.client.renderer.entity.RenderLayerParent<
                    net.minecraft.client.player.AbstractClientPlayer,
                    net.minecraft.client.model.PlayerModel<
                        net.minecraft.client.player.AbstractClientPlayer>> parent =
                    (net.minecraft.client.renderer.entity.RenderLayerParent<
                        net.minecraft.client.player.AbstractClientPlayer,
                        net.minecraft.client.model.PlayerModel<
                            net.minecraft.client.player.AbstractClientPlayer>>) renderer;
                helper.register(new VisualPartsLayer(
                    parent,
                    context.bakeLayer(VisualParts.SKIN_LAYER),
                    context.bakeLayer(VisualParts.PROP_LAYER),
                    context.bakeLayer(VisualParts.BODY_LAYER),
                    context.bakeLayer(VisualParts.DETAIL_LAYER)));
            });
    }
}
