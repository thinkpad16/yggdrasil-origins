package org.origins_yggdrasil.yggdrasil_origins.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.origins_yggdrasil.yggdrasil_origins.client.ClientVisuals;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Body proportions: height and width are separate, so a dwarf comes out shorter and stockier
 * and an elf taller and thinner, rather than merely "a different size".
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerScaleMixin {

    @Inject(method = "scale(Lnet/minecraft/client/player/AbstractClientPlayer;"
        + "Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At("TAIL"))
    private void yggdrasil$scaleByClass(AbstractClientPlayer player, PoseStack pose, float partialTick,
                                        CallbackInfo info) {
        VisualProfile profile = ClientVisuals.of(player.getUUID());
        if (profile.isDefault()) return;
        pose.scale(profile.width(), profile.height(), profile.width());
    }
}
