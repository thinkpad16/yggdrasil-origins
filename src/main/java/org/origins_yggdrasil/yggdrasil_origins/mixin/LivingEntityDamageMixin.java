package org.origins_yggdrasil.yggdrasil_origins.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.origins_yggdrasil.yggdrasil_origins.power.PowerEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Damage modifiers of our own engine: {@code modify_damage_taken},
 * {@code modify_damage_dealt} and {@code modify_projectile_damage}.
 * Fabric events cannot do this: they only allow or deny damage, they never change its amount.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @ModifyVariable(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float yggdrasil$modifyDamage(float amount, DamageSource source) {

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return amount;

        float result = amount;

        if (self instanceof ServerPlayer victim) {
            result = PowerEngine.INSTANCE.modifyIncoming(victim, source, result);
        }

        if (source.getEntity() instanceof ServerPlayer attacker) {
            boolean projectile = source.getDirectEntity() != null && source.getDirectEntity() != attacker;
            result = PowerEngine.INSTANCE.modifyOutgoing(attacker, self, source, result, projectile);
        }

        return result;
    }
}
