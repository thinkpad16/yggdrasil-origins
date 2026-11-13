package org.origins_yggdrasil.yggdrasil_origins.mixin;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualProfile;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hitbox follows the class looks: a dwarf is shorter and wider, a giant is taller.
 * Both sides read one source, otherwise the model and the collision box would drift apart.
 */
@Mixin(Player.class)
public abstract class PlayerDimensionsMixin {

    @Inject(method = "getDefaultDimensions", at = @At("RETURN"), cancellable = true)
    private void yggdrasil$resize(Pose pose, CallbackInfoReturnable<EntityDimensions> info) {
        Player self = (Player) (Object) this;
        VisualProfile profile = VisualSource.get(self);
        if (profile.isDefault()) return;

        // scale(), not a fresh box: it carries the eye height and the attachment points
        // (where a boat or a horse seats the player) along, and leaves fixed poses alone.
        // A fresh box lost the seat and every scaled player rode half a block above it.
        info.setReturnValue(info.getReturnValue().scale(profile.width(), profile.height()));
    }
}
