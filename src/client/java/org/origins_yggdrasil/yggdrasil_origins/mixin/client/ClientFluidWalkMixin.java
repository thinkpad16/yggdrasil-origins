package org.origins_yggdrasil.yggdrasil_origins.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.origins_yggdrasil.yggdrasil_origins.power.ClientFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client half of {@code walk_on_fluid}: without it walking on lava would stutter,
 * because the client predicts physics itself. The "not sneaking" check runs locally.
 */
@Mixin(LivingEntity.class)
public abstract class ClientFluidWalkMixin {

    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void yggdrasil$clientWalkOnFluid(FluidState state, CallbackInfoReturnable<Boolean> info) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() || self != Minecraft.getInstance().player) return;
        boolean lava = state.is(FluidTags.LAVA);
        boolean allowed = lava ? ClientFlags.walkOnLava : ClientFlags.walkOnWater;
        if (allowed && !state.isEmpty() && !self.isShiftKeyDown()) info.setReturnValue(true);
    }
}
