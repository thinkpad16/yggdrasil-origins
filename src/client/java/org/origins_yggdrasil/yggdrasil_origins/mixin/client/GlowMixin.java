package org.origins_yggdrasil.yggdrasil_origins.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.origins_yggdrasil.yggdrasil_origins.power.ClientFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code entity_glow}: wounded enemies glow for the power owner (drawn on the client). */
@Mixin(Entity.class)
public abstract class GlowMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void yggdrasil$glowWounded(CallbackInfoReturnable<Boolean> info) {
        if (!ClientFlags.glowWounded) return;
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide() || self == Minecraft.getInstance().player) return;
        if (self instanceof LivingEntity living && living.getHealth() < living.getMaxHealth() * 0.5F) {
            info.setReturnValue(true);
        }
    }
}
