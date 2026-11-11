package org.origins_yggdrasil.yggdrasil_origins.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.origins_yggdrasil.yggdrasil_origins.power.PowerEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** {@code modify_status_effect_duration} and {@code modify_status_effect_amplifier}. */
@Mixin(LivingEntity.class)
public abstract class EffectMixin {

    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;"
        + "Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance yggdrasil$modifyEffect(MobEffectInstance value, MobEffectInstance ignored,
                                                     Entity source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer player && value != null) {
            return PowerEngine.INSTANCE.modifyEffect(player, value);
        }
        return value;
    }
}
