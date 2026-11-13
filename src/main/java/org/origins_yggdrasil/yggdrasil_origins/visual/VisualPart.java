package org.origins_yggdrasil.yggdrasil_origins.visual;

import java.util.Locale;

/**
 * Visual parts. Names from JSON are converted into this enum **once**, when the data is read,
 * so that no string comparison happens inside a frame.
 * <p>
 * {@code anchor} is what it attaches to, {@code detail} marks small things invisible from afar
 * (those are skipped at distance), {@code hiddenBy} is the armour piece that covers it.
 */
public enum VisualPart {

    ELF_EARS(Anchor.HEAD, false, Hide.NONE, true, 3),
    WOLF_EARS(Anchor.HEAD, false, Hide.HELMET, true, 3),
    HAIR_TUFT(Anchor.HEAD, false, Hide.HELMET, true, 3),
    BROW(Anchor.HEAD, true, Hide.HELMET, true, 3),
    BEARD(Anchor.HEAD, false, Hide.NONE, false, 3),
    TUSKS(Anchor.HEAD, true, Hide.NONE, false, 3),
    HORNS(Anchor.HEAD, false, Hide.HELMET, false, 3),
    FINS(Anchor.HEAD, false, Hide.NONE, false, 3),
    GILLS(Anchor.HEAD, true, Hide.NONE, false, 3),
    FRILL(Anchor.HEAD, false, Hide.NONE, false, 3),
    ANTENNAE(Anchor.HEAD, false, Hide.HELMET, false, 3),
    HALO(Anchor.HEAD, false, Hide.NONE, false, 3),
    CRESCENT(Anchor.HEAD, false, Hide.NONE, false, 3),
    EMBERS(Anchor.HEAD, true, Hide.NONE, false, 3),

    WINGS(Anchor.BODY, false, Hide.NONE, false, 3),
    SHARDS(Anchor.BODY, false, Hide.NONE, false, 3),
    PLATES(Anchor.BODY, false, Hide.CHEST, false, 3),
    BONE_SPUR(Anchor.BODY, false, Hide.CHEST, false, 3),
    RIBS(Anchor.BODY, false, Hide.CHEST, false, 3),
    SCARF(Anchor.BODY, false, Hide.NONE, false, 3),
    WISPS(Anchor.BODY, true, Hide.NONE, false, 3),
    STREAMERS(Anchor.BODY, false, Hide.NONE, false, 3),
    VAMBRACE(Anchor.ARM, false, Hide.CHEST, false, 3),
    TENDRILS(Anchor.BODY, false, Hide.NONE, false, 3),
    CARAPACE(Anchor.BODY, false, Hide.CHEST, false, 3),
    TAIL(Anchor.BODY, false, Hide.NONE, false, 3),
    DRAGON_TAIL(Anchor.BODY, false, Hide.NONE, false, 3),
    DEMON_TAIL(Anchor.BODY, false, Hide.NONE, false, 3),
    SERPENT_TAIL(Anchor.BODY, false, Hide.NONE, false, 3),
    ANTLERS(Anchor.HEAD, false, Hide.HELMET, false, 3),
    ROOTS(Anchor.BODY, false, Hide.NONE, false, 3),
    MOTES(Anchor.BODY, true, Hide.NONE, false, 3),
    FLAME_CROWN(Anchor.HEAD, true, Hide.HELMET, false, 3),
    FROST_SHARDS(Anchor.BODY, true, Hide.NONE, false, 3),

    MARKINGS(Anchor.HEAD, true, Hide.HELMET, false, 3),
    FORELOCK(Anchor.HEAD, false, Hide.HELMET, false, 3),
    WREATH(Anchor.HEAD, false, Hide.HELMET, false, 3),
    DIADEM(Anchor.HEAD, false, Hide.HELMET, false, 3),
    HEADBAND(Anchor.HEAD, false, Hide.HELMET, false, 3),
    IRON_SPINE(Anchor.BODY, false, Hide.CHEST, false, 3),
    VANES(Anchor.BODY, false, Hide.NONE, false, 3),
    SASH(Anchor.BODY, false, Hide.CHEST, false, 3),
    GORGET(Anchor.BODY, false, Hide.CHEST, false, 3),
    STONE_FISTS(Anchor.ARM, true, Hide.NONE, false, 3),
    ARMBAND(Anchor.ARM, true, Hide.CHEST, false, 3),
    TRAVEL_PACK(Anchor.BODY, false, Hide.NONE, false, 3),
    AMBER_SPIKES(Anchor.BODY, false, Hide.NONE, false, 3),
    AMBER_SPURS(Anchor.ARM, true, Hide.NONE, false, 3),
    EARRING(Anchor.HEAD, true, Hide.HELMET, false, 3),
    BRAID(Anchor.HEAD, false, Hide.HELMET, false, 3),
    PAULDRONS(Anchor.BODY, false, Hide.CHEST, false, 3),
    MANE(Anchor.BODY, false, Hide.CHEST, false, 3),
    TORC(Anchor.BODY, false, Hide.CHEST, false, 3),
    CREST(Anchor.HEAD, false, Hide.HELMET, false, 3),

