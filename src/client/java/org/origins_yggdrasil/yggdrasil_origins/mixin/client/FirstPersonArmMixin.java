package org.origins_yggdrasil.yggdrasil_origins.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.origins_yggdrasil.yggdrasil_origins.client.render.FirstPersonParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a player's own arm elements on the hand they see in first person.
 * <p>
 * The hook sits at the end of the vanilla hand render, where the pose stack is back at the
 * hand's own base - the same place the third person layer starts from.
 */
@Mixin(PlayerRenderer.class)
public abstract class FirstPersonArmMixin {

    @Inject(method = "renderRightHand", at = @At("TAIL"))
    private void yggdrasil$rightHand(PoseStack pose, MultiBufferSource buffers, int light,
                                     AbstractClientPlayer player, CallbackInfo info) {
        yggdrasil$hand(pose, buffers, light, player, HumanoidArm.RIGHT);
    }

    @Inject(method = "renderLeftHand", at = @At("TAIL"))
    private void yggdrasil$leftHand(PoseStack pose, MultiBufferSource buffers, int light,
                                    AbstractClientPlayer player, CallbackInfo info) {
        yggdrasil$hand(pose, buffers, light, player, HumanoidArm.LEFT);
    }

    @SuppressWarnings("unchecked")
    private void yggdrasil$hand(PoseStack pose, MultiBufferSource buffers, int light,
                                AbstractClientPlayer player, HumanoidArm side) {
        PlayerRenderer renderer = (PlayerRenderer) (Object) this;
        FirstPersonParts.render(pose, buffers, light, player, side,
            (PlayerModel<AbstractClientPlayer>) renderer.getModel());
    }
}
