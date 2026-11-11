package org.origins_yggdrasil.yggdrasil_origins.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.origins_yggdrasil.yggdrasil_origins.power.PowerEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code effect_immunity}: the effect is never applied at all.
 * Removing it a tick later would leave a window where poison still bites.
 */
@Mixin(LivingEntity.class)
public abstract class EffectImmunityMixin {

    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void yggdrasil$blockImmuneEffects(MobEffectInstance instance, CallbackInfoReturnable<Boolean> info) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer player && PowerEngine.INSTANCE.immuneTo(player, instance.getEffect())) {
            info.setReturnValue(false);
        }
    }
}
