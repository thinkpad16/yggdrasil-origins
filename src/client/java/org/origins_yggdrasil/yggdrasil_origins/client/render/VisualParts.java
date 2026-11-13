package org.origins_yggdrasil.yggdrasil_origins.client.render;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;

/**
 * Geometry of the small visual parts. All plain boxes in the vanilla spirit:
 * ears and fangs are noticeable, yet never turn the player into another creature.
 */
public final class VisualParts {

    public static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "textures/entity/visual_parts.png");
    /** The same layout, empty but for the pixels that carry their own light. */
    public static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "yggdrasil_origins", "textures/entity/visual_parts_glow.png");

    /** Parts coloured by the player skin (ears) - drawn against the head UV. */
    public static final ModelLayerLocation SKIN_LAYER = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "visual_skin"), "main");

    /** Parts with their own texture (horns, fangs, fins, halo...). */
    public static final ModelLayerLocation PROP_LAYER = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "visual_props"), "main");

    /** Parts on the body: wings, plates, tail. */
    public static final ModelLayerLocation BODY_LAYER = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "visual_body"), "main");

    /** Double-density parts (drawn at 0.5 scale). */
    public static final ModelLayerLocation DETAIL_LAYER = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, "visual_detail"), "main");

    public static final String ELF_EARS_LEFT = "elf_ear_left";
    public static final String ELF_EARS_RIGHT = "elf_ear_right";
    public static final String WOLF_EARS_LEFT = "wolf_ear_left";
    public static final String WOLF_EARS_RIGHT = "wolf_ear_right";
    public static final String TUSK_LEFT = "tusk_left";
    public static final String TUSK_RIGHT = "tusk_right";
    public static final String HORN_LEFT = "horn_left";
    public static final String HORN_RIGHT = "horn_right";
    public static final String HALO = "halo";
    public static final String FIN_LEFT = "fin_left";
    public static final String FIN_RIGHT = "fin_right";
    public static final String ANTENNA_LEFT = "antenna_left";
    public static final String ANTENNA_RIGHT = "antenna_right";
    public static final String RIBS = "ribs";
    public static final String EMBER_A = "ember_a";
    public static final String EMBER_B = "ember_b";
    public static final String EMBER_C = "ember_c";
    public static final String EMBER_D = "ember_d";
    public static final String BEARD = "beard";
    public static final String BROW = "brow";
    public static final String HAIR_TUFT = "hair_tuft";
    public static final String CRESCENT = "crescent";
    public static final String GILL_LEFT = "gill_left";
    public static final String GILL_RIGHT = "gill_right";
    public static final String FRILL = "frill";

    // body parts
    public static final String WING_LEFT = "wing_left";
    public static final String WING_RIGHT = "wing_right";
    public static final String SHARD_LEFT = "shard_left";
    public static final String SHARD_RIGHT = "shard_right";
    public static final String PLATE_LEFT = "plate_left";
    public static final String PLATE_RIGHT = "plate_right";
    public static final String BONE_SPUR = "bone_spur";
    public static final String SCARF = "scarf";
    public static final String WISP_LEFT = "wisp_left";
    public static final String WISP_BACK = "wisp_back";
    public static final String STREAMER_RIGHT = "streamer_right";
    public static final String STREAMER_LEFT = "streamer_left";
    public static final String STREAMER_TIP_RIGHT = "streamer_tip_right";
    public static final String STREAMER_TIP_LEFT = "streamer_tip_left";
    public static final String VAMBRACE = "vambrace";
    public static final String VAMBRACE_LEFT = "vambrace_left";
    public static final String ARM_SCALES_LEFT = "arm_scales_left";
    public static final String WISP_RIGHT = "wisp_right";
    public static final String TENDRIL_LEFT = "tendril_left";
    public static final String TENDRIL_RIGHT = "tendril_right";
    public static final String CARAPACE = "carapace";
    public static final String TAIL = "tail";
    public static final String DRAGON_TAIL = "dragon_tail";
    public static final String TAIL_MID = "tail_mid";
    public static final String TAIL_TIP = "tail_tip";
    public static final String DRAGON_TAIL_MID = "dragon_tail_mid";
    public static final String DRAGON_TAIL_TIP = "dragon_tail_tip";
    public static final String DEMON_TAIL = "demon_tail";
    public static final String DEMON_TAIL_MID = "demon_tail_mid";
    public static final String DEMON_TAIL_TIP = "demon_tail_tip";
    public static final String SERPENT_TAIL = "serpent_tail";
    public static final String SERPENT_TAIL_MID = "serpent_tail_mid";
    public static final String SERPENT_TAIL_TIP = "serpent_tail_tip";
    public static final String ANTLER_LEFT = "antler_left";
    public static final String ANTLER_RIGHT = "antler_right";
    public static final String ROOT_LEFT = "root_left";
    public static final String ROOT_RIGHT = "root_right";
    public static final String ROOT_BACK = "root_back";
    public static final String MOTE_A = "mote_a";
    public static final String MOTE_B = "mote_b";
    public static final String MOTE_C = "mote_c";
    public static final String FLAME_CROWN_A = "flame_crown_a";
    public static final String FLAME_CROWN_B = "flame_crown_b";
    public static final String FLAME_CROWN_C = "flame_crown_c";
    public static final String FLAME_CROWN_D = "flame_crown_d";
    public static final String FROST_SHARD_A = "frost_shard_a";
    public static final String FROST_SHARD_B = "frost_shard_b";
    public static final String FROST_SHARD_C = "frost_shard_c";
    public static final String MANE = "mane";
    public static final String TORC = "torc";
    public static final String CLAW_RIGHT = "claw_right";
    public static final String CLAW_LEFT = "claw_left";
    public static final String CREST = "crest";
    public static final String FORELOCK = "forelock";
    public static final String WREATH = "wreath";
    public static final String HORN_RAM_RIGHT = "horn_ram_right";
    public static final String HORN_RAM_LEFT = "horn_ram_left";
    public static final String HORN_SPIKE_RIGHT = "horn_spike_right";
    public static final String HORN_SPIKE_LEFT = "horn_spike_left";
    public static final String ANTLER_SHORT_RIGHT = "antler_short_right";
    public static final String ANTLER_SHORT_LEFT = "antler_short_left";
    public static final String ANTLER_WIDE_RIGHT = "antler_wide_right";
    public static final String ANTLER_WIDE_LEFT = "antler_wide_left";
    public static final String CREST_FIN = "crest_fin";
    public static final String CREST_TWIN = "crest_twin";
    public static final String MANE_COLLAR = "mane_collar";
    public static final String MANE_FALL = "mane_fall";
    public static final String BEARD_FORK = "beard_fork";
    public static final String BEARD_SHORT = "beard_short";
    public static final String TAIL_SLIM = "tail_slim";
    public static final String TAIL_SLIM_MID = "tail_slim_mid";
    public static final String TAIL_SLIM_TIP = "tail_slim_tip";
    public static final String TAIL_BOB = "tail_bob";
    public static final String TAIL_BOB_MID = "tail_bob_mid";
    public static final String TAIL_BOB_TIP = "tail_bob_tip";
    public static final String STONE_FIST_RIGHT = "stone_fist_right";
    public static final String STONE_FIST_LEFT = "stone_fist_left";
    public static final String IRON_SPINE = "iron_spine";
    public static final String DIADEM = "diadem";
    public static final String VANE_RIGHT = "vane_right";
    public static final String VANE_LEFT = "vane_left";
    public static final String SASH = "sash";
    public static final String GORGET = "gorget";
    public static final String HEADBAND = "headband";
    public static final String ARMBAND_RIGHT = "armband_right";
    public static final String ARMBAND_LEFT = "armband_left";
    public static final String TRAVEL_PACK = "travel_pack";
    public static final String SPIKES_CHEST = "spikes_chest";
    public static final String SPIKES_ARM_RIGHT = "spikes_arm_right";
    public static final String SPIKES_ARM_LEFT = "spikes_arm_left";
    public static final String MARK_A = "mark_a";
    public static final String MARK_B = "mark_b";
    public static final String MARK_C = "mark_c";
    public static final String EARRING_RIGHT = "earring_right";
    public static final String EARRING_LEFT = "earring_left";
    public static final String PAULDRON_RIGHT = "pauldron_right";
    public static final String PAULDRON_LEFT = "pauldron_left";
    public static final String BRAID_BACK = "braid_back";
    public static final String BRAID_PAIR = "braid_pair";
    public static final String BRAID_TOP = "braid_top";
    public static final String WING_HAND_LEFT = "wing_hand_left";
    public static final String WING_HAND_RIGHT = "wing_hand_right";
    public static final String TENDRIL_TIP_LEFT = "tendril_tip_left";
    public static final String TENDRIL_TIP_RIGHT = "tendril_tip_right";
    public static final String ARM_SCALES = "arm_scales";

    private VisualParts() { }

    /** Ears take their UV from the head side face, so the colour matches the player skin. */
    public static LayerDefinition skinMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // elf ear: a base plus a narrowed tip
        root.addOrReplaceChild(ELF_EARS_RIGHT,
            CubeListBuilder.create()
                .texOffs(0, 9).addBox(-1.6F, -2.5F, -0.5F, 1.8F, 3.0F, 1.0F)
                .texOffs(0, 9).addBox(-1.5F, -4.4F, -0.4F, 1.2F, 2.4F, 1.0F, new CubeDeformation(-0.1F)),
            PartPose.offsetAndRotation(-4.1F, -3.6F, 0.0F, 0.0F, 0.0F, -1.22F));
        root.addOrReplaceChild(ELF_EARS_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(0, 9).addBox(-0.2F, -2.5F, -0.5F, 1.8F, 3.0F, 1.0F)
                .texOffs(0, 9).addBox(0.3F, -4.4F, -0.4F, 1.2F, 2.4F, 1.0F, new CubeDeformation(-0.1F)),
            PartPose.offsetAndRotation(4.1F, -3.6F, 0.0F, 0.0F, 0.0F, 1.22F));

        // beast ear: an outer part and a narrower inner one
        root.addOrReplaceChild(WOLF_EARS_RIGHT,
            CubeListBuilder.create()
                .texOffs(2, 9).addBox(-1.5F, -2.5F, -0.5F, 3.0F, 3.0F, 1.0F)
                .texOffs(2, 9).addBox(-0.5F, -1.5F, -0.9F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F)),
            PartPose.offsetAndRotation(-3.4F, -7.4F, 0.0F, 0.0F, 0.0F, -0.25F));
        root.addOrReplaceChild(WOLF_EARS_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(2, 9).addBox(-1.5F, -2.5F, -0.5F, 3.0F, 3.0F, 1.0F)
                .texOffs(2, 9).addBox(-0.5F, -1.5F, -0.9F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F)),
            PartPose.offsetAndRotation(3.4F, -7.4F, 0.0F, 0.0F, 0.0F, 0.25F));

        // brow: a ridge above the eyes with a faint bridge of the nose.
        // UVs at (7,7): a box's front face starts at (u + depth, v + depth), so this lands on
        // the top rows of the head's front (8..16, 8..16) - the player's own forehead and
        // hairline. At (0,0) it landed in the unused corner of the skin, which a downloaded
        // skin fills with black and the default skins leave empty.
        root.addOrReplaceChild(BROW,
            CubeListBuilder.create()
                .texOffs(7, 7).addBox(-4.0F, -1.3F, -0.6F, 8.0F, 1.4F, 1.1F)
                .texOffs(7, 7).addBox(-4.3F, -1.5F, -0.7F, 2.4F, 1.4F, 1.2F, new CubeDeformation(-0.15F))
                .texOffs(7, 7).addBox(1.9F, -1.5F, -0.7F, 2.4F, 1.4F, 1.2F, new CubeDeformation(-0.15F))
                .texOffs(7, 7).addBox(-1.1F, -0.7F, -0.7F, 2.2F, 1.1F, 1.1F, new CubeDeformation(-0.2F)),
            PartPose.offset(0.0F, -4.5F, -4.2F));

        // hair tuft: four strands of different height, painted from the top of the head
        // (8..16, 0..8 on the skin) - the hair the player already has. (0,0) was the unused
        // corner of the skin, see the brow above.
        root.addOrReplaceChild(HAIR_TUFT,
            CubeListBuilder.create()
                .texOffs(8, 0).addBox(-3.0F, -1.5F, -3.0F, 3.0F, 2.0F, 3.0F)
                .texOffs(9, 1).addBox(0.5F, -2.0F, -2.0F, 3.0F, 2.0F, 3.0F)
                .texOffs(10, 2).addBox(-1.5F, -1.5F, 0.5F, 3.0F, 2.0F, 3.0F)
                .texOffs(11, 1).addBox(-1.0F, -2.6F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.2F)),
            PartPose.offset(0.0F, -8.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** The remaining parts use our own 32x32 texture. */
    public static LayerDefinition propMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE;

        // fangs up (orc) and down (serpentfolk), growing from the corners of the mouth
        // tusks grow upward from the lower jaw, slightly in front of the face
        root.addOrReplaceChild(TUSK_RIGHT,
            CubeListBuilder.create()
                .texOffs(17, 0).addBox(-0.65F, -2.2F, -0.85F, 1.3F, 2.2F, 1.45F, none)
                .texOffs(17, 0).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 1.2F, 1.0F, new CubeDeformation(-0.15F)),
            PartPose.offsetAndRotation(-1.8F, -1.2F, -4.4F, 0.0F, 0.0F, -0.12F));
        root.addOrReplaceChild(TUSK_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(17, 0).addBox(-0.65F, -2.2F, -0.85F, 1.3F, 2.2F, 1.45F, none)
                .texOffs(17, 0).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 1.2F, 1.0F, new CubeDeformation(-0.15F)),
            PartPose.offsetAndRotation(1.8F, -1.2F, -4.4F, 0.0F, 0.0F, 0.12F));
        // horns: two segments leaning backwards

        // halo - a thin disc above the head
        // the halo is a ring of four thin bars, not a solid plate

        // fins on the sides of the head

        // antennae
        root.addOrReplaceChild(ANTENNA_RIGHT,
            CubeListBuilder.create()
                .texOffs(24, 0).addBox(-0.7F, -5.2F, -0.7F, 1.4F, 5.2F, 1.4F, none)
                .texOffs(24, 0).addBox(-0.8F, -6.3F, -0.8F, 1.6F, 1.3F, 1.6F, new CubeDeformation(-0.1F))
                .texOffs(24, 0).addBox(-0.75F, -3.0F, -0.75F, 1.5F, 0.8F, 1.5F, new CubeDeformation(-0.2F)),
            PartPose.offsetAndRotation(-2.0F, -8.0F, -1.0F, -0.5F, 0.0F, -0.3F));
        root.addOrReplaceChild(ANTENNA_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(24, 0).addBox(-0.7F, -5.2F, -0.7F, 1.4F, 5.2F, 1.4F, none)
                .texOffs(24, 0).addBox(-0.8F, -6.3F, -0.8F, 1.6F, 1.3F, 1.6F, new CubeDeformation(-0.1F))
                .texOffs(24, 0).addBox(-0.75F, -3.0F, -0.75F, 1.5F, 0.8F, 1.5F, new CubeDeformation(-0.2F)),
            PartPose.offsetAndRotation(2.0F, -8.0F, -1.0F, -0.5F, 0.0F, 0.3F));

        // glowing eyes

        // embers above the head
        root.addOrReplaceChild(EMBER_A,
            CubeListBuilder.create().texOffs(68, 0).addBox(-0.90F, -0.90F, -0.90F, 1.8F, 1.8F, 1.8F, none),
            PartPose.offset(-3.0F, -10.0F, -1.0F));
        root.addOrReplaceChild(EMBER_B,
            CubeListBuilder.create().texOffs(68, 0).addBox(-0.70F, -0.70F, -0.70F, 1.4F, 1.4F, 1.4F, none),
            PartPose.offset(0.5F, -11.5F, 1.0F));
        root.addOrReplaceChild(EMBER_D,
            CubeListBuilder.create().texOffs(68, 0).addBox(-0.60F, -0.60F, -0.60F, 1.2F, 1.2F, 1.2F, none),
            PartPose.offset(-1.0F, -12.0F, 1.5F));
        root.addOrReplaceChild(EMBER_C,
            CubeListBuilder.create().texOffs(68, 0).addBox(-0.80F, -0.80F, -0.80F, 1.6F, 1.6F, 1.6F, none),
            PartPose.offset(3.0F, -10.5F, 0.0F));

        // beard: its own hair tone, standing slightly forward

        // crescent above the head (lunaris)
        root.addOrReplaceChild(CRESCENT,
            CubeListBuilder.create()
                .texOffs(83, 76).addBox(-3.0F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F, none)
                .texOffs(83, 76).addBox(-2.5F, -1.5F, -0.5F, 1.0F, 2.0F, 1.0F, none)
                .texOffs(83, 76).addBox(-1.5F, -2.5F, -0.5F, 3.0F, 1.0F, 1.0F, none)
                .texOffs(83, 76).addBox(1.5F, -1.5F, -0.5F, 1.0F, 2.0F, 1.0F, none)
                .texOffs(83, 76).addBox(2.0F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F, none),
            PartPose.offset(0.0F, -9.5F, 0.0F));

        // gills on the neck
        root.addOrReplaceChild(GILL_RIGHT,
            CubeListBuilder.create()
                .texOffs(50, 0).addBox(-1.0F, -1.5F, -1.5F, 1.0F, 3.0F, 3.0F, none)
                .texOffs(50, 0).addBox(-1.1F, -2.6F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F))
                .texOffs(50, 0).addBox(-1.1F, 1.6F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)),
            PartPose.offset(-4.0F, -1.0F, 0.0F));
        root.addOrReplaceChild(GILL_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(50, 0).addBox(0.0F, -1.5F, -1.5F, 1.0F, 3.0F, 3.0F, none)
                .texOffs(50, 0).addBox(0.1F, -2.6F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F))
                .texOffs(50, 0).addBox(0.1F, 1.6F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)),
            PartPose.offset(4.0F, -1.0F, 0.0F));

        // a frill collar (serpentfolk)
        // antlers: two forked branches, leaning back and out
        root.addOrReplaceChild(ANTLER_RIGHT,
            CubeListBuilder.create()
                .texOffs(54, 98).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(-0.05F))
                .texOffs(54, 98).addBox(-2.4F, -4.4F, -0.4F, 2.0F, 0.8F, 0.8F, new CubeDeformation(-0.1F))
                .texOffs(54, 98).addBox(-3.0F, -6.2F, -0.4F, 0.8F, 2.2F, 0.8F, new CubeDeformation(-0.15F))
                .texOffs(54, 98).addBox(-2.0F, -7.4F, -0.4F, 1.8F, 0.8F, 0.8F, new CubeDeformation(-0.15F)),
            PartPose.offsetAndRotation(-2.6F, -7.4F, -1.0F, -0.22F, 0.0F, -0.26F));
        root.addOrReplaceChild(ANTLER_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(54, 98).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(-0.05F))
                .texOffs(54, 98).addBox(0.4F, -4.4F, -0.4F, 2.0F, 0.8F, 0.8F, new CubeDeformation(-0.1F))
                .texOffs(54, 98).addBox(2.2F, -6.2F, -0.4F, 0.8F, 2.2F, 0.8F, new CubeDeformation(-0.15F))
                .texOffs(54, 98).addBox(0.2F, -7.4F, -0.4F, 1.8F, 0.8F, 0.8F, new CubeDeformation(-0.15F)),
            PartPose.offsetAndRotation(2.6F, -7.4F, -1.0F, -0.22F, 0.0F, 0.26F));

        // short antlers: two prongs, no higher than the hair
        root.addOrReplaceChild(ANTLER_SHORT_RIGHT,
            CubeListBuilder.create()
                .texOffs(54, 98).addBox(-0.5F, -3.4F, -0.5F, 1.0F, 3.4F, 1.0F, new CubeDeformation(-0.05F))
                .texOffs(54, 98).addBox(-1.8F, -4.2F, -0.4F, 1.6F, 0.8F, 0.8F, new CubeDeformation(-0.15F))
                .texOffs(54, 98).addBox(-0.4F, -4.6F, -0.4F, 0.8F, 1.4F, 0.8F, new CubeDeformation(-0.15F)),
            PartPose.offsetAndRotation(-2.8F, -7.2F, -0.6F, -0.16F, 0.0F, -0.40F));
        root.addOrReplaceChild(ANTLER_SHORT_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(54, 98).addBox(-0.5F, -3.4F, -0.5F, 1.0F, 3.4F, 1.0F, new CubeDeformation(-0.05F))
                .texOffs(54, 98).addBox(0.2F, -4.2F, -0.4F, 1.6F, 0.8F, 0.8F, new CubeDeformation(-0.15F))
                .texOffs(54, 98).addBox(-0.4F, -4.6F, -0.4F, 0.8F, 1.4F, 0.8F, new CubeDeformation(-0.15F)),
            PartPose.offsetAndRotation(2.8F, -7.2F, -0.6F, -0.16F, 0.0F, 0.40F));

        // wide antlers: a beam that runs out sideways with three tines standing off it
        root.addOrReplaceChild(ANTLER_WIDE_RIGHT,
            CubeListBuilder.create()
                .texOffs(54, 98).addBox(-4.6F, -1.0F, -0.5F, 4.6F, 1.0F, 1.0F, new CubeDeformation(-0.05F))
                .texOffs(54, 98).addBox(-1.6F, -2.6F, -0.4F, 0.8F, 1.8F, 0.8F, new CubeDeformation(-0.15F))
                .texOffs(54, 98).addBox(-3.0F, -3.0F, -0.4F, 0.8F, 2.2F, 0.8F, new CubeDeformation(-0.15F))
                .texOffs(54, 98).addBox(-4.4F, -2.4F, -0.4F, 0.8F, 1.6F, 0.8F, new CubeDeformation(-0.2F)),
            PartPose.offsetAndRotation(-2.6F, -7.6F, -0.8F, -0.10F, 0.0F, -0.18F));
        root.addOrReplaceChild(ANTLER_WIDE_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(54, 98).addBox(0.0F, -1.0F, -0.5F, 4.6F, 1.0F, 1.0F, new CubeDeformation(-0.05F))
                .texOffs(54, 98).addBox(0.8F, -2.6F, -0.4F, 0.8F, 1.8F, 0.8F, new CubeDeformation(-0.15F))
                .texOffs(54, 98).addBox(2.2F, -3.0F, -0.4F, 0.8F, 2.2F, 0.8F, new CubeDeformation(-0.15F))
                .texOffs(54, 98).addBox(3.6F, -2.4F, -0.4F, 0.8F, 1.6F, 0.8F, new CubeDeformation(-0.2F)),
            PartPose.offsetAndRotation(2.6F, -7.6F, -0.8F, -0.10F, 0.0F, 0.18F));

        // a ring of flame above the head: four tongues that rise out of step
        for (String name : new String[] {FLAME_CROWN_A, FLAME_CROWN_B, FLAME_CROWN_C, FLAME_CROWN_D}) {
            root.addOrReplaceChild(name,
                CubeListBuilder.create()
                    .texOffs(91, 98).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.3F))
                    .texOffs(91, 98).addBox(-0.6F, -4.4F, -0.6F, 1.2F, 1.6F, 1.2F, new CubeDeformation(-0.4F)),
                PartPose.offset(0.0F, -9.0F, 0.0F));
        }

        // braided hair, one shape per variant: down the back, a pair at the sides, a topknot
        root.addOrReplaceChild(BRAID_BACK,
            CubeListBuilder.create()
                .texOffs(59, 131).addBox(-1.4F, -1.0F, 3.4F, 2.8F, 6.0F, 2.4F, new CubeDeformation(-0.1F))
                .texOffs(59, 131).addBox(-1.0F, 4.6F, 3.8F, 2.0F, 1.6F, 1.6F, new CubeDeformation(-0.2F)),
            PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.12F, 0.0F, 0.0F));
        root.addOrReplaceChild(BRAID_PAIR,
            CubeListBuilder.create()
                .texOffs(59, 131).addBox(-4.6F, -2.0F, 1.0F, 2.0F, 5.4F, 2.0F, new CubeDeformation(-0.1F))
                .texOffs(59, 131).addBox(2.6F, -2.0F, 1.0F, 2.0F, 5.4F, 2.0F, new CubeDeformation(-0.1F))
                .texOffs(59, 131).addBox(-4.2F, 3.0F, 1.4F, 1.2F, 1.4F, 1.2F, new CubeDeformation(-0.2F))
                .texOffs(59, 131).addBox(3.0F, 3.0F, 1.4F, 1.2F, 1.4F, 1.2F, new CubeDeformation(-0.2F)),
            PartPose.offset(0.0F, -1.0F, 0.0F));
        root.addOrReplaceChild(BRAID_TOP,
            CubeListBuilder.create()
                .texOffs(59, 131).addBox(-1.4F, -2.4F, -1.4F, 2.8F, 2.4F, 2.8F, new CubeDeformation(-0.15F))
                .texOffs(59, 131).addBox(-1.0F, -4.4F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.25F)),
            PartPose.offset(0.0F, -7.8F, 0.0F));

        // a pearl diadem: a band across the brow with a drop at the middle
        root.addOrReplaceChild(DIADEM,
            CubeListBuilder.create()
                .texOffs(0, 146).addBox(-4.8F, -0.7F, -4.6F, 9.6F, 1.4F, 1.0F, new CubeDeformation(-0.1F))
                .texOffs(0, 146).addBox(-4.8F, -0.7F, -3.8F, 1.2F, 1.4F, 3.0F, new CubeDeformation(-0.15F))
                .texOffs(0, 146).addBox(3.6F, -0.7F, -3.8F, 1.2F, 1.4F, 3.0F, new CubeDeformation(-0.15F))
                .texOffs(0, 146).addBox(-0.8F, -1.0F, -4.9F, 1.6F, 1.8F, 1.0F, new CubeDeformation(-0.2F)),
            PartPose.offset(0.0F, -5.4F, 0.0F));

        // one tall sail instead of separate plates
        root.addOrReplaceChild(CREST_FIN,
            CubeListBuilder.create()
                .texOffs(85, 116).addBox(-0.9F, -5.0F, -3.4F, 1.8F, 5.0F, 7.6F, new CubeDeformation(-0.25F))
                .texOffs(85, 116).addBox(-0.9F, -3.0F, 4.0F, 1.8F, 3.0F, 1.8F, new CubeDeformation(-0.3F)),
            PartPose.offset(0.0F, -7.6F, 0.4F));

        // two low ridges running back over the skull, one either side of the middle
        root.addOrReplaceChild(CREST_TWIN,
            CubeListBuilder.create()
                .texOffs(85, 116).addBox(-2.6F, -2.2F, -3.2F, 1.6F, 2.2F, 7.0F, new CubeDeformation(-0.25F))
                .texOffs(85, 116).addBox(1.0F, -2.2F, -3.2F, 1.6F, 2.2F, 7.0F, new CubeDeformation(-0.25F))
                .texOffs(85, 116).addBox(-2.2F, -1.4F, 3.6F, 1.4F, 1.4F, 1.8F, new CubeDeformation(-0.3F))
                .texOffs(85, 116).addBox(0.8F, -1.4F, 3.6F, 1.4F, 1.4F, 1.8F, new CubeDeformation(-0.3F)),
            PartPose.offset(0.0F, -7.4F, 0.0F));

        // a cloth band with its knot and two tails at the back of the head
        root.addOrReplaceChild(HEADBAND,
            CubeListBuilder.create()
                .texOffs(82, 146).addBox(-4.6F, -0.8F, -4.8F, 9.2F, 1.8F, 0.9F, new CubeDeformation(-0.1F))
                .texOffs(82, 146).addBox(-4.8F, -0.8F, -4.0F, 0.9F, 1.8F, 8.3F, new CubeDeformation(-0.15F))
                .texOffs(82, 146).addBox(3.9F, -0.8F, -4.0F, 0.9F, 1.8F, 8.3F, new CubeDeformation(-0.15F))
                .texOffs(82, 146).addBox(-4.6F, -0.8F, 4.2F, 9.2F, 1.8F, 0.9F, new CubeDeformation(-0.1F))
                .texOffs(82, 146).addBox(-1.2F, 0.2F, 4.0F, 2.4F, 4.2F, 1.0F, new CubeDeformation(-0.2F)),
            PartPose.offset(0.0F, -5.2F, 0.0F));

        // a wreath of leaves and small blooms, resting where a crown would sit
        root.addOrReplaceChild(WREATH,
            CubeListBuilder.create()
                .texOffs(83, 160).addBox(-4.7F, -0.9F, -4.9F, 9.4F, 1.8F, 1.0F, new CubeDeformation(-0.05F))
                .texOffs(83, 160).addBox(-4.7F, -0.9F, 3.9F, 9.4F, 1.8F, 1.0F, new CubeDeformation(-0.05F))
                .texOffs(83, 160).addBox(-4.9F, -0.9F, -3.9F, 1.0F, 1.8F, 7.8F, new CubeDeformation(-0.05F))
                .texOffs(83, 160).addBox(3.9F, -0.9F, -3.9F, 1.0F, 1.8F, 7.8F, new CubeDeformation(-0.05F))
                .texOffs(0, 176).addBox(-3.4F, -1.8F, -5.0F, 1.8F, 1.8F, 1.8F, new CubeDeformation(-0.2F))
                .texOffs(0, 176).addBox(1.6F, -1.8F, -5.0F, 1.8F, 1.8F, 1.8F, new CubeDeformation(-0.2F))
                .texOffs(0, 176).addBox(-5.0F, -1.8F, -0.9F, 1.8F, 1.8F, 1.8F, new CubeDeformation(-0.2F))
                .texOffs(0, 176).addBox(3.2F, -1.8F, -0.9F, 1.8F, 1.8F, 1.8F, new CubeDeformation(-0.2F))
                .texOffs(0, 176).addBox(-0.9F, -1.8F, 4.0F, 1.8F, 1.8F, 1.8F, new CubeDeformation(-0.2F)),
            PartPose.offset(0.0F, -7.4F, 0.0F));

        // the forelock: one long lock from the crown, falling past the ear
        root.addOrReplaceChild(FORELOCK,
            CubeListBuilder.create()
                .texOffs(72, 131).addBox(-1.2F, -1.0F, -1.2F, 2.4F, 1.8F, 2.4F, new CubeDeformation(-0.1F))
                .texOffs(72, 131).addBox(1.0F, -1.0F, -1.0F, 3.0F, 1.4F, 2.0F, new CubeDeformation(-0.15F))
                .texOffs(72, 131).addBox(3.8F, -0.8F, -1.0F, 2.2F, 3.0F, 2.0F, new CubeDeformation(-0.15F))
                .texOffs(72, 131).addBox(4.2F, 1.8F, -0.8F, 1.8F, 3.0F, 1.6F, new CubeDeformation(-0.2F))
                .texOffs(72, 131).addBox(4.4F, 4.6F, -0.6F, 1.6F, 2.6F, 1.4F, new CubeDeformation(-0.25F)),
            PartPose.offsetAndRotation(0.0F, -8.2F, 0.4F, 0.0F, 0.0F, 0.08F));

        // a crest: a line of plates down the middle of the skull, tallest at the front
        root.addOrReplaceChild(CREST,
            CubeListBuilder.create()
                .texOffs(85, 116).addBox(-1.0F, -5.6F, -3.6F, 2.0F, 5.6F, 1.8F, new CubeDeformation(-0.2F))
                .texOffs(85, 116).addBox(-1.0F, -4.8F, -1.6F, 2.0F, 4.8F, 1.8F, new CubeDeformation(-0.25F))
                .texOffs(85, 116).addBox(-1.0F, -3.8F, 0.4F, 2.0F, 3.8F, 1.8F, new CubeDeformation(-0.3F))
                .texOffs(85, 116).addBox(-1.0F, -2.8F, 2.4F, 2.0F, 2.8F, 1.6F, new CubeDeformation(-0.35F)),
            PartPose.offset(0.0F, -7.6F, 0.0F));

        root.addOrReplaceChild(FRILL,
            CubeListBuilder.create()
                .texOffs(59, 0).addBox(-5.0F, -2.0F, 0.0F, 3.0F, 4.0F, 1.0F, none)
                .texOffs(59, 0).addBox(2.0F, -2.0F, 0.0F, 3.0F, 4.0F, 1.0F, none)
                .texOffs(59, 0).addBox(-5.4F, -1.6F, 0.15F, 0.9F, 3.4F, 0.8F, new CubeDeformation(-0.2F))
                .texOffs(59, 0).addBox(4.5F, -1.6F, 0.15F, 0.9F, 3.4F, 0.8F, new CubeDeformation(-0.2F))
                .texOffs(59, 0).addBox(-5.0F, 1.9F, 0.2F, 2.4F, 0.9F, 0.8F, new CubeDeformation(-0.25F))
                .texOffs(59, 0).addBox(2.6F, 1.9F, 0.2F, 2.4F, 0.9F, 0.8F, new CubeDeformation(-0.25F)),
            PartPose.offsetAndRotation(0.0F, -2.0F, 3.0F, 0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 256);
    }

    /** Parts attached to the body and legs. */
    public static LayerDefinition bodyMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE;

        // small wings behind the back (angel)

        // stone shards on the shoulders (geonite)

        // metal plates (ferrine)

        // bone ribs on the chest (undead) - a mark that does not depend on the skin
        root.addOrReplaceChild(RIBS,
            CubeListBuilder.create()
                .texOffs(86, 0).addBox(-4.0F, 1.0F, -0.6F, 8.0F, 1.0F, 1.0F, new CubeDeformation(-0.1F))
                .texOffs(86, 0).addBox(-3.6F, 3.4F, -0.6F, 7.2F, 1.0F, 1.0F, new CubeDeformation(-0.15F))
                .texOffs(86, 0).addBox(-3.0F, 5.8F, -0.6F, 6.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(86, 0).addBox(-0.6F, 0.6F, -0.7F, 1.2F, 7.0F, 1.0F, new CubeDeformation(-0.2F)),
            PartPose.offset(0.0F, 1.0F, -2.25F));

        // a bone spur from the shoulder (undead)
        root.addOrReplaceChild(BONE_SPUR,
            CubeListBuilder.create()
                .texOffs(77, 0).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F, none)
                .texOffs(77, 0).addBox(-0.5F, -5.6F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.15F))
                .texOffs(77, 0).addBox(-0.5F, -2.6F, 1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(77, 0).addBox(-0.5F, -2.2F, -1.5F, 1.0F, 1.8F, 1.0F, new CubeDeformation(-0.25F))
                .texOffs(77, 0).addBox(-1.5F, -3.4F, -0.4F, 1.0F, 1.6F, 0.9F, new CubeDeformation(-0.3F)),
            PartPose.offsetAndRotation(-4.0F, 1.5F, 0.0F, 0.0F, 0.0F, -0.45F));

        // a scarf trailing behind the back (vortan)
        root.addOrReplaceChild(SCARF,
            CubeListBuilder.create()
                .texOffs(0, 38).addBox(-4.0F, 0.0F, -2.6F, 8.0F, 2.0F, 5.0F, none)
                .texOffs(27, 38).addBox(-1.5F, 1.0F, 2.4F, 3.0F, 7.0F, 1.0F, none)
                .texOffs(27, 38).addBox(1.0F, 1.4F, 2.5F, 2.0F, 5.0F, 1.0F, new CubeDeformation(-0.1F))
                .texOffs(0, 38).addBox(-2.0F, -0.4F, 1.8F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.2F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // glowing wind sparks (sylph)
        root.addOrReplaceChild(WISP_RIGHT,
            CubeListBuilder.create().texOffs(87, 57).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, none),
            PartPose.offset(-6.0F, 2.0F, 0.0F));
        root.addOrReplaceChild(WISP_BACK,
            CubeListBuilder.create().texOffs(87, 57).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, none),
            PartPose.offset(0.0F, 6.0F, 4.5F));
        root.addOrReplaceChild(WISP_LEFT,
            CubeListBuilder.create().texOffs(87, 57).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, none),
            PartPose.offset(6.0F, 4.0F, 0.0F));

        // shadow tendrils (mara)
        root.addOrReplaceChild(TENDRIL_RIGHT,
            CubeListBuilder.create()
                .texOffs(36, 38).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 7.0F, 1.4F, none)
                .texOffs(36, 38).addBox(-1.8F, 0.5F, 0.3F, 1.0F, 5.0F, 1.0F, new CubeDeformation(-0.15F)),
            PartPose.offsetAndRotation(-3.0F, 1.0F, 1.8F, 0.25F, 0.0F, 0.2F));
        root.addOrReplaceChild(TENDRIL_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(36, 38).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 7.0F, 1.4F, none)
                .texOffs(36, 38).addBox(0.8F, 0.5F, 0.3F, 1.0F, 5.0F, 1.0F, new CubeDeformation(-0.15F)),
            PartPose.offsetAndRotation(3.0F, 1.0F, 1.8F, 0.25F, 0.0F, -0.2F));

        // a chitin carapace (cantharis)

        // tail (werewolf) - swings in time with the steps
        // dragon tail: scaled, in two segments


        // roots that trail from the hips and drag along the ground
        // a traveller's pack: a roll, a bag and the straps that hold them on
        root.addOrReplaceChild(TRAVEL_PACK,
            CubeListBuilder.create()
                .texOffs(49, 160).addBox(-3.4F, 0.6F, 0.0F, 6.8F, 6.0F, 2.6F, new CubeDeformation(-0.1F))
                .texOffs(49, 160).addBox(-3.0F, -0.6F, 0.2F, 6.0F, 1.6F, 2.0F, new CubeDeformation(-0.2F))
                .texOffs(49, 160).addBox(-3.4F, -0.8F, -4.6F, 1.2F, 3.0F, 5.0F, new CubeDeformation(-0.3F))
                .texOffs(49, 160).addBox(2.2F, -0.8F, -4.6F, 1.2F, 3.0F, 5.0F, new CubeDeformation(-0.3F)),
            PartPose.offset(0.0F, 1.2F, 2.2F));

        // and a smaller cluster through the collarbone
        root.addOrReplaceChild(SPIKES_CHEST,
            CubeListBuilder.create()
                .texOffs(70, 160).addBox(-3.4F, 0.4F, -3.4F, 1.8F, 4.0F, 2.4F, new CubeDeformation(-0.3F))
                .texOffs(70, 160).addBox(1.6F, 0.8F, -3.4F, 1.8F, 3.6F, 2.4F, new CubeDeformation(-0.3F))
                .texOffs(70, 160).addBox(-1.0F, 2.6F, -3.5F, 2.0F, 4.4F, 2.4F, new CubeDeformation(-0.25F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.30F, 0.0F, 0.0F));

        // a row of iron studs down the spine
        root.addOrReplaceChild(IRON_SPINE,
            CubeListBuilder.create()
                .texOffs(104, 131).addBox(-0.9F, -0.4F, 0.0F, 1.8F, 11.0F, 0.8F, new CubeDeformation(-0.1F))
                .texOffs(104, 131).addBox(-1.4F, 0.0F, 0.0F, 2.8F, 3.6F, 1.2F, new CubeDeformation(-0.1F))
                .texOffs(104, 131).addBox(-1.2F, 3.8F, 0.0F, 2.4F, 3.2F, 1.1F, new CubeDeformation(-0.15F))
                .texOffs(104, 131).addBox(-1.0F, 7.2F, 0.0F, 2.0F, 2.8F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(104, 131).addBox(-0.7F, 10.2F, 0.0F, 1.4F, 1.6F, 0.9F, new CubeDeformation(-0.25F)),
            PartPose.offset(0.0F, 1.0F, 1.8F));

        // two vanes off the shoulder blades, set to catch the air
        root.addOrReplaceChild(VANE_RIGHT,
            CubeListBuilder.create()
                .texOffs(25, 146).addBox(-1.4F, -1.0F, 0.0F, 2.8F, 7.0F, 1.4F, new CubeDeformation(-0.15F))
                .texOffs(25, 146).addBox(-1.0F, 5.4F, 0.2F, 2.0F, 2.6F, 1.0F, new CubeDeformation(-0.25F)),
            PartPose.offsetAndRotation(-3.0F, 1.2F, 1.8F, -0.22F, 0.0F, -0.30F));
        root.addOrReplaceChild(VANE_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(25, 146).addBox(-1.4F, -1.0F, 0.0F, 2.8F, 7.0F, 1.4F, new CubeDeformation(-0.15F))
                .texOffs(25, 146).addBox(-1.0F, 5.4F, 0.2F, 2.0F, 2.6F, 1.0F, new CubeDeformation(-0.25F)),
            PartPose.offsetAndRotation(3.0F, 1.2F, 1.8F, -0.22F, 0.0F, 0.30F));

        // a sash round the waist, with one end hanging loose
        root.addOrReplaceChild(SASH,
            CubeListBuilder.create()
                .texOffs(36, 146).addBox(-4.4F, 0.0F, -2.7F, 8.8F, 2.4F, 1.1F, new CubeDeformation(-0.1F))
                .texOffs(36, 146).addBox(-4.4F, 0.0F, 1.6F, 8.8F, 2.4F, 1.1F, new CubeDeformation(-0.1F))
                .texOffs(36, 146).addBox(-4.6F, 0.0F, -2.0F, 1.1F, 2.4F, 4.0F, new CubeDeformation(-0.1F))
                .texOffs(36, 146).addBox(3.5F, 0.0F, -2.0F, 1.1F, 2.4F, 4.0F, new CubeDeformation(-0.1F))
                .texOffs(36, 146).addBox(1.8F, 1.6F, -3.0F, 2.0F, 4.6F, 1.0F, new CubeDeformation(-0.2F)),
            PartPose.offset(0.0F, 7.4F, 0.0F));

        // a gorget over the collarbone, with a drop of amber set in it
        root.addOrReplaceChild(GORGET,
            CubeListBuilder.create()
                .texOffs(63, 146).addBox(-3.0F, -0.6F, -0.9F, 6.0F, 2.2F, 1.4F, new CubeDeformation(-0.1F))
                .texOffs(63, 146).addBox(-4.6F, 0.2F, -0.4F, 2.0F, 1.8F, 2.0F, new CubeDeformation(-0.2F))
                .texOffs(63, 146).addBox(2.6F, 0.2F, -0.4F, 2.0F, 1.8F, 2.0F, new CubeDeformation(-0.2F))
                .texOffs(63, 146).addBox(-1.0F, 0.6F, -1.3F, 2.0F, 1.6F, 0.8F, new CubeDeformation(-0.15F)),
            PartPose.offset(0.0F, 0.6F, -2.0F));

        // a mane: thick fur over the shoulders and down the nape
        root.addOrReplaceChild(MANE,
            CubeListBuilder.create()
                .texOffs(0, 116).addBox(-5.0F, -0.6F, -3.0F, 10.0F, 5.0F, 6.4F, new CubeDeformation(-0.1F))
                .texOffs(0, 116).addBox(-4.0F, 3.6F, -2.6F, 8.0F, 3.0F, 5.6F, new CubeDeformation(-0.25F))
                .texOffs(0, 116).addBox(-2.6F, 5.6F, 1.6F, 5.2F, 4.0F, 1.8F, new CubeDeformation(-0.3F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // pauldrons: a plate riding above each shoulder, with a ridge along the top
        root.addOrReplaceChild(PAULDRON_RIGHT,
            CubeListBuilder.create()
                .texOffs(40, 131).addBox(-1.5F, -1.6F, -2.6F, 3.0F, 2.6F, 5.2F, new CubeDeformation(-0.1F))
                .texOffs(40, 131).addBox(-1.1F, -2.4F, -1.6F, 2.4F, 1.0F, 3.2F, new CubeDeformation(-0.2F)),
            PartPose.offsetAndRotation(-7.1F, 0.6F, 0.0F, 0.0F, 0.0F, -0.08F));
        root.addOrReplaceChild(PAULDRON_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(40, 131).addBox(-1.5F, -1.6F, -2.6F, 3.0F, 2.6F, 5.2F, new CubeDeformation(-0.1F))
                .texOffs(40, 131).addBox(-1.3F, -2.4F, -1.6F, 2.4F, 1.0F, 3.2F, new CubeDeformation(-0.2F)),
            PartPose.offsetAndRotation(7.1F, 0.6F, 0.0F, 0.0F, 0.0F, 0.08F));

        // a standing collar of fur around the neck
        root.addOrReplaceChild(MANE_COLLAR,
            CubeListBuilder.create()
                .texOffs(0, 116).addBox(-4.6F, -3.4F, -3.0F, 9.2F, 4.6F, 6.4F, new CubeDeformation(-0.1F))
                .texOffs(0, 116).addBox(-3.6F, 0.6F, -2.8F, 7.2F, 2.4F, 5.6F, new CubeDeformation(-0.25F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // fur that falls down the back instead of ringing the neck
        root.addOrReplaceChild(MANE_FALL,
            CubeListBuilder.create()
                .texOffs(0, 116).addBox(-4.6F, -0.4F, 0.6F, 9.2F, 4.0F, 3.4F, new CubeDeformation(-0.1F))
                .texOffs(0, 116).addBox(-3.4F, 3.0F, 1.4F, 6.8F, 6.0F, 2.6F, new CubeDeformation(-0.2F))
                .texOffs(0, 116).addBox(-2.2F, 8.4F, 1.8F, 4.4F, 4.0F, 2.0F, new CubeDeformation(-0.3F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // a torc: an open ring sitting on the collarbone
        root.addOrReplaceChild(TORC,
            CubeListBuilder.create()
                .texOffs(37, 116).addBox(-4.4F, 0.0F, -2.6F, 8.8F, 1.6F, 1.2F, new CubeDeformation(-0.2F))
                .texOffs(37, 116).addBox(-4.4F, 0.0F, 1.4F, 8.8F, 1.6F, 1.2F, new CubeDeformation(-0.2F))
                .texOffs(37, 116).addBox(-4.6F, 0.0F, -2.4F, 1.2F, 1.6F, 4.0F, new CubeDeformation(-0.2F))
                .texOffs(37, 116).addBox(3.4F, 0.0F, -2.4F, 1.2F, 1.6F, 4.0F, new CubeDeformation(-0.2F))
                .texOffs(37, 116).addBox(-1.2F, 1.0F, -3.0F, 2.4F, 2.0F, 1.0F, new CubeDeformation(-0.3F)),
            PartPose.offset(0.0F, 0.6F, 0.0F));

        root.addOrReplaceChild(ROOT_RIGHT,
            CubeListBuilder.create()
                .texOffs(69, 98).addBox(-1.2F, 0.0F, -1.2F, 2.4F, 9.0F, 2.4F, new CubeDeformation(-0.3F))
                .texOffs(69, 98).addBox(-0.8F, 8.0F, -2.6F, 1.6F, 1.6F, 3.0F, new CubeDeformation(-0.4F)),
            PartPose.offsetAndRotation(-3.2F, 10.0F, 1.2F, 0.10F, 0.0F, 0.16F));
        root.addOrReplaceChild(ROOT_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(69, 98).addBox(-1.2F, 0.0F, -1.2F, 2.4F, 9.0F, 2.4F, new CubeDeformation(-0.3F))
                .texOffs(69, 98).addBox(-0.8F, 8.0F, -2.6F, 1.6F, 1.6F, 3.0F, new CubeDeformation(-0.4F)),
            PartPose.offsetAndRotation(3.2F, 10.0F, 1.2F, 0.10F, 0.0F, -0.16F));
        root.addOrReplaceChild(ROOT_BACK,
            CubeListBuilder.create()
                .texOffs(69, 98).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(-0.25F))
                .texOffs(69, 98).addBox(-0.7F, 10.0F, -0.7F, 1.4F, 1.4F, 2.6F, new CubeDeformation(-0.35F)),
            PartPose.offsetAndRotation(0.0F, 9.6F, 2.6F, 0.22F, 0.0F, 0.0F));

        // a shroud hanging off the shoulders, with a longer trailing edge

        // three motes that circle the body
        for (String name : new String[] {MOTE_A, MOTE_B, MOTE_C}) {
            root.addOrReplaceChild(name,
                CubeListBuilder.create()
                    .texOffs(82, 98).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.2F)),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        }

        // shards of ice orbiting the shoulders
        for (String name : new String[] {FROST_SHARD_A, FROST_SHARD_B, FROST_SHARD_C}) {
            root.addOrReplaceChild(name,
                CubeListBuilder.create()
                    .texOffs(100, 98).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.25F))
                    .texOffs(100, 98).addBox(-0.6F, -3.2F, -0.6F, 1.2F, 1.6F, 1.2F, new CubeDeformation(-0.4F)),
                PartPose.offset(0.0F, 3.0F, 0.0F));
        }

        return LayerDefinition.create(mesh, 128, 256);
    }


    /**
     * Double-density parts: the boxes are twice as large and shrink by half when drawn.
     * Four times as many texels land on the same part, so the rings on a horn,
     * the barbs of a feather and individual scales become visible.
     */
    public static LayerDefinition detailMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE;

        root.addOrReplaceChild(HORN_RIGHT,
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 6.0F, 4.0F, none)
                .texOffs(0, 0).addBox(-1.6F, -10.8F, -2.8F, 4.0F, 6.0F, 4.0F, new CubeDeformation(-0.5F))
                .texOffs(0, 0).addBox(-1.2F, -14.4F, -4.0F, 3.0F, 5.0F, 3.0F, new CubeDeformation(-0.8F)),
            PartPose.offsetAndRotation(-8.0F, -14.0F, 0.0F, -0.3F, 0.0F, -0.5F));
        root.addOrReplaceChild(HORN_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 6.0F, 4.0F, none)
                .texOffs(0, 0).addBox(-2.4F, -10.8F, -2.8F, 4.0F, 6.0F, 4.0F, new CubeDeformation(-0.5F))
                .texOffs(0, 0).addBox(-1.8F, -14.4F, -4.0F, 3.0F, 5.0F, 3.0F, new CubeDeformation(-0.8F)),
            PartPose.offsetAndRotation(8.0F, -14.0F, 0.0F, -0.3F, 0.0F, 0.5F));
        // a ram's curl: up, back, round and forward again, in four shortening segments
        root.addOrReplaceChild(HORN_RAM_RIGHT,
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, -5.6F, -2.0F, 4.0F, 5.6F, 4.0F, none)
                .texOffs(0, 0).addBox(-1.8F, -9.4F, 0.6F, 3.8F, 5.0F, 3.8F, new CubeDeformation(-0.2F))
                .texOffs(0, 0).addBox(-1.6F, -8.6F, 4.2F, 3.4F, 4.6F, 3.4F, new CubeDeformation(-0.5F))
                .texOffs(0, 0).addBox(-1.4F, -5.4F, 5.8F, 3.0F, 4.0F, 3.0F, new CubeDeformation(-0.8F)),
            PartPose.offsetAndRotation(-8.0F, -13.0F, 0.0F, -0.1F, 0.0F, -0.42F));
        root.addOrReplaceChild(HORN_RAM_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-2.0F, -5.6F, -2.0F, 4.0F, 5.6F, 4.0F, none)
                .texOffs(0, 0).addBox(-2.0F, -9.4F, 0.6F, 3.8F, 5.0F, 3.8F, new CubeDeformation(-0.2F))
                .texOffs(0, 0).addBox(-1.8F, -8.6F, 4.2F, 3.4F, 4.6F, 3.4F, new CubeDeformation(-0.5F))
                .texOffs(0, 0).addBox(-1.6F, -5.4F, 5.8F, 3.0F, 4.0F, 3.0F, new CubeDeformation(-0.8F)),
            PartPose.offsetAndRotation(8.0F, -13.0F, 0.0F, -0.1F, 0.0F, 0.42F));

        // a straight spike, swept back off the brow and tapering to a point
        root.addOrReplaceChild(HORN_SPIKE_RIGHT,
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.7F, -5.2F, -1.7F, 3.4F, 5.2F, 3.4F, new CubeDeformation(-0.05F))
                .texOffs(0, 0).addBox(-1.4F, -9.6F, 0.0F, 2.8F, 5.0F, 2.8F, new CubeDeformation(-0.25F))
                .texOffs(0, 0).addBox(-1.1F, -13.6F, 1.8F, 2.2F, 4.8F, 2.2F, new CubeDeformation(-0.45F)),
            PartPose.offsetAndRotation(-7.4F, -13.6F, -1.0F, -0.72F, 0.0F, -0.30F));
        root.addOrReplaceChild(HORN_SPIKE_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-1.7F, -5.2F, -1.7F, 3.4F, 5.2F, 3.4F, new CubeDeformation(-0.05F))
                .texOffs(0, 0).addBox(-1.4F, -9.6F, 0.0F, 2.8F, 5.0F, 2.8F, new CubeDeformation(-0.25F))
                .texOffs(0, 0).addBox(-1.1F, -13.6F, 1.8F, 2.2F, 4.8F, 2.2F, new CubeDeformation(-0.45F)),
            PartPose.offsetAndRotation(7.4F, -13.6F, -1.0F, -0.72F, 0.0F, 0.30F));

        root.addOrReplaceChild(FIN_RIGHT,
            CubeListBuilder.create()
                .texOffs(33, 0).addBox(-2.0F, -4.0F, 0.0F, 2.0F, 8.0F, 6.0F, none)
                .texOffs(33, 0).addBox(-2.0F, -6.4F, 0.8F, 2.0F, 6.0F, 4.0F, new CubeDeformation(-0.4F))
                .texOffs(33, 0).addBox(-2.0F, 2.8F, 0.8F, 2.0F, 4.0F, 4.0F, new CubeDeformation(-0.5F)),
            PartPose.offsetAndRotation(-8.0F, -6.0F, 0.0F, 0.0F, -0.4F, -0.2F));
        root.addOrReplaceChild(FIN_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(33, 0).addBox(0.0F, -4.0F, 0.0F, 2.0F, 8.0F, 6.0F, none)
                .texOffs(33, 0).addBox(0.0F, -6.4F, 0.8F, 2.0F, 6.0F, 4.0F, new CubeDeformation(-0.4F))
                .texOffs(33, 0).addBox(0.0F, 2.8F, 0.8F, 2.0F, 4.0F, 4.0F, new CubeDeformation(-0.5F)),
            PartPose.offsetAndRotation(8.0F, -6.0F, 0.0F, 0.0F, 0.4F, 0.2F));
        root.addOrReplaceChild(BEARD,
            CubeListBuilder.create()
                .texOffs(0, 15).addBox(-7.0F, 0.0F, -2.0F, 14.0F, 8.0F, 2.0F, none)
                .texOffs(0, 15).addBox(-8.0F, -4.0F, -2.0F, 2.0F, 4.0F, 2.0F, none)
                .texOffs(0, 15).addBox(6.0F, -4.0F, -2.0F, 2.0F, 4.0F, 2.0F, none)
                .texOffs(0, 15).addBox(-5.0F, -2.4F, -2.2F, 10.0F, 2.8F, 2.0F, new CubeDeformation(-0.2F))
                .texOffs(0, 15).addBox(-5.0F, 1.0F, -3.4F, 10.0F, 7.0F, 1.6F, new CubeDeformation(-0.2F))
                .texOffs(0, 15).addBox(-2.6F, 2.4F, -4.4F, 5.2F, 5.6F, 1.4F, new CubeDeformation(-0.3F))
                .texOffs(0, 15).addBox(-5.2F, 7.2F, -1.8F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.3F))
                .texOffs(0, 15).addBox(3.2F, 7.2F, -1.8F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.3F)),
            PartPose.offset(0.0F, -3.0F, -7.2F));
        // a forked beard: the jaw is covered and two braids hang from it
        root.addOrReplaceChild(BEARD_FORK,
            CubeListBuilder.create()
                .texOffs(0, 15).addBox(-6.0F, 0.0F, -2.0F, 12.0F, 4.0F, 2.0F, none)
                .texOffs(0, 15).addBox(-8.0F, -4.0F, -2.0F, 2.0F, 4.0F, 2.0F, none)
                .texOffs(0, 15).addBox(6.0F, -4.0F, -2.0F, 2.0F, 4.0F, 2.0F, none)
                .texOffs(0, 15).addBox(-5.0F, -2.4F, -2.2F, 10.0F, 2.8F, 2.0F, new CubeDeformation(-0.2F))
                .texOffs(0, 15).addBox(-4.8F, 3.4F, -2.8F, 4.2F, 8.0F, 2.0F, new CubeDeformation(-0.2F))
                .texOffs(0, 15).addBox(0.6F, 3.4F, -2.8F, 4.2F, 8.0F, 2.0F, new CubeDeformation(-0.2F))
                .texOffs(0, 15).addBox(-4.4F, 10.8F, -2.6F, 3.4F, 2.2F, 1.8F, new CubeDeformation(-0.35F))
                .texOffs(0, 15).addBox(1.0F, 10.8F, -2.6F, 3.4F, 2.2F, 1.8F, new CubeDeformation(-0.35F)),
            PartPose.offset(0.0F, -3.0F, -7.2F));

        // a trimmed beard, close to the jaw
        root.addOrReplaceChild(BEARD_SHORT,
            CubeListBuilder.create()
                .texOffs(0, 15).addBox(-6.4F, 0.0F, -2.0F, 12.8F, 4.4F, 2.0F, none)
                .texOffs(0, 15).addBox(-8.0F, -4.0F, -2.0F, 2.0F, 4.0F, 2.0F, none)
                .texOffs(0, 15).addBox(6.0F, -4.0F, -2.0F, 2.0F, 4.0F, 2.0F, none)
                .texOffs(0, 15).addBox(-5.0F, -2.4F, -2.2F, 10.0F, 2.4F, 2.0F, new CubeDeformation(-0.2F))
                .texOffs(0, 15).addBox(-3.6F, 3.6F, -2.6F, 7.2F, 3.0F, 1.8F, new CubeDeformation(-0.25F)),
            PartPose.offset(0.0F, -3.0F, -7.2F));

        root.addOrReplaceChild(HALO,
            CubeListBuilder.create()
                .texOffs(50, 76).addBox(-4.0F, 0.0F, -6.4F, 8.0F, 1.0F, 2.0F, none)
                .texOffs(50, 76).addBox(-4.0F, 0.0F, 4.4F, 8.0F, 1.0F, 2.0F, none)
                .texOffs(50, 76).addBox(-6.4F, 0.0F, -4.0F, 2.0F, 1.0F, 8.0F, none)
                .texOffs(50, 76).addBox(4.4F, 0.0F, -4.0F, 2.0F, 1.0F, 8.0F, none)
                .texOffs(50, 76).addBox(-5.8F, 0.0F, -5.8F, 2.8F, 1.0F, 2.8F, new CubeDeformation(-0.1F))
                .texOffs(50, 76).addBox(3.0F, 0.0F, -5.8F, 2.8F, 1.0F, 2.8F, new CubeDeformation(-0.1F))
                .texOffs(50, 76).addBox(-5.8F, 0.0F, 3.0F, 2.8F, 1.0F, 2.8F, new CubeDeformation(-0.1F))
                .texOffs(50, 76).addBox(3.0F, 0.0F, 3.0F, 2.8F, 1.0F, 2.8F, new CubeDeformation(-0.1F)),
            PartPose.offset(0.0F, -19.0F, 0.0F));
        // wing: a shoulder (bone plus covert feathers) and a "hand" with the primaries.
        // Volume comes from the thick leading bone and two feather layers at different depths.
        PartDefinition wingRight = root.addOrReplaceChild(WING_RIGHT,
            CubeListBuilder.create()
                .texOffs(33, 15).addBox(-7.0F, -1.5F, -1.5F, 7.0F, 4.0F, 3.0F, none)
                .texOffs(54, 15).addBox(-7.0F, 1.5F, -0.9F, 7.0F, 13.0F, 2.0F, none)
                .texOffs(54, 15).addBox(-5.5F, 6.0F, 0.9F, 5.0F, 9.0F, 2.0F, new CubeDeformation(-0.15F)),
            PartPose.offsetAndRotation(-3.0F, -2.0F, 4.0F, 0.15F, 0.52F, -0.32F));
        wingRight.addOrReplaceChild(WING_HAND_RIGHT,
            CubeListBuilder.create()
                .texOffs(33, 15).addBox(-7.0F, -1.5F, -1.3F, 7.0F, 3.0F, 3.0F, new CubeDeformation(-0.1F))
                .texOffs(54, 15).addBox(-9.0F, 1.0F, -0.8F, 9.0F, 20.0F, 2.0F, none)
                .texOffs(54, 15).addBox(-7.5F, 13.0F, 0.8F, 7.0F, 13.0F, 2.0F, new CubeDeformation(-0.15F))
                .texOffs(54, 15).addBox(-5.5F, 24.0F, -0.4F, 5.0F, 9.0F, 2.0F, new CubeDeformation(-0.3F)),
            PartPose.offsetAndRotation(-7.0F, 0.0F, 0.0F, 0.0F, 0.62F, 0.0F));
        PartDefinition wingLeft = root.addOrReplaceChild(WING_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(33, 15).addBox(0.0F, -1.5F, -1.5F, 7.0F, 4.0F, 3.0F, none)
                .texOffs(54, 15).addBox(0.0F, 1.5F, -0.9F, 7.0F, 13.0F, 2.0F, none)
                .texOffs(54, 15).addBox(0.5F, 6.0F, 0.9F, 5.0F, 9.0F, 2.0F, new CubeDeformation(-0.15F)),
            PartPose.offsetAndRotation(3.0F, -2.0F, 4.0F, 0.15F, -0.52F, 0.32F));
        wingLeft.addOrReplaceChild(WING_HAND_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(33, 15).addBox(0.0F, -1.5F, -1.3F, 7.0F, 3.0F, 3.0F, new CubeDeformation(-0.1F))
                .texOffs(54, 15).addBox(0.0F, 1.0F, -0.8F, 9.0F, 20.0F, 2.0F, none)
                .texOffs(54, 15).addBox(0.5F, 13.0F, 0.8F, 7.0F, 13.0F, 2.0F, new CubeDeformation(-0.15F))
                .texOffs(54, 15).addBox(0.5F, 24.0F, -0.4F, 5.0F, 9.0F, 2.0F, new CubeDeformation(-0.3F)),
            PartPose.offsetAndRotation(7.0F, 0.0F, 0.0F, 0.0F, -0.62F, 0.0F));
        root.addOrReplaceChild(SHARD_RIGHT,
            CubeListBuilder.create()
                .texOffs(77, 15).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 6.0F, 4.0F, none)
                .texOffs(77, 15).addBox(-1.0F, -9.0F, -3.0F, 2.0F, 4.0F, 2.0F, none)
                .texOffs(77, 15).addBox(-2.8F, -4.4F, 1.2F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.2F)),
            PartPose.offsetAndRotation(-7.6F, 2.0F, 0.0F, 0.1F, 0.0F, -0.3F));
        root.addOrReplaceChild(SHARD_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(77, 15).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 6.0F, 4.0F, none)
                .texOffs(77, 15).addBox(-1.0F, -9.0F, -3.0F, 2.0F, 4.0F, 2.0F, none)
                .texOffs(77, 15).addBox(0.8F, -4.4F, 1.2F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.2F)),
            PartPose.offsetAndRotation(7.6F, 2.0F, 0.0F, 0.1F, 0.0F, 0.3F));
        // the pauldron is curved: a top ridge, a base and a side face
        root.addOrReplaceChild(PLATE_RIGHT,
            CubeListBuilder.create()
                .texOffs(94, 15).addBox(-2.0F, -2.0F, -5.0F, 2.0F, 8.0F, 10.0F, none)
                .texOffs(94, 15).addBox(-1.4F, -3.6F, -4.0F, 2.0F, 2.4F, 8.0F, new CubeDeformation(-0.2F))
                .texOffs(94, 15).addBox(-2.4F, 0.0F, -3.0F, 1.6F, 5.0F, 6.0F, new CubeDeformation(-0.3F))
                .texOffs(94, 15).addBox(-1.8F, 6.0F, -4.0F, 2.0F, 2.0F, 8.0F, new CubeDeformation(-0.3F)),
            PartPose.offset(-15.8F, 2.0F, 0.0F));
        root.addOrReplaceChild(PLATE_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(94, 15).addBox(0.0F, -2.0F, -5.0F, 2.0F, 8.0F, 10.0F, none)
                .texOffs(94, 15).addBox(-0.6F, -3.6F, -4.0F, 2.0F, 2.4F, 8.0F, new CubeDeformation(-0.2F))
                .texOffs(94, 15).addBox(0.8F, 0.0F, -3.0F, 1.6F, 5.0F, 6.0F, new CubeDeformation(-0.3F))
                .texOffs(94, 15).addBox(-0.2F, 6.0F, -4.0F, 2.0F, 2.0F, 8.0F, new CubeDeformation(-0.3F)),
            PartPose.offset(15.8F, 2.0F, 0.0F));
        // the carapace is a dome rather than a plate: the middle juts out, the edges press in
        root.addOrReplaceChild(CARAPACE,
            CubeListBuilder.create()
                .texOffs(45, 38).addBox(-8.0F, 0.0F, 0.0F, 16.0F, 16.0F, 1.6F, none)
                .texOffs(45, 38).addBox(-6.0F, 1.0F, 1.4F, 12.0F, 14.0F, 1.6F, new CubeDeformation(-0.1F))
                .texOffs(45, 38).addBox(-3.6F, 2.4F, 2.8F, 7.2F, 11.0F, 1.6F, new CubeDeformation(-0.2F))
                .texOffs(45, 38).addBox(-7.4F, 3.0F, 0.8F, 14.8F, 1.6F, 1.6F, new CubeDeformation(-0.25F))
                .texOffs(45, 38).addBox(-7.4F, 9.0F, 0.8F, 14.8F, 1.6F, 1.6F, new CubeDeformation(-0.25F)),
            PartPose.offset(0.0F, 2.0F, 3.8F));
        // the tail is a chain of segments: each turns on its own, so the tail bends
        // instead of sticking out like a stick
        PartDefinition tail = root.addOrReplaceChild(TAIL,
            CubeListBuilder.create()
                .texOffs(82, 38).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 11.0F, none),
            PartPose.offsetAndRotation(0.0F, 21.0F, 3.0F, -0.42F, 0.0F, 0.0F));
        PartDefinition tailMid = tail.addOrReplaceChild(TAIL_MID,
            CubeListBuilder.create()
                .texOffs(82, 38).addBox(-2.2F, -2.2F, 0.0F, 4.4F, 4.4F, 9.0F, none),
            PartPose.offsetAndRotation(0.0F, 0.4F, 10.5F, -0.06F, 0.0F, 0.0F));
        tailMid.addOrReplaceChild(TAIL_TIP,
            CubeListBuilder.create()
                .texOffs(82, 38).addBox(-1.6F, -1.6F, 0.0F, 3.2F, 3.2F, 8.0F, none)
                .texOffs(82, 38).addBox(-1.0F, -1.0F, 7.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.2F)),
            PartPose.offsetAndRotation(0.0F, 0.3F, 8.6F, 0.14F, 0.0F, 0.0F));
        // A demon tail: thin as a whip the whole way down, ending in a flat spade. Nothing
        // about it is furred or scaled - it reads as a different animal entirely.
        // a slim tail that ends in a tuft
        PartDefinition slim = root.addOrReplaceChild(TAIL_SLIM,
            CubeListBuilder.create()
                .texOffs(82, 38).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 11.0F, none),
            PartPose.offsetAndRotation(0.0F, 21.0F, 3.0F, -0.42F, 0.0F, 0.0F));
        PartDefinition slimMid = slim.addOrReplaceChild(TAIL_SLIM_MID,
            CubeListBuilder.create()
                .texOffs(82, 38).addBox(-1.6F, -1.6F, 0.0F, 3.2F, 3.2F, 9.0F, none),
            PartPose.offsetAndRotation(0.0F, 0.4F, 10.5F, -0.06F, 0.0F, 0.0F));
        slimMid.addOrReplaceChild(TAIL_SLIM_TIP,
            CubeListBuilder.create()
                .texOffs(82, 38).addBox(-1.2F, -1.2F, 0.0F, 2.4F, 2.4F, 4.0F, none)
                .texOffs(82, 38).addBox(-2.6F, -2.6F, 3.0F, 5.2F, 5.2F, 5.0F, new CubeDeformation(-0.2F)),
            PartPose.offsetAndRotation(0.0F, 0.3F, 8.6F, 0.14F, 0.0F, 0.0F));

        // a short bob, thick at the base and gone in three segments
        PartDefinition bob = root.addOrReplaceChild(TAIL_BOB,
            CubeListBuilder.create()
                .texOffs(82, 38).addBox(-3.4F, -3.4F, 0.0F, 6.8F, 6.8F, 6.0F, none),
            PartPose.offsetAndRotation(0.0F, 21.6F, 3.0F, -0.56F, 0.0F, 0.0F));
        PartDefinition bobMid = bob.addOrReplaceChild(TAIL_BOB_MID,
            CubeListBuilder.create()
                .texOffs(82, 38).addBox(-2.6F, -2.6F, 0.0F, 5.2F, 5.2F, 4.0F, new CubeDeformation(-0.1F)),
            PartPose.offsetAndRotation(0.0F, 0.2F, 5.6F, -0.08F, 0.0F, 0.0F));
        bobMid.addOrReplaceChild(TAIL_BOB_TIP,
            CubeListBuilder.create()
                .texOffs(82, 38).addBox(-1.8F, -1.8F, 0.0F, 3.6F, 3.6F, 3.0F, new CubeDeformation(-0.15F)),
            PartPose.offsetAndRotation(0.0F, 0.2F, 3.6F, 0.16F, 0.0F, 0.0F));

        PartDefinition demonTail = root.addOrReplaceChild(DEMON_TAIL,
            CubeListBuilder.create()
                .texOffs(92, 76).addBox(-1.4F, -1.4F, 0.0F, 2.8F, 2.8F, 11.0F, new CubeDeformation(-0.05F)),
            PartPose.offsetAndRotation(0.0F, 21.0F, 2.6F, -0.30F, 0.0F, 0.0F));
        PartDefinition demonMid = demonTail.addOrReplaceChild(DEMON_TAIL_MID,
            CubeListBuilder.create()
                .texOffs(92, 76).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 10.0F, new CubeDeformation(-0.05F)),
            PartPose.offsetAndRotation(0.0F, 0.2F, 10.6F, -0.04F, 0.0F, 0.0F));
        demonMid.addOrReplaceChild(DEMON_TAIL_TIP,
            CubeListBuilder.create()
                .texOffs(92, 76).addBox(-0.7F, -0.7F, 0.0F, 1.4F, 1.4F, 6.0F, new CubeDeformation(-0.05F))
                .texOffs(0, 98).addBox(-2.0F, -0.5F, 5.0F, 4.0F, 1.0F, 5.0F, new CubeDeformation(-0.1F))
                .texOffs(0, 98).addBox(-1.2F, -0.5F, 9.4F, 2.4F, 1.0F, 2.0F, new CubeDeformation(-0.2F)),
            PartPose.offsetAndRotation(0.0F, 0.2F, 9.7F, 0.10F, 0.0F, 0.0F));

        // A serpent tail: one unbroken muscle, thick at the hips and drawn down to a point,
        // with no crest and no tuft. It is round where the dragon one is ridged.
        PartDefinition serpentTail = root.addOrReplaceChild(SERPENT_TAIL,
            CubeListBuilder.create()
                .texOffs(19, 98).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 12.0F, none),
            PartPose.offsetAndRotation(0.0F, 21.4F, 3.0F, -0.22F, 0.0F, 0.0F));
        PartDefinition serpentMid = serpentTail.addOrReplaceChild(SERPENT_TAIL_MID,
            CubeListBuilder.create()
                .texOffs(19, 98).addBox(-1.9F, -1.9F, 0.0F, 3.8F, 3.8F, 11.0F, new CubeDeformation(-0.05F)),
            PartPose.offsetAndRotation(0.0F, 0.2F, 11.6F, -0.03F, 0.0F, 0.0F));
        serpentMid.addOrReplaceChild(SERPENT_TAIL_TIP,
            CubeListBuilder.create()
                .texOffs(19, 98).addBox(-1.3F, -1.3F, 0.0F, 2.6F, 2.6F, 8.0F, new CubeDeformation(-0.05F))
                .texOffs(19, 98).addBox(-0.8F, -0.8F, 7.6F, 1.6F, 1.6F, 4.0F, new CubeDeformation(-0.15F))
                .texOffs(19, 98).addBox(-0.4F, -0.4F, 11.2F, 0.8F, 0.8F, 2.4F, new CubeDeformation(-0.2F)),
            PartPose.offsetAndRotation(0.0F, 0.15F, 10.6F, 0.08F, 0.0F, 0.0F));

        PartDefinition dragonTail = root.addOrReplaceChild(DRAGON_TAIL,
            CubeListBuilder.create()
                .texOffs(0, 57).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 11.0F, none)
                .texOffs(37, 57).addBox(-0.4F, -4.6F, 1.0F, 0.8F, 2.0F, 9.0F, new CubeDeformation(-0.05F))
                .texOffs(37, 57).addBox(-0.4F, -5.8F, 2.0F, 0.8F, 1.6F, 2.4F, new CubeDeformation(-0.05F))
                .texOffs(37, 57).addBox(-0.4F, -5.6F, 6.4F, 0.8F, 1.4F, 2.2F, new CubeDeformation(-0.05F)),
            PartPose.offsetAndRotation(0.0F, 21.0F, 3.0F, -0.38F, 0.0F, 0.0F));
        PartDefinition dragonMid = dragonTail.addOrReplaceChild(DRAGON_TAIL_MID,
            CubeListBuilder.create()
                .texOffs(58, 57).addBox(-2.2F, -2.2F, 0.0F, 4.4F, 4.4F, 9.0F, none)
                .texOffs(37, 57).addBox(-0.35F, -3.7F, 0.8F, 0.7F, 1.8F, 7.4F, new CubeDeformation(-0.05F))
                .texOffs(37, 57).addBox(-0.35F, -4.7F, 1.8F, 0.7F, 1.3F, 2.0F, new CubeDeformation(-0.05F)),
            PartPose.offsetAndRotation(0.0F, 0.4F, 10.5F, -0.05F, 0.0F, 0.0F));
        dragonMid.addOrReplaceChild(DRAGON_TAIL_TIP,
            CubeListBuilder.create()
                .texOffs(58, 57).addBox(-1.6F, -1.6F, 0.0F, 3.2F, 3.2F, 8.0F, none)
                .texOffs(58, 57).addBox(-0.6F, -1.0F, 7.0F, 1.2F, 2.4F, 3.0F, new CubeDeformation(-0.2F)),
            PartPose.offsetAndRotation(0.0F, 0.3F, 8.6F, 0.12F, 0.0F, 0.0F));
        // wind streamers: a tapering ribbon from each shoulder, carried by the air
        PartDefinition streamerRight = root.addOrReplaceChild(STREAMER_RIGHT,
            CubeListBuilder.create()
                .texOffs(96, 57).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(-0.3F))
                .texOffs(96, 57).addBox(-0.8F, 7.0F, -0.8F, 1.6F, 7.0F, 1.6F, new CubeDeformation(-0.35F)),
            PartPose.offsetAndRotation(-7.0F, -2.0F, 3.0F, 0.22F, 0.0F, -0.30F));
        streamerRight.addOrReplaceChild(STREAMER_TIP_RIGHT,
            CubeListBuilder.create()
                .texOffs(96, 57).addBox(-0.7F, 0.0F, -0.7F, 1.4F, 8.0F, 1.4F, new CubeDeformation(-0.4F))
                .texOffs(96, 57).addBox(-0.5F, 6.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(-0.3F)),
            PartPose.offsetAndRotation(0.0F, 13.0F, 0.0F, 0.18F, 0.0F, 0.0F));
        PartDefinition streamerLeft = root.addOrReplaceChild(STREAMER_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(96, 57).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(-0.3F))
                .texOffs(96, 57).addBox(-0.8F, 7.0F, -0.8F, 1.6F, 7.0F, 1.6F, new CubeDeformation(-0.35F)),
            PartPose.offsetAndRotation(7.0F, -2.0F, 3.0F, 0.22F, 0.0F, 0.30F));
        streamerLeft.addOrReplaceChild(STREAMER_TIP_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(96, 57).addBox(-0.7F, 0.0F, -0.7F, 1.4F, 8.0F, 1.4F, new CubeDeformation(-0.4F))
                .texOffs(96, 57).addBox(-0.5F, 6.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(-0.3F)),
            PartPose.offsetAndRotation(0.0F, 13.0F, 0.0F, 0.18F, 0.0F, 0.0F));
        // iron vambrace: a riveted sleeve over the arm, visible even in full armour
        root.addOrReplaceChild(VAMBRACE,
            CubeListBuilder.create()
                .texOffs(0, 76).addBox(-2.6F, 7.0F, -5.2F, 3.0F, 10.0F, 10.4F, new CubeDeformation(-0.2F))
                .texOffs(0, 76).addBox(-3.0F, 9.0F, -3.0F, 2.0F, 2.4F, 6.0F, new CubeDeformation(-0.25F))
                .texOffs(0, 76).addBox(-3.0F, 13.4F, -3.0F, 2.0F, 2.4F, 6.0F, new CubeDeformation(-0.25F))
                .texOffs(0, 76).addBox(-2.4F, 2.0F, -3.4F, 2.4F, 5.0F, 7.0F, new CubeDeformation(-0.3F)),
            PartPose.offset(-4.6F, 0.0F, 0.0F));
        // talons growing past the knuckles: three per hand, the middle one longest, each
        // leaning a little outward so they read as claws and not as a fork
        // rough stone grown over the forearm and the knuckles
        root.addOrReplaceChild(STONE_FIST_RIGHT,
            CubeListBuilder.create()
                .texOffs(85, 131).addBox(-1.4F, -4.0F, -3.0F, 2.4F, 8.0F, 6.0F, new CubeDeformation(-0.1F))
                .texOffs(85, 131).addBox(-1.9F, 3.0F, -2.0F, 2.4F, 4.0F, 4.0F, new CubeDeformation(-0.2F))
                .texOffs(85, 131).addBox(-1.0F, -6.0F, -1.2F, 1.6F, 3.0F, 2.4F, new CubeDeformation(-0.3F)),
            PartPose.offset(-6.7F, 14.0F, 0.0F));
        root.addOrReplaceChild(STONE_FIST_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(85, 131).addBox(-1.0F, -4.0F, -3.0F, 2.4F, 8.0F, 6.0F, new CubeDeformation(-0.1F))
                .texOffs(85, 131).addBox(-0.5F, 3.0F, -2.0F, 2.4F, 4.0F, 4.0F, new CubeDeformation(-0.2F))
                .texOffs(85, 131).addBox(-0.6F, -6.0F, -1.2F, 1.6F, 3.0F, 2.4F, new CubeDeformation(-0.3F)),
            PartPose.offset(6.7F, 14.0F, 0.0F));

        // war paint, painted on at double density so a stripe can be half a pixel thin:
        // bars across the cheeks, a band over the brow, or three dashes drawn down a cheek
        root.addOrReplaceChild(MARK_A,
            CubeListBuilder.create()
                .texOffs(0, 131).addBox(-6.4F, -0.8F, -0.4F, 3.2F, 1.6F, 1.4F, new CubeDeformation(-0.05F))
                .texOffs(0, 131).addBox(3.2F, -0.8F, -0.4F, 3.2F, 1.6F, 1.4F, new CubeDeformation(-0.05F)),
            PartPose.offset(0.0F, -9.2F, -8.8F));
        root.addOrReplaceChild(MARK_B,
            CubeListBuilder.create()
                .texOffs(0, 131).addBox(-7.0F, -0.8F, -0.4F, 14.0F, 1.6F, 1.4F, new CubeDeformation(-0.05F)),
            PartPose.offset(0.0F, -11.4F, -8.8F));
        root.addOrReplaceChild(MARK_C,
            CubeListBuilder.create()
                .texOffs(0, 131).addBox(-5.0F, -1.4F, -0.4F, 1.6F, 2.4F, 1.4F, new CubeDeformation(-0.05F))
                .texOffs(0, 131).addBox(-2.4F, -0.2F, -0.4F, 1.6F, 2.4F, 1.4F, new CubeDeformation(-0.05F))
                .texOffs(0, 131).addBox(0.2F, 1.0F, -0.4F, 1.6F, 2.4F, 1.4F, new CubeDeformation(-0.05F)),
            PartPose.offset(0.0F, -11.2F, -8.8F));

        // a hanging earring on each side, clear of the skull so it never sinks into it
        root.addOrReplaceChild(EARRING_RIGHT,
            CubeListBuilder.create()
                .texOffs(33, 131).addBox(-0.4F, 0.0F, -0.4F, 1.4F, 3.2F, 0.8F, new CubeDeformation(-0.05F))
                .texOffs(33, 131).addBox(-0.4F, -0.8F, -0.6F, 1.4F, 0.8F, 1.2F, new CubeDeformation(-0.1F)),
            PartPose.offset(-8.9F, -5.2F, -1.6F));
        root.addOrReplaceChild(EARRING_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(33, 131).addBox(-1.0F, 0.0F, -0.4F, 1.4F, 3.2F, 0.8F, new CubeDeformation(-0.05F))
                .texOffs(33, 131).addBox(-1.0F, -0.8F, -0.6F, 1.4F, 0.8F, 1.2F, new CubeDeformation(-0.1F)),
            PartPose.offset(8.9F, -5.2F, -1.6F));

        // the same amber breaking out along the forearms
        root.addOrReplaceChild(SPIKES_ARM_RIGHT,
            CubeListBuilder.create()
                .texOffs(70, 160).addBox(-1.6F, -2.0F, -1.0F, 2.9F, 5.6F, 2.0F, new CubeDeformation(-0.2F))
                .texOffs(70, 160).addBox(-1.2F, 3.0F, -1.0F, 2.2F, 4.4F, 1.8F, new CubeDeformation(-0.35F)),
            PartPose.offsetAndRotation(-6.9F, 9.0F, 0.0F, 0.0F, 0.0F, -0.22F));
        root.addOrReplaceChild(SPIKES_ARM_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(70, 160).addBox(-1.3F, -2.0F, -1.0F, 2.9F, 5.6F, 2.0F, new CubeDeformation(-0.2F))
                .texOffs(70, 160).addBox(-1.0F, 3.0F, -1.0F, 2.2F, 4.4F, 1.8F, new CubeDeformation(-0.35F)),
            PartPose.offsetAndRotation(6.9F, 9.0F, 0.0F, 0.0F, 0.0F, 0.22F));

        // a cloth band round the upper arm
        root.addOrReplaceChild(ARMBAND_RIGHT,
            CubeListBuilder.create()
                .texOffs(0, 160).addBox(-5.6F, -1.3F, -5.6F, 11.2F, 2.6F, 11.2F, new CubeDeformation(-0.05F))
                .texOffs(0, 160).addBox(-6.0F, -0.6F, -1.4F, 1.6F, 1.4F, 2.8F, new CubeDeformation(-0.2F)),
            PartPose.offset(-2.0F, 7.4F, 0.0F));
        root.addOrReplaceChild(ARMBAND_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(0, 160).addBox(-5.6F, -1.3F, -5.6F, 11.2F, 2.6F, 11.2F, new CubeDeformation(-0.05F))
                .texOffs(0, 160).addBox(4.4F, -0.6F, -1.4F, 1.6F, 1.4F, 2.8F, new CubeDeformation(-0.2F)),
            PartPose.offset(2.0F, 7.4F, 0.0F));

        root.addOrReplaceChild(CLAW_RIGHT,
            CubeListBuilder.create()
                .texOffs(72, 116).addBox(-1.6F, 0.0F, -1.4F, 1.2F, 5.0F, 1.2F, new CubeDeformation(-0.25F))
                .texOffs(72, 116).addBox(0.4F, 0.0F, -1.6F, 1.2F, 6.0F, 1.2F, new CubeDeformation(-0.2F))
                .texOffs(72, 116).addBox(2.4F, 0.0F, -1.4F, 1.2F, 4.6F, 1.2F, new CubeDeformation(-0.3F)),
            PartPose.offsetAndRotation(-4.0F, 19.0F, 0.0F, -0.10F, 0.0F, 0.0F));
        root.addOrReplaceChild(CLAW_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(72, 116).addBox(0.4F, 0.0F, -1.4F, 1.2F, 5.0F, 1.2F, new CubeDeformation(-0.25F))
                .texOffs(72, 116).addBox(-1.6F, 0.0F, -1.6F, 1.2F, 6.0F, 1.2F, new CubeDeformation(-0.2F))
                .texOffs(72, 116).addBox(-3.6F, 0.0F, -1.4F, 1.2F, 4.6F, 1.2F, new CubeDeformation(-0.3F)),
            PartPose.offsetAndRotation(4.0F, 19.0F, 0.0F, -0.10F, 0.0F, 0.0F));

        root.addOrReplaceChild(ARM_SCALES,
            CubeListBuilder.create()
                .texOffs(29, 76).addBox(-3.2F, 8.0F, -4.0F, 2.0F, 12.0F, 8.0F, CubeDeformation.NONE)
                .texOffs(29, 76).addBox(-3.2F, 2.0F, -3.0F, 2.0F, 6.0F, 6.0F, CubeDeformation.NONE)
                .texOffs(29, 76).addBox(-3.8F, 10.0F, -1.6F, 2.0F, 4.0F, 4.0F, new CubeDeformation(-0.3F))
                .texOffs(29, 76).addBox(-3.8F, 15.2F, -1.6F, 2.0F, 4.0F, 4.0F, new CubeDeformation(-0.4F)),
            PartPose.offset(-4.0F, 0.0F, 0.0F));

        // Mirrored twins for the other arm. Arm pieces used to be drawn on the right arm only,
        // which read as a lopsided figure rather than as armour.
        root.addOrReplaceChild(VAMBRACE_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(0, 76).addBox(-0.4F, 7.0F, -4.6F, 3.0F, 10.0F, 9.0F, new CubeDeformation(-0.2F))
                .texOffs(0, 76).addBox(1.0F, 9.0F, -3.0F, 2.0F, 2.4F, 6.0F, new CubeDeformation(-0.25F))
                .texOffs(0, 76).addBox(1.0F, 13.4F, -3.0F, 2.0F, 2.4F, 6.0F, new CubeDeformation(-0.25F))
                .texOffs(0, 76).addBox(0.0F, 2.0F, -3.4F, 2.4F, 5.0F, 7.0F, new CubeDeformation(-0.3F)),
            PartPose.offset(4.6F, 0.0F, 0.0F));
        root.addOrReplaceChild(ARM_SCALES_LEFT,
            CubeListBuilder.create().mirror()
                .texOffs(29, 76).addBox(1.2F, 8.0F, -4.0F, 2.0F, 12.0F, 8.0F, CubeDeformation.NONE)
                .texOffs(29, 76).addBox(1.2F, 2.0F, -3.0F, 2.0F, 6.0F, 6.0F, CubeDeformation.NONE)
                .texOffs(29, 76).addBox(1.8F, 10.0F, -1.6F, 2.0F, 4.0F, 4.0F, new CubeDeformation(-0.3F))
                .texOffs(29, 76).addBox(1.8F, 15.2F, -1.6F, 2.0F, 4.0F, 4.0F, new CubeDeformation(-0.4F)),
            PartPose.offset(4.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 256);
    }

}
