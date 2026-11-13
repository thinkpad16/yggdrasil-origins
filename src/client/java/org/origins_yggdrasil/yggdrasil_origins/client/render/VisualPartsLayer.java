package org.origins_yggdrasil.yggdrasil_origins.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import org.origins_yggdrasil.yggdrasil_origins.client.ClientVisuals;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualPart;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualProfile;
import org.origins_yggdrasil.yggdrasil_origins.visual.VisualStyle;

import java.util.List;

/**
 * Draws the visual parts: on the head, on the body, on the arm.
 * <p>
 * The optimisation is deliberate: the part list arrives already parsed ({@link VisualPart}),
 * each render type buffer is fetched once per frame, small details are skipped at distance,
 * and anything armour would cover is not drawn at all.
 */
public class VisualPartsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** Beyond this distance small things (fangs, eyes, gills, sparks) are invisible anyway. */
    private static final double DETAIL_DISTANCE_SQR = 20.0 * 20.0;
    /** And beyond this nothing is drawn at all: the silhouette no longer reads. */
    private static final double ANY_DISTANCE_SQR = 64.0 * 64.0;

    private final ModelPart skinRoot;
    private final ModelPart propRoot;
    private final ModelPart bodyRoot;
    private final ModelPart detailRoot;
    /** One instance per layer: rendering is single-threaded, so no per-frame allocation. */
    private final Buffers out = new Buffers();
    /** The part being drawn and the settings it was given, read by {@link #pivot}. */
    private VisualPart shaping;
    private VisualStyle.Trim shapingTrim;

    public VisualPartsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                            ModelPart skinRoot, ModelPart propRoot, ModelPart bodyRoot,
                            ModelPart detailRoot) {
        super(parent);
        this.skinRoot = skinRoot;
        this.propRoot = propRoot;
        this.bodyRoot = bodyRoot;
        this.detailRoot = detailRoot;
    }

    /**
     * Double-density parts are drawn at 0.5 scale: their boxes are twice as large,
     * so four times as many texels land on them.
     */
    private void detail(PoseStack pose, VertexConsumer consumer, int light, int tint, String... names) {
        pose.pushPose();
        pose.scale(0.5F, 0.5F, 0.5F);
        draw(detailRoot, pose, consumer, light, tint, names);
        pose.popPose();
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float age,
                       float headYaw, float headPitch) {

        if (player.isInvisible() || player.isSpectator()) return;
        VisualProfile profile = ClientVisuals.of(player.getUUID());
        List<VisualPart> parts = profile.parts();
        if (parts.isEmpty()) return;

        double distance = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()
            .distanceToSqr(player.getX(), player.getY(), player.getZ());
        if (distance > ANY_DISTANCE_SQR) return;
        boolean details = distance <= DETAIL_DISTANCE_SQR;

        // glowing parts breathe slightly - one factor per frame, no extra maths
        float pulse = 0.78F + 0.22F * (0.5F + 0.5F * Mth.sin(age * 0.08F));

        boolean helmet = wearsArmor(player, EquipmentSlot.HEAD);
        boolean elytra = player.getItemBySlot(EquipmentSlot.CHEST)
            .getItem() instanceof net.minecraft.world.item.ElytraItem;
        // an elytra covers the back like a chestplate does, and it has wings of its own
        boolean chest = wearsArmor(player, EquipmentSlot.CHEST) || elytra;
        // the part colour is multiplied by the class tint, so a dark tint would crush it twice:
        // solid parts take half the saturation, glowing ones take it in full
        int glowTint = profile.tint();
        int tint = soften(glowTint);
        var style = ClientVisuals.styleOf(player.getUUID());

        // buffers are fetched once: fewer MultiBufferSource lookups per frame
        out.reset(buffers, player);

        boolean head = false;
        boolean body = false;
        for (VisualPart part : parts) {
            if (!visible(part, details, helmet, chest) || (elytra && onTheBack(part))) continue;
            if (!worn(style, profile, part)) continue;
            switch (part.anchor()) {
                case HEAD -> {
                    if (!head) {
                        pose.pushPose();
                        getParentModel().head.translateAndRotate(pose);
                        head = true;
                    }
                    var trim = style.of(part);
                    int ownTint = trim.tint() == VisualStyle.NO_TINT ? tint : soften(trim.tint());
                    int ownGlow = trim.tint() == VisualStyle.NO_TINT ? glowTint : trim.tint();
                    pose.pushPose();
                    shape(pose, part, trim);
                    shaping = part;
                    shapingTrim = trim;
                    renderHead(part, pose, out, light, ownTint, ownGlow, age, limbSwing,
                        limbSwingAmount, shine(part, trim, pulse), player, trim.variant());
                    pose.popPose();
                }
                case BODY -> {
                    if (head) {
                        pose.popPose();
                        head = false;
                    }
                    if (!body) {
                        pose.pushPose();
                        getParentModel().body.translateAndRotate(pose);
                        body = true;
                    }
                    var trim = style.of(part);
                    int ownTint = trim.tint() == VisualStyle.NO_TINT ? tint : soften(trim.tint());
                    int ownGlow = trim.tint() == VisualStyle.NO_TINT ? glowTint : trim.tint();
                    pose.pushPose();
                    shape(pose, part, trim);
                    shaping = part;
                    shapingTrim = trim;
                    renderBody(part, pose, out, light, ownTint, ownGlow, age, limbSwing,
                        limbSwingAmount, shine(part, trim, pulse), player, trim.variant());
                    pose.popPose();
                }
                case ARM -> {
                    if (head) {
                        pose.popPose();
                        head = false;
                    }
                    if (body) {
                        pose.popPose();
                        body = false;
                    }
                    var trim = style.of(part);
                    int ownTint = trim.tint() == VisualStyle.NO_TINT ? tint : soften(trim.tint());
                    shaping = part;
                    shapingTrim = trim;
                    // a slim skin has arms a pixel narrower, so an arm piece cut for the
                    // wide model would hang half a pixel off the sleeve; armour inflates the
                    // arm by a pixel, so over a chestplate the piece rides that much further
                    // out instead of fighting the sleeve for the same pixels
                    float narrow = (slimArms(player) ? 0.5F : 0.0F)
                        - (chest ? 1.0F : 0.0F);
                    narrow /= 16.0F;
                    pose.pushPose();
                    getParentModel().rightArm.translateAndRotate(pose);
                    pose.translate(narrow, 0.0F, 0.0F);
                    shape(pose, part, trim);
                    detail(pose, out.props(), light, ownTint, armPiece(part, false));
                    pose.popPose();
                    // both arms: one-sided arm pieces look like a mistake, not like armour
                    pose.pushPose();
                    getParentModel().leftArm.translateAndRotate(pose);
                    pose.translate(-narrow, 0.0F, 0.0F);
                    shape(pose, part, trim);
                    detail(pose, out.props(), light, ownTint, armPiece(part, true));
                    pose.popPose();
                }
                default -> { }
            }
        }
        shaping = null;
        shapingTrim = null;
        if (head) pose.popPose();
        if (body) pose.popPose();
    }

    /** The breathing of a glowing part, turned up or down by whoever wears it. */
    private static float shine(VisualPart part, VisualStyle.Trim trim, float pulse) {
        return part.glows() ? pulse * trim.glow() : pulse;
    }

    /**
     * Whether this element is actually on the player: an optional one waits until it is
     * asked for, and any element can be put away from the picker.
     */
    public static boolean worn(VisualStyle style, VisualProfile profile, VisualPart part) {
        VisualStyle.Trim trim = style.of(part);
        if (trim.hidden()) return false;
        return !profile.optionalPart(part) || trim.shown();
    }

    /** Elements that share the back with a pair of elytra wings and would clash with them. */
    private static final java.util.EnumSet<VisualPart> BACK_MOUNTED =
        java.util.EnumSet.of(VisualPart.WINGS, VisualPart.VANES, VisualPart.STREAMERS,
            VisualPart.MANE, VisualPart.IRON_SPINE, VisualPart.TRAVEL_PACK);

    private static boolean onTheBack(VisualPart part) {
        return BACK_MOUNTED.contains(part);
    }

    private static boolean visible(VisualPart part, boolean details, boolean helmet, boolean chest) {
        if (part.detail() && !details) return false;
        return switch (part.hiddenBy()) {
            case HELMET -> !helmet;
            case CHEST -> !chest;
            case NONE -> true;
        };
    }

    /**
     * Dims - or brightens - a colour for the breathing of glowing parts. The channels are
     * capped: a player may turn the shine up, and without the cap a bright red would spill
     * over into green and change the colour instead of the strength.
     */
    private static int dim(int tint, float factor) {
        int r = Math.min(255, (int) (((tint >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((tint >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((tint & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Shapes that a variant gives a part, as {scale, pitch, yaw, roll}. Parts whose variants
     * change more than a pose - horns, ears, tails - do that work inside their own case and
     * are deliberately absent here.
     */
    private static final java.util.Map<VisualPart, float[][]> VARIANT_SHAPES =
        java.util.Map.ofEntries(
            java.util.Map.entry(VisualPart.HAIR_TUFT, new float[][] {
                {1.00F, 0, 0, 0}, {1.18F, -0.18F, 0, 0}, {0.82F, 0.14F, 0.3F, 0.12F}}),
            java.util.Map.entry(VisualPart.BROW, new float[][] {
                {1.00F, 0, 0, 0}, {0.72F, 0, 0, 0}, {1.00F, 0.05F, 0, 0}}),
            java.util.Map.entry(VisualPart.BEARD, new float[][] {
                {1.00F, 0, 0, 0}, {0.74F, 0, 0, 0}, {1.16F, 0.10F, 0, 0}}),
            java.util.Map.entry(VisualPart.TUSKS, new float[][] {
                {1.00F, 0, 0, 0}, {1.30F, -0.22F, 0, 0}, {0.74F, 0.18F, 0, 0}}),
            java.util.Map.entry(VisualPart.FANGS, new float[][] {
                {1.00F, 0, 0, 0}, {1.35F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.FINS, new float[][] {
                {1.00F, 0, 0, 0}, {1.22F, -0.20F, 0, 0}, {0.80F, 0.16F, 0, 0}}),
            java.util.Map.entry(VisualPart.GILLS, new float[][] {
                {1.00F, 0, 0, 0}, {1.25F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.FRILL, new float[][] {
                {1.00F, 0, 0, 0}, {1.28F, -0.16F, 0, 0}, {1.00F, 0.10F, 0, 0}}),
            java.util.Map.entry(VisualPart.ANTENNAE, new float[][] {
                {1.00F, 0, 0, 0}, {1.24F, -0.24F, 0, 0}, {0.78F, 0.20F, 0, 0}}),
            java.util.Map.entry(VisualPart.EMBERS, new float[][] {
                {1.00F, 0, 0, 0}, {1.45F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.WINGS, new float[][] {
                {1.00F, 0, 0, 0}, {1.18F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.SHARDS, new float[][] {
                {1.00F, 0, 0, 0}, {1.26F, 0, 0, 0.16F}, {0.76F, 0, 0, -0.12F}}),
            java.util.Map.entry(VisualPart.PLATES, new float[][] {
                {1.00F, 0, 0, 0}, {1.20F, 0, 0, 0.20F}, {0.78F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.BONE_SPUR, new float[][] {
                {1.00F, 0, 0, 0}, {1.30F, 0, 0, -0.22F}, {1.00F, 0, 0, -0.10F}}),
            java.util.Map.entry(VisualPart.RIBS, new float[][] {
                {1.00F, 0, 0, 0}, {0.82F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.SCARF, new float[][] {
                {1.00F, 0, 0, 0}, {1.22F, 0, 0, 0}, {0.80F, 0, 0.26F, 0}}),
            java.util.Map.entry(VisualPart.WISPS, new float[][] {
                {1.00F, 0, 0, 0}, {1.40F, 0, 0, 0}, {0.70F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.STREAMERS, new float[][] {
                {1.00F, 0, 0, 0}, {1.25F, 0, 0, 0}, {1.00F, 0.08F, 0, 0}}),
            java.util.Map.entry(VisualPart.VAMBRACE, new float[][] {
                {1.00F, 0, 0, 0}, {1.18F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.TENDRILS, new float[][] {
                {1.00F, 0, 0, 0}, {1.30F, 0, 0, 0}, {0.74F, 0.20F, 0, 0}}),
            java.util.Map.entry(VisualPart.CARAPACE, new float[][] {
                {1.00F, 0, 0, 0}, {0.80F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.DRAGON_TAIL, new float[][] {
                {1.00F, 0, 0, 0}, {1.16F, 0, 0, 0}, {1.00F, -0.06F, 0, 0}}),
            java.util.Map.entry(VisualPart.SERPENT_TAIL, new float[][] {
                {1.00F, 0, 0, 0}, {1.18F, 0, 0, 0}, {1.00F, 0.05F, 0, 0}}),
            java.util.Map.entry(VisualPart.ROOTS, new float[][] {
                {1.00F, 0, 0, 0}, {1.24F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.MOTES, new float[][] {
                {1.00F, 0, 0, 0}, {1.40F, 0, 0, 0}, {0.72F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.FLAME_CROWN, new float[][] {
                {1.00F, 0, 0, 0}, {1.30F, 0, 0, 0}, {0.74F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.FROST_SHARDS, new float[][] {
                {1.00F, 0, 0, 0}, {1.30F, 0, 0, 0}, {0.72F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.ARM_SCALES, new float[][] {
                {1.00F, 0, 0, 0}, {1.20F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.EARRING, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0.22F}}),
            java.util.Map.entry(VisualPart.PAULDRONS, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.DIADEM, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}, {1.00F, -0.06F, 0, 0}}),
            java.util.Map.entry(VisualPart.HEADBAND, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}, {1.00F, 0.08F, 0, 0}}),
            java.util.Map.entry(VisualPart.IRON_SPINE, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.VANES, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}, {1.00F, 0.12F, 0, 0}}),
            java.util.Map.entry(VisualPart.SASH, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.GORGET, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.STONE_FISTS, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}, {1.00F, -0.10F, 0, 0}}),
            java.util.Map.entry(VisualPart.ARMBAND, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.TRAVEL_PACK, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}, {1.00F, 0.08F, 0, 0}}),
            java.util.Map.entry(VisualPart.AMBER_SPIKES, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0.10F, 0, 0}, {1.00F, -0.08F, 0, 0}}),
            java.util.Map.entry(VisualPart.AMBER_SPURS, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0.10F}, {1.00F, 0, 0, -0.10F}}),
            java.util.Map.entry(VisualPart.FORELOCK, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0.18F}, {1.00F, 0, 0, -0.26F}}),
            java.util.Map.entry(VisualPart.WREATH, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}, {1.00F, -0.10F, 0, 0}}),
            java.util.Map.entry(VisualPart.TORC, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}}),
            java.util.Map.entry(VisualPart.CLAWS, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, -0.18F, 0, 0}, {1.00F, -0.08F, 0, 0}}),
            java.util.Map.entry(VisualPart.CREST, new float[][] {
                {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}, {1.00F, 0, 0, 0}}));

    /**
     * Variants that change the proportions rather    /**
     * Variants that change the proportions rather than the size: a crest can be a low ridge
     * or a tall fin out of the same boxes. Stretching along one axis only is what makes two
     * variants read as different shapes instead of the same shape twice.
     */
    private static final java.util.Map<VisualPart, float[][]> VARIANT_STRETCH =
        java.util.Map.ofEntries(
            java.util.Map.entry(VisualPart.FRILL, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.00F, 1.00F}, {1.30F, 0.70F, 1.10F}}),
            java.util.Map.entry(VisualPart.DIADEM, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.45F, 1.10F}, {1.00F, 0.80F, 0.90F}}),
            java.util.Map.entry(VisualPart.HEADBAND, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.50F, 1.00F}, {1.00F, 0.75F, 1.00F}}),
            java.util.Map.entry(VisualPart.IRON_SPINE, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.30F, 0.85F, 1.20F}, {0.80F, 1.25F, 1.00F}}),
            java.util.Map.entry(VisualPart.VANES, new float[][] {
                {1.00F, 1.00F, 1.00F}, {0.80F, 1.30F, 1.00F}, {1.35F, 0.80F, 1.10F}}),
            java.util.Map.entry(VisualPart.SASH, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.40F, 1.10F}, {1.00F, 0.80F, 0.90F}}),
            java.util.Map.entry(VisualPart.GORGET, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.15F, 1.25F, 1.10F}, {0.85F, 0.85F, 1.00F}}),
            java.util.Map.entry(VisualPart.STONE_FISTS, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.20F, 0.85F, 1.15F}, {0.85F, 1.30F, 0.90F}}),
            java.util.Map.entry(VisualPart.BROW, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.00F, 1.00F}, {1.15F, 0.80F, 1.00F}}),
            java.util.Map.entry(VisualPart.FANGS, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.00F, 1.00F}, {0.85F, 1.40F, 0.90F}}),
            java.util.Map.entry(VisualPart.GILLS, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.00F, 1.00F}, {0.90F, 1.30F, 1.00F}}),
            java.util.Map.entry(VisualPart.EMBERS, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.00F, 1.00F}, {1.30F, 1.30F, 1.30F}}),
            java.util.Map.entry(VisualPart.WINGS, new float[][] {
                {1.00F, 1.00F, 1.00F}, {0.78F, 1.18F, 1.00F}, {1.28F, 0.80F, 1.05F}}),
            java.util.Map.entry(VisualPart.BONE_SPUR, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.00F, 1.00F}, {1.30F, 0.80F, 1.00F}}),
            java.util.Map.entry(VisualPart.RIBS, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.00F, 1.00F}, {1.00F, 1.15F, 1.20F}}),
            java.util.Map.entry(VisualPart.STREAMERS, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.00F, 1.00F}, {0.80F, 1.30F, 0.80F}}),
            java.util.Map.entry(VisualPart.VAMBRACE, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.00F, 1.00F}, {0.95F, 1.25F, 0.95F}}),
            java.util.Map.entry(VisualPart.CARAPACE, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.00F, 1.00F}, {1.10F, 0.90F, 1.15F}}),
            java.util.Map.entry(VisualPart.DRAGON_TAIL, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.00F, 1.00F}, {1.20F, 1.20F, 0.85F}}),
            java.util.Map.entry(VisualPart.SERPENT_TAIL, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.00F, 1.00F}, {0.85F, 0.85F, 1.20F}}),
            java.util.Map.entry(VisualPart.ROOTS, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.00F, 1.00F}, {1.25F, 0.90F, 1.15F}}),
            java.util.Map.entry(VisualPart.ARM_SCALES, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.00F, 1.00F}, {1.20F, 0.90F, 1.10F}}),
            java.util.Map.entry(VisualPart.TORC, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.10F, 1.35F, 1.10F}, {0.92F, 0.70F, 0.95F}}),
            java.util.Map.entry(VisualPart.CLAWS, new float[][] {
                {1.00F, 1.00F, 1.00F}, {0.90F, 1.55F, 0.90F}, {1.30F, 0.85F, 1.00F}}),
            java.util.Map.entry(VisualPart.EARRING, new float[][] {
                {1.00F, 1.00F, 1.00F}, {0.90F, 2.00F, 0.90F}, {1.30F, 0.70F, 1.30F}}),
            java.util.Map.entry(VisualPart.PAULDRONS, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.25F, 1.30F, 0.90F}, {0.85F, 0.85F, 1.30F}}),
            java.util.Map.entry(VisualPart.FORELOCK, new float[][] {
                {1.00F, 1.00F, 1.00F}, {0.85F, 1.35F, 0.85F}, {1.20F, 0.70F, 1.20F}}),
            java.util.Map.entry(VisualPart.WREATH, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.50F, 1.00F}, {1.00F, 0.75F, 1.00F}}),
            java.util.Map.entry(VisualPart.ARMBAND, new float[][] {
                {1.00F, 1.00F, 1.00F}, {1.00F, 1.60F, 1.00F}, {1.00F, 0.70F, 1.00F}}),
            java.util.Map.entry(VisualPart.TRAVEL_PACK, new float[][] {
                {1.00F, 1.00F, 1.00F}, {0.80F, 1.25F, 1.10F}, {1.25F, 0.85F, 0.90F}}),
            java.util.Map.entry(VisualPart.AMBER_SPIKES, new float[][] {
                {1.00F, 1.00F, 1.00F}, {0.80F, 1.30F, 0.90F}, {1.30F, 0.80F, 1.10F}}),
            java.util.Map.entry(VisualPart.AMBER_SPURS, new float[][] {
                {1.00F, 1.00F, 1.00F}, {0.85F, 1.30F, 0.85F}, {1.10F, 0.85F, 1.10F}}));


    /**
     * Applies a player's own nudge and size to whatever is about to be drawn. Offsets are in
     * model pixels, so the numbers in the screen read the same as the ones modellers use.
     */

    /**
     * The group transform of a part: only the nudge, which moves the whole thing. Size, tilt
     * and the variant pose are applied further in, around each model part's own joint, so a
     * resized ear stays on the head it grows out of.
     */
    private static void shape(PoseStack pose, VisualPart part, VisualStyle.Trim trim) {
        if (trim.dx() != 0.0F || trim.dy() != 0.0F || trim.dz() != 0.0F) {
            pose.translate(trim.dx() / 16.0F, trim.dy() / 16.0F, trim.dz() / 16.0F);
        }
    }

    /** The pivot for whatever the layer is drawing at this moment. */
    private boolean pivot(PoseStack pose, ModelPart piece) {
        return pivot(pose, piece, shaping, shapingTrim);
    }

    /**
     * Turns and resizes one model part around the point it is attached at. Every piece knows
     * its own joint, so the right ear turns about the right side of the head and the left ear
     * about the left - one shared pivot could never do both.
     */
    public static boolean pivot(PoseStack pose, ModelPart piece, VisualPart shaping,
                                VisualStyle.Trim trim) {
        if (trim == null || shaping == null) return false;
        float[][] shapes = VARIANT_SHAPES.get(shaping);
        float[][] stretch = VARIANT_STRETCH.get(shaping);
        boolean hasShape = shapes != null && trim.variant() > 0 && trim.variant() < shapes.length;
        boolean hasStretch = stretch != null && trim.variant() > 0 && trim.variant() < stretch.length;
        if (trim.roll() == 0.0F && trim.scale() == 1.0F && !hasShape && !hasStretch) return false;

        pose.pushPose();
        pose.translate(piece.x / 16.0F, piece.y / 16.0F, piece.z / 16.0F);
        if (trim.roll() != 0.0F) pose.mulPose(com.mojang.math.Axis.ZP.rotation(trim.roll()));
        if (hasShape) {
            float[] v = shapes[trim.variant()];
            if (v[1] != 0.0F) pose.mulPose(com.mojang.math.Axis.XP.rotation(v[1]));
            if (v[2] != 0.0F) pose.mulPose(com.mojang.math.Axis.YP.rotation(v[2]));
            if (v[3] != 0.0F) pose.mulPose(com.mojang.math.Axis.ZP.rotation(v[3]));
            if (v[0] != 1.0F) pose.scale(v[0], v[0], v[0]);
        }
        if (hasStretch) {
            float[] v = stretch[trim.variant()];
            pose.scale(v[0], v[1], v[2]);
        }
        if (trim.scale() != 1.0F) {
            // a band may only grow thicker: a wider one would float off the head it circles
            float across = shaping.encircles() ? 1.0F : trim.scale();
            pose.scale(across, trim.scale(), across);
        }
        pose.translate(-piece.x / 16.0F, -piece.y / 16.0F, -piece.z / 16.0F);
        return true;
    }

    private void renderHead(VisualPart part, PoseStack pose, Buffers out, int light, int tint,
                            int glowTint, float age, float limbSwing, float limbSwingAmount,
                            float pulse, AbstractClientPlayer player, int variant) {
        switch (part) {
            case ELF_EARS -> {
                // an elf ear twitches barely at all - this is no beast
                float flick = flick(age, 11.0F) * 0.45F;
                float sweep = switch (variant) { case 1 -> 0.34F; case 2 -> -0.12F; default -> 0.0F; };
                float length = switch (variant) { case 1 -> 1.15F; case 2 -> 0.78F; default -> 1.0F; };
                setRot(skinRoot, VisualParts.ELF_EARS_RIGHT, 0.0F, -0.08F - sweep - flick * 0.18F);
                setRot(skinRoot, VisualParts.ELF_EARS_LEFT, 0.0F, 0.08F + sweep + flick * 0.18F);
                setScale(skinRoot, VisualParts.ELF_EARS_RIGHT, length);
                setScale(skinRoot, VisualParts.ELF_EARS_LEFT, length);
                draw(skinRoot, pose, out.skin(), light, 0xFFFFFF,
                    VisualParts.ELF_EARS_LEFT, VisualParts.ELF_EARS_RIGHT);
            }
            case WOLF_EARS -> {
                // a beast ear flicks every few seconds - first one, then the other
                float flick = flick(age, 0.0F);
                float flickOther = flick(age, 37.0F);
                float fold = switch (variant) {
                    case 1 -> 0.48F;       // folded down
                    case 2 -> -0.34F;      // swept back and up
                    default -> 0.0F;
                };
                float spread = variant == 2 ? 0.34F : 0.0F;
                setRot(skinRoot, VisualParts.WOLF_EARS_RIGHT,
                    fold - flick * 0.30F, -0.18F - fold * 0.5F - spread - flick * 0.22F);
                setRot(skinRoot, VisualParts.WOLF_EARS_LEFT,
                    fold - flickOther * 0.30F, 0.18F + fold * 0.5F + spread + flickOther * 0.22F);
                draw(skinRoot, pose, out.skin(), light, 0xFFFFFF,
                    VisualParts.WOLF_EARS_LEFT, VisualParts.WOLF_EARS_RIGHT);
            }
            case HAIR_TUFT -> {
                // curls bounce on the stride and settle when their owner stands still
                ModelPart tuft = child(skinRoot, VisualParts.HAIR_TUFT);
                if (tuft != null) {
                    tuft.xRot = -limbSwingAmount * 0.11F + Mth.sin(age * 0.072F) * 0.03F;
                    tuft.zRot = Mth.cos(limbSwing * 0.33F) * 0.07F * limbSwingAmount;
                }
                draw(skinRoot, pose, out.skin(), light, 0xFFFFFF, VisualParts.HAIR_TUFT);
            }
            case BROW -> draw(skinRoot, pose, out.skin(), light, 0xFFFFFF, VisualParts.BROW);
            case BEARD -> {
                // three beards rather than one beard at three sizes: full, forked, trimmed
                String shape = switch (variant) {
                    case 1 -> VisualParts.BEARD_FORK;
                    case 2 -> VisualParts.BEARD_SHORT;
                    default -> VisualParts.BEARD;
                };
                // the beard lags behind the step: it swings with the walk, it does not live apart
                ModelPart beard = child(detailRoot, shape);
                if (beard != null) {
                    beard.xRot = Mth.cos(limbSwing * 0.33F) * 0.05F * limbSwingAmount
                        + Mth.sin(age * 0.043F) * 0.015F;
                    beard.zRot = Mth.sin(limbSwing * 0.33F) * 0.04F * limbSwingAmount;
                }
                detail(pose, out.props(), light, tint, shape);
            }
            case TUSKS -> draw(propRoot, pose, out.props(), light, 0xFFFFFF,
                VisualParts.TUSK_LEFT, VisualParts.TUSK_RIGHT);
            case FANGS -> draw(propRoot, pose, out.props(), light, 0xFFFFFF,
                VisualParts.FANG_LEFT, VisualParts.FANG_RIGHT);
            case HORNS -> {
                // three horns that are three different horns: a straight pair, a ram's curl
                // and a swept-back spike. A stretched copy of one would still read as one.
                String right = switch (variant) {
                    case 1 -> VisualParts.HORN_RAM_RIGHT;
                    case 2 -> VisualParts.HORN_SPIKE_RIGHT;
                    default -> VisualParts.HORN_RIGHT;
                };
                String left = switch (variant) {
                    case 1 -> VisualParts.HORN_RAM_LEFT;
                    case 2 -> VisualParts.HORN_SPIKE_LEFT;
                    default -> VisualParts.HORN_LEFT;
                };
                detail(pose, out.props(), light, tint, left, right);
            }
            case FINS -> {
                // in water the fins quiver slightly, on land they rest
                float flutter = player.isInWater() ? Mth.sin(age * 0.35F) * 0.12F : 0.0F;
                setRot(detailRoot, VisualParts.FIN_RIGHT, 0.0F, -0.25F - flutter);
                setRot(detailRoot, VisualParts.FIN_LEFT, 0.0F, 0.25F + flutter);
                detail(pose, out.props(), light, tint,
                    VisualParts.FIN_LEFT, VisualParts.FIN_RIGHT);
            }
            case GILLS -> {
                // gills move only in water - on land they stay closed
                float breath = player.isInWater() ? Mth.sin(age * 0.30F) * 0.14F : 0.0F;
                setRot(propRoot, VisualParts.GILL_RIGHT, 0.0F, -0.08F - breath);
                setRot(propRoot, VisualParts.GILL_LEFT, 0.0F, 0.08F + breath);
                draw(propRoot, pose, out.props(), light, tint,
                    VisualParts.GILL_LEFT, VisualParts.GILL_RIGHT);
            }
            case FRILL -> {
                // the frill flares when its owner runs or threatens
                ModelPart frill = child(propRoot, VisualParts.FRILL);
                if (frill != null) {
                    float flare = player.isSprinting() ? 0.30F : 0.0F;
                    frill.xRot = -0.10F - flare + Mth.sin(age * 0.084F) * 0.03F;
                }
                draw(propRoot, pose, out.props(), light, tint, VisualParts.FRILL);
            }
            case ANTENNAE -> {
                // the antennae are alive: a slow sway, out of step with each other
                float sway = Mth.sin(age * 0.08F) * 0.09F;
                float lag = Mth.sin(age * 0.08F - 0.8F) * 0.07F;
                setRot(propRoot, VisualParts.ANTENNA_RIGHT, -0.12F + lag, -0.20F - sway);
                setRot(propRoot, VisualParts.ANTENNA_LEFT, -0.12F - lag, 0.20F + sway);
                draw(propRoot, pose, out.props(), light, tint,
                    VisualParts.ANTENNA_LEFT, VisualParts.ANTENNA_RIGHT);
            }
            case HALO -> {
                // the halo is not nailed to the head: it turns slowly and breathes
                ModelPart halo = child(detailRoot, VisualParts.HALO);
                if (halo != null) {
                    halo.yRot = (age * 0.018F) % ((float) Math.PI * 2F);
                    halo.zRot = Mth.sin(age * 0.045F) * 0.05F + (variant == 1 ? 0.38F : 0.0F);
                    halo.xRot = switch (variant) {
                        case 1 -> 0.22F;
                        case 2 -> 1.45F;   // stood on edge, a disc behind the head
                        default -> 0.0F;
                    };
                }
                pose.pushPose();
                pose.translate(0.0F, Mth.sin(age * 0.055F) * 0.5F / 16.0F, 0.0F);
                // a halo that does not give off light is just a hat
                detail(pose, out.glow(), LightTexture.FULL_BRIGHT, dim(glowTint, pulse),
                    VisualParts.HALO);
                pose.popPose();
            }
            case CRESCENT -> {
                ModelPart crescent = child(propRoot, VisualParts.CRESCENT);
                if (crescent != null) {
                    crescent.zRot = Mth.sin(age * 0.037F) * 0.11F + switch (variant) {
                        case 1 -> 1.05F;
                        case 2 -> -0.55F;
                        default -> 0.0F;
                    };
                    crescent.xRot = variant == 2 ? 0.42F : 0.0F;
                }
                pose.pushPose();
                pose.translate(0.0F, Mth.sin(age * 0.05F + 1.0F) * 0.6F / 16.0F, 0.0F);
                draw(propRoot, pose, out.glow(), LightTexture.FULL_BRIGHT,
                    dim(glowTint, pulse), VisualParts.CRESCENT);
                pose.popPose();
            }
            case ANTLERS -> {
                // the beams creak faintly, the way real antlers shift with the head
                float creak = Mth.sin(age * 0.035F) * 0.02F;
                String right = switch (variant) {
                    case 1 -> VisualParts.ANTLER_SHORT_RIGHT;
                    case 2 -> VisualParts.ANTLER_WIDE_RIGHT;
                    default -> VisualParts.ANTLER_RIGHT;
                };
                String left = switch (variant) {
                    case 1 -> VisualParts.ANTLER_SHORT_LEFT;
                    case 2 -> VisualParts.ANTLER_WIDE_LEFT;
                    default -> VisualParts.ANTLER_LEFT;
                };
                ModelPart beamRight = child(propRoot, right);
                ModelPart beamLeft = child(propRoot, left);
                // back to the pose the model was built with, then the creak on top of it
                if (beamRight != null) {
                    beamRight.resetPose();
                    beamRight.xRot += creak;
                }
                if (beamLeft != null) {
                    beamLeft.resetPose();
                    beamLeft.xRot -= creak;
                }
                draw(propRoot, pose, out.props(), light, tint, left, right);
            }
            case FLAME_CROWN -> {
                flames(pose, out.glow(), LightTexture.FULL_BRIGHT, dim(glowTint, pulse), age,
                    propRoot, new String[] {VisualParts.FLAME_CROWN_A, VisualParts.FLAME_CROWN_B,
                                            VisualParts.FLAME_CROWN_C, VisualParts.FLAME_CROWN_D},
                    4.6F, -9.5F);
            }
            case MARKINGS -> detail(pose, out.props(), light, tint, switch (variant) {
                case 1 -> VisualParts.MARK_B;
                case 2 -> VisualParts.MARK_C;
                default -> VisualParts.MARK_A;
            });
            case EARRING -> {
                // an earring swings a little on its own, a beat behind the head
                float swing = Mth.sin(age * 0.095F) * 0.06F * (1.0F + limbSwingAmount * 2.0F);
                for (String side : new String[] {VisualParts.EARRING_RIGHT, VisualParts.EARRING_LEFT}) {
                    ModelPart ring = child(detailRoot, side);
                    if (ring != null) ring.zRot = swing;
                }
                detail(pose, out.props(), light, tint,
                    VisualParts.EARRING_RIGHT, VisualParts.EARRING_LEFT);
            }
            case BRAID -> {
                String braid = switch (variant) {
                    case 1 -> VisualParts.BRAID_PAIR;
                    case 2 -> VisualParts.BRAID_TOP;
                    default -> VisualParts.BRAID_BACK;
                };
                ModelPart hair = child(propRoot, braid);
                if (hair != null) {
                    // hair trails the stride, and a topknot only nods
                    float sway = variant == 2 ? 0.04F : 0.12F;
                    hair.xRot = Mth.cos(limbSwing * 0.4F) * limbSwingAmount * sway;
                    hair.zRot = Mth.sin(age * 0.057F) * 0.03F;
                }
                draw(propRoot, pose, out.props(), light, tint, braid);
            }
            case WREATH -> {
                // it sits on the head and barely moves: a wreath is not jewellery
                ModelPart wreath = child(propRoot, VisualParts.WREATH);
                if (wreath != null) wreath.zRot = Mth.sin(age * 0.03F) * 0.015F;
                draw(propRoot, pose, out.props(), light, tint, VisualParts.WREATH);
            }
            case FORELOCK -> {
                // the lock swings behind the step and settles when standing still
                ModelPart lock = child(propRoot, VisualParts.FORELOCK);
                if (lock != null) {
                    lock.zRot = 0.08F + Mth.sin(age * 0.065F) * 0.05F
                        + limbSwingAmount * 0.10F;
                    lock.xRot = Mth.cos(limbSwing * 0.33F) * limbSwingAmount * 0.12F;
                }
                draw(propRoot, pose, out.props(), light, tint, VisualParts.FORELOCK);
            }
            case DIADEM -> draw(propRoot, pose, out.props(), light, tint, VisualParts.DIADEM);
            case HEADBAND -> {
                // the loose ends lift a little when their owner runs
                ModelPart band = child(propRoot, VisualParts.HEADBAND);
                if (band != null) {
                    band.xRot = -limbSwingAmount * 0.06F + Mth.sin(age * 0.058F) * 0.015F;
                }
                draw(propRoot, pose, out.props(), light, tint, VisualParts.HEADBAND);
            }
            case CREST -> {
                String shape = switch (variant) {
                    case 1 -> VisualParts.CREST_FIN;
                    case 2 -> VisualParts.CREST_TWIN;
                    default -> VisualParts.CREST;
                };
                // the crest leans back a touch when the wearer is sprinting
                ModelPart crest = child(propRoot, shape);
                if (crest != null) crest.xRot = player.isSprinting() ? -0.16F : 0.0F;
                draw(propRoot, pose, out.props(), light, tint, shape);
            }
            case EMBERS -> {
                pose.pushPose();
                pose.translate(0.0F, Mth.sin(age * 0.145F) * 0.6F / 16.0F, 0.0F);
                draw(propRoot, pose, out.glow(), LightTexture.FULL_BRIGHT, dim(glowTint, pulse),
                    VisualParts.EMBER_A, VisualParts.EMBER_B, VisualParts.EMBER_C, VisualParts.EMBER_D);
                pose.popPose();
            }
            default -> { }
        }
    }

    /**
     * A short twitch every few seconds: 0 most of the time, a quick spike to 1 and back.
     * The {@code offset} spreads the two ears apart so they do not twitch together.
     */
    private static float flick(float age, float offset) {
        float phase = ((age + offset) % 95.0F) / 95.0F;
        if (phase > 0.07F) return 0.0F;
        return Mth.sin(phase / 0.07F * (float) Math.PI);
    }

    /** A streamer and its tip, addressed directly like the wings. */
    private void poseStreamer(String name, String tipName, float lean, float sway, float tipSway) {
        ModelPart streamer = child(detailRoot, name);
        if (streamer == null) return;
        streamer.xRot = lean;
        streamer.zRot = sway;
        ModelPart tip = child(streamer, tipName);
        if (tip == null) return;
        tip.xRot = 0.18F + lean * 0.4F;
        tip.zRot = tipSway;
    }

    /** Wing shoulder and hand addressed directly: no model tree search every frame. */
    private void poseWing(String wingName, String handName,
                          float lean, float sweep, float lift, float fold) {
        ModelPart wing = child(detailRoot, wingName);
        if (wing == null) return;
        wing.xRot = lean;
        wing.yRot = sweep;
        wing.zRot = lift;
        ModelPart hand = child(wing, handName);
        if (hand == null) return;
        hand.xRot = 0.0F;
        hand.yRot = fold;
        hand.zRot = 0.0F;
    }

    private void renderBody(VisualPart part, PoseStack pose, Buffers out, int light, int tint,
                            int glowTint, float age, float limbSwing, float limbSwingAmount,
                            float pulse, AbstractClientPlayer player, int variant) {
        switch (part) {
            case WINGS -> {
                // a positive shoulder yRot on the right wing sweeps it BACK behind the spine,
                // a positive zRot lifts the tip (in model space +Y points down).
                // Wings open on the way up and fold on landing, and they take a moment over
                // it: snapping between the two poses was the harshest thing on the model.
                float open = wingOpen(player, !player.onGround());
                float beat = Mth.sin(age * 0.22F) * (0.03F + 0.13F * open);
                float sweep = Mth.lerp(open, 0.52F, 0.30F);
                float lift = Mth.lerp(open, -0.32F, 0.34F) + beat;
                // while sneaking the torso pitches forward by ~0.5 rad and the wings go with it,
                // tipping upwards - so most of that pitch is subtracted back out
                float lean = Mth.lerp(open, 0.15F, 0.05F) - (player.isCrouching() ? 0.42F : 0.0F);
                float fold = Mth.lerp(open, 0.62F, 0.18F) - beat * 0.4F * open;
                // the three shapes are carried differently as well as cut differently:
                // narrow wings are held high and close, broad ones hang low and wide
                lift += switch (variant) { case 1 -> 0.16F; case 2 -> -0.12F; default -> 0.0F; };
                sweep += switch (variant) { case 1 -> -0.06F; case 2 -> 0.08F; default -> 0.0F; };
                poseWing(VisualParts.WING_RIGHT, VisualParts.WING_HAND_RIGHT, lean, sweep, lift, fold);
                poseWing(VisualParts.WING_LEFT, VisualParts.WING_HAND_LEFT, lean, -sweep, -lift, -fold);
                detail(pose, out.props(), light, tint,
                    VisualParts.WING_LEFT, VisualParts.WING_RIGHT);
            }
            case SHARDS -> detail(pose, out.props(), light, tint,
                VisualParts.SHARD_LEFT, VisualParts.SHARD_RIGHT);
            case PLATES -> detail(pose, out.props(), light, tint,
                VisualParts.PLATE_LEFT, VisualParts.PLATE_RIGHT);
            case STREAMERS -> {
                // ribbons of air: they lag behind the step and keep drifting when standing
                float drift = Mth.sin(age * 0.051F) * 0.14F;
                float lag = Mth.cos(limbSwing * 0.5F) * 0.22F * limbSwingAmount;
                poseStreamer(VisualParts.STREAMER_RIGHT, VisualParts.STREAMER_TIP_RIGHT,
                    0.22F + lag, -0.30F - drift, drift * 0.5F);
                poseStreamer(VisualParts.STREAMER_LEFT, VisualParts.STREAMER_TIP_LEFT,
                    0.22F + lag, 0.30F + drift, -drift * 0.5F);
                detail(pose, out.props(), light, tint,
                    VisualParts.STREAMER_LEFT, VisualParts.STREAMER_RIGHT);
            }
            case BONE_SPUR -> draw(bodyRoot, pose, out.props(), light, 0xFFFFFF, VisualParts.BONE_SPUR);
            case RIBS -> draw(bodyRoot, pose, out.props(), light, 0xFFFFFF, VisualParts.RIBS);
            case CARAPACE -> {
                // the shell lifts at the back when its owner runs, the way wing cases do
                ModelPart shell = child(detailRoot, VisualParts.CARAPACE);
                if (shell != null) {
                    shell.resetPose();
                    shell.xRot -= limbSwingAmount * 0.13F + Mth.sin(age * 0.034F) * 0.02F;
                }
                detail(pose, out.props(), light, tint, VisualParts.CARAPACE);
            }
            case WISPS -> {
                pose.pushPose();
                pose.translate(0.0F, Mth.sin(age * 0.12F) * 1.5F / 16.0F, 0.0F);
                draw(bodyRoot, pose, out.glow(), LightTexture.FULL_BRIGHT, dim(glowTint, pulse),
                    VisualParts.WISP_LEFT, VisualParts.WISP_RIGHT, VisualParts.WISP_BACK);
                pose.popPose();
            }
            case SCARF -> {
                ModelPart scarf = child(bodyRoot, VisualParts.SCARF);
                if (scarf != null) {
                    // the scarf tail lags while running and sways gently when standing
                    // half the swing of before, and the stride beat is slower than the legs
                    scarf.xRot = -Mth.cos(limbSwing * 0.33F) * 0.12F * limbSwingAmount
                        + Mth.sin(age * 0.062F) * 0.03F;
                    scarf.zRot = Mth.sin(limbSwing * 0.33F) * 0.05F * limbSwingAmount;
                }
                draw(bodyRoot, pose, out.props(), light, tint, VisualParts.SCARF);
            }
            case TENDRILS -> {
                float sway = Mth.sin(age * 0.069F) * 0.10F;
                setRot(bodyRoot, VisualParts.TENDRIL_RIGHT, 0.25F, 0.2F + sway);
                setRot(bodyRoot, VisualParts.TENDRIL_LEFT, 0.25F, -0.2F - sway);
                draw(bodyRoot, pose, out.props(), light, tint,
                    VisualParts.TENDRIL_LEFT, VisualParts.TENDRIL_RIGHT);
            }
            case TAIL -> {
                // the wave travels along the segments with a delay - the tail lashes, it does not stick out
                // a negative xRot drops the tail back and down (in model space +Y points down);
                // sneaking pitches the body forward, so the tail has to drop further still
                float base = (player.isCrouching() ? -0.82F : -0.42F) + switch (variant) {
                    case 1 -> -0.22F;      // the slim one is carried a little lower
                    case 2 -> 0.46F;       // the bob rides high, the way a leader walks
                    default -> 0.0F;
                };
                String root = switch (variant) {
                    case 1 -> VisualParts.TAIL_SLIM;
                    case 2 -> VisualParts.TAIL_BOB;
                    default -> VisualParts.TAIL;
                };
                String mid = switch (variant) {
                    case 1 -> VisualParts.TAIL_SLIM_MID;
                    case 2 -> VisualParts.TAIL_BOB_MID;
                    default -> VisualParts.TAIL_MID;
                };
                String tip = switch (variant) {
                    case 1 -> VisualParts.TAIL_SLIM_TIP;
                    case 2 -> VisualParts.TAIL_BOB_TIP;
                    default -> VisualParts.TAIL_TIP;
                };
                // a short tail wags harder than a long one
                float width = variant == 2 ? 0.52F : 0.38F;
                wagTail(root, mid, tip, base, limbSwingAmount, age, width);
                detail(pose, out.props(), light, tint, root);
            }
            case PAULDRONS -> draw(bodyRoot, pose, out.props(), light, tint,
                VisualParts.PAULDRON_RIGHT, VisualParts.PAULDRON_LEFT);
            case AMBER_SPIKES -> {
                // the shards shift a little with the breath, like something still growing
                float breath = Mth.sin(age * 0.04F) * 0.02F;
                ModelPart chest = child(bodyRoot, VisualParts.SPIKES_CHEST);
                if (chest != null) {
                    chest.resetPose();
                    chest.xRot -= breath;
                }
                draw(bodyRoot, pose, out.props(), light, tint, VisualParts.SPIKES_CHEST);
            }
            case IRON_SPINE -> draw(bodyRoot, pose, out.props(), light, tint,
                VisualParts.IRON_SPINE);
            case TRAVEL_PACK -> {
                // the pack settles with the stride, the way a loaded bag does
                ModelPart pack = child(bodyRoot, VisualParts.TRAVEL_PACK);
                if (pack != null) {
                    pack.xRot = -limbSwingAmount * 0.05F + Mth.sin(age * 0.047F) * 0.015F;
                }
                draw(bodyRoot, pose, out.props(), light, tint, VisualParts.TRAVEL_PACK);
            }
            case GORGET -> draw(bodyRoot, pose, out.props(), light, tint, VisualParts.GORGET);
            case SASH -> {
                // the loose end swings across the hip with the stride
                ModelPart sash = child(bodyRoot, VisualParts.SASH);
                if (sash != null) {
                    sash.zRot = Mth.cos(limbSwing * 0.33F) * limbSwingAmount * 0.09F
                        + Mth.sin(age * 0.066F) * 0.02F;
                }
                draw(bodyRoot, pose, out.props(), light, tint, VisualParts.SASH);
            }
            case VANES -> {
                // vanes open when their owner runs or falls, and fold when they stand still
                float open = Mth.clamp(limbSwingAmount * 1.4F
                    + (player.getDeltaMovement().y < -0.2 ? 0.5F : 0.0F), 0.0F, 1.0F);
                float breathe = Mth.sin(age * 0.076F) * 0.04F;
                ModelPart right = child(bodyRoot, VisualParts.VANE_RIGHT);
                ModelPart left = child(bodyRoot, VisualParts.VANE_LEFT);
                if (right != null) {
                    right.zRot = -0.30F - open * 0.45F - breathe;
                    right.xRot = -0.22F - open * 0.30F;
                }
                if (left != null) {
                    left.zRot = 0.30F + open * 0.45F + breathe;
                    left.xRot = -0.22F - open * 0.30F;
                }
                draw(bodyRoot, pose, out.props(), light, tint,
                    VisualParts.VANE_RIGHT, VisualParts.VANE_LEFT);
            }
            case MANE -> {
                String shape = switch (variant) {
                    case 1 -> VisualParts.MANE_COLLAR;
                    case 2 -> VisualParts.MANE_FALL;
                    default -> VisualParts.MANE;
                };
                // fur lifts with the stride and settles when standing
                ModelPart mane = child(bodyRoot, shape);
                if (mane != null) {
                    mane.xRot = -limbSwingAmount * 0.10F + Mth.sin(age * 0.031F) * 0.02F;
                }
                draw(bodyRoot, pose, out.props(), light, tint, shape);
            }
            case TORC -> {
                // metal does not flex: it only swings a hair on the collarbone
                ModelPart torc = child(bodyRoot, VisualParts.TORC);
                if (torc != null) torc.zRot = Mth.sin(age * 0.039F) * 0.02F * (1.0F + limbSwingAmount);
                draw(bodyRoot, pose, out.props(), light, tint, VisualParts.TORC);
            }
            case ROOTS -> {
                // roots drag: they lag a long way behind the step and recoil afterwards
                float drag = -Mth.cos(limbSwing * 0.5F) * 0.34F * limbSwingAmount;
                float creep = Mth.sin(age * 0.027F) * 0.05F;
                setRot(bodyRoot, VisualParts.ROOT_RIGHT, 0.10F + drag + creep, 0.0F);
                setRot(bodyRoot, VisualParts.ROOT_LEFT, 0.10F + drag - creep, 0.0F);
                setRot(bodyRoot, VisualParts.ROOT_BACK, 0.22F + drag * 1.3F + creep, 0.0F);
                draw(bodyRoot, pose, out.props(), light, tint,
                    VisualParts.ROOT_LEFT, VisualParts.ROOT_RIGHT, VisualParts.ROOT_BACK);
            }
            case MOTES -> {
                drift(pose, out.glow(), LightTexture.FULL_BRIGHT, dim(glowTint, pulse), age,
                    bodyRoot, new String[] {VisualParts.MOTE_A, VisualParts.MOTE_B,
                                            VisualParts.MOTE_C}, 7.0F, 6.0F);
            }
            case FROST_SHARDS -> {
                ring(pose, out.props(), light, tint, age, bodyRoot,
                    new String[] {VisualParts.FROST_SHARD_A, VisualParts.FROST_SHARD_B,
                                  VisualParts.FROST_SHARD_C}, 8.5F, 3.0F);
            }
            case DEMON_TAIL -> {
                // light and whippy: a wider swing than fur or scale, and it keeps moving
                // when the body is still
                float base = (player.isCrouching() ? -0.88F : -0.34F) + switch (variant) {
                    case 1 -> -0.46F;
                    case 2 -> 0.52F;       // curled up behind the shoulder
                    default -> 0.0F;
                };
                flickTail(VisualParts.DEMON_TAIL, VisualParts.DEMON_TAIL_MID,
                    VisualParts.DEMON_TAIL_TIP, base, limbSwingAmount, limbSwing, age);
                detail(pose, out.props(), light, tint, VisualParts.DEMON_TAIL);
            }
            case SERPENT_TAIL -> {
                // heavy and slow: the wave travels further down before it shows
                float base = player.isCrouching() ? -0.70F : -0.30F;
                slitherTail(VisualParts.SERPENT_TAIL, VisualParts.SERPENT_TAIL_MID,
                    VisualParts.SERPENT_TAIL_TIP, base, limbSwingAmount, age);
                detail(pose, out.props(), light, tint, VisualParts.SERPENT_TAIL);
            }
            case DRAGON_TAIL -> {
                float base = player.isCrouching() ? -0.76F : -0.38F;
                swingTail(VisualParts.DRAGON_TAIL, VisualParts.DRAGON_TAIL_MID,
                    VisualParts.DRAGON_TAIL_TIP, base, limbSwing, limbSwingAmount, age);
                detail(pose, out.props(), light, tint, VisualParts.DRAGON_TAIL);
            }
            default -> { }
        }
    }

    /**
     * Spreads parts evenly around a ring and turns them. Each one bobs on its own phase, so
     * the ring reads as a few separate things circling rather than one spinning object.
     *
     * @param speed  radians per tick; negative turns the other way
     * @param radius how far out from the centre, in model pixels
     * @param bob    how far each part rises and falls
     * @param height where the ring sits relative to the anchor
     */
    /**
     * Blazing: fire stands where it stands and climbs. The tongues keep their places round
     * the head and rise and fall out of step with each other - fire does not circle.
     */
    private void flames(PoseStack pose, VertexConsumer buffer, int light, int colour, float age,
                        ModelPart anchor, String[] names, float radius, float height) {
        for (int i = 0; i < names.length; i++) {
            ModelPart part = child(anchor, names[i]);
            if (part == null) continue;
            float angle = (float) (Math.PI * 2.0 / names.length) * i;
            float own = age * (0.17F + 0.025F * i) + i * 2.1F;
            part.x = Mth.cos(angle) * radius;
            part.z = Mth.sin(angle) * radius;
            // +Y points down in model space, so climbing means going negative
            part.y = height - Math.abs(Mth.sin(own)) * 1.8F;
            part.yRot = -angle + Mth.sin(own * 0.6F) * 0.22F;
            part.xRot = Mth.sin(own * 0.9F) * 0.10F;
        }
        draw(anchor, pose, buffer, light, colour, names);
    }

    /**
     * Aurelite: dust in a sunbeam. Each mote keeps its own lazy ellipse at its own speed,
     * so they drift apart and together again instead of marching in a ring.
     */
    private void drift(PoseStack pose, VertexConsumer buffer, int light, int colour, float age,
                       ModelPart anchor, String[] names, float radius, float height) {
        for (int i = 0; i < names.length; i++) {
            ModelPart part = child(anchor, names[i]);
            if (part == null) continue;
            float own = age * (0.028F + 0.011F * i) + i * 2.4F;
            part.x = Mth.cos(own) * (radius - i * 0.9F);
            part.z = Mth.sin(own * 0.78F + i) * (radius * 0.72F);
            part.y = height + Mth.sin(age * 0.05F + i * 1.9F) * 2.4F;
            part.yRot = -own;
        }
        draw(anchor, pose, buffer, light, colour, names);
    }

    /**
     * Hydroletilus: ice is solid. The shards hold one rigid formation that turns slowly as a
     * whole and rises and settles with it - nothing here drifts on its own.
     */
    private void ring(PoseStack pose, VertexConsumer buffer, int light, int colour, float age,
                      ModelPart anchor, String[] names, float radius, float height) {
        float turn = age * 0.013F;
        float rise = Mth.sin(turn * 2.6F) * 0.9F;
        for (int i = 0; i < names.length; i++) {
            ModelPart part = child(anchor, names[i]);
            if (part == null) continue;
            float angle = turn + (float) (Math.PI * 2.0 / names.length) * i;
            part.x = Mth.cos(angle) * radius;
            part.z = Mth.sin(angle) * radius;
            part.y = height + rise;
            part.yRot = -angle;
            part.xRot = 0.16F;                 // every shard leans outward, and stays leaning
        }
        draw(anchor, pose, buffer, light, colour, names);
    }

    /**
     * The three segments of a tail: root, middle, tip. Each origin's tail is posed by its
     * own method below - a wolf, a devil, a serpent and a dragon do not move alike, and one
     * shared wave with different numbers made all four read as the same animal.
     */
    private ModelPart[] chain(String rootName, String midName, String tipName) {
        ModelPart root = child(detailRoot, rootName);
        if (root == null) return null;
        ModelPart mid = root.hasChild(midName) ? root.getChild(midName) : null;
        ModelPart tip = mid != null && mid.hasChild(tipName) ? mid.getChild(tipName) : null;
        return new ModelPart[] {root, mid, tip};
    }

    /**
     * Werewolf: a wag. It runs on its own clock rather than on the stride, so the tail
     * moves while its owner stands still, and it goes faster and wider the harder they run.
     */
    private void wagTail(String rootName, String midName, String tipName, float base,
                         float limbSwingAmount, float age, float width) {
        ModelPart[] parts = chain(rootName, midName, tipName);
        if (parts == null) return;
        float beat = age * (0.10F + limbSwingAmount * 0.42F);
        float swing = 0.10F + limbSwingAmount * width;
        parts[0].yRot = Mth.sin(beat) * swing;
        // the faster the stride the higher the tail is carried
        parts[0].xRot = base - limbSwingAmount * 0.22F;
        if (parts[1] != null) {
            parts[1].yRot = Mth.sin(beat - 0.55F) * swing * 0.85F;
            parts[1].xRot = -0.05F;
        }
        if (parts[2] != null) {
            parts[2].yRot = Mth.sin(beat - 1.10F) * swing * 0.70F;
            parts[2].xRot = 0.12F + Mth.sin(beat * 0.5F) * 0.05F;
        }
    }

    /**
     * Tiefling: a whip. It hangs almost still, then snaps once - and the snap travels from
     * the root to the tip rather than turning the whole tail at once.
     */
    private void flickTail(String rootName, String midName, String tipName, float base,
                           float limbSwingAmount, float limbSwing, float age) {
        ModelPart[] parts = chain(rootName, midName, tipName);
        if (parts == null) return;
        float idle = Mth.sin(age * 0.045F);
        float walk = Mth.cos(limbSwing * 0.4F) * limbSwingAmount;
        parts[0].yRot = idle * 0.09F + walk * 0.10F + flick(age, 23.0F) * 0.45F;
        parts[0].xRot = base;
        if (parts[1] != null) {
            parts[1].yRot = idle * 0.13F + walk * 0.12F + flick(age, 21.0F) * 0.70F;
            parts[1].xRot = -0.09F;
        }
        if (parts[2] != null) {
            parts[2].yRot = idle * 0.17F + walk * 0.14F + flick(age, 19.0F) * 0.95F;
            parts[2].xRot = 0.20F + flick(age, 19.0F) * 0.35F;
        }
    }

    /**
     * Serpentfolk: a slither. The wave never stops and it grows as it runs down the tail,
     * and the body rolls a little as each bend passes through it.
     */
    private void slitherTail(String rootName, String midName, String tipName, float base,
                             float limbSwingAmount, float age) {
        ModelPart[] parts = chain(rootName, midName, tipName);
        if (parts == null) return;
        float beat = age * (0.07F + limbSwingAmount * 0.10F);
        parts[0].yRot = Mth.sin(beat) * (0.16F + limbSwingAmount * 0.14F);
        parts[0].xRot = base;
        parts[0].zRot = Mth.cos(beat) * 0.05F;
        if (parts[1] != null) {
            parts[1].yRot = Mth.sin(beat - 0.9F) * (0.22F + limbSwingAmount * 0.18F);
            parts[1].xRot = -0.04F;
            parts[1].zRot = Mth.cos(beat - 0.9F) * 0.06F;
        }
        if (parts[2] != null) {
            parts[2].yRot = Mth.sin(beat - 1.8F) * (0.28F + limbSwingAmount * 0.22F);
            parts[2].xRot = 0.07F;
            parts[2].zRot = Mth.cos(beat - 1.8F) * 0.07F;
        }
    }

    /**
     * Dragonborn: weight. The tail answers the stride late, heaves as the body does, and
     * the tip is always a step behind the rest of it.
     */
    private void swingTail(String rootName, String midName, String tipName, float base,
                           float limbSwing, float limbSwingAmount, float age) {
        ModelPart[] parts = chain(rootName, midName, tipName);
        if (parts == null) return;
        float beat = limbSwing * 0.26F;
        float heave = Mth.sin(age * 0.035F);
        parts[0].yRot = Mth.cos(beat) * 0.20F * limbSwingAmount + heave * 0.04F;
        parts[0].xRot = base + limbSwingAmount * 0.09F;
        if (parts[1] != null) {
            parts[1].yRot = Mth.cos(beat - 1.1F) * 0.19F * limbSwingAmount;
            parts[1].xRot = -0.05F + heave * 0.03F;
        }
        if (parts[2] != null) {
            parts[2].yRot = Mth.cos(beat - 2.2F) * 0.24F * limbSwingAmount;
            parts[2].xRot = 0.10F + Mth.sin(age * 0.04F + 1.0F) * 0.05F;
        }
    }

    private static void setYaw(ModelPart root, String name, float yRot) {
        ModelPart part = child(root, name);
        if (part != null) part.yRot = yRot;
    }

    private static void setRot(ModelPart root, String name, float xRot, float zRot) {
        ModelPart part = child(root, name);
        if (part == null) return;
        part.xRot = xRot;
        part.zRot = zRot;
    }

    /** The model part an arm element is made of, for one side. */
    public static String armPiece(VisualPart part, boolean left) {
        return switch (part) {
            case ARMBAND -> left ? VisualParts.ARMBAND_LEFT : VisualParts.ARMBAND_RIGHT;
            case AMBER_SPURS -> left ? VisualParts.SPIKES_ARM_LEFT : VisualParts.SPIKES_ARM_RIGHT;
            case VAMBRACE -> left ? VisualParts.VAMBRACE_LEFT : VisualParts.VAMBRACE;
            case CLAWS -> left ? VisualParts.CLAW_LEFT : VisualParts.CLAW_RIGHT;
            case STONE_FISTS -> left ? VisualParts.STONE_FIST_LEFT : VisualParts.STONE_FIST_RIGHT;
            default -> left ? VisualParts.ARM_SCALES_LEFT : VisualParts.ARM_SCALES;
        };
    }

    /** Alex-shaped skins carry arms three pixels wide instead of four. */
    public static boolean slimArms(AbstractClientPlayer player) {
        return player.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM;
    }

    private static boolean wearsArmor(AbstractClientPlayer player, EquipmentSlot slot) {
        return player.getItemBySlot(slot).getItem() instanceof ArmorItem;
    }

    private void draw(ModelPart root, PoseStack pose, VertexConsumer consumer, int light, int tint,
                      String... names) {
        int color = 0xFF000000 | (tint & 0xFFFFFF);
        for (String name : names) {
            ModelPart part = child(root, name);
            if (part == null) continue;
            boolean pivoted = pivot(pose, part);
            part.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, color);
            if (pivoted) pose.popPose();
        }
        // the lit detail goes on in a second pass, after every piece has had the first one:
        // asking the buffer source for another type ends the one above
        if (shaping == null || !shaping.accented()) return;
        VertexConsumer lit = out.accent();
        float shine = shapingTrim == null ? 1.0F : shapingTrim.glow();
        int glow = 0xFF000000 | dim(0xFFFFFF, Math.min(1.0F, shine));
        for (String name : names) {
            ModelPart part = child(root, name);
            if (part == null) continue;
            boolean pivoted = pivot(pose, part);
            part.render(pose, lit, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, glow);
            if (pivoted) pose.popPose();
        }
    }

    /**
     * Half the tint saturation. The class tint multiplies the part colour, so a dark tint
     * (the tiefling one is nearly maroon) would turn a horn into a black blob.
     */
    public static int soften(int tint) {
        int r = (255 + ((tint >> 16) & 0xFF)) / 2;
        int g = (255 + ((tint >> 8) & 0xFF)) / 2;
        int b = (255 + (tint & 0xFF)) / 2;
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Resizes one piece about its own joint. Scaling the pose instead would move the piece
     * away from the body - a horn grown that way leaves the skull behind and floats.
     */
    private void setScale(ModelPart root, String name, float scale) {
        ModelPart part = child(root, name);
        if (part == null) return;
        part.xScale = scale;
        part.yScale = scale;
        part.zScale = scale;
    }

    /** How far each player's wings are open, eased so they never snap between poses. */
    private static final java.util.Map<AbstractClientPlayer, Float> WING_OPEN =
        new java.util.WeakHashMap<>();

    private static float wingOpen(AbstractClientPlayer player, boolean flying) {
        float target = flying ? 1.0F : 0.0F;
        float open = WING_OPEN.getOrDefault(player, target);
        open += (target - open) * 0.06F;
        if (Math.abs(target - open) < 0.004F) open = target;
        WING_OPEN.put(player, open);
        return open;
    }

    /** Parts come from data - a missing one must not bring down the whole world render. */
    private static ModelPart child(ModelPart root, String name) {
        return root.hasChild(name) ? root.getChild(name) : null;
    }

    /** Lazy buffer fetch: each type is taken at most once per frame. */
    private static final class Buffers {

        private MultiBufferSource source;
        private AbstractClientPlayer player;

        private void reset(MultiBufferSource source, AbstractClientPlayer player) {
            this.source = source;
            this.player = player;
        }

        // These are deliberately not cached. A buffer source ends one render type when it is
        // asked for another, so a consumer held across a switch is already dead - it throws
        // "Not building!" the moment something draws into it. Asking every time is cheap.
        private VertexConsumer skin() {
            return source.getBuffer(RenderType.entityCutoutNoCull(player.getSkin().texture()));
        }

        private VertexConsumer props() {
            return source.getBuffer(RenderType.entityCutoutNoCull(VisualParts.TEXTURE));
        }

        /**
         * Light of their own - and solid. The lit passes used to be drawn with types that
         * write colour but no depth ({@code eyes}, translucent emissive): the clouds, drawn
         * after the entities, then passed the depth test over those very pixels and were
         * painted on top of them. That is what made an element dim, or wash out to white,
         * the moment a cloud went behind it. A cutout type writes depth like any other part,
         * so nothing later can paint over it; the light is handed in full bright instead.
         */
        private VertexConsumer glow() {
            return source.getBuffer(RenderType.entityCutoutNoCull(VisualParts.TEXTURE));
        }

        /** The sheet that holds only the lit details of a part, drawn the same way. */
        private VertexConsumer accent() {
            return source.getBuffer(RenderType.entityCutoutNoCull(VisualParts.GLOW_TEXTURE));
        }
    }
}
