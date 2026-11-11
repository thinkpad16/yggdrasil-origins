package org.origins_yggdrasil.yggdrasil_origins.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.origins_yggdrasil.yggdrasil_origins.power.PowerEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code ignore_water}: the current does not push - the dead walk along the bottom. */
@Mixin(Entity.class)
public abstract class WaterPushMixin {

    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
    private void yggdrasil$ignoreWater(CallbackInfoReturnable<Boolean> info) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ServerPlayer player && PowerEngine.INSTANCE.ignoresWaterPush(player)) {
            info.setReturnValue(false);
        }
    }
}
