package org.origins_yggdrasil.yggdrasil_origins.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.origins_yggdrasil.yggdrasil_origins.power.PowerEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * {@code modify_food}: nutrition value and side effects of food.
 * <p>
 * On {@code Player.eat}, not {@code LivingEntity.eat}: the player version feeds the hunger bar
 * first and only then calls the living one, so changing the food there came too late for the
 * nutrition and saturation. The changed food is passed on to the living one, so its effects
 * still follow it.
 */
@Mixin(Player.class)
public abstract class FoodMixin {

    @ModifyVariable(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;"
        + "Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD"), argsOnly = true)
    private FoodProperties yggdrasil$modifyFood(FoodProperties value, Level level, ItemStack stack) {
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer player) {
            return PowerEngine.INSTANCE.modifyFood(player, stack, value);
        }
        return value;
    }
}
