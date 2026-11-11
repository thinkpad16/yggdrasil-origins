package org.origins_yggdrasil.yggdrasil_origins.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.entity.Entity;
import org.origins_yggdrasil.yggdrasil_origins.power.ClientFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sight inside lava. Vanilla drops the fog to a quarter of a block unless the entity has Fire
 * Resistance; a lava-born class gets that same clear view without the effect, so the privilege
 * comes from the origin rather than from a potion.
 */
@Mixin(FogRenderer.class)
public abstract class LavaSightMixin {

    @Inject(method = "setupFog", at = @At("RETURN"))
    private static void yggdrasil$seeThroughLava(Camera camera, FogRenderer.FogMode mode,
                                                 float farPlane, boolean createFog,
                                                 float partialTick, CallbackInfo info) {
        if (!ClientFlags.lavaSight) return;
        Entity entity = camera.getEntity();
        if (entity != net.minecraft.client.Minecraft.getInstance().player) return;
        if (camera.getFluidInCamera() != FogType.LAVA) return;
        // the numbers vanilla itself uses for a fire-resistant entity
        RenderSystem.setShaderFogStart(0.0F);
        RenderSystem.setShaderFogEnd(3.0F);
    }
}
