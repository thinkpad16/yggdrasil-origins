package org.origins_yggdrasil.yggdrasil_origins.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.origins_yggdrasil.yggdrasil_origins.power.ClientFlags;
import org.origins_yggdrasil.yggdrasil_origins.power.PowerEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Swim speed from powers. The engine computes it (not an attribute, see {@code PowerEngine.Swim}):
 * the server reads it directly, the client uses the synced number so movement does not stutter.
 */
@Mixin(LivingEntity.class)
public abstract class SwimSpeedMixin {

    /**
     * How much harder a lava-born body pushes against lava. Vanilla lava movement is roughly
     * a fifth of water, so this brings it back to about the speed of a swimmer.
     */
    private static final double LAVA_PUSH = 5.0D;

    /** How fast a lava-born body climbs while holding jump, and how fast it sinks on sneak. */
    private static final double LAVA_RISE = 0.09D;
    private static final double LAVA_DIVE = -0.07D;
    /** Left-over sink when doing nothing: lava carries, but it is still not air. */
    private static final double LAVA_FLOAT = -0.012D;

    @Shadow
    protected boolean jumping;

    private boolean yggdrasil$swimsInLava(LivingEntity self) {
        return self instanceof ServerPlayer player
            ? PowerEngine.INSTANCE.swimsInLava(player)
            : ClientFlags.lavaSwim;
    }

    /**
     * Vertical movement in lava. Horizontal push alone is not swimming: vanilla halves the
     * delta every tick and keeps adding gravity, so a body still sinks to the bottom and
     * cannot climb out. This gives a lava-born one the vertical control a swimmer has -
     * hold jump to rise, sneak to go down, drift gently otherwise.
     */
    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"))
    private void yggdrasil$lavaBuoyancy(Vec3 travel, CallbackInfo info) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player) || self.isPassenger()) return;
        if (!self.isInLava() || self.isInWater()) return;
        if (!yggdrasil$swimsInLava(self)) return;

        Vec3 delta = self.getDeltaMovement();
        // the vertical speed is set outright rather than nudged: vanilla halves the delta and
        // re-adds gravity every tick, so anything short of this still reads as a slow fall
        double rise = this.jumping ? LAVA_RISE : (self.isShiftKeyDown() ? LAVA_DIVE : LAVA_FLOAT);
        self.setDeltaMovement(delta.x, rise, delta.z);
    }

    @ModifyVariable(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), argsOnly = true)
    private Vec3 yggdrasil$swimSpeed(Vec3 travel) {
        LivingEntity self = (LivingEntity) (Object) this;
        // only players have powers - bail out for mobs right away
        if (!(self instanceof Player)) return travel;
        if (self.isPassenger()) return travel;

        // lava normally drags a body to a crawl; a lava-born one moves in it like water
        if (self.isInLava() && !self.isInWater()) {
            boolean swims = self instanceof ServerPlayer player
                ? PowerEngine.INSTANCE.swimsInLava(player)
                : ClientFlags.lavaSwim;
            return swims ? travel.multiply(LAVA_PUSH, 1.0D, LAVA_PUSH) : travel;
        }
        if (!self.isInWater()) return travel;

        double factor = self instanceof ServerPlayer player
            ? PowerEngine.INSTANCE.swimMultiplier(player)
            : ClientFlags.swimSpeed;
        if (Math.abs(factor - 1.0D) < 1.0E-4D) return travel;
        return travel.multiply(factor, 1.0D, factor);
    }
}