    ARM_SCALES(Anchor.ARM, true, Hide.CHEST, false, 3),
    CLAWS(Anchor.ARM, true, Hide.NONE, false, 3);

    /**
     * Parts drawn on the emissive buffer: those are the only ones whose shine a player can
     * turn up or down, so the picker asks about it for them and nobody else.
     */
    private static final java.util.EnumSet<VisualPart> GLOWING =
        java.util.EnumSet.of(HALO, CRESCENT, EMBERS, WISPS, MOTES, FLAME_CROWN);

    public boolean glows() {
        return GLOWING.contains(this);
    }

    /**
     * Elements with a lit detail rather than a lit whole: the stone in a gorget, the tips of
     * a crest. They are drawn a second time from a sheet that is empty but for those pixels.
     */
    private static final java.util.EnumSet<VisualPart> ACCENTED =
        java.util.EnumSet.of(GORGET, DIADEM, TORC, IRON_SPINE, CREST, SHARDS, FROST_SHARDS);

    public boolean accented() {
        return ACCENTED.contains(this);
    }

    /**
     * Elements that lie flat against the skin. They may not be shrunk as far as the rest:
     * a plate or a painted stripe that is made small enough simply disappears inside the
     * body it is painted on.
     */
    private static final java.util.EnumSet<VisualPart> HUGGING =
        java.util.EnumSet.of(CARAPACE, FRILL, MARKINGS, SCARF, TORC, RIBS, BROW,
            ARM_SCALES, GILLS, PLATES, VAMBRACE, PAULDRONS, MANE, DIADEM, HEADBAND, SASH,
            GORGET, IRON_SPINE, ARMBAND, WREATH);

    /**
     * Marks that run right round the head or the waist. Making one bigger may only make it
     * thicker: widening it would lift the band off the body it is tied around.
     */
    private static final java.util.EnumSet<VisualPart> ENCIRCLING =
        java.util.EnumSet.of(DIADEM, HEADBAND, TORC, SASH, GORGET);

    public boolean encircles() {
        return ENCIRCLING.contains(this);
    }

    /** The smallest a player may make this part. */
    public float minScale() {
        return HUGGING.contains(this) ? 0.85F : VisualStyle.MIN_SCALE;
    }

    public float maxScale() {
        return VisualStyle.MAX_SCALE;
    }

    /**
     * Elements with no room to spare: they either lie on the skin or already stand as far
     * off it as they can. Nudging one of these the full distance would bury it or leave it
     * hanging in the air, which is what made some of them look badly fitted.
     */
    private static final java.util.EnumSet<VisualPart> TIGHT = tight();

    private static java.util.EnumSet<VisualPart> tight() {
        java.util.EnumSet<VisualPart> set = java.util.EnumSet.copyOf(HUGGING);
        set.add(TUSKS);
        set.add(VANES);
        set.add(TENDRILS);
        set.add(EARRING);
        set.add(AMBER_SPURS);
        set.add(FINS);
        return set;
    }

    /**
     * Marks painted flat onto the skin. They are thinner than anything else, so the nudge
     * that merely seats a horn would push one of these straight through the face.
     */
    private static final java.util.EnumSet<VisualPart> FLUSH =
        java.util.EnumSet.of(MARKINGS);

    /** How far a player may nudge this element, in model pixels. */
    public float maxShift() {
        if (FLUSH.contains(this)) return 0.2F;
        return TIGHT.contains(this) ? 0.4F : VisualStyle.MAX_SHIFT;
    }

    public float maxRoll() {
        return TIGHT.contains(this) ? 0.09F : VisualStyle.Trim.MAX_ROLL;
    }

    public enum Anchor { HEAD, BODY, ARM }

    public enum Hide { NONE, HELMET, CHEST }

    private final Anchor anchor;
    private final boolean detail;
    private final Hide hiddenBy;
    private final boolean skinColored;
    private final int variants;

    VisualPart(Anchor anchor, boolean detail, Hide hiddenBy, boolean skinColored) {
        this(anchor, detail, hiddenBy, skinColored, 1);
    }

    VisualPart(Anchor anchor, boolean detail, Hide hiddenBy, boolean skinColored, int variants) {
        this.anchor = anchor;
        this.detail = detail;
        this.hiddenBy = hiddenBy;
        this.skinColored = skinColored;
        this.variants = Math.max(1, variants);
    }

    public Anchor anchor() {
        return anchor;
    }

    /** A small detail: unreadable at distance anyway, so it is not drawn there. */
    public boolean detail() {
        return detail;
    }

    public Hide hiddenBy() {
        return hiddenBy;
    }

    /** Drawn with the player skin texture rather than our own. */
    public boolean skinColored() {
        return skinColored;
    }

    /** How many shapes a player can pick between for this part. 1 means it is fixed. */
    public int variants() {
        return variants;
    }

    /** Whether a player may recolour this part. Skin-coloured parts follow the skin instead. */
    public boolean recolourable() {
        return !skinColored;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static VisualPart of(String name) {
        if (name == null) return null;
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
