package org.origins_yggdrasil.yggdrasil_origins.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.origins_yggdrasil.yggdrasil_origins.power.PowerEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;

/**
 * Milk, {@code /effect clear} and a clear_effect action do not wash off what an origin is.
 * <p>
 * The method is wrapped rather than cancelled at its head: a cancel there would take the
 * method away from every other mod that hooks it (Potions_LD keeps potion sickness through
 * milk the same way), and whichever mod loaded first would win. Wrapped, each one sets its
 * own effects aside, lets the rest of the chain run, and puts them back.
 */
@Mixin(LivingEntity.class)
public abstract class EffectClearMixin {

    @Shadow
    @Final
    private Map<Holder<MobEffect>, MobEffectInstance> activeEffects;

    @WrapMethod(method = "removeAllEffects")
    private boolean yggdrasil$keepPowerEffects(Operation<Boolean> original) {
        if (!((Object) this instanceof ServerPlayer player)) return original.call();
        List<MobEffectInstance> kept = PowerEngine.INSTANCE.liftPowerEffects(player, activeEffects);
        if (kept.isEmpty()) return original.call();
        try {
            return original.call();
        } finally {
            for (MobEffectInstance instance : kept) {
                activeEffects.putIfAbsent(instance.getEffect(), instance);
            }
        }
    }
}
