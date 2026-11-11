package org.origins_yggdrasil.yggdrasil_origins.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.origins_yggdrasil.yggdrasil_origins.power.PowerEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code modify_exhaustion} and {@code modify_harvest}. */
@Mixin(Player.class)
public abstract class PlayerMiscMixin {

    @ModifyVariable(method = "causeFoodExhaustion(F)V", at = @At("HEAD"), argsOnly = true)
    private float yggdrasil$modifyExhaustion(float amount) {
        Player self = (Player) (Object) this;
        return self instanceof ServerPlayer player
            ? PowerEngine.INSTANCE.modifyExhaustion(player, amount)
            : amount;
    }

    @Inject(method = "hasCorrectToolForDrops", at = @At("RETURN"), cancellable = true)
    private void yggdrasil$allowHarvest(BlockState state, CallbackInfoReturnable<Boolean> info) {
        if (info.getReturnValue()) return;
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer player && PowerEngine.INSTANCE.allowsHarvest(player, state)) {
            info.setReturnValue(true);
        }
    }
}
