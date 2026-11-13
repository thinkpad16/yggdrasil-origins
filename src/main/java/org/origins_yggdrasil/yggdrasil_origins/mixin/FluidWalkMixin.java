package org.origins_yggdrasil.yggdrasil_origins.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.origins_yggdrasil.yggdrasil_origins.power.PowerEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code walk_on_fluid} - server side (the client half lives in a separate mixin). */
@Mixin(LivingEntity.class)
public abstract class FluidWalkMixin {

    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void yggdrasil$walkOnFluid(FluidState state, CallbackInfoReturnable<Boolean> info) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) return;
        // sneaking sinks, as the client predicts: otherwise the two disagree and it stutters
        if (player.isShiftKeyDown()) return;
        boolean lava = state.is(FluidTags.LAVA);
        if (!lava && !state.is(FluidTags.WATER)) return;
        if (PowerEngine.INSTANCE.walksOnFluid(player, lava)) info.setReturnValue(true);
    }
}
