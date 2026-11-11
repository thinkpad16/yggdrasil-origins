package org.origins_yggdrasil.yggdrasil_origins.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.origins_yggdrasil.yggdrasil_origins.power.PowerEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** {@code disable_regen}: removes natural regeneration only, leaving hunger alone. */
@Mixin(FoodData.class)
public abstract class FoodDataRegenMixin {

    @Redirect(method = "tick(Lnet/minecraft/world/entity/player/Player;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"),
        require = 0)
    private void yggdrasil$skipNaturalRegen(Player player, float amount) {
        if (player instanceof ServerPlayer serverPlayer && PowerEngine.INSTANCE.regenDisabled(serverPlayer)) {
            return;
        }
        player.heal(amount);
    }
}
