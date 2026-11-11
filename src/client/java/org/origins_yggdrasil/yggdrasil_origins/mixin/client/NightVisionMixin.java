package org.origins_yggdrasil.yggdrasil_origins.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.origins_yggdrasil.yggdrasil_origins.power.ClientFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Partial night vision: vanilla always applies the effect in full, while the data has {@code strength}.
 * We scale the brightness by that strength, so 0.45 lights noticeably less than full.
 */
@Mixin(GameRenderer.class)
public abstract class NightVisionMixin {

    @Inject(method = "getNightVisionScale", at = @At("RETURN"), cancellable = true)
    private static void yggdrasil$scaleNightVision(LivingEntity entity, float partialTick,
                                                   CallbackInfoReturnable<Float> info) {
        float strength = ClientFlags.nightVisionStrength;
        if (strength < 1f && entity == net.minecraft.client.Minecraft.getInstance().player) {
            info.setReturnValue(info.getReturnValue() * strength);
        }
    }
}
