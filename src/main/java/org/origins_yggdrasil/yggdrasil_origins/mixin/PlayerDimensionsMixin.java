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

        EntityDimensions base = info.getReturnValue();
        float width = base.width() * profile.width();
        float height = base.height() * profile.height();
        info.setReturnValue(EntityDimensions.scalable(width, height)
            .withEyeHeight(base.eyeHeight() * profile.height()));
    }
}
