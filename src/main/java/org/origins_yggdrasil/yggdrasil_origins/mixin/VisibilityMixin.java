package org.origins_yggdrasil.yggdrasil_origins.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.origins_yggdrasil.yggdrasil_origins.power.PowerEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * How far away mobs notice the player: little folk get lost among the stones,
 * while a half-giant is a target visible across half a field.
 * <p>
 * The vanilla method already accounts for sneaking and mob headwear; we only scale the
 * result, so sneaking keeps working exactly as before.
 */
@Mixin(LivingEntity.class)
public abstract class VisibilityMixin {

    @Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
    private void yggdrasil_origins$notice(Entity looker, CallbackInfoReturnable<Double> info) {
        if (!(((Object) this) instanceof ServerPlayer player)) return;
        double multiplier = PowerEngine.INSTANCE.noticeMultiplier(player);
        if (multiplier == 1.0D) return;
        info.setReturnValue(Math.max(0.0D, Math.min(1.0D, info.getReturnValue() * multiplier)));
    }
}
