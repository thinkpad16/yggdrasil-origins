package org.origins_yggdrasil.yggdrasil_origins.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ArmorItem;
import org.origins_yggdrasil.yggdrasil_origins.client.ClientVisuals;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualPart;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualProfile;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualStyle;

/**
 * Arm elements on the hand you see in first person.
 * <p>
 * Claws and bracers are the parts a player looks at most, and until now they were the one
 * thing only everybody else could see. The geometry, the colour and the player's own
 * trimming all come from the same place the third person layer uses, so the hand in front
 * of you is the hand other people see.
 */
public final class FirstPersonParts {

    /** Baked once per resource pack: baking a layer every frame would cost more than drawing. */
    private static net.minecraft.client.model.geom.EntityModelSet source;
    private static ModelPart detailRoot;

    private FirstPersonParts() { }

    private static ModelPart detailRoot() {
        var models = Minecraft.getInstance().getEntityModels();
        if (models != source || detailRoot == null) {
            source = models;
            detailRoot = models.bakeLayer(VisualParts.DETAIL_LAYER);
        }
        return detailRoot;
    }

    public static void render(PoseStack pose, MultiBufferSource buffers, int light,
                              AbstractClientPlayer player, HumanoidArm side,
                              PlayerModel<AbstractClientPlayer> model) {
        if (player.isInvisible()) return;
        VisualProfile profile = ClientVisuals.of(player.getUUID());
        if (profile.parts().isEmpty()) return;
        boolean chest = player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorItem;
        boolean left = side == HumanoidArm.LEFT;
        ModelPart arm = left ? model.leftArm : model.rightArm;
        VisualStyle style = ClientVisuals.styleOf(player.getUUID());
        int tint = VisualPartsLayer.soften(profile.tint());

        for (VisualPart part : profile.parts()) {
            if (part.anchor() != VisualPart.Anchor.ARM) continue;
            if (part.hiddenBy() == VisualPart.Hide.CHEST && chest) continue;
            // an element the origin only offers, or one put away, is not on the arm either:
            // the hand a player sees has to agree with the body everyone else sees
            if (!VisualPartsLayer.worn(style, profile, part)) continue;
            String name = VisualPartsLayer.armPiece(part, left);
            ModelPart root = detailRoot();
            // data can name a part this build does not have; a missing hand is not a crash
            if (!root.hasChild(name)) continue;
            ModelPart piece = root.getChild(name);
            VisualStyle.Trim trim = style.of(part);
            int colour = trim.tint() == VisualStyle.NO_TINT ? tint
                : VisualPartsLayer.soften(trim.tint());

            pose.pushPose();
            arm.translateAndRotate(pose);
            // the same pixel the third-person layer accounts for: a slim arm is narrower
            // on its outer side by a whole pixel, and armour is wider by one
            float out = chest ? -1.0F : (VisualPartsLayer.slimArms(player) ? 1.0F : 0.0F);
            if (out != 0.0F) pose.translate((left ? -out : out) / 16.0F, 0.0F, 0.0F);
            if (trim.dx() != 0.0F || trim.dy() != 0.0F || trim.dz() != 0.0F) {
                pose.translate(trim.dx() / 16.0F, trim.dy() / 16.0F, trim.dz() / 16.0F);
            }
            // the same half scale the third person view draws double-density parts at
            pose.scale(0.5F, 0.5F, 0.5F);
            boolean pivoted = VisualPartsLayer.pivot(pose, piece, part, trim);
            piece.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(VisualParts.TEXTURE)),
                light, OverlayTexture.NO_OVERLAY, 0xFF000000 | (colour & 0xFFFFFF));
            if (pivoted) pose.popPose();
            pose.popPose();
        }
    }
}
