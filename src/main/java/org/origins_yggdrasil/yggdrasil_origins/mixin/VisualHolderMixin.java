package org.origins_yggdrasil.yggdrasil_origins.mixin;

import net.minecraft.world.entity.player.Player;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualHolder;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** The visual profile stored on the player itself, so the hitbox need not look it up in a map. */
@Mixin(Player.class)
public abstract class VisualHolderMixin implements VisualHolder {

    @Unique
    private VisualProfile yggdrasil$profile;

    @Override
    public VisualProfile yggdrasil$visual() {
        return yggdrasil$profile;
    }

    @Override
    public void yggdrasil$visual(VisualProfile profile) {
        this.yggdrasil$profile = profile;
    }
}
