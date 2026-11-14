package org.origins_yggdrasil.yggdrasil_origins.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/**
 * The colour of a player's hair, read off their skin: fur that grows on a player (the mane)
 * takes it, so it matches whoever wears it instead of being one brown for everybody.
 * <p>
 * The skin's hat layer top is used when it is painted, the head top otherwise. The skin lives
 * on the GPU only, so it is read back once per skin and the answer kept.
 */
public final class HairColour {

    /** The mane's fur in the atlas is grey with this average brightness (of 255). */
    private static final float FUR_GREY = 200f / 255f;
    /** What the fur looked like before it took the hair colour: brown, under the class tint. */
    private static final int OLD_BROWN = 0x96785A;

    private static final Map<ResourceLocation, Integer> KNOWN = new HashMap<>();

    private HairColour() { }

    /** The tint for grey fur on this player; without a readable skin, the old brown. */
    public static int furTint(AbstractClientPlayer player, int classTint) {
        int hair = of(player);
        if (hair < 0) return multiply(classTint, OLD_BROWN);
        // the fur is grey, not white: lift the hair colour by as much so it lands on it
        int r = Math.min(255, Math.round(((hair >> 16) & 0xFF) / FUR_GREY));
        int g = Math.min(255, Math.round(((hair >> 8) & 0xFF) / FUR_GREY));
        int b = Math.min(255, Math.round((hair & 0xFF) / FUR_GREY));
        return (r << 16) | (g << 8) | b;
    }

    /** The average hair colour as 0xRRGGBB, or -1 if the skin cannot be read. */
    public static int of(AbstractClientPlayer player) {
        ResourceLocation skin = player.getSkin().texture();
        Integer known = KNOWN.get(skin);
        if (known != null) return known;
        int colour = read(skin);
        KNOWN.put(skin, colour);
        return colour;
    }

    private static int read(ResourceLocation skin) {
        try {
            RenderSystem.assertOnRenderThread();
            var texture = Minecraft.getInstance().getTextureManager().getTexture(skin);
            RenderSystem.bindTexture(texture.getId());
            int width = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int height = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            if (width < 64 || height < 32) return -1;
            try (NativeImage image = new NativeImage(width, height, false)) {
                image.downloadTexture(0, false);
                int scale = width / 64;
                // hat layer top first: when a skin has hair there, that is what everyone sees
                int colour = average(image, 40 * scale, 0, 8 * scale);
                if (colour < 0) colour = average(image, 8 * scale, 0, 8 * scale);
                org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins.LOGGER.debug(
                    "Hair colour of {}: {}", skin, colour < 0 ? "none" : String.format("#%06X", colour));
                return colour;
            }
        } catch (Exception | LinkageError failed) {
            org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins.LOGGER.warn(
                "Could not read skin {} for the hair colour - the mane keeps its old brown", skin, failed);
            return -1;
        }
    }

    /** The average of the opaque pixels in a square, or -1 if fewer than a quarter are. */
    private static int average(NativeImage image, int x0, int y0, int size) {
        long r = 0, g = 0, b = 0;
        int count = 0;
        for (int y = y0; y < y0 + size; y++) {
            for (int x = x0; x < x0 + size; x++) {
                int abgr = image.getPixelRGBA(x, y);   // despite the name: ABGR
                if ((abgr >>> 24) < 128) continue;
                r += abgr & 0xFF;
                g += (abgr >> 8) & 0xFF;
                b += (abgr >> 16) & 0xFF;
                count++;
            }
        }
        if (count < size * size / 4) return -1;
        return (int) (r / count) << 16 | (int) (g / count) << 8 | (int) (b / count);
    }

    private static int multiply(int a, int b) {
        int r = ((a >> 16) & 0xFF) * ((b >> 16) & 0xFF) / 255;
        int g = ((a >> 8) & 0xFF) * ((b >> 8) & 0xFF) / 255;
        int bl = (a & 0xFF) * (b & 0xFF) / 255;
        return (r << 16) | (g << 8) | bl;
    }
}
